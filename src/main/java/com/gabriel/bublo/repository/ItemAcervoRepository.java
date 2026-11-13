package com.gabriel.bublo.repository;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Autor;
import com.gabriel.bublo.model.Categoria;
import com.gabriel.bublo.model.DVD;
import com.gabriel.bublo.model.ItemAcervo;
import com.gabriel.bublo.model.Livro;
import com.gabriel.bublo.model.PublicacaoDigital;
import com.gabriel.bublo.model.Revista;
import com.gabriel.bublo.model.TipoMaterial;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de itens do acervo (JDBC puro).
 * Persiste ItemAcervo e subclasses (Livro, Revista, DVD, PublicacaoDigital)
 * e gerencia o relacionamento muitos-para-muitos com Autores.
 */
@Repository
public class ItemAcervoRepository {

    private final JdbcTemplate jdbc;

    private static final String BASE_SELECT = "SELECT ia.id, ia.tipo, ia.titulo, ia.descricao, ia.editora, " +
        "ia.ano, ia.data_publicacao, ia.paginas, ia.volume, ia.idioma, ia.categoria_id, ia.isbn, ia.issn, " +
        "ia.duracao_minutos, ia.doi, ia.url_externa, " +
        "c.id AS cat_id, c.nome AS cat_nome, c.ativo AS cat_ativo " +
        "FROM item_acervo ia LEFT JOIN categorias c ON ia.categoria_id = c.id";

    public ItemAcervoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ItemAcervo> ITEM_MAPPER = (rs, rowNum) -> {
        TipoMaterial tipo = TipoMaterial.valueOf(rs.getString("tipo"));
        ItemAcervo item;
        switch (tipo) {
            case LIVRO -> item = new Livro();
            case REVISTA -> item = new Revista();
            case DVD -> item = new DVD();
            case DIGITAL -> item = new PublicacaoDigital();
            default -> throw new IllegalArgumentException("Tipo desconhecido: " + tipo);
        }
        item.setId(rs.getLong("id"));
        item.setTipo(tipo);
        item.setTitulo(rs.getString("titulo"));
        item.setDescricao(rs.getString("descricao"));
        item.setEditora(rs.getString("editora"));
        item.setAno(rs.getObject("ano", Integer.class));
        item.setDataPublicacao(rs.getObject("data_publicacao", LocalDate.class));
        item.setPaginas(rs.getObject("paginas", Integer.class));
        item.setIdioma(rs.getString("idioma"));

        Categoria cat = new Categoria();
        cat.setId(rs.getObject("cat_id", Long.class));
        cat.setNome(rs.getString("cat_nome"));
        cat.setAtivo(rs.getBoolean("cat_ativo"));
        item.setCategoria(cat);

        if (item instanceof Livro l) l.setIsbn(rs.getString("isbn"));
        if (item instanceof Revista r) {
            r.setIssn(rs.getString("issn"));
            r.setVolume(rs.getObject("volume", Integer.class));
        }
        if (item instanceof DVD d) d.setDuracaoMinutos(rs.getObject("duracao_minutos", Integer.class));
        if (item instanceof PublicacaoDigital pd) {
            pd.setDoi(rs.getString("doi"));
            pd.setUrlExterna(rs.getString("url_externa"));
        }
        // autores carregados separadamente
        return item;
    };

    public ItemAcervo salvar(ItemAcervo item) {
        if (item.getId() == null) {
            Long id = insertReturnId(
                "INSERT INTO item_acervo (tipo, titulo, descricao, editora, ano, data_publicacao, " +
                "paginas, idioma, categoria_id, isbn, issn, duracao_minutos, doi, url_externa, ativo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
                item.getTipo().name(), item.getTitulo(), item.getDescricao(), item.getEditora(),
                item.getAno(), item.getDataPublicacao(), item.getPaginas(), item.getIdioma(),
                item.getCategoria() != null ? item.getCategoria().getId() : null,
                isbnDe(item), issnDe(item), duracaoDe(item), doiDe(item), urlDe(item));
            item.setId(id);
        } else {
            jdbc.update(
                "UPDATE item_acervo SET tipo=?, titulo=?, descricao=?, editora=?, ano=?, data_publicacao=?, " +
                "paginas=?, idioma=?, categoria_id=?, isbn=?, issn=?, duracao_minutos=?, doi=?, url_externa=? " +
                "WHERE id=?",
                item.getTipo().name(), item.getTitulo(), item.getDescricao(), item.getEditora(),
                item.getAno(), item.getDataPublicacao(), item.getPaginas(), item.getIdioma(),
                item.getCategoria() != null ? item.getCategoria().getId() : null,
                isbnDe(item), issnDe(item), duracaoDe(item), doiDe(item), urlDe(item), item.getId());
        }
        // sincroniza autores (muitos-para-muitos)
        jdbc.update("DELETE FROM item_acervo_autor WHERE item_acervo_id = ?", item.getId());
        for (Autor a : item.getAutores()) {
            if (a.getId() == null) {
                a.setId(criarOuReutilizarAutor(a.getNome()));
            }
            jdbc.update("INSERT INTO item_acervo_autor (item_acervo_id, autor_id) VALUES (?, ?)",
                item.getId(), a.getId());
        }
        carregarAutores(item);
        return item;
    }

