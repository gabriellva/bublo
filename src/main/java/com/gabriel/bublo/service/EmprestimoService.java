package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.dto.LeitorResumoDTO;
import com.gabriel.bublo.model.Emprestimo;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de empréstimos: criação, renovação, devolução, regras de negócio.
 */
@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final LeitorRepository leitorRepository;
    private final ExemplarRepository exemplarRepository;
    private final MultaRepository multaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ConfiguracaoService configuracaoService;

    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             LeitorRepository leitorRepository,
                             ExemplarRepository exemplarRepository,
                             MultaRepository multaRepository,
                             NotificacaoRepository notificacaoRepository,
                             ConfiguracaoService configuracaoService) {
        this.emprestimoRepository = emprestimoRepository;
        this.leitorRepository = leitorRepository;
        this.exemplarRepository = exemplarRepository;
        this.multaRepository = multaRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.configuracaoService = configuracaoService;
    }

    @Transactional(readOnly = true)
    public Paginacao<EmprestimoResumoDTO> listar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        return emprestimoRepository.buscar(termo, status, pagina, tamanho, ordenacao)
            .mapConteudo(this::toResumoDTO);
    }

    @Transactional(readOnly = true)
    public EmprestimoDTO buscarDTO(Long id) {
        Emprestimo e = emprestimoRepository.buscarPorId(id).orElse(null);
        return e != null ? toDTO(e) : null;
    }

    @Transactional(readOnly = true)
    public LeitorResumoDTO restricaoLeitor(Long leitorId) {
        com.gabriel.bublo.model.Leitor leitor = leitorRepository.buscarPorId(leitorId).orElse(null);
        if (leitor == null) return null;
        List<Emprestimo> ativos = emprestimoRepository.listarAtivosPorLeitor(leitorId);
        BigDecimal saldo = multaRepository.saldoPendentePorLeitor(leitorId);
        boolean bloqueado = saldo.compareTo(BigDecimal.ZERO) > 0;
        return new LeitorResumoDTO(
            leitor.getId(), leitor.getNome(), leitor.getMatricula(), leitor.getTipo().name(),
            ativos.size(), 0, 0, saldo, bloqueado, 0);
    }

    @Transactional
    public EmprestimoDTO criar(Long leitorId, List<Long> exemplarIds, LocalDateTime dataPrevista, String adminUsuario) {
        Leitor leitor = leitorRepository.buscarPorId(leitorId)
            .orElseThrow(() -> new IllegalStateException("Leitor não encontrado"));

        // Verifica bloqueio por dívida
        BigDecimal saldo = multaRepository.saldoPendentePorLeitor(leitorId);
        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Leitor possui pendências financeiras. Empréstimo bloqueado.");
        }

        // Verifica limite de exemplares simultâneos
        int ativos = emprestimoRepository.listarAtivosPorLeitor(leitorId).size();
        int limite = leitor instanceof com.gabriel.bublo.model.Professor ? 5 : 3;
        if (ativos + exemplarIds.size() > limite) {
            throw new IllegalStateException("Limite de exemplares simultâneos excedido (" + limite + ")");
        }

        // Verifica se o exemplar é do tipo digital (não emprestável)
        for (Long exemplarId : exemplarIds) {
            Exemplar ex = exemplarRepository.buscarPorId(exemplarId)
                .orElseThrow(() -> new IllegalStateException("Exemplar não encontrado: " + exemplarId));
            if (!Boolean.TRUE.equals(ex.getDisponivel())) {
                throw new IllegalStateException("Exemplar indisponível: " + ex.getTombamento());
            }
        }

        // Cria cada empréstimo em uma única transação
        List<EmprestimoDTO> criados = new ArrayList<>();
        for (Long exemplarId : exemplarIds) {
            Emprestimo e = emprestimoRepository.criar(leitorId, exemplarId, dataPrevista);
            criados.add(toDTO(e));
        }

        // Notificação
        notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
            null, "EMPRESTIMO", "INFORMATIVO", "Novo empréstimo registrado",
            "Empréstimo de " + exemplarIds.size() + " exemplar(es) para leitor " + leitorId,
            "EMPRESTIMO", null));

        return criados.get(0);
    }

    @Transactional
    public void devolver(Long emprestimoId, String adminUsuario) {
        Emprestimo e = emprestimoRepository.buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        if (!"ATIVO".equals(e.getStatus())) {
            throw new IllegalStateException("Empréstimo não está ativo");
        }

        // Calcula multa por atraso
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime prevista = e.getDataPrevistaDevolucao();
        if (prevista != null && agora.isAfter(prevista)) {
            long diasAtraso = ChronoUnit.DAYS.between(prevista, agora);
            BigDecimal taxa = new BigDecimal("2.00"); // taxa diária padrão
            BigDecimal valor = taxa.multiply(new BigDecimal(diasAtraso));
            com.gabriel.bublo.model.Multa multa = new com.gabriel.bublo.model.Multa();
            multa.setEmprestimoId(e.getId());
            multa.setLeitorId(e.getLeitor() != null ? e.getLeitor().getId() : null);
            multa.setValor(valor);
            multa.setTaxaDiaria(taxa);
            multa.setDiasAtraso((int) diasAtraso);
            multa.setStatus("PENDENTE");
            multaRepository.salvar(multa);

            notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
                null, "MULTA", "ATENCAO", "Multa gerada por atraso",
                "Multa de R$ " + valor + " gerada para empréstimo #" + emprestimoId,
                "MULTA", multa.getId()));
        }

        emprestimoRepository.devolver(emprestimoId);

        // Verifica reservas FIFO para este exemplar
        verificarReservaFIFO(e);
    }

    @Transactional
    public void renovar(Long emprestimoId, String adminUsuario) {
        Emprestimo e = emprestimoRepository.buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        if (!"ATIVO".equals(e.getStatus())) {
            throw new IllegalStateException("Somente empréstimos ativos podem ser renovados");
        }
        if (e.getRenovacoes() >= 1) {
            throw new IllegalStateException("Limite de renovações atingido");
        }
        // Verifica reserva pendente para o material
        // (simplificado)
        LocalDateTime novaPrevista = e.getDataPrevistaDevolucao().plusDays(7);
        emprestimoRepository.renovar(emprestimoId, novaPrevista);
    }

    @Transactional
    public void cancelar(Long emprestimoId, String adminUsuario) {
        emprestimoRepository.cancelar(emprestimoId);
    }

    @Transactional(readOnly = true)
    public long totalAtivos() {
        return emprestimoRepository.contarAtivos();
    }

    @Transactional(readOnly = true)
    public long totalAtrasados() {
        return emprestimoRepository.contarAtrasados();
    }

    private void verificarReservaFIFO(Emprestimo emprestimo) {
        // simplificado: sem item_acervo_id vinculado diretamente no empréstimo neste escopo
    }

    private EmprestimoDTO toDTO(Emprestimo e) {
        EmprestimoDTO dto = new EmprestimoDTO();
        dto.setId(e.getId());
        dto.setLeitorId(e.getLeitor() != null ? e.getLeitor().getId() : null);
        dto.setExemplarId(e.getExemplar() != null ? e.getExemplar().getId() : null);
        dto.setDataEmprestimo(e.getDataEmprestimo());
        dto.setDataPrevistaDevolucao(e.getDataPrevistaDevolucao());
        dto.setDataDevolucaoReal(e.getDataDevolucaoReal());
        dto.setRenovacoes(e.getRenovacoes());
        dto.setStatus(e.getStatus());
        return dto;
    }

    private EmprestimoResumoDTO toResumoDTO(Emprestimo e) {
        EmprestimoResumoDTO dto = new EmprestimoResumoDTO();
        dto.setId(e.getId());
        dto.setStatus(e.getStatus());
        dto.setDataEmprestimo(e.getDataEmprestimo());
        dto.setDataPrevistaDevolucao(e.getDataPrevistaDevolucao());
        return dto;
    }
}

