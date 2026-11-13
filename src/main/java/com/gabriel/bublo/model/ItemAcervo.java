package com.gabriel.bublo.model;

/**
 * Classe base abstrata para itens do acervo.
 * Demonstra herança e abstração: todos os materiais compartilham
 * campos e comportamento comuns, mas cada tipo tem atributos próprios.
 */
public abstract class ItemAcervo {

    private Long id;
    private TipoMaterial tipo;
    private String titulo;
    private String descricao;
    private String editora;
    private Integer ano;
    private java.time.LocalDate dataPublicacao;
    private Integer paginas;
    private String idioma;
    private Categoria categoria;
    private java.util.List<Autor> autores;

    public ItemAcervo() {
        this.autores = new java.util.ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoMaterial getTipo() {
        return tipo;
    }

    public void setTipo(TipoMaterial tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public java.time.LocalDate getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(java.time.LocalDate dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

    public Integer getPaginas() {
        return paginas;
    }

    public void setPaginas(Integer paginas) {
        this.paginas = paginas;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public java.util.List<Autor> getAutores() {
        return autores;
    }

    public void setAutores(java.util.List<Autor> autores) {
        this.autores = autores != null ? autores : new java.util.ArrayList<>();
    }

    /**
     * Polimorfismo: cada subclasse define sua capa genérica local.
     */
    public abstract String getCapaGenerica();

    /**
     * Polimorfismo: cada subclasse define o rótulo do campo específico.
     */
    public abstract String getCampoEspecificoLabel();

    /**
     * Polimorfismo: cada subclasse exibe seu valor específico.
     */
    public abstract String getCampoEspecificoValor();
}
