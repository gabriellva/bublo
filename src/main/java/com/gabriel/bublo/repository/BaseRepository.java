package com.gabriel.bublo.repository;

import com.gabriel.bublo.model.ItemAcervo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * Repositório base com utilitários JDBC puro.
 * Demonstra encapsulamento de acesso a dados e gerenciamento de conexões
 * via JdbcTemplate (PreparedStatement, transações).
 */
@Repository
public abstract class BaseRepository {

    protected final JdbcTemplate jdbc;

    public BaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected Long insertReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    protected void update(String sql, Object... args) {
        jdbc.update(sql, args);
    }

    protected <T> T queryForObject(String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
        List<T> results = jdbc.query(sql, mapper, args);
        return results.isEmpty() ? null : results.get(0);
    }

    protected <T> List<T> query(String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
        return jdbc.query(sql, mapper, args);
    }

    protected Integer queryForInt(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    protected boolean exists(String sql, Object... args) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }
}
