package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Notificacao;
import com.gabriel.bublo.websocket.NotificationWebSocketHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de notificações (JDBC puro).
 */
@Repository
public class NotificacaoRepository {

    private final JdbcTemplate jdbc;
    private static final RowMapper<Notificacao> MAPPER = (rs, rowNum) -> {
        Notificacao n = new Notificacao();
        n.setId(rs.getLong("id"));
        n.setCategoria(rs.getString("categoria"));
        n.setPrioridade(rs.getString("prioridade"));
        n.setTitulo(rs.getString("titulo"));
        n.setMensagem(rs.getString("mensagem"));
        n.setRegistroTipo(rs.getString("registro_tipo"));
        long registroId = rs.getLong("registro_id");
        n.setRegistroId(rs.wasNull() ? null : registroId);
        n.setLida(rs.getBoolean("lida"));
        n.setResolvida(rs.getBoolean("resolvida"));
        n.setDataCriacao(rs.getObject("data_criacao", LocalDateTime.class));
        n.setDataResolucao(rs.getObject("data_resolucao", LocalDateTime.class));
        return n;
    };

    private final NotificationWebSocketHandler webSocketHandler;

    public NotificacaoRepository(JdbcTemplate jdbc, NotificationWebSocketHandler webSocketHandler) {
        this.jdbc = jdbc;
        this.webSocketHandler = webSocketHandler;
    }

    public Notificacao salvar(Notificacao n) {
        Long id = insertReturnId(
            "INSERT INTO notificacoes (categoria, prioridade, titulo, mensagem, registro_tipo, " +
            "registro_id, lida, resolvida, data_criacao) VALUES (?, ?, ?, ?, ?, ?, FALSE, FALSE, NOW())",
            n.getCategoria(), n.getPrioridade(), n.getTitulo(), n.getMensagem(),
            n.getRegistroTipo(), n.getRegistroId());
        n.setId(id);
        publicarAposCommit(id, n.getTitulo());
        return n;
    }

    private void publicarAposCommit(Long id, String titulo) {
        Runnable envio = () -> webSocketHandler.enviar("notificacao", java.util.Map.of("id", id, "titulo", titulo));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            envio.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                envio.run();
            }
        });
    }

    public Optional<Notificacao> buscarPorId(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM notificacoes WHERE id = ?", MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Paginacao<Notificacao> buscar(String categoria, String prioridade, Boolean lida, Boolean resolvida,
                                         int pagina, int tamanho) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (categoria != null && !categoria.isBlank()) {
            where.append(" AND categoria = ?"); args.add(categoria);
        }
        if (prioridade != null && !prioridade.isBlank()) {
            where.append(" AND prioridade = ?"); args.add(prioridade);
        }
        if (lida != null) {
            where.append(" AND lida = ?"); args.add(lida);
        }
        if (resolvida != null) {
            where.append(" AND resolvida = ?"); args.add(resolvida);
        }
        String sql = "SELECT * FROM notificacoes " + where +
            " ORDER BY data_criacao DESC LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<Notificacao> conteudo = jdbc.query(sql, MAPPER, args.toArray());

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM notificacoes " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
    }

    public int contarNaoLidas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notificacoes WHERE lida = FALSE", Integer.class);
    }

    public void marcarLida(Long id) {
        jdbc.update("UPDATE notificacoes SET lida = TRUE WHERE id = ?", id);
    }

    public void marcarTodasLidas() {
        jdbc.update("UPDATE notificacoes SET lida = TRUE WHERE lida = FALSE");
    }

    public void marcarVariasLidas(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        jdbc.update("UPDATE notificacoes SET lida = TRUE WHERE id IN (" + placeholders + ")", ids.toArray());
    }

    public void resolver(Long id) {
        jdbc.update("UPDATE notificacoes SET resolvida = TRUE, data_resolucao = NOW() WHERE id = ?", id);
    }

    private Long insertReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }
}
