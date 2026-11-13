package com.gabriel.bublo.repository;

import com.gabriel.bublo.model.Configuracao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de configurações (chave-valor).
 */
@Repository
public class ConfiguracaoRepository {

    private final JdbcTemplate jdbc;
    private static final RowMapper<Configuracao> MAPPER = (rs, rowNum) -> {
        Configuracao c = new Configuracao();
        c.setChave(rs.getString("chave"));
        c.setValor(rs.getString("valor"));
        c.setAtualizadoEm(rs.getObject("atualizado_em", java.time.LocalDateTime.class));
        c.setAtualizadoPor(rs.getString("atualizado_por"));
        return c;
    };

    public ConfiguracaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Configuracao> buscar(String chave) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM configuracoes WHERE chave = ?", MAPPER, chave));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void salvar(Configuracao c) {
        jdbc.update(
            "INSERT INTO configuracoes (chave, valor, atualizado_em, atualizado_por) VALUES (?, ?, NOW(), ?) " +
            "ON CONFLICT (chave) DO UPDATE SET valor = EXCLUDED.valor, atualizado_em = NOW(), atualizado_por = EXCLUDED.atualizado_por",
            c.getChave(), c.getValor(), c.getAtualizadoPor());
    }

    public List<Configuracao> listarTodas() {
        return jdbc.query("SELECT * FROM configuracoes ORDER BY chave", MAPPER);
    }
}
