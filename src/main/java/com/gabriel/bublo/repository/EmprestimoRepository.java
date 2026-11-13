package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Emprestimo;
import com.gabriel.bublo.model.Exemplar;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de empréstimos (JDBC puro).
 * Utiliza SELECT ... FOR UPDATE para bloqueio de linha do exemplar (concorrência).
 */
@Repository
public class EmprestimoRepository {

    private static final java.util.Set<String> COLUNAS_ORDENAVEIS =
        java.util.Set.of("data_emprestimo", "data_prevista_devolucao", "data_devolucao_real", "status", "renovacoes");

    private final JdbcTemplate jdbc;
    private final ExemplarRepository exemplarRepository;
    private final LeitorRepository leitorRepository;

    private static final RowMapper<Emprestimo> MAPPER = (rs, rowNum) -> {
        Emprestimo e = new Emprestimo();
        e.setId(rs.getLong("id"));
        e.setDataEmprestimo(rs.getObject("data_emprestimo", LocalDateTime.class));
        e.setDataPrevistaDevolucao(rs.getObject("data_prevista_devolucao", LocalDateTime.class));
        e.setDataDevolucaoReal(rs.getObject("data_devolucao_real", LocalDateTime.class));
        e.setRenovacoes(rs.getObject("renovacoes", Integer.class));
        e.setStatus(rs.getString("status"));
        e.setObservacoes(rs.getString("observacoes"));
        return e;
    };

    public EmprestimoRepository(JdbcTemplate jdbc, ExemplarRepository exemplarRepository,
                                LeitorRepository leitorRepository) {
        this.jdbc = jdbc;
        this.exemplarRepository = exemplarRepository;
        this.leitorRepository = leitorRepository;
    }

    @Transactional
    public Emprestimo criar(Long leitorId, Long exemplarId, LocalDateTime dataPrevista) {
        // Bloqueia a linha do exemplar para evitar empréstimo duplicado concorrente
        jdbc.queryForObject("SELECT id FROM exemplares WHERE id = ? FOR UPDATE", Integer.class, exemplarId);

        // Revalida disponibilidade no momento da confirmação
        Exemplar ex = exemplarRepository.buscarPorId(exemplarId)
            .orElseThrow(() -> new IllegalStateException("Exemplar não encontrado"));
        if (!Boolean.TRUE.equals(ex.getDisponivel())) {
            throw new IllegalStateException("Exemplar indisponível");
        }

        Long id = insertReturnId(
            "INSERT INTO emprestimos (leitor_id, exemplar_id, data_emprestimo, data_prevista_devolucao, " +
            "renovacoes, status, observacoes) VALUES (?, ?, NOW(), ?, 0, 'ATIVO', NULL)",
            leitorId, exemplarId, dataPrevista);

        // Marca exemplar indisponível
        exemplarRepository.definirDisponibilidade(exemplarId, false);

        Emprestimo e = new Emprestimo();
        e.setId(id);
        e.setLeitor(leitorRepository.buscarPorId(leitorId).orElse(null));
        e.setExemplar(exemplarRepository.buscarPorId(exemplarId).orElse(null));
        e.setDataPrevistaDevolucao(dataPrevista);
        e.setStatus("ATIVO");
        return e;
    }

