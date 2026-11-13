package com.gabriel.bublo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de cadastro/edição de item do acervo.
 */
public class ItemAcervoDTO {

    private Long id;

    @NotBlank(message = "Tipo de material é obrigatório")
    private String tipo; // LIVRO, REVISTA, DVD, DIGITAL

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255)
    private String titulo;

    @Size(max = 2000)
    private String descricao;

    @Size(max = 150)
    private String editora;

    private Integer ano;

    private String dataPublicacao;

    private Integer paginas;

    @Size(max = 50)
    private String idioma;

    private Long categoriaId;

    @Size(max = 50)
    private String isbn;

    @Size(max = 50)
    private String issn;

    private Integer duracaoMinutos;
    private Integer volume;

    @Size(max = 200)
    private String doi;

    @Size(max = 500)
    private String urlExterna;

    // IDs de autores (novos ou existentes)
    private java.util.List<Long> autorIds;

    // Novos nomes de autores digitados durante o preenchimento
    private java.util.List<String> novosAutores;

    private String categoriaNome;
    private java.util.List<String> autoresNomes = new java.util.ArrayList<>();
    private long totalExemplares;
    private long exemplaresDisponiveis;

    // Getters e setters
    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }
    public java.util.List<String> getAutoresNomes() { return autoresNomes; }
    public void setAutoresNomes(java.util.List<String> autoresNomes) { this.autoresNomes = autoresNomes; }
    public long getTotalExemplares() { return totalExemplares; }
    public void setTotalExemplares(long totalExemplares) { this.totalExemplares = totalExemplares; }
    public long getExemplaresDisponiveis() { return exemplaresDisponiveis; }
    public void setExemplaresDisponiveis(long exemplaresDisponiveis) { this.exemplaresDisponiveis = exemplaresDisponiveis; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEditora() { return editora; }
    public void setEditora(String editora) { this.editora = editora; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public String getDataPublicacao() { return dataPublicacao; }
    public void setDataPublicacao(String dataPublicacao) { this.dataPublicacao = dataPublicacao; }
    public Integer getPaginas() { return paginas; }
    public void setPaginas(Integer paginas) { this.paginas = paginas; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public Integer getVolume() { return volume; }
    public void setVolume(Integer volume) { this.volume = volume; }
    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }
    public String getUrlExterna() { return urlExterna; }
    public void setUrlExterna(String urlExterna) { this.urlExterna = urlExterna; }
    public java.util.List<Long> getAutorIds() { return autorIds; }
    public void setAutorIds(java.util.List<Long> autorIds) { this.autorIds = autorIds; }
    public java.util.List<String> getNovosAutores() { return novosAutores; }
    public void setNovosAutores(java.util.List<String> novosAutores) { this.novosAutores = novosAutores; }
}
