package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.dto.EmprestimoDTO;
import com.gabriel.bublo.dto.EmprestimoResumoDTO;
import com.gabriel.bublo.dto.LeitorResumoDTO;
import com.gabriel.bublo.model.Emprestimo;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.Multa;
import com.gabriel.bublo.model.Notificacao;
import com.gabriel.bublo.model.Professor;
import com.gabriel.bublo.model.Reserva;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import com.gabriel.bublo.repository.AuditoriaRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.model.TipoMaterial;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de empréstimos: criação, renovação, devolução, regras de negócio.
 */
@Service
public class EmprestimoService {

    static final int LIMITE_ALUNOS_PADRAO = 3;
    static final int LIMITE_PROFESSORES_PADRAO = 5;
    static final int PRAZO_ALUNOS_PADRAO = 7;
    static final int PRAZO_PROFESSORES_PADRAO = 14;
    static final BigDecimal TAXA_DIARIA_PADRAO = new BigDecimal("2.00");
    static final int MAXIMO_RENOVACOES = 1;

    private final EmprestimoRepository emprestimoRepository;
    private final LeitorRepository leitorRepository;
    private final ExemplarRepository exemplarRepository;
    private final MultaRepository multaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ReservaRepository reservaRepository;
    private final ConfiguracaoService configuracaoService;
    private final ItemAcervoRepository itemAcervoRepository;
    private final AuditoriaRepository auditoriaRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             LeitorRepository leitorRepository,
                             ExemplarRepository exemplarRepository,
                             MultaRepository multaRepository,
                             NotificacaoRepository notificacaoRepository,
                             ReservaRepository reservaRepository,
                             ConfiguracaoService configuracaoService,
                             ItemAcervoRepository itemAcervoRepository,
                             AuditoriaRepository auditoriaRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.leitorRepository = leitorRepository;
        this.exemplarRepository = exemplarRepository;
        this.multaRepository = multaRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.reservaRepository = reservaRepository;
        this.configuracaoService = configuracaoService;
        this.itemAcervoRepository = itemAcervoRepository;
        this.auditoriaRepository = auditoriaRepository;
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
    public List<Object[]> emprestimosPorMes(int meses) {
        return emprestimoRepository.contarPorMes(meses);
    }

    @Transactional(readOnly = true)
    public int prazoAlunosEmDias() {
        return configuracaoService.buscarInteiro("prazo_alunos", PRAZO_ALUNOS_PADRAO);
    }

    @Transactional(readOnly = true)
    public int prazoProfessoresEmDias() {
        return configuracaoService.buscarInteiro("prazo_professores", PRAZO_PROFESSORES_PADRAO);
    }

    private int prazoPadraoEmDias(Leitor leitor) {
        return leitor instanceof Professor ? prazoProfessoresEmDias() : prazoAlunosEmDias();
    }

    @Transactional(readOnly = true)
    public LeitorResumoDTO restricaoLeitor(Long leitorId) {
        Leitor leitor = leitorRepository.buscarPorId(leitorId).orElse(null);
        if (leitor == null) return null;
        List<Emprestimo> ativos = emprestimoRepository.listarAtivosPorLeitor(leitorId);
        BigDecimal saldo = multaRepository.saldoPendentePorLeitor(leitorId);
        boolean bloqueado = saldo.compareTo(BigDecimal.ZERO) > 0;
        LeitorResumoDTO resumo = new LeitorResumoDTO(
            leitor.getId(), leitor.getNome(), leitor.getMatricula(), leitor.getTipo().name(),
            ativos.size(), 0, 0, saldo, bloqueado, 0);
        resumo.setLimiteEmprestimos(limiteDoLeitor(leitor));
        resumo.setPrazoDias(prazoPadraoEmDias(leitor));
        return resumo;
    }

