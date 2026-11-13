package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Auditoria;
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

/**
 * Repositório de auditoria (JDBC puro) — somente leitura e inserção.
 */
@Repository
public class AuditoriaRepository {

    private final JdbcTemplate jdbc;
    private static final RowMapper<Auditoria> MAPPER = (rs, rowNum) -> {
        Auditoria a = new Auditoria();
        a.setId(rs.getLong("id"));
        a.setAdminUsuario(rs.getString("admin_usuario"));
        a.setDataHora(rs.getObject("data_hora", LocalDateTime.class));
        a.setTipoOperacao(rs.getString("tipo_operacao"));
        a.setModulo(rs.getString("modulo"));
        a.setRegistroAfectado(rs.getString("registro_afetado"));
        a.setValoresAntes(rs.getString("valores_antes"));
        a.setValoresDepois(rs.getString("valores_depois"));
        a.setJustificativa(rs.getString("justificativa"));
        return a;
    };

    public AuditoriaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(String adminUsuario, String tipoOperacao, String modulo,
                          String registroAfectado, String valoresAntes, String valoresDepois,
                          String justificativa) {
        jdbc.update(
            "INSERT INTO auditoria (admin_usuario, data_hora, tipo_operacao, modulo, " +
            "registro_afetado, valores_antes, valores_depois, justificativa) " +
            "VALUES (?, NOW(), ?, ?, ?, ?, ?, ?)",
            adminUsuario, tipoOperacao, modulo, registroAfectado, valoresAntes, valoresDepois, justificativa);
    }

    public Paginacao<Auditoria> buscar(String termo, String modulo, String tipoOperacao,
                                       int pagina, int tamanho) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (termo != null && !termo.isBlank()) {
            where.append(" AND (admin_usuario ILIKE ? OR modulo ILIKE ? OR tipo_operacao ILIKE ?)");
            String t = "%" + termo + "%";
            args.add(t); args.add(t); args.add(t);
        }
        if (modulo != null && !modulo.isBlank()) {
            where.append(" AND modulo = ?"); args.add(modulo);
        }
        if (tipoOperacao != null && !tipoOperacao.isBlank()) {
            where.append(" AND tipo_operacao = ?"); args.add(tipoOperacao);
        }
        String sql = "SELECT * FROM auditoria " + where +
            " ORDER BY data_hora DESC LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<Auditoria> conteudo = jdbc.query(sql, MAPPER, args.toArray());

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM auditoria " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
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
