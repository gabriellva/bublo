package com.gabriel.bublo.repository;

import com.gabriel.bublo.model.Pagamento;
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
import java.util.List;

/**
 * Repositório de pagamentos (JDBC puro).
 */
@Repository
public class PagamentoRepository {

    private final JdbcTemplate jdbc;
    private static final RowMapper<Pagamento> MAPPER = (rs, rowNum) -> {
        Pagamento p = new Pagamento();
        p.setId(rs.getLong("id"));
        p.setMultaId(rs.getLong("multa_id"));
        p.setValor(rs.getBigDecimal("valor"));
        p.setFormaPagamento(rs.getString("forma_pagamento"));
        p.setDataPagamento(rs.getObject("data_pagamento", LocalDateTime.class));
        p.setResponsavel(rs.getString("responsavel"));
        return p;
    };

    public PagamentoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pagamento salvar(Pagamento pagamento) {
        Long id = insertReturnId(
            "INSERT INTO pagamentos (multa_id, valor, forma_pagamento, data_pagamento, responsavel) " +
            "VALUES (?, ?, ?, NOW(), ?)",
            pagamento.getMultaId(), pagamento.getValor(),
            pagamento.getFormaPagamento(), pagamento.getResponsavel());
        pagamento.setId(id);
        return pagamento;
    }

    public List<Pagamento> listarPorMulta(Long multaId) {
        return jdbc.query("SELECT * FROM pagamentos WHERE multa_id = ? ORDER BY data_pagamento DESC", MAPPER, multaId);
    }

    public Pagamento buscarPorId(Long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM pagamentos WHERE id = ?", MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
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
