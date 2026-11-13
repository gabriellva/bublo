package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Multa;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.PagamentoRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de multas e pagamentos.
 */
@Service
public class MultaService {

    private final MultaRepository multaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final LeitorRepository leitorRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final NotificacaoRepository notificacaoRepository;

    public MultaService(MultaRepository multaRepository,
                        PagamentoRepository pagamentoRepository,
                        LeitorRepository leitorRepository,
                        EmprestimoRepository emprestimoRepository,
                        NotificacaoRepository notificacaoRepository) {
        this.multaRepository = multaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.leitorRepository = leitorRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional(readOnly = true)
    public Paginacao<MultaDTO> listar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        return multaRepository.buscar(termo, status, pagina, tamanho, ordenacao)
            .mapConteudo(this::toDTO);
    }

    @Transactional(readOnly = true)
    public MultaDTO buscarDTO(Long id) {
        Multa m = multaRepository.buscarPorId(id).orElse(null);
        return m != null ? toDTO(m) : null;
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoPendente(Long leitorId) {
        return multaRepository.saldoPendentePorLeitor(leitorId);
    }

    @Transactional(readOnly = true)
    public boolean leitorBloqueado(Long leitorId) {
        return multaRepository.saldoPendentePorLeitor(leitorId).compareTo(BigDecimal.ZERO) > 0;
    }

    @Transactional(readOnly = true)
    public long totalPendentes() {
        return multaRepository.contarPendentes();
    }

    @Transactional
    public void pagar(Long multaId, BigDecimal valor, String formaPagamento, String adminUsuario) {
        Multa multa = multaRepository.buscarPorId(multaId)
            .orElseThrow(() -> new IllegalStateException("Multa não encontrada"));
        BigDecimal saldo = multa.getSaldoPendente();
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Valor do pagamento deve ser maior que zero");
        }
        if (valor.compareTo(saldo) > 0) {
            throw new IllegalStateException("Valor excede o saldo pendente da multa");
        }

        com.gabriel.bublo.model.Pagamento pagamento = new com.gabriel.bublo.model.Pagamento();
        pagamento.setMultaId(multaId);
        pagamento.setValor(valor);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setResponsavel(adminUsuario);
        pagamentoRepository.salvar(pagamento);

        BigDecimal novoPago = multa.getValorPago() == null ? BigDecimal.ZERO : multa.getValorPago();
        novoPago = novoPago.add(valor);
        multa.setValorPago(novoPago);
        if (novoPago.compareTo(multa.getValor()) >= 0) {
            multa.setStatus("PAGA");
            notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
                null, "MULTA", "INFORMATIVO", "Multa quitada",
                "Multa #" + multaId + " paga integralmente", "MULTA", multaId));
        } else {
            multa.setStatus("PAGA_PARCIAL");
        }
        multaRepository.salvar(multa);
    }

    @Transactional
    public void estornar(Long pagamentoId, String justificativa, String adminUsuario) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalStateException("Justificativa é obrigatória para estorno");
        }
        com.gabriel.bublo.model.Pagamento alvo = pagamentoRepository.buscarPorId(pagamentoId);
        if (alvo == null) {
            throw new IllegalStateException("Pagamento não encontrado");
        }

        Multa multa = multaRepository.buscarPorId(alvo.getMultaId())
            .orElseThrow(() -> new IllegalStateException("Multa associada não encontrada"));

        BigDecimal novoPago = multa.getValorPago() == null ? BigDecimal.ZERO : multa.getValorPago();
        novoPago = novoPago.subtract(alvo.getValor());
        if (novoPago.compareTo(BigDecimal.ZERO) < 0) novoPago = BigDecimal.ZERO;
        multa.setValorPago(novoPago);
        if (novoPago.compareTo(BigDecimal.ZERO) <= 0) {
            multa.setStatus("PENDENTE");
        } else {
            multa.setStatus("PAGA_PARCIAL");
        }
        multaRepository.salvar(multa);

        notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
            null, "MULTA", "ATENCAO", "Estorno de pagamento",
            "Estorno de R$ " + alvo.getValor() + " na multa #" + multa.getId() + ". Justificativa: " + justificativa,
            "MULTA", multa.getId()));
    }

    private MultaDTO toDTO(Multa m) {
        MultaDTO dto = new MultaDTO();
        dto.setId(m.getId());
        dto.setLeitorId(m.getLeitorId());
        dto.setValor(m.getValor());
        dto.setValorPago(m.getValorPago());
        dto.setSaldoPendente(m.getSaldoPendente());
        dto.setStatus(m.getStatus());
        dto.setDataGeracao(m.getDataGeracao());
        dto.setDiasAtraso(m.getDiasAtraso());
        return dto;
    }
}

// DTO local
class MultaDTO {
    private Long id;
    private Long leitorId;
    private java.math.BigDecimal valor;
    private java.math.BigDecimal valorPago;
    private java.math.BigDecimal saldoPendente;
    private String status;
    private java.time.LocalDateTime dataGeracao;
    private Integer diasAtraso;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeitorId() { return leitorId; }
    public void setLeitorId(Long leitorId) { this.leitorId = leitorId; }
    public java.math.BigDecimal getValor() { return valor; }
    public void setValor(java.math.BigDecimal valor) { this.valor = valor; }
    public java.math.BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(java.math.BigDecimal valorPago) { this.valorPago = valorPago; }
    public java.math.BigDecimal getSaldoPendente() { return saldoPendente; }
    public void setSaldoPendente(java.math.BigDecimal saldoPendente) { this.saldoPendente = saldoPendente; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.time.LocalDateTime getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(java.time.LocalDateTime dataGeracao) { this.dataGeracao = dataGeracao; }
    public Integer getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(Integer diasAtraso) { this.diasAtraso = diasAtraso; }
}
