package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.dto.MultaDTO;
import com.gabriel.bublo.model.Multa;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.PagamentoRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.AuditoriaRepository;
import com.gabriel.bublo.model.Pagamento;
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
    private final AuditoriaRepository auditoriaRepository;

    public MultaService(MultaRepository multaRepository,
                        PagamentoRepository pagamentoRepository,
                        LeitorRepository leitorRepository,
                        EmprestimoRepository emprestimoRepository,
                        NotificacaoRepository notificacaoRepository,
                        AuditoriaRepository auditoriaRepository) {
        this.multaRepository = multaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.leitorRepository = leitorRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public Paginacao<MultaDTO> listar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        return multaRepository.buscar(termo, status, pagina, tamanho, ordenacao)
            .mapConteudo(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<MultaDTO> historicoDoLeitor(Long leitorId) {
        return multaRepository.listarPorLeitor(leitorId, 100).stream().map(this::toDTO).toList();
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

    @Transactional(readOnly = true)
    public List<Pagamento> listarPagamentos(Long multaId) {
        return pagamentoRepository.listarPorMulta(multaId);
    }

    @Transactional
    public void pagar(Long multaId, BigDecimal valor, String formaPagamento, String adminUsuario) {
        // Trava a multa: dois pagamentos simultâneos não podem somar acima do valor devido.
        Multa multa = multaRepository.buscarPorIdParaAtualizar(multaId)
            .orElseThrow(() -> new IllegalStateException("Multa não encontrada"));
        if (formaPagamento == null || formaPagamento.isBlank()) {
            throw new IllegalStateException("Informe a forma de pagamento");
        }
        if (formaPagamento.trim().length() > 50) {
            throw new IllegalStateException("A forma de pagamento deve ter no máximo 50 caracteres");
        }
        if ("PAGA".equals(multa.getStatus()) || "CANCELADA".equals(multa.getStatus())) {
            throw new IllegalStateException("Esta multa não aceita novos pagamentos");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Valor do pagamento deve ser maior que zero");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException("Informe o valor com no máximo duas casas decimais");
        }
        valor = valor.setScale(2, java.math.RoundingMode.UNNECESSARY);
        BigDecimal saldo = multa.getSaldoPendente();
        if (valor.compareTo(saldo) > 0) {
            throw new IllegalStateException("Valor excede o saldo pendente da multa");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setMultaId(multaId);
        pagamento.setValor(valor);
        pagamento.setFormaPagamento(formaPagamento.trim());
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
        auditoriaRepository.registrar(adminUsuario, "PAGAMENTO_MULTA", "Multas", "Multa #" + multaId,
            null, "R$ " + valor + " (" + formaPagamento + "); status " + multa.getStatus(), null);
    }

    @Transactional
    public void estornar(Long pagamentoId, String justificativa, String adminUsuario) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalStateException("Justificativa é obrigatória para estorno");
        }
        Pagamento alvo = pagamentoRepository.buscarPorId(pagamentoId);
        if (alvo == null) {
            throw new IllegalStateException("Pagamento não encontrado");
        }

        Multa multa = multaRepository.buscarPorIdParaAtualizar(alvo.getMultaId())
            .orElseThrow(() -> new IllegalStateException("Multa associada não encontrada"));
        // Só um estorno por pagamento: sem a marca, cada chamada subtrairia o valor de novo.
        if (!pagamentoRepository.marcarEstornado(pagamentoId, adminUsuario, justificativa.trim())) {
            throw new IllegalStateException("Este pagamento já foi estornado");
        }

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
        auditoriaRepository.registrar(adminUsuario, "ESTORNO_PAGAMENTO", "Multas", "Pagamento #" + pagamentoId,
            "R$ " + alvo.getValor() + " pago", "multa #" + multa.getId() + " " + multa.getStatus(), justificativa.trim());
    }

    private MultaDTO toDTO(Multa m) {
        MultaDTO dto = new MultaDTO();
        dto.setId(m.getId());
        dto.setLeitorId(m.getLeitorId());
        leitorRepository.buscarPorId(m.getLeitorId()).ifPresent(l -> {
            dto.setLeitorNome(l.getNome());
            dto.setLeitorMatricula(l.getMatricula());
        });
        dto.setValor(m.getValor());
        dto.setValorPago(m.getValorPago());
        dto.setSaldoPendente(m.getSaldoPendente());
        dto.setStatus(m.getStatus());
        dto.setDataGeracao(m.getDataGeracao());
        dto.setDiasAtraso(m.getDiasAtraso());
        return dto;
    }
}
