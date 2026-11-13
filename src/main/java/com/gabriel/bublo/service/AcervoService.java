package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ItemAcervoDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Autor;
import com.gabriel.bublo.model.ItemAcervo;
import com.gabriel.bublo.model.TipoMaterial;
import com.gabriel.bublo.model.Livro;
import com.gabriel.bublo.model.Revista;
import com.gabriel.bublo.model.DVD;
import com.gabriel.bublo.model.PublicacaoDigital;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.model.Categoria;
import com.gabriel.bublo.model.Exemplar;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço do acervo: cadastro, edição, busca, exemplares.
 */
@Service
public class AcervoService {

    private static final int QUANTIDADE_MAXIMA_LOTE = 100;
    private static final String IDIOMA_PADRAO = "Português";

    private final ItemAcervoRepository itemAcervoRepository;
    private final ExemplarRepository exemplarRepository;

    public AcervoService(ItemAcervoRepository itemAcervoRepository,
                         ExemplarRepository exemplarRepository) {
        this.itemAcervoRepository = itemAcervoRepository;
        this.exemplarRepository = exemplarRepository;
    }

    @Transactional(readOnly = true)
    public ItemAcervoDTO buscarDTO(Long id) {
        if (!itemAcervoRepository.estaAtivo(id)) return null;
        ItemAcervo item = itemAcervoRepository.buscarPorId(id).orElse(null);
        return item != null ? toDTO(item) : null;
    }

    @Transactional(readOnly = true)
    public ItemAcervo buscarPorId(Long id) {
        return itemAcervoRepository.buscarPorId(id).orElse(null);
    }

    @Transactional
    public ItemAcervoDTO salvar(ItemAcervoDTO dto, String adminUsuario) {
        ItemAcervo item = construirItem(dto);
        try {
            item = itemAcervoRepository.salvar(item);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Já existe um material cadastrado com este ISBN/ISSN.");
        }
        return toDTO(item);
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return itemAcervoRepository.listarCategorias();
    }

    @Transactional(readOnly = true)
    public List<com.gabriel.bublo.dto.ExemplarOpcaoDTO> listarExemplaresDisponiveis() {
        return exemplarRepository.listarDisponiveisParaEmprestimo();
    }

    @Transactional(readOnly = true)
    public List<Exemplar> listarExemplares(Long itemId) {
        return exemplarRepository.listarPorItem(itemId);
    }

