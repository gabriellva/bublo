package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.ItemAcervo;
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
 * Repositório de exemplares físicos (JDBC puro).
 * Geração automática de tombamento sequencial imutável (BA-000001...).
 */
@Repository
public class ExemplarRepository {

    private final JdbcTemplate jdbc;
    private final ItemAcervoRepository itemAcervoRepository;

    private static final RowMapper<Exemplar> MAPPER = (rs, rowNum) -> {
        Exemplar e = new Exemplar();
        e.setId(rs.getLong("id"));
        e.setItemAcervoId(rs.getLong("item_acervo_id"));
        e.setTombamento(rs.getString("tombamento"));
        e.setDisponivel(rs.getBoolean("disponivel"));
        e.setObservacoes(rs.getString("observacoes"));
        e.setDataCadastro(rs.getObject("data_cadastro", LocalDateTime.class));
        return e;
    };

    public ExemplarRepository(JdbcTemplate jdbc, ItemAcervoRepository itemAcervoRepository) {
        this.jdbc = jdbc;
        this.itemAcervoRepository = itemAcervoRepository;
    }

    public Exemplar salvar(ItemAcervo item) {
        String tombamento = proxTombamento();
        Long id = insertReturnId(
            "INSERT INTO exemplares (item_acervo_id, tombamento, disponivel, observacoes, data_cadastro) " +
            "VALUES (?, ?, TRUE, NULL, NOW())", item.getId(), tombamento);
        Exemplar e = new Exemplar();
        e.setId(id);
        e.setTombamento(tombamento);
        e.setDisponivel(true);
        e.setItemAcervo(item);
        return e;
    }

    public List<Exemplar> salvarLote(ItemAcervo item, int quantidade) {
        List<Exemplar> criados = new ArrayList<>();
        // Atômico: todos ou nenhum (chamado dentro de transação no Service)
        for (int i = 0; i < quantidade; i++) {
            criados.add(salvar(item));
        }
        return criados;
    }

    public Optional<Exemplar> buscarPorTombamento(String tombamento) {
        try {
            Exemplar e = jdbc.queryForObject(
                "SELECT * FROM exemplares WHERE tombamento = ?", MAPPER, tombamento);
            return Optional.ofNullable(e);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<Exemplar> buscarPorId(Long id) {
        try {
            Exemplar e = jdbc.queryForObject("SELECT * FROM exemplares WHERE id = ?", MAPPER, id);
            if (e != null) vincularItem(e);
            return Optional.ofNullable(e);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<Exemplar> buscarDisponiveisPorItem(Long itemId) {
        List<Exemplar> lista = jdbc.query(
            "SELECT * FROM exemplares WHERE item_acervo_id = ? AND disponivel = TRUE ORDER BY tombamento",
            MAPPER, itemId);
        lista.forEach(this::vincularItem);
        return lista;
    }

    public List<Exemplar> listarPorItem(Long itemId) {
        List<Exemplar> lista = jdbc.query("SELECT * FROM exemplares WHERE item_acervo_id = ? ORDER BY tombamento", MAPPER, itemId);
        lista.forEach(this::vincularItem);
        return lista;
    }

    private void vincularItem(Exemplar e) {
        ItemAcervo item = itemAcervoRepository.buscarPorId(e.getItemAcervoId()).orElse(null);
        e.setItemAcervo(item);
    }

    public void definirDisponibilidade(Long exemplarId, Boolean disponivel) {
        jdbc.update("UPDATE exemplares SET disponivel = ? WHERE id = ?", disponivel, exemplarId);
    }

    public void registrarObservacao(Long exemplarId, String observacoes) {
        jdbc.update("UPDATE exemplares SET observacoes = ? WHERE id = ?", observacoes, exemplarId);
    }

    public long contarDisponiveis() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exemplares WHERE disponivel = TRUE", Long.class);
    }

    public long contarIndisponiveis() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exemplares WHERE disponivel = FALSE", Long.class);
    }

    public long totalExemplares() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exemplares", Long.class);
    }

    private String proxTombamento() {
        // Prefixo BA-, sequencial com 6 dígitos, nunca reaproveita
        Integer prox = jdbc.queryForObject(
            "SELECT COALESCE(MAX(CAST(substring(tombamento from '\\d+') AS INTEGER)), 0) + 1 FROM exemplares",
            Integer.class);
        return String.format("BA-%06d", prox);
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