    @Transactional
    public EmprestimoDTO criar(Long leitorId, List<Long> exemplarIds, LocalDateTime dataPrevista, String adminUsuario) {
        if (exemplarIds == null || exemplarIds.isEmpty()) {
            throw new IllegalStateException("Selecione ao menos um exemplar.");
        }
        List<Long> exemplaresUnicos = exemplarIds.stream().distinct().toList();

        Leitor leitor = leitorRepository.buscarPorId(leitorId)
            .orElseThrow(() -> new IllegalStateException("Leitor não encontrado"));
        if (!Boolean.TRUE.equals(leitor.getAtivo())) {
            throw new IllegalStateException("Leitor inativo. Não é possível registrar empréstimos.");
        }
        if (dataPrevista == null || dataPrevista.toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("A data prevista de devolução não pode ser anterior a hoje.");
        }
        int prazoMaximo = prazoPadraoEmDias(leitor);
        if (dataPrevista.toLocalDate().isAfter(LocalDate.now().plusDays(prazoMaximo))) {
            throw new IllegalStateException("A data prevista não pode ultrapassar o prazo de " + prazoMaximo
                + " dia(s) permitido para este tipo de leitor.");
        }

        // Serializa empréstimos simultâneos do mesmo leitor para que o limite não seja ultrapassado.
        leitorRepository.travar(leitorId);

        BigDecimal saldo = multaRepository.saldoPendentePorLeitor(leitorId);
        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Leitor possui pendências financeiras. Empréstimo bloqueado.");
        }

        int ativos = emprestimoRepository.listarAtivosPorLeitor(leitorId).size();
        int limite = limiteDoLeitor(leitor);
        if (ativos + exemplaresUnicos.size() > limite) {
            throw new IllegalStateException("Limite de exemplares simultâneos excedido (" + limite
                + "; o leitor já possui " + ativos + " ativo(s)).");
        }

        // Reservas vencidas não podem mais segurar exemplares da fila.
        for (var vencida : reservaRepository.listarExpiradasNaoProcessadas()) {
            reservaRepository.expirar(vencida.getId());
        }

        Map<Long, Integer> pedidosPorItem = new LinkedHashMap<>();
        for (Long exemplarId : exemplaresUnicos) {
            Exemplar ex = exemplarRepository.buscarPorId(exemplarId)
                .orElseThrow(() -> new IllegalStateException("Exemplar não encontrado: " + exemplarId));
            if (!Boolean.TRUE.equals(ex.getDisponivel())) {
                throw new IllegalStateException("Exemplar indisponível: " + ex.getTombamento());
            }
            Long itemId = ex.getItemAcervoId();
            var item = itemAcervoRepository.buscarPorId(itemId)
                .orElseThrow(() -> new IllegalStateException("Material não encontrado para o exemplar " + ex.getTombamento()));
            if (!itemAcervoRepository.estaAtivo(itemId)) {
                throw new IllegalStateException("O material do exemplar " + ex.getTombamento() + " foi excluído do acervo.");
            }
            if (item.getTipo() == TipoMaterial.DIGITAL) {
                throw new IllegalStateException("Publicações digitais não podem ser emprestadas.");
            }
            pedidosPorItem.merge(itemId, 1, Integer::sum);
        }

        // Fila de reservas: quem está à frente do leitor tem prioridade sobre os exemplares livres.
        Map<Long, Reserva> reservasDoLeitor = new LinkedHashMap<>();
        for (var entrada : pedidosPorItem.entrySet()) {
            Long itemId = entrada.getKey();
            List<Reserva> fila = reservaRepository.listarPendentesPorItem(itemId);
            int aFrente = fila.size();
            for (int i = 0; i < fila.size(); i++) {
                Reserva r = fila.get(i);
                if (r.getLeitor() != null && leitorId.equals(r.getLeitor().getId())) {
                    aFrente = i;
                    reservasDoLeitor.put(itemId, r);
                    break;
                }
            }
            int livres = exemplarRepository.buscarDisponiveisPorItem(itemId).size() - aFrente;
            if (entrada.getValue() > livres) {
                throw new IllegalStateException("Este material está reservado para outros leitores na fila. "
                    + "Só o primeiro da fila pode retirá-lo.");
            }
        }

