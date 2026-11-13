package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Multa;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de multas (JDBC puro).
 */
@Repository
public class MultaRepository {

    private static final java.util.Set<String> COLUNAS_ORDENAVEIS =
        java.util.Set.of("data_geracao", "valor", "valor_pago", "status", "dias_atraso");

    private final JdbcTemplate jdbc;
    private static final RowMapper<Multa> MAPPER = (rs, rowNum) -> {
        Multa m = new Multa();
        m.setId(rs.getLong("id"));
        m.setEmprestimoId(rs.getObject("emprestimo_id", Long.class));
        m.setLeitorId(rs.getLong("leitor_id"));
        m.setValor(rs.getBigDecimal("valor"));
        m.setTaxaDiaria(rs.getBigDecimal("taxa_diaria"));
        m.setDiasAtraso(rs.getObject("dias_atraso", Integer.class));
        m.setDataGeracao(rs.getObject("data_geracao", LocalDateTime.class));
        m.setStatus(rs.getString("status"));
        m.setValorPago(rs.getBigDecimal("valor_pago"));
        return m;
    };

    public MultaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Multa salvar(Multa multa) {
        if (multa.getId() == null) {
            Long id = insertReturnId(
                "INSERT INTO multas (emprestimo_id, leitor_id, valor, taxa_diaria, dias_atraso, " +
                "data_geracao, status, valor_pago) VALUES (?, ?, ?, ?, ?, NOW(), ?, 0)",
                multa.getEmprestimoId(), multa.getLeitorId(), multa.getValor(),
                multa.getTaxaDiaria(), multa.getDiasAtraso(), multa.getStatus());
            multa.setId(id);
        } else {
            jdbc.update(
                "UPDATE multas SET valor=?, taxa_diaria=?, dias_atraso=?, status=?, valor_pago=? WHERE id=?",
                multa.getValor(), multa.getTaxaDiaria(), multa.getDiasAtraso(),
                multa.getStatus(), multa.getValorPago(), multa.getId());
        }
        return multa;
    }

    public Optional<Multa> buscarPorId(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM multas WHERE id = ?", MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Paginacao<Multa> buscar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (termo != null && !termo.isBlank()) {
            where.append(" AND m.leitor_id IN (SELECT id FROM leitores WHERE nome ILIKE ? OR matricula ILIKE ?)");
            String t = "%" + termo + "%";
            args.add(t); args.add(t);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND m.status = ?"); args.add(status);
        }
        String sql = "SELECT * FROM multas m " + where +
            " ORDER BY " + OrdenacaoSegura.resolver(ordenacao, "m.", COLUNAS_ORDENAVEIS, "m.data_geracao DESC") +
            " LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<Multa> conteudo = jdbc.query(sql, MAPPER, args.toArray());

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM multas m " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
    }

    public List<Multa> listarPendentesPorLeitor(Long leitorId) {
        return jdbc.query(
            "SELECT * FROM multas WHERE leitor_id = ? AND status IN ('PENDENTE','PAGA_PARCIAL') ORDER BY data_geracao ASC",
            MAPPER, leitorId);
    }

    public BigDecimal saldoPendentePorLeitor(Long leitorId) {
        BigDecimal s = jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor - valor_pago), 0) FROM multas WHERE leitor_id = ? AND status IN ('PENDENTE','PAGA_PARCIAL')",
            BigDecimal.class, leitorId);
        return s == null ? BigDecimal.ZERO : s;
    }

    public long contarPendentes() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM multas WHERE status IN ('PENDENTE','PAGA_PARCIAL')", Long.class);
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