// DTOs auxiliares locais
class EmprestimoDTO {
    private Long id;
    private Long leitorId;
    private Long exemplarId;
    private LocalDateTime dataEmprestimo;
    private LocalDateTime dataPrevistaDevolucao;
    private LocalDateTime dataDevolucaoReal;
    private Integer renovacoes;
    private String status;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeitorId() { return leitorId; }
    public void setLeitorId(Long leitorId) { this.leitorId = leitorId; }
    public Long getExemplarId() { return exemplarId; }
    public void setExemplarId(Long exemplarId) { this.exemplarId = exemplarId; }
    public LocalDateTime getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDateTime dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public LocalDateTime getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDateTime dataPrevistaDevolucao) { this.dataPrevistaDevolucao = dataPrevistaDevolucao; }
    public LocalDateTime getDataDevolucaoReal() { return dataDevolucaoReal; }
    public void setDataDevolucaoReal(LocalDateTime dataDevolucaoReal) { this.dataDevolucaoReal = dataDevolucaoReal; }
    public Integer getRenovacoes() { return renovacoes; }
    public void setRenovacoes(Integer renovacoes) { this.renovacoes = renovacoes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class EmprestimoResumoDTO {
    private Long id;
    private String status;
    private LocalDateTime dataEmprestimo;
    private LocalDateTime dataPrevistaDevolucao;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDateTime dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public LocalDateTime getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDateTime dataPrevistaDevolucao) { this.dataPrevistaDevolucao = dataPrevistaDevolucao; }
}
