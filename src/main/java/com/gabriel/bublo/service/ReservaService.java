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

    private final ReservaRepository reservaRepository;
    private final ItemAcervoRepository itemAcervoRepository;
    private final LeitorRepository leitorRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          ItemAcervoRepository itemAcervoRepository,
                          LeitorRepository leitorRepository,
                          NotificacaoRepository notificacaoRepository,
                          ExemplarRepository exemplarRepository,
                          EmprestimoRepository emprestimoRepository) {
        this.reservaRepository = reservaRepository;
        this.itemAcervoRepository = itemAcervoRepository;
        this.leitorRepository = leitorRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.exemplarRepository = exemplarRepository;
        this.emprestimoRepository = emprestimoRepository;
    }

    @Transactional(readOnly = true)
    public Paginacao<ReservaDTO> listar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        return reservaRepository.buscar(termo, status, pagina, tamanho, ordenacao)
            .mapConteudo(this::toDTO);
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
        if (item.getTipo() == com.gabriel.bublo.model.TipoMaterial.DIGITAL) {
            throw new IllegalStateException("Publicações digitais não podem ser reservadas.");
        }
        if (prazoRetirada == null || prazoRetirada.toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("O prazo de retirada não pode ser anterior a hoje.");
        }
        if (!exemplarRepository.buscarDisponiveisPorItem(itemId).isEmpty()) {
            throw new IllegalStateException("Este material possui exemplares disponíveis. Registre um empréstimo em vez de uma reserva.");
        }
        Reserva r = reservaRepository.criar(leitorId, itemId, prazoRetirada);
        notificacaoRepository.salvar(new com.gabriel.bublo.model.Notificacao(
            null, "RESERVA", "INFORMATIVO", "Nova reserva",
            "Reserva #" + r.getId() + " criada para o item " + itemId + ". Posição na fila: " + r.getPosicao(),
            "RESERVA", r.getId()));
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
    }

    @Transactional
    public void expirar(Long reservaId) {
        reservaRepository.expirar(reservaId);
    }

    @Transactional
    public void atender(Long reservaId, String adminUsuario) {
        reservaRepository.atender(reservaId);
    }

    private ReservaDTO toDTO(Reserva r) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(r.getId());
        dto.setLeitorId(r.getLeitor() != null ? r.getLeitor().getId() : null);
        dto.setLeitorNome(r.getLeitor() != null ? r.getLeitor().getNome() : null);
        dto.setItemId(r.getItemAcervo() != null ? r.getItemAcervo().getId() : null);
        dto.setItemTitulo(r.getItemAcervo() != null ? r.getItemAcervo().getTitulo() : null);
        dto.setPosicao(r.getPosicao());
        dto.setPrazoRetirada(r.getPrazoRetirada());
        dto.setStatus(r.getStatus());
        return dto;
    }
}