    public Optional<ItemAcervo> buscarPorId(Long id) {
        try {
            ItemAcervo item = jdbc.queryForObject(BASE_SELECT + " WHERE ia.id = ?", ITEM_MAPPER, id);
            if (item != null) carregarAutores(item);
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void deletar(Long id) {
        jdbc.update("DELETE FROM item_acervo WHERE id = ?", id);
    }

    public boolean existeEmprestimoAtivo(Long itemId) {
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM emprestimos e JOIN exemplares ex ON ex.id = e.exemplar_id " +
            "WHERE ex.item_acervo_id = ? AND e.status = 'ATIVO'", Integer.class, itemId);
        return c != null && c > 0;
    }

    public boolean existeReservaPendente(Long itemId) {
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM reservas WHERE item_acervo_id = ? AND status = 'AGUARDANDO'",
            Integer.class, itemId);
        return c != null && c > 0;
    }

    public Paginacao<ItemAcervo> buscar(String termo, Long categoriaId, TipoMaterial tipo,
                                        Boolean disponivel, int pagina, int tamanho, String ordenacao) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE ia.ativo = TRUE");
        if (termo != null && !termo.isBlank()) {
            where.append(" AND (ia.titulo ILIKE ? OR ia.isbn ILIKE ? OR ia.issn ILIKE ?)");
            String t = "%" + termo + "%";
            args.add(t); args.add(t); args.add(t);
        }
        if (categoriaId != null) {
            where.append(" AND ia.categoria_id = ?"); args.add(categoriaId);
        }
        if (tipo != null) {
            where.append(" AND ia.tipo = ?"); args.add(tipo.name());
        }
        if (disponivel != null) {
            where.append(" AND EXISTS (SELECT 1 FROM exemplares e WHERE e.item_acervo_id = ia.id AND e.disponivel = ?)");
            args.add(disponivel);
        }

        String sql = BASE_SELECT + " " + where +
            " ORDER BY " + (ordenacao != null ? ordenacao : "ia.titulo ASC") +
            " LIMIT ? OFFSET ?";
        args.add(tamanho);
        args.add((long) pagina * tamanho);

        List<ItemAcervo> conteudo = jdbc.query(sql, ITEM_MAPPER, args.toArray());
        conteudo.forEach(this::carregarAutores);

        StringBuilder csql = new StringBuilder("SELECT COUNT(*) FROM item_acervo ia " + where);
        Long total = jdbc.queryForObject(csql.toString(), Long.class, args.subList(0, args.size() - 2).toArray());

        return new Paginacao<>(conteudo, total, pagina, tamanho);
    }

    public List<ItemAcervo> listarTodos() {
        List<ItemAcervo> lista = jdbc.query(BASE_SELECT + " ORDER BY ia.titulo ASC", ITEM_MAPPER);
        lista.forEach(this::carregarAutores);
        return lista;
    }

    public List<Autor> buscarAutoresSugestao(String termo) {
        String sql = "SELECT id, nome, nome_normalizado FROM autores WHERE nome_normalizado ILIKE ? ORDER BY nome LIMIT 20";
        return jdbc.query(sql, (rs, rn) -> {
            Autor a = new Autor();
            a.setId(rs.getLong("id"));
            a.setNome(rs.getString("nome"));
            a.setNomeNormalizado(rs.getString("nome_normalizado"));
            return a;
        }, "%" + normalizar(termo) + "%");
    }

    public Long criarOuReutilizarAutor(String nome) {
        String normalizado = normalizar(nome);
        try {
            return jdbc.queryForObject("SELECT id FROM autores WHERE nome_normalizado = ?", Long.class, normalizado);
        } catch (EmptyResultDataAccessException e) {
            return insertReturnId("INSERT INTO autores (nome, nome_normalizado) VALUES (?, ?)", nome, normalizado);
        }
    }

    public Autor buscarAutorPorId(Long id) {
        try {
            return jdbc.queryForObject("SELECT id, nome, nome_normalizado FROM autores WHERE id = ?",
                (rs, rn) -> {
                    Autor a = new Autor();
                    a.setId(rs.getLong("id"));
                    a.setNome(rs.getString("nome"));
                    a.setNomeNormalizado(rs.getString("nome_normalizado"));
                    return a;
                }, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void carregarAutores(ItemAcervo item) {
        List<Autor> autores = jdbc.query(
            "SELECT a.id, a.nome, a.nome_normalizado FROM autores a " +
            "JOIN item_acervo_autor iaa ON iaa.autor_id = a.id WHERE iaa.item_acervo_id = ?",
            (rs, rn) -> {
                Autor a = new Autor();
                a.setId(rs.getLong("id"));
                a.setNome(rs.getString("nome"));
                a.setNomeNormalizado(rs.getString("nome_normalizado"));
                return a;
            }, item.getId());
        item.setAutores(autores);
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

    private String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private String isbnDe(ItemAcervo i) { return i instanceof Livro ? ((Livro) i).getIsbn() : null; }
    private String issnDe(ItemAcervo i) { return i instanceof Revista ? ((Revista) i).getIssn() : null; }
    private Integer duracaoDe(ItemAcervo i) { return i instanceof DVD ? ((DVD) i).getDuracaoMinutos() : null; }
    private String doiDe(ItemAcervo i) { return i instanceof PublicacaoDigital ? ((PublicacaoDigital) i).getDoi() : null; }
    private String urlDe(ItemAcervo i) { return i instanceof PublicacaoDigital ? ((PublicacaoDigital) i).getUrlExterna() : null; }
}
