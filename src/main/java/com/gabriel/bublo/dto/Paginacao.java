package com.gabriel.bublo.dto;

import java.util.List;

/**
 * Resultado paginado genérico.
 * @param <T> tipo do conteúdo
 */
public class Paginacao<T> {

    private List<T> conteudo;
    private long total;
    private int pagina;
    private int tamanho;
    private int totalPaginas;

    public Paginacao() {}

    public Paginacao(List<T> conteudo, long total, int pagina, int tamanho) {
        this.conteudo = conteudo;
        this.total = total;
        this.pagina = pagina;
        this.tamanho = tamanho;
        this.totalPaginas = (int) Math.ceil((double) total / Math.max(tamanho, 1));
    }

    public List<T> getConteudo() { return conteudo; }
    public void setConteudo(List<T> conteudo) { this.conteudo = conteudo; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }
    public int getTamanho() { return tamanho; }
    public void setTamanho(int tamanho) { this.tamanho = tamanho; }
    public int getTotalPaginas() { return totalPaginas; }
    public void setTotalPaginas(int totalPaginas) { this.totalPaginas = totalPaginas; }

    public <R> Paginacao<R> mapConteudo(java.util.function.Function<T, R> funcao) {
        List<R> mapeado = conteudo == null ? null : conteudo.stream().map(funcao).collect(java.util.stream.Collectors.toList());
        Paginacao<R> novo = new Paginacao<>();
        novo.setConteudo(mapeado);
        novo.setTotal(total);
        novo.setPagina(pagina);
        novo.setTamanho(tamanho);
        novo.setTotalPaginas(totalPaginas);
        return novo;
    }
}