    public Optional<Emprestimo> buscarPorId(Long id) {
        try {
            Emprestimo e = jdbc.queryForObject(
                "SELECT * FROM emprestimos WHERE id = ?", MAPPER, id);
            if (e != null) vincular(e);
            return Optional.ofNullable(e);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Paginacao<Emprestimo> buscar(String termo, String status, int pagina, int tamanho, String ordenacao) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (termo != null && !termo.isBlank()) {
            where.append(" AND (e.id::text = ? OR e.leitor_id IN (SELECT id FROM leitores WHERE nome ILIKE ? OR matricula ILIKE ?)" +
                " OR e.exemplar_id IN (SELECT id FROM exemplares WHERE tombamento ILIKE ?))");
            args.add(termo.trim());
            String t = "%" + termo.trim() + "%";
            args.add(t); args.add(t); args.add(t);
        }
        if ("ATRASADO".equals(status)) {
            where.append(" AND e.status = 'ATIVO' AND e.data_prevista_devolucao < NOW()");
        } else if (status != null && !status.isBlank()) {
            where.append(" AND e.status = ?"); args.add(status);
        }
        String sql = "SELECT * FROM emprestimos e " + where +
            " ORDER BY " + OrdenacaoSegura.resolver(ordenacao, "e.", COLUNAS_ORDENAVEIS, "e.data_emprestimo DESC") +
            " LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<Emprestimo> conteudo = jdbc.query(sql, MAPPER, args.toArray());
        conteudo.forEach(this::vincular);

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM emprestimos e " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
    }

    public List<Emprestimo> listarAtivosPorLeitor(Long leitorId) {
        List<Emprestimo> lista = jdbc.query(
            "SELECT * FROM emprestimos WHERE leitor_id = ? AND status = 'ATIVO' ORDER BY data_prevista_devolucao ASC",
            MAPPER, leitorId);
        lista.forEach(this::vincular);
        return lista;
    }

    public List<Emprestimo> listarAtrasados() {
        List<Emprestimo> lista = jdbc.query(
            "SELECT * FROM emprestimos WHERE status = 'ATIVO' AND data_prevista_devolucao < NOW() ORDER BY data_prevista_devolucao ASC",
            MAPPER);
        lista.forEach(this::vincular);
        return lista;
    }

    public long contarAtivos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM emprestimos WHERE status = 'ATIVO'", Long.class);
    }

    public long contarAtrasados() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM emprestimos WHERE status = 'ATIVO' AND data_prevista_devolucao < NOW()", Long.class);
    }

    public void devolver(Long emprestimoId) {
        Emprestimo e = buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        jdbc.update("UPDATE emprestimos SET status = 'DEVOLVIDO', data_devolucao_real = NOW() WHERE id = ?", emprestimoId);
        exemplarRepository.definirDisponibilidade(e.getExemplar().getId(), true);
    }

    public void cancelar(Long emprestimoId) {
        Emprestimo e = buscarPorId(emprestimoId)
            .orElseThrow(() -> new IllegalStateException("Empréstimo não encontrado"));
        jdbc.update("UPDATE emprestimos SET status = 'CANCELADO' WHERE id = ?", emprestimoId);
        exemplarRepository.definirDisponibilidade(e.getExemplar().getId(), true);
    }

    public void renovar(Long emprestimoId, LocalDateTime novaPrevista) {
        jdbc.update("UPDATE emprestimos SET renovacoes = renovacoes + 1, data_prevista_devolucao = ? WHERE id = ?",
            novaPrevista, emprestimoId);
    }

    private void vincular(Emprestimo e) {
        Long[] ids = jdbc.queryForObject(
            "SELECT leitor_id, exemplar_id FROM emprestimos WHERE id = ?",
            (rs, rowNum) -> new Long[] {rs.getLong("leitor_id"), rs.getLong("exemplar_id")}, e.getId());
        e.setLeitor(leitorRepository.buscarPorId(ids[0]).orElse(null));
        e.setExemplar(exemplarRepository.buscarPorId(ids[1]).orElse(null));
    }

    public List<Object[]> contarPorMes(int meses) {
        return jdbc.query(
            "SELECT to_char(date_trunc('month', data_emprestimo), 'MM/YYYY') AS mes, COUNT(*) AS total " +
            "FROM emprestimos WHERE data_emprestimo >= date_trunc('month', NOW()) - make_interval(months => ?) " +
            "GROUP BY date_trunc('month', data_emprestimo) ORDER BY date_trunc('month', data_emprestimo)",
            (rs, rowNum) -> new Object[] {rs.getString("mes"), rs.getLong("total")}, meses - 1);
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
