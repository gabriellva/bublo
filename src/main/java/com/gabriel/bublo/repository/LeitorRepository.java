package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.TipoLeitor;
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
 * Repositório de leitores (JDBC puro).
 * Persiste Leitor e subclasses (Aluno, Professor).
 */
@Repository
public class LeitorRepository {

    private static final java.util.Set<String> COLUNAS_ORDENAVEIS =
        java.util.Set.of("nome", "email", "matricula", "tipo", "ativo", "data_cadastro");

    private final JdbcTemplate jdbc;

    private static final RowMapper<Leitor> LEITOR_MAPPER = (rs, rowNum) -> {
        TipoLeitor tipo = TipoLeitor.valueOf(rs.getString("tipo"));
        Leitor leitor;
        if (tipo == TipoLeitor.PROFESSOR) {
            leitor = new com.gabriel.bublo.model.Professor();
        } else {
            leitor = new com.gabriel.bublo.model.Aluno();
        }
        leitor.setId(rs.getLong("id"));
        leitor.setTipo(tipo);
        leitor.setNome(rs.getString("nome"));
        leitor.setEmail(rs.getString("email"));
        leitor.setMatricula(rs.getString("matricula"));
        leitor.setCpf(rs.getString("cpf"));
        leitor.setEndereco(rs.getString("endereco"));
        leitor.setTelefone(rs.getString("telefone"));
        leitor.setCursoDepartamento(rs.getString("curso_departamento"));
        leitor.setAtivo(rs.getBoolean("ativo"));
        leitor.setDataCadastro(rs.getObject("data_cadastro", LocalDateTime.class));
        return leitor;
    };

    public LeitorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Leitor salvar(Leitor leitor) {
        if (leitor.getId() == null) {
            String matricula = gerarMatricula(leitor.getTipo());
            leitor.setMatricula(matricula);
            Long id = insertReturnId(
                "INSERT INTO leitores (tipo, nome, email, matricula, cpf, endereco, telefone, " +
                "curso_departamento, ativo, data_cadastro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                leitor.getTipo().name(), leitor.getNome(), leitor.getEmail(), matricula,
                leitor.getCpf(), leitor.getEndereco(), leitor.getTelefone(),
                leitor.getCursoDepartamento(), leitor.getAtivo() != null ? leitor.getAtivo() : true);
            leitor.setId(id);
        } else {
            jdbc.update(
                "UPDATE leitores SET tipo=?, nome=?, email=?, cpf=?, endereco=?, telefone=?, " +
                "curso_departamento=?, ativo=? WHERE id=?",
                leitor.getTipo().name(), leitor.getNome(), leitor.getEmail(),
                leitor.getCpf(), leitor.getEndereco(), leitor.getTelefone(),
                leitor.getCursoDepartamento(), leitor.getAtivo() != null ? leitor.getAtivo() : true, leitor.getId());
        }
        return leitor;
    }

    public Optional<Leitor> buscarPorId(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM leitores WHERE id = ?", LEITOR_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Leitor> buscarPorMatricula(String matricula) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM leitores WHERE matricula = ?", LEITOR_MAPPER, matricula));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean emailExiste(String email, Long excluirId) {
        String sql = excluirId != null
            ? "SELECT COUNT(*) FROM leitores WHERE email = ? AND id <> ?"
            : "SELECT COUNT(*) FROM leitores WHERE email = ?";
        Integer c = excluirId != null
            ? jdbc.queryForObject(sql, Integer.class, email, excluirId)
            : jdbc.queryForObject(sql, Integer.class, email);
        return c != null && c > 0;
    }

    public void deletar(Long id) {
        jdbc.update("DELETE FROM leitores WHERE id = ?", id);
    }

    public Paginacao<Leitor> buscar(String termo, TipoLeitor tipo, Boolean ativo,
                                    int pagina, int tamanho, String ordenacao) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (termo != null && !termo.isBlank()) {
            where.append(" AND (nome ILIKE ? OR email ILIKE ? OR matricula ILIKE ?)");
            String t = "%" + termo + "%";
            args.add(t); args.add(t); args.add(t);
        }
        if (tipo != null) {
            where.append(" AND tipo = ?"); args.add(tipo.name());
        }
        if (ativo != null) {
            where.append(" AND ativo = ?"); args.add(ativo);
        }

        String sql = "SELECT * FROM leitores " + where +
            " ORDER BY " + OrdenacaoSegura.resolver(ordenacao, "", COLUNAS_ORDENAVEIS, "nome ASC") +
            " LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<Leitor> conteudo = jdbc.query(sql, LEITOR_MAPPER, args.toArray());

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM leitores " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
    }

    public List<Leitor> listarAtivos() {
        return jdbc.query("SELECT * FROM leitores WHERE ativo = TRUE ORDER BY nome ASC", LEITOR_MAPPER);
    }

    private String gerarMatricula(TipoLeitor tipo) {
        String prefixo = tipo == TipoLeitor.PROFESSOR ? "PROF" : "ALU";
        // Busca o próximo número sequencial para o tipo
        Integer prox = jdbc.queryForObject(
            "SELECT COALESCE(MAX(CAST(substring(matricula from '\\d+') AS INTEGER)), 0) + 1 " +
            "FROM leitores WHERE matricula LIKE ?", Integer.class, prefixo + "-%");
        return String.format("%s-%05d", prefixo, prox);
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
