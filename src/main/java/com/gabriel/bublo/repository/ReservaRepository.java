package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.ItemAcervo;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.Reserva;
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
 * Repositório de reservas (JDBC puro).
 * Fila FIFO por item do acervo.
 */
@Repository
public class ReservaRepository {

    private static final java.util.Set<String> COLUNAS_ORDENAVEIS =
        java.util.Set.of("posicao", "data_reserva", "prazo_retirada", "status");

    private final JdbcTemplate jdbc;
    private final ItemAcervoRepository itemAcervoRepository;
    private final LeitorRepository leitorRepository;

    private static final RowMapper<Reserva> MAPPER = (rs, rowNum) -> {
        Reserva r = new Reserva();
        r.setId(rs.getLong("id"));
        r.setDataReserva(rs.getObject("data_reserva", LocalDateTime.class));
        r.setPosicao(rs.getObject("posicao", Integer.class));
        r.setPrazoRetirada(rs.getObject("prazo_retirada", LocalDateTime.class));
        r.setStatus(rs.getString("status"));
        return r;
    };

    public ReservaRepository(JdbcTemplate jdbc, ItemAcervoRepository itemAcervoRepository,
                             LeitorRepository leitorRepository) {
        this.jdbc = jdbc;
        this.itemAcervoRepository = itemAcervoRepository;
        this.leitorRepository = leitorRepository;
    }

    public Reserva criar(Long leitorId, Long itemId, LocalDateTime prazoRetirada) {
        // Verifica duplicidade: mesma reserva ativa não pode existir
        Integer existente = jdbc.queryForObject(
            "SELECT COUNT(*) FROM reservas WHERE leitor_id = ? AND item_acervo_id = ? AND status = 'AGUARDANDO'",
            Integer.class, leitorId, itemId);
        if (existente != null && existente > 0) {
            throw new IllegalStateException("Já existe uma reserva ativa para este material");
        }

        // Próxima posição na fila FIFO
        Integer proxPos = jdbc.queryForObject(
            "SELECT COALESCE(MAX(posicao), 0) + 1 FROM reservas WHERE item_acervo_id = ? AND status = 'AGUARDANDO'",
            Integer.class, itemId);

        Long id = insertReturnId(
            "INSERT INTO reservas (leitor_id, item_acervo_id, data_reserva, posicao, prazo_retirada, status) " +
            "VALUES (?, ?, NOW(), ?, ?, 'AGUARDANDO')",
            leitorId, itemId, proxPos, prazoRetirada);

        Reserva r = new Reserva();
        r.setId(id);
        r.setLeitor(leitorRepository.buscarPorId(leitorId).orElse(null));
        r.setItemAcervo(itemAcervoRepository.buscarPorId(itemId).orElse(null));
        r.setPosicao(proxPos);
        r.setPrazoRetirada(prazoRetirada);
        r.setStatus("AGUARDANDO");
        return r;
    }

    public Optional<Reserva> buscarPorId(Long id) {
        try {
            Reserva r = jdbc.queryForObject("SELECT * FROM reservas WHERE id = ?", MAPPER, id);
            if (r != null) vincular(r);
            return Optional.ofNullable(r);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Paginacao<Reserva> buscar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (termo != null && !termo.isBlank()) {
            where.append(" AND (r.id::text = ? OR r.item_acervo_id IN (SELECT id FROM item_acervo WHERE titulo ILIKE ?)" +
                " OR r.leitor_id IN (SELECT id FROM leitores WHERE nome ILIKE ? OR matricula ILIKE ?))");
            args.add(termo.trim());
            String t = "%" + termo.trim() + "%";
            args.add(t); args.add(t); args.add(t);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND r.status = ?"); args.add(status);
        }
        String sql = "SELECT * FROM reservas r " + where +
            " ORDER BY " + OrdenacaoSegura.resolver(ordenacao, "r.", COLUNAS_ORDENAVEIS, "r.data_reserva DESC") +
            " LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<Reserva> conteudo = jdbc.query(sql, MAPPER, args.toArray());
        conteudo.forEach(this::vincular);

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM reservas r " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
    }

    public List<Reserva> listarPendentesPorItem(Long itemId) {
        List<Reserva> lista = jdbc.query(
            "SELECT * FROM reservas WHERE item_acervo_id = ? AND status = 'AGUARDANDO' ORDER BY posicao ASC",
            MAPPER, itemId);
        lista.forEach(this::vincular);
        return lista;
    }

    public void atender(Long reservaId) {
        jdbc.update("UPDATE reservas SET status = 'ATENDIDA' WHERE id = ?", reservaId);
    }

    public void cancelar(Long reservaId) {
        jdbc.update("UPDATE reservas SET status = 'CANCELADA' WHERE id = ?", reservaId);
    }

    public void expirar(Long reservaId) {
        jdbc.update("UPDATE reservas SET status = 'EXPIRADA' WHERE id = ?", reservaId);
    }

    public long contarAguardando() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservas WHERE status = 'AGUARDANDO'", Long.class);
    }

    public List<Reserva> listarExpiradasNaoProcessadas() {
        return jdbc.query(
            "SELECT * FROM reservas WHERE status = 'AGUARDANDO' AND prazo_retirada < NOW() ORDER BY data_reserva ASC",
            MAPPER);
    }

    private void vincular(Reserva r) {
        Long[] ids = jdbc.queryForObject(
            "SELECT leitor_id, item_acervo_id FROM reservas WHERE id = ?",
            (rs, rowNum) -> new Long[] {rs.getLong("leitor_id"), rs.getLong("item_acervo_id")}, r.getId());
        r.setLeitor(leitorRepository.buscarPorId(ids[0]).orElse(null));
        r.setItemAcervo(itemAcervoRepository.buscarPorId(ids[1]).orElse(null));
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