        List<EmprestimoDTO> criados = new ArrayList<>();
        for (Long exemplarId : exemplaresUnicos) {
            Emprestimo e = emprestimoRepository.criar(leitorId, exemplarId, dataPrevista);
            criados.add(toDTO(e));
        }
        reservasDoLeitor.values().forEach(r -> reservaRepository.atender(r.getId()));

        notificacaoRepository.salvar(new Notificacao(
            null, "EMPRESTIMO", "INFORMATIVO", "Novo empréstimo registrado",
            "Empréstimo de " + exemplaresUnicos.size() + " exemplar(es) para " + leitor.getNome(),
            "EMPRESTIMO", criados.get(0).getId()));
        auditoriaRepository.registrar(adminUsuario, "CRIACAO_EMPRESTIMO", "Emprestimos",
            "Empréstimo #" + criados.get(0).getId(), null,
            leitor.getNome() + ": " + exemplaresUnicos.size() + " exemplar(es), devolução até "
                + dataPrevista.toLocalDate(), null);

        return criados.get(0);
    }

    @Transactional
    public BigDecimal devolver(Long emprestimoId, String adminUsuario) {
        Emprestimo e = emprestimoRepository.buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        if (!"ATIVO".equals(e.getStatus())) {
            throw new IllegalStateException("Empréstimo não está ativo");
        }
        // A baixa é condicional ao status ATIVO: se outra requisição já devolveu, nenhuma multa é gerada de novo.
        if (!emprestimoRepository.devolver(emprestimoId)) {
            throw new IllegalStateException("Empréstimo não está ativo");
        }

        long diasAtraso = diasDeAtraso(e.getDataPrevistaDevolucao(), LocalDate.now());
        BigDecimal valorMulta = BigDecimal.ZERO;
        if (diasAtraso > 0) {
            BigDecimal taxa = configuracaoService.buscarDecimal("taxa_diaria_multa", TAXA_DIARIA_PADRAO);
            BigDecimal valor = taxa.multiply(BigDecimal.valueOf(diasAtraso));
            if (valor.signum() > 0) {
                valorMulta = valor;
                Multa multa = new Multa();
                multa.setEmprestimoId(e.getId());
                multa.setLeitorId(e.getLeitor().getId());
                multa.setValor(valor);
                multa.setTaxaDiaria(taxa);
                multa.setDiasAtraso((int) diasAtraso);
                multa.setStatus("PENDENTE");
                multaRepository.salvar(multa);

                notificacaoRepository.salvar(new Notificacao(
                    null, "MULTA", "ATENCAO", "Multa gerada por atraso",
                    "Multa de R$ " + valor + " gerada para o empréstimo #" + emprestimoId + " (" + diasAtraso + " dia(s) de atraso)",
                    "MULTA", multa.getId()));
            }
        }

        auditoriaRepository.registrar(adminUsuario, "DEVOLUCAO", "Emprestimos", "Empréstimo #" + emprestimoId,
            "ATIVO", "DEVOLVIDO" + (valorMulta.signum() > 0 ? " (multa R$ " + valorMulta + ")" : ""), null);
        avisarPrimeiroDaFila(e);
        return valorMulta;
    }

    @Transactional
    public void renovar(Long emprestimoId, String adminUsuario) {
        Emprestimo e = emprestimoRepository.buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        if (!"ATIVO".equals(e.getStatus())) {
            throw new IllegalStateException("Somente empréstimos ativos podem ser renovados");
        }
        if (e.getRenovacoes() >= MAXIMO_RENOVACOES) {
            throw new IllegalStateException("Limite de renovações atingido");
        }
        if (diasDeAtraso(e.getDataPrevistaDevolucao(), LocalDate.now()) > 0) {
            throw new IllegalStateException("Empréstimo em atraso não pode ser renovado. Registre a devolução.");
        }
        Long itemId = itemDoEmprestimo(e);
        if (itemId != null && !reservaRepository.listarPendentesPorItem(itemId).isEmpty()) {
            throw new IllegalStateException("Há reserva pendente para este material. A renovação não é permitida.");
        }
        LocalDateTime novaPrevista = e.getDataPrevistaDevolucao().plusDays(prazoPadraoEmDias(e.getLeitor()));
        if (!emprestimoRepository.renovar(emprestimoId, novaPrevista, MAXIMO_RENOVACOES)) {
            throw new IllegalStateException("Limite de renovações atingido");
        }
        auditoriaRepository.registrar(adminUsuario, "RENOVACAO_EMPRESTIMO", "Emprestimos", "Empréstimo #" + emprestimoId,
            "devolução até " + e.getDataPrevistaDevolucao().toLocalDate(),
            "devolução até " + novaPrevista.toLocalDate(), null);
    }

    @Transactional
    public void cancelar(Long emprestimoId, String adminUsuario) {
        Emprestimo e = emprestimoRepository.buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        if (!"ATIVO".equals(e.getStatus()) || !emprestimoRepository.cancelar(emprestimoId)) {
            throw new IllegalStateException("Somente empréstimos ativos podem ser cancelados");
        }
        auditoriaRepository.registrar(adminUsuario, "CANCELAMENTO_EMPRESTIMO", "Emprestimos",
            "Empréstimo #" + emprestimoId, "ATIVO", "CANCELADO", null);
        avisarPrimeiroDaFila(e);
    }

    @Transactional(readOnly = true)
    public long totalAtivos() {
        return emprestimoRepository.contarAtivos();
    }

    @Transactional(readOnly = true)
    public long totalAtrasados() {
        return emprestimoRepository.contarAtrasados();
    }

    static long diasDeAtraso(LocalDateTime prevista, LocalDate hoje) {
        if (prevista == null) return 0;
        return Math.max(0, ChronoUnit.DAYS.between(prevista.toLocalDate(), hoje));
    }

    private int limiteDoLeitor(Leitor leitor) {
        return leitor instanceof Professor
            ? configuracaoService.buscarInteiro("limite_professores", LIMITE_PROFESSORES_PADRAO)
            : configuracaoService.buscarInteiro("limite_alunos", LIMITE_ALUNOS_PADRAO);
    }

    private Long itemDoEmprestimo(Emprestimo e) {
        return e.getExemplar() != null ? e.getExemplar().getItemAcervoId() : null;
    }

    private void avisarPrimeiroDaFila(Emprestimo emprestimo) {
        Long itemId = itemDoEmprestimo(emprestimo);
        if (itemId == null) return;
        var fila = reservaRepository.listarPendentesPorItem(itemId);
        if (fila.isEmpty()) return;
        var primeira = fila.get(0);
        notificacaoRepository.salvar(new Notificacao(
            null, "RESERVA", "ATENCAO", "Material reservado disponível",
            "O material da reserva #" + primeira.getId() + " foi devolvido e está disponível para retirada.",
            "RESERVA", primeira.getId()));
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
        dto.setDataDevolucaoReal(e.getDataDevolucaoReal());
        dto.setRenovacoes(e.getRenovacoes());
        dto.setAtrasado("ATIVO".equals(e.getStatus()) && diasDeAtraso(e.getDataPrevistaDevolucao(), LocalDate.now()) > 0);
        if (e.getLeitor() != null) {
            dto.setLeitorId(e.getLeitor().getId());
            dto.setLeitorNome(e.getLeitor().getNome());
            dto.setLeitorMatricula(e.getLeitor().getMatricula());
        }
        if (e.getExemplar() != null) {
            dto.setExemplarId(e.getExemplar().getId());
            dto.setTombamento(e.getExemplar().getTombamento());
            dto.setTitulo(e.getExemplar().getItemAcervo() != null ? e.getExemplar().getItemAcervo().getTitulo() : null);
        }
        return dto;
    }
}
