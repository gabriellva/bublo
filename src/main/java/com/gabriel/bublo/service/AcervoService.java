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
import com.gabriel.bublo.repository.AuditoriaRepository;
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
    private static final int TAMANHO_MAXIMO_AUTOR = 150;
    private static final long TAMANHO_MAXIMO_CAPA = 2L * 1024 * 1024;

    private final ItemAcervoRepository itemAcervoRepository;
    private final ExemplarRepository exemplarRepository;
    private final AuditoriaRepository auditoriaRepository;

    public AcervoService(ItemAcervoRepository itemAcervoRepository,
                         ExemplarRepository exemplarRepository,
                         AuditoriaRepository auditoriaRepository) {
        this.itemAcervoRepository = itemAcervoRepository;
        this.exemplarRepository = exemplarRepository;
        this.auditoriaRepository = auditoriaRepository;
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
        return salvar(dto, null, false, adminUsuario);
    }

    /**
     * Salva o material e, opcionalmente, a capa: um arquivo novo substitui a capa atual;
     * {@code removerCapa} apaga a capa atual (ignorado se um arquivo novo foi enviado).
     */
    @Transactional
    public ItemAcervoDTO salvar(ItemAcervoDTO dto, org.springframework.web.multipart.MultipartFile capa,
                                boolean removerCapa, String adminUsuario) {
        boolean novo = dto.getId() == null;
        boolean temArquivo = capa != null && !capa.isEmpty();
        // Valida a imagem antes de gravar qualquer coisa.
        byte[] dadosCapa = null;
        String tipoCapa = null;
        if (temArquivo) {
            dadosCapa = lerCapa(capa);
            tipoCapa = tipoDaImagem(dadosCapa);
        }
        if (!novo) {
            // Trocar o tipo apagaria campos próprios (ISBN, ISSN...) e poderia deixar exemplares em material digital.
            ItemAcervo atual = itemAcervoRepository.buscarPorId(dto.getId())
                .orElseThrow(() -> new IllegalStateException("Item não encontrado"));
            if (!atual.getTipo().name().equals(dto.getTipo())) {
                throw new IllegalStateException("O tipo do material não pode ser alterado depois do cadastro.");
            }
        }
        ItemAcervo item = construirItem(dto);
        try {
            item = itemAcervoRepository.salvar(item);
        } catch (DuplicateKeyException e) {
            String causa = String.valueOf(e.getMessage());
            if (causa.contains("idx_item_acervo_isbn") || causa.contains("idx_item_acervo_issn")) {
                throw new IllegalStateException("Já existe um material cadastrado com este ISBN/ISSN.");
            }
            throw e;
        }
        if (temArquivo) {
            itemAcervoRepository.salvarCapa(item.getId(), dadosCapa, tipoCapa);
        } else if (removerCapa && !novo) {
            itemAcervoRepository.removerCapa(item.getId());
        }
        if (temArquivo || removerCapa) {
            item = itemAcervoRepository.buscarPorId(item.getId()).orElse(item);
        }
        auditoriaRepository.registrar(adminUsuario, novo ? "CADASTRO_MATERIAL" : "EDICAO_MATERIAL", "Acervo",
            "Material #" + item.getId(), null, item.getTitulo() + (temArquivo ? " (capa enviada)" : removerCapa ? " (capa removida)" : ""), null);
        return toDTO(item);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ItemAcervoRepository.Capa> buscarCapa(Long itemId) {
        return itemAcervoRepository.buscarCapa(itemId);
    }

    private byte[] lerCapa(org.springframework.web.multipart.MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO_CAPA) {
            throw new IllegalStateException("A capa deve ter no máximo 2 MB.");
        }
        try {
            return arquivo.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Não foi possível ler a imagem da capa.");
        }
    }

    /**
     * Descobre o tipo pelos primeiros bytes do arquivo (e não pelo nome ou pelo Content-Type, que o
     * cliente controla). Só JPG, PNG e WebP: SVG ficou de fora porque pode carregar script.
     */
    static String tipoDaImagem(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
            && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        throw new IllegalStateException("A capa deve ser uma imagem JPG, PNG ou WebP.");
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
        cadastrarExemplaresLote(itemId, 1, adminUsuario);
    }

    @Transactional
    public void cadastrarExemplaresLote(Long itemId, int quantidade, String adminUsuario) {
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA_LOTE) {
            throw new IllegalStateException("Informe uma quantidade entre 1 e " + QUANTIDADE_MAXIMA_LOTE + ".");
        }
        ItemAcervo item = itemAcervoRepository.buscarPorId(itemId)
            .orElseThrow(() -> new IllegalStateException("Item não encontrado"));
        if (!itemAcervoRepository.estaAtivo(itemId)) {
            throw new IllegalStateException("Material excluído do acervo. Não é possível cadastrar exemplares.");
        }
        if (item.getTipo() == TipoMaterial.DIGITAL) {
            throw new IllegalStateException("Publicações digitais não possuem exemplares físicos.");
        }
        exemplarRepository.salvarLote(item, quantidade);
        auditoriaRepository.registrar(adminUsuario, "CADASTRO_EXEMPLAR", "Acervo", "Material #" + itemId,
            null, quantidade + " exemplar(es) de " + item.getTitulo(), null);
    }

    /**
     * Exclui um exemplar. Sem histórico de empréstimos ele é apagado; com histórico recebe baixa
     * (some das listas, o histórico fica). Exemplar emprestado não pode sair do acervo.
     * @return true se foi apagado de vez, false se recebeu baixa
     */
    @Transactional
    public boolean excluirExemplar(Long exemplarId, String adminUsuario) {
        Exemplar ex = exemplarRepository.buscarPorId(exemplarId)
            .orElseThrow(() -> new IllegalStateException("Exemplar não encontrado"));
        if (ex.isBaixado()) {
            throw new IllegalStateException("Este exemplar já foi excluído do acervo.");
        }
        if (!Boolean.TRUE.equals(ex.getDisponivel())) {
            throw new IllegalStateException("O exemplar " + ex.getTombamento()
                + " está emprestado. Registre a devolução antes de excluí-lo.");
        }
        Long itemId = ex.getItemAcervoId();
        if (exemplarRepository.contarPorItem(itemId) <= 1 && itemAcervoRepository.existeReservaPendente(itemId)) {
            throw new IllegalStateException("Este é o último exemplar do material e há reservas aguardando. "
                + "Cancele as reservas antes de excluí-lo.");
        }
        boolean apagado = !exemplarRepository.temHistoricoDeEmprestimos(exemplarId);
        if (apagado) {
            exemplarRepository.excluir(exemplarId);
        } else {
            exemplarRepository.baixar(exemplarId, "Excluído pelo administrador");
        }
        auditoriaRepository.registrar(adminUsuario, apagado ? "EXCLUSAO_EXEMPLAR" : "BAIXA_EXEMPLAR", "Acervo",
            ex.getTombamento(), "disponível", apagado ? "excluído" : "baixado (histórico preservado)", null);
        return apagado;
    }

    @Transactional
    public void excluir(Long id, String adminUsuario, String justificativa) {
        ItemAcervo item = itemAcervoRepository.buscarPorId(id)
            .orElseThrow(() -> new IllegalStateException("Item não encontrado"));
        if (!itemAcervoRepository.estaAtivo(id)) {
            throw new IllegalStateException("Este material já foi excluído.");
        }
        if (itemAcervoRepository.existeEmprestimoAtivo(id)) {
            throw new IllegalStateException("Não é possível excluir: existem empréstimos ativos");
        }
        if (itemAcervoRepository.existeReservaPendente(id)) {
            throw new IllegalStateException("Não é possível excluir: existem reservas pendentes");
        }
        itemAcervoRepository.deletar(id);
        auditoriaRepository.registrar(adminUsuario, "EXCLUSAO_MATERIAL", "Acervo", "Material #" + id,
            item.getTitulo(), null, justificativa);
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
        // Mesmo autor digitado de formas diferentes ("Machado", "machado ") é um só: indexa por id.
        java.util.Map<Long, Autor> autores = new java.util.LinkedHashMap<>();
        if (dto.getAutorIds() != null) {
            for (Long id : dto.getAutorIds()) {
                Autor a = itemAcervoRepository.buscarAutorPorId(id);
                if (a != null) autores.putIfAbsent(a.getId(), a);
            }
        }
        if (dto.getNovosAutores() != null) {
            for (String bruto : dto.getNovosAutores()) {
                if (bruto == null || bruto.isBlank()) continue;
                String nome = bruto.trim().replaceAll("\\s+", " ");
                if (nome.length() > TAMANHO_MAXIMO_AUTOR) {
                    throw new IllegalStateException("O nome do autor deve ter no máximo " + TAMANHO_MAXIMO_AUTOR + " caracteres.");
                }
                Autor a = new Autor();
                a.setNome(nome);
                a.setId(itemAcervoRepository.criarOuReutilizarAutor(nome));
                autores.putIfAbsent(a.getId(), a);
            }
        }
        item.setAutores(new ArrayList<>(autores.values()));
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
        dto.setTipoRotulo(item.getTipo().getLabel());
        dto.setTitulo(item.getTitulo());
        dto.setTemCapa(item.isTemCapa());
        dto.setCapaVersao(item.getCapaVersao());
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
