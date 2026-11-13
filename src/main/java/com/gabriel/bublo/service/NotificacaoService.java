package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.dto.NotificacaoDTO;
import com.gabriel.bublo.model.Notificacao;
import com.gabriel.bublo.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de notificações.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional(readOnly = true)
    public Paginacao<NotificacaoDTO> listar(String categoria, String prioridade, Boolean lida, Boolean resolvida,
                                            int pagina, int tamanho) {
        return notificacaoRepository.buscar(categoria, prioridade, lida, resolvida, pagina, tamanho)
            .mapConteudo(this::toDTO);
    }

    @Transactional(readOnly = true)
    public int contarNaoLidas() {
        return notificacaoRepository.contarNaoLidas();
    }

    @Transactional(readOnly = true)
    public NotificacaoDTO buscarDTO(Long id) {
        Notificacao n = notificacaoRepository.buscarPorId(id).orElse(null);
        return n != null ? toDTO(n) : null;
    }

    @Transactional
    public void marcarLida(Long id) {
        notificacaoRepository.marcarLida(id);
    }

    @Transactional
    public void marcarTodasLidas() {
        notificacaoRepository.marcarTodasLidas();
    }

    @Transactional
    public void marcarVariasLidas(List<Long> ids) {
        notificacaoRepository.marcarVariasLidas(ids);
    }

    @Transactional
    public void resolver(Long id) {
        notificacaoRepository.resolver(id);
    }

    private NotificacaoDTO toDTO(Notificacao n) {
        NotificacaoDTO dto = new NotificacaoDTO();
        dto.setId(n.getId());
        dto.setCategoria(n.getCategoria());
        dto.setPrioridade(n.getPrioridade());
        dto.setTitulo(n.getTitulo());
        dto.setMensagem(n.getMensagem());
        dto.setLida(n.getLida());
        dto.setResolvida(n.getResolvida());
        dto.setDataCriacao(n.getDataCriacao());
        return dto;
    }
}
