package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.dto.ReservaDTO;
import com.gabriel.bublo.model.Reserva;
import com.gabriel.bublo.model.ItemAcervo;
import com.gabriel.bublo.repository.ReservaRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de reservas (fila FIFO).
 */
@Service
public class ReservaService {

    /** Teto do prazo de retirada: uma reserva sem teto seguraria a fila do título indefinidamente. */
    static final int PRAZO_RETIRADA_MAXIMO_DIAS = 30;

    private final ReservaRepository reservaRepository;
    private final ItemAcervoRepository itemAcervoRepository;
    private final LeitorRepository leitorRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final AuditoriaRepository auditoriaRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          ItemAcervoRepository itemAcervoRepository,
                          LeitorRepository leitorRepository,
                          NotificacaoRepository notificacaoRepository,
                          ExemplarRepository exemplarRepository,
                          EmprestimoRepository emprestimoRepository,
                          AuditoriaRepository auditoriaRepository) {
        this.reservaRepository = reservaRepository;
        this.itemAcervoRepository = itemAcervoRepository;
        this.leitorRepository = leitorRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.exemplarRepository = exemplarRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public Paginacao<ReservaDTO> listar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        var livresPorItem = new java.util.HashMap<Long, Integer>();
        return reservaRepository.buscar(termo, status, pagina, tamanho, ordenacao)
            .mapConteudo(r -> toDTO(r, livresPorItem));
    }

    @Transactional(readOnly = true)
    public List<ReservaDTO> historicoDoLeitor(Long leitorId) {
        return reservaRepository.listarPorLeitor(leitorId, 100).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ReservaDTO buscarDTO(Long id) {
        Reserva r = reservaRepository.buscarPorId(id).orElse(null);
        return r != null ? toDTO(r) : null;
    }

    @Transactional(readOnly = true)
    public long totalAguardando() {
        return reservaRepository.contarAguardando();
    }

    @Transactional
    public ReservaDTO criar(Long leitorId, Long itemId, LocalDateTime prazoRetirada, String adminUsuario) {
        var leitor = leitorRepository.buscarPorId(leitorId)
            .orElseThrow(() -> new IllegalStateException("Leitor não encontrado"));
        if (!Boolean.TRUE.equals(leitor.getAtivo())) {
            throw new IllegalStateException("Leitor inativo. Não é possível registrar reservas.");
        }
        var item = itemAcervoRepository.buscarPorId(itemId)
            .orElseThrow(() -> new IllegalStateException("Material não encontrado"));
        if (!itemAcervoRepository.estaAtivo(itemId)) {
            throw new IllegalStateException("Material excluído do acervo. Não é possível reservar.");
        }
        if (item.getTipo() == com.gabriel.bublo.model.TipoMaterial.DIGITAL) {
            throw new IllegalStateException("Publicações digitais não podem ser reservadas: são apenas para consulta e não têm exemplares físicos.");
        }
        if (prazoRetirada == null || prazoRetirada.toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("O prazo de retirada não pode ser anterior a hoje.");
        }
        if (prazoRetirada.toLocalDate().isAfter(java.time.LocalDate.now().plusDays(PRAZO_RETIRADA_MAXIMO_DIAS))) {
            throw new IllegalStateException("O prazo de retirada não pode passar de " + PRAZO_RETIRADA_MAXIMO_DIAS + " dias.");
        }
        if (emprestimoRepository.leitorTemEmprestimoAtivoDoItem(leitorId, itemId)) {
            throw new IllegalStateException("Este leitor já está com um exemplar deste material emprestado; não precisa reservar.");
        }
        if (exemplarRepository.contarPorItem(itemId) == 0) {
            throw new IllegalStateException("Este material não tem exemplares cadastrados, então não há o que reservar. Cadastre um exemplar na página do material primeiro.");
        }
        // Exemplares livres já comprometidos com a fila não contam como disponíveis para novas reservas.
        int livres = exemplarRepository.buscarDisponiveisPorItem(itemId).size();
        int naFila = reservaRepository.listarPendentesPorItem(itemId).size();
        if (livres > naFila) {
            throw new IllegalStateException("Este material ainda tem exemplares disponíveis. Só é possível reservar quando todos os exemplares estão emprestados — registre um empréstimo.");
        }
        Reserva r = reservaRepository.criar(leitorId, itemId, prazoRetirada);
        notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
            null, "RESERVA", "INFORMATIVO", "Nova reserva",
            "Reserva #" + r.getId() + " de \"" + item.getTitulo() + "\" para " + leitor.getNome()
                + ". Posição na fila: " + r.getPosicao(),
            "RESERVA", r.getId()));
        auditoriaRepository.registrar(adminUsuario, "CRIACAO_RESERVA", "Reservas", "Reserva #" + r.getId(),
            null, leitor.getNome() + " -> " + item.getTitulo() + " (posição " + r.getPosicao() + ")", null);
        return toDTO(r);
    }

    @Transactional
    public void cancelar(Long reservaId, String adminUsuario) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId)
            .orElseThrow(() -> new IllegalStateException("Reserva não encontrada"));
        if (!"AGUARDANDO".equals(reserva.getStatus())) {
            throw new IllegalStateException("Somente reservas aguardando retirada podem ser canceladas.");
        }
        reservaRepository.cancelar(reservaId);
        notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
            null, "RESERVA", "INFORMATIVO", "Reserva cancelada",
            "Reserva #" + reservaId + " foi cancelada", "RESERVA", reservaId));
        auditoriaRepository.registrar(adminUsuario, "CANCELAMENTO_RESERVA", "Reservas", "Reserva #" + reservaId,
            "AGUARDANDO", "CANCELADA", null);
    }

    /** Expira as reservas cujo prazo de retirada já passou (o prazo vale até o fim do dia). */
    @Transactional
    public int expirarVencidas() {
        var vencidas = reservaRepository.listarExpiradasNaoProcessadas();
        for (Reserva r : vencidas) {
            reservaRepository.expirar(r.getId());
            notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
                null, "RESERVA", "INFORMATIVO", "Reserva expirada",
                "Reserva #" + r.getId() + " expirou por falta de retirada no prazo.", "RESERVA", r.getId()));
            auditoriaRepository.registrar("sistema", "EXPIRACAO_RESERVA", "Reservas", "Reserva #" + r.getId(),
                "AGUARDANDO", "EXPIRADA", "Prazo de retirada vencido");
        }
        return vencidas.size();
    }

    @Transactional
    public void expirar(Long reservaId) {
        reservaRepository.expirar(reservaId);
    }

    @Transactional
    public void atender(Long reservaId, String adminUsuario) {
        reservaRepository.atender(reservaId);
        auditoriaRepository.registrar(adminUsuario, "ATENDIMENTO_RESERVA", "Reservas", "Reserva #" + reservaId,
            "AGUARDANDO", "ATENDIDA", null);
    }

    private ReservaDTO toDTO(Reserva r) {
        return toDTO(r, new java.util.HashMap<>());
    }

    /**
     * A reserva está pronta para retirada quando há exemplar livre para a sua posição na fila
     * (exemplares livres são entregues na ordem da fila: a posição N só é atendida se houver N livres).
     */
    private ReservaDTO toDTO(Reserva r, java.util.Map<Long, Integer> livresPorItem) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(r.getId());
        dto.setLeitorId(r.getLeitor() != null ? r.getLeitor().getId() : null);
        dto.setLeitorNome(r.getLeitor() != null ? r.getLeitor().getNome() : null);
        dto.setItemId(r.getItemAcervo() != null ? r.getItemAcervo().getId() : null);
        dto.setItemTitulo(r.getItemAcervo() != null ? r.getItemAcervo().getTitulo() : null);
        dto.setPosicao(r.getPosicao());
        dto.setPrazoRetirada(r.getPrazoRetirada());
        dto.setStatus(r.getStatus());
        if ("AGUARDANDO".equals(r.getStatus()) && r.getItemAcervo() != null && r.getPosicao() != null) {
            int livres = livresPorItem.computeIfAbsent(r.getItemAcervo().getId(),
                id -> exemplarRepository.buscarDisponiveisPorItem(id).size());
            dto.setProntaParaRetirada(r.getPosicao() <= livres);
        }
        return dto;
    }
}