    @Transactional(readOnly = true)
    public Paginacao<ItemAcervoDTO> buscar(String termo, Long categoriaId, TipoMaterial tipo,
                                           Boolean disponivel, int pagina, int tamanho, String ordenacao) {
        return itemAcervoRepository.buscar(termo, categoriaId, tipo, disponivel, pagina, tamanho, ordenacao)
            .mapConteudo(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ItemAcervoDTO> listarTodos() {
        return itemAcervoRepository.listarTodos().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Autor> sugestoesAutores(String termo) {
        return itemAcervoRepository.buscarAutoresSugestao(termo);
    }

    @Transactional(readOnly = true)
    public long totalTitulos() {
        return itemAcervoRepository.contarTitulos();
    }

    @Transactional
    public void cadastrarExemplar(Long itemId, String adminUsuario) {
        ItemAcervo item = itemAcervoRepository.buscarPorId(itemId)
            .orElseThrow(() -> new IllegalStateException("Item não encontrado"));
        exemplarRepository.salvar(item);
    }

    @Transactional
    public void cadastrarExemplaresLote(Long itemId, int quantidade, String adminUsuario) {
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA_LOTE) {
            throw new IllegalStateException("Informe uma quantidade entre 1 e " + QUANTIDADE_MAXIMA_LOTE + ".");
        }
        ItemAcervo item = itemAcervoRepository.buscarPorId(itemId)
            .orElseThrow(() -> new IllegalStateException("Item não encontrado"));
        exemplarRepository.salvarLote(item, quantidade);
    }

    @Transactional
    public void excluir(Long id, String adminUsuario, String justificativa) {
        ItemAcervo item = itemAcervoRepository.buscarPorId(id)
            .orElseThrow(() -> new IllegalStateException("Item não encontrado"));
        if (itemAcervoRepository.existeEmprestimoAtivo(id)) {
            throw new IllegalStateException("Não é possível excluir: existem empréstimos ativos");
        }
        if (itemAcervoRepository.existeReservaPendente(id)) {
            throw new IllegalStateException("Não é possível excluir: existem reservas pendentes");
        }
        itemAcervoRepository.deletar(id);
    }

    private ItemAcervo construirItem(ItemAcervoDTO dto) {
        TipoMaterial tipo;
        try {
            tipo = TipoMaterial.valueOf(dto.getTipo());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Tipo de material inválido.");
        }
        ItemAcervo item;
        switch (tipo) {
            case LIVRO -> item = new Livro();
            case REVISTA -> item = new Revista();
            case DVD -> item = new DVD();
            case DIGITAL -> item = new PublicacaoDigital();
            default -> throw new IllegalArgumentException("Tipo inválido: " + tipo);
        }
        item.setId(dto.getId());
        item.setTipo(tipo);
        item.setTitulo(dto.getTitulo());
        item.setDescricao(dto.getDescricao());
        item.setEditora(dto.getEditora());
        item.setAno(dto.getAno());
        item.setDataPublicacao(parseData(dto.getDataPublicacao()));
        item.setPaginas(dto.getPaginas());
        item.setIdioma(dto.getIdioma() == null || dto.getIdioma().isBlank() ? IDIOMA_PADRAO : dto.getIdioma().trim());
        if (dto.getCategoriaId() != null) {
            com.gabriel.bublo.model.Categoria cat = new com.gabriel.bublo.model.Categoria();
            cat.setId(dto.getCategoriaId());
            cat.setNome("");
            item.setCategoria(cat);
        }
        if (item instanceof Livro l) l.setIsbn(vazioParaNulo(dto.getIsbn()));
        if (item instanceof Revista r) {
            r.setIssn(vazioParaNulo(dto.getIssn()));
            r.setVolume(dto.getVolume());
        }
        if (item instanceof DVD d) d.setDuracaoMinutos(dto.getDuracaoMinutos());
        if (item instanceof PublicacaoDigital pd) {
            pd.setDoi(dto.getDoi());
            pd.setUrlExterna(dto.getUrlExterna());
        }
        List<Autor> autores = new ArrayList<>();
        if (dto.getAutorIds() != null) {
            for (Long id : dto.getAutorIds()) {
                Autor a = itemAcervoRepository.buscarAutorPorId(id);
                if (a != null) autores.add(a);
            }
        }
        if (dto.getNovosAutores() != null) {
            dto.getNovosAutores().removeIf(nome -> nome == null || nome.isBlank());
            for (String nome : dto.getNovosAutores()) {
                if (nome != null && !nome.isBlank()) {
                    Autor a = new Autor();
                    a.setNome(nome.trim());
                    Long id = itemAcervoRepository.criarOuReutilizarAutor(nome.trim());
                    a.setId(id);
                    autores.add(a);
                }
            }
        }
        item.setAutores(autores);
        return item;
    }

    private LocalDate parseData(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Data de publicação inválida.");
        }
    }

    private String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ItemAcervoDTO toDTO(ItemAcervo item) {
        ItemAcervoDTO dto = new ItemAcervoDTO();
        dto.setId(item.getId());
        dto.setTipo(item.getTipo().name());
        dto.setTitulo(item.getTitulo());
        dto.setDescricao(item.getDescricao());
        dto.setEditora(item.getEditora());
        dto.setAno(item.getAno());
        dto.setDataPublicacao(item.getDataPublicacao() != null ? item.getDataPublicacao().toString() : null);
        dto.setPaginas(item.getPaginas());
        dto.setIdioma(item.getIdioma());
        dto.setCategoriaId(item.getCategoria() != null ? item.getCategoria().getId() : null);
        if (item instanceof Livro l) dto.setIsbn(l.getIsbn());
        if (item instanceof Revista r) dto.setIssn(r.getIssn());
        if (item instanceof Revista r) dto.setVolume(r.getVolume());
        if (item instanceof DVD d) dto.setDuracaoMinutos(d.getDuracaoMinutos());
        if (item instanceof PublicacaoDigital pd) {
            dto.setDoi(pd.getDoi());
            dto.setUrlExterna(pd.getUrlExterna());
        }
        dto.setAutorIds(item.getAutores().stream().map(Autor::getId).collect(Collectors.toList()));
        dto.setAutoresNomes(item.getAutores().stream().map(Autor::getNome).collect(Collectors.toList()));
        dto.setCategoriaNome(item.getCategoria() != null ? item.getCategoria().getNome() : null);
        if (item.getId() != null) {
            dto.setTotalExemplares(exemplarRepository.contarPorItem(item.getId()));
            dto.setExemplaresDisponiveis(exemplarRepository.contarDisponiveisPorItem(item.getId()));
        }
        return dto;
    }
}
