package com.gabriel.bublo.repository;

import com.gabriel.bublo.model.Usuario;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório de usuários (administradores) para autenticação.
 */
@Repository
public class UsuarioRepository {

    private final JdbcTemplate jdbc;
    private static final RowMapper<Usuario> MAPPER = (rs, rowNum) -> {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setSenhaHash(rs.getString("senha_hash"));
        u.setAtivo(rs.getBoolean("ativo"));
        u.setDataCriacao(rs.getObject("data_criacao", java.time.LocalDateTime.class));
        return u;
    };

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                "SELECT * FROM usuarios WHERE username = ?", MAPPER, username));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void criar(Usuario usuario) {
        jdbc.update(
            "INSERT INTO usuarios (username, senha_hash, ativo, data_criacao) VALUES (?, ?, TRUE, NOW())",
            usuario.getUsername(), usuario.getSenhaHash());
    }

    public void alterarSenha(Long id, String senhaHash) {
        jdbc.update("UPDATE usuarios SET senha_hash = ? WHERE id = ?", senhaHash, id);
    }
}
