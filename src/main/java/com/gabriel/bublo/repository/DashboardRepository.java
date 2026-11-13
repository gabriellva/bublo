package com.gabriel.bublo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de leitura que alimentam os gráficos do dashboard.
 */
@Repository
public class DashboardRepository {

    private static final int LIMITE_RANKING = 6;
    private static final int MESES_COMPARACAO = 6;

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> emprestimosNoTempo(LocalDate de, LocalDate ate, boolean porMes) {
        String passo = porMes ? "month" : "day";
        String formato = porMes ? "MM/YYYY" : "DD/MM";
        String sql = "SELECT to_char(b.bucket, '" + formato + "') AS rotulo, COUNT(e.id) AS total " +
            "FROM generate_series(date_trunc('" + passo + "', ?::date), ?::date, interval '1 " + passo + "') AS b(bucket) " +
            "LEFT JOIN emprestimos e ON date_trunc('" + passo + "', e.data_emprestimo) = b.bucket " +
            "AND e.data_emprestimo::date BETWEEN ?::date AND ?::date " +
            "GROUP BY b.bucket ORDER BY b.bucket";
        return serie(sql, de, ate, de, ate);
    }

    public Map<String, Object> comparacaoMensal(LocalDate ate) {
        String sql = "SELECT to_char(b.mes, 'MM/YYYY') AS rotulo, " +
            "(SELECT COUNT(*) FROM emprestimos e WHERE date_trunc('month', e.data_emprestimo) = b.mes) AS emprestimos, " +
            "(SELECT COUNT(*) FROM emprestimos e WHERE e.data_devolucao_real IS NOT NULL " +
            "AND date_trunc('month', e.data_devolucao_real) = b.mes) AS devolucoes " +
            "FROM generate_series(date_trunc('month', ?::date) - make_interval(months => ?), date_trunc('month', ?::date), interval '1 month') AS b(mes) " +
            "ORDER BY b.mes";
        List<String> rotulos = new ArrayList<>();
        List<Long> emprestimos = new ArrayList<>();
        List<Long> devolucoes = new ArrayList<>();
        jdbc.query(sql, rs -> {
            rotulos.add(rs.getString("rotulo"));
            emprestimos.add(rs.getLong("emprestimos"));
            devolucoes.add(rs.getLong("devolucoes"));
        }, ate, MESES_COMPARACAO - 1, ate);
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("rotulos", rotulos);
        mapa.put("emprestimos", emprestimos);
        mapa.put("devolucoes", devolucoes);
        return mapa;
    }

    public Map<String, Object> emprestimosPorCategoria(LocalDate de, LocalDate ate) {
        String sql = "SELECT COALESCE(c.nome, 'Sem categoria') AS rotulo, COUNT(*) AS total " +
            "FROM emprestimos e JOIN exemplares x ON x.id = e.exemplar_id " +
            "JOIN item_acervo ia ON ia.id = x.item_acervo_id LEFT JOIN categorias c ON c.id = ia.categoria_id " +
            "WHERE e.data_emprestimo::date BETWEEN ?::date AND ?::date " +
            "GROUP BY c.nome ORDER BY total DESC, rotulo LIMIT ?";
        return serie(sql, de, ate, LIMITE_RANKING);
    }

    public Map<String, Object> maisEmprestados(LocalDate de, LocalDate ate) {
        String sql = "SELECT ia.titulo AS rotulo, COUNT(*) AS total " +
            "FROM emprestimos e JOIN exemplares x ON x.id = e.exemplar_id " +
            "JOIN item_acervo ia ON ia.id = x.item_acervo_id " +
            "WHERE e.data_emprestimo::date BETWEEN ?::date AND ?::date " +
            "GROUP BY ia.id, ia.titulo ORDER BY total DESC, rotulo LIMIT ?";
        return serie(sql, de, ate, LIMITE_RANKING);
    }

    public Map<String, Object> leitoresPorTipo() {
        return serie("SELECT CASE tipo WHEN 'PROFESSOR' THEN 'Professores' ELSE 'Alunos' END AS rotulo, COUNT(*) AS total " +
            "FROM leitores WHERE ativo = TRUE GROUP BY tipo ORDER BY tipo");
    }

    public Map<String, Object> disponibilidade() {
        return serie("SELECT CASE disponivel WHEN TRUE THEN 'Disponíveis' ELSE 'Emprestados' END AS rotulo, COUNT(*) AS total " +
            "FROM exemplares GROUP BY disponivel ORDER BY disponivel DESC");
    }

    public Map<String, Object> multasGeradasEPagas(LocalDate de, LocalDate ate, boolean porMes) {
        String passo = porMes ? "month" : "day";
        String formato = porMes ? "MM/YYYY" : "DD/MM";
        String sql = "SELECT to_char(b.bucket, '" + formato + "') AS rotulo, " +
            "(SELECT COALESCE(SUM(m.valor), 0) FROM multas m WHERE date_trunc('" + passo + "', m.data_geracao) = b.bucket " +
            "AND m.data_geracao::date BETWEEN ?::date AND ?::date) AS geradas, " +
            "(SELECT COALESCE(SUM(p.valor), 0) FROM pagamentos p WHERE date_trunc('" + passo + "', p.data_pagamento) = b.bucket " +
            "AND p.data_pagamento::date BETWEEN ?::date AND ?::date) AS pagas " +
            "FROM generate_series(date_trunc('" + passo + "', ?::date), ?::date, interval '1 " + passo + "') AS b(bucket) " +
            "ORDER BY b.bucket";
        List<String> rotulos = new ArrayList<>();
        List<BigDecimal> geradas = new ArrayList<>();
        List<BigDecimal> pagas = new ArrayList<>();
        jdbc.query(sql, rs -> {
            rotulos.add(rs.getString("rotulo"));
            geradas.add(rs.getBigDecimal("geradas"));
            pagas.add(rs.getBigDecimal("pagas"));
        }, de, ate, de, ate, de, ate);
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("rotulos", rotulos);
        mapa.put("geradas", geradas);
        mapa.put("pagas", pagas);
        return mapa;
    }

    public long emprestimosNoPeriodo(LocalDate de, LocalDate ate) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM emprestimos WHERE data_emprestimo::date BETWEEN ?::date AND ?::date",
            Long.class, de, ate);
    }

    public BigDecimal arrecadadoNoPeriodo(LocalDate de, LocalDate ate) {
        return jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor), 0) FROM pagamentos WHERE data_pagamento::date BETWEEN ?::date AND ?::date",
            BigDecimal.class, de, ate);
    }

    public BigDecimal saldoPendenteTotal() {
        return jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor - valor_pago), 0) FROM multas WHERE status IN ('PENDENTE', 'PAGA_PARCIAL')",
            BigDecimal.class);
    }

    private Map<String, Object> serie(String sql, Object... args) {
        List<String> rotulos = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        jdbc.query(sql, rs -> {
            rotulos.add(rs.getString("rotulo"));
            valores.add(rs.getLong("total"));
        }, args);
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("rotulos", rotulos);
        mapa.put("valores", valores);
        return mapa;
    }
}
