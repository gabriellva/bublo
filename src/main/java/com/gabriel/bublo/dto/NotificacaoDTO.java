package com.gabriel.bublo.dto;

import java.time.LocalDateTime;

public class NotificacaoDTO {
    private Long id;
    private String categoria;
    private String prioridade;
    private String titulo;
    private String mensagem;
    private Boolean lida;
    private Boolean resolvida;
    private java.time.LocalDateTime dataCriacao;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public Boolean getLida() { return lida; }
    public void setLida(Boolean lida) { this.lida = lida; }
    public Boolean getResolvida() { return resolvida; }
    public void setResolvida(Boolean resolvida) { this.resolvida = resolvida; }
    public java.time.LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(java.time.LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
