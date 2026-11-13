package com.gabriel.bublo.model;

import java.time.LocalDateTime;

/**
 * Notificação do sistema.
 */
public class Notificacao {

    private Long id;
    private String categoria;
    private String prioridade;
    private String titulo;
    private String mensagem;
    private String registroTipo;
    private Long registroId;
    private Boolean lida;
    private Boolean resolvida;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataResolucao;

    public Notificacao() {}

    public Notificacao(Long id, String categoria, String prioridade, String titulo,
                       String mensagem, String registroTipo, Long registroId) {
        this.id = id;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.registroTipo = registroTipo;
        this.registroId = registroId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getRegistroTipo() {
        return registroTipo;
    }

    public void setRegistroTipo(String registroTipo) {
        this.registroTipo = registroTipo;
    }

    public Long getRegistroId() {
        return registroId;
    }

    public void setRegistroId(Long registroId) {
        this.registroId = registroId;
    }

    public Boolean getLida() {
        return lida;
    }

    public void setLida(Boolean lida) {
        this.lida = lida;
    }

    public Boolean getResolvida() {
        return resolvida;
    }

    public void setResolvida(Boolean resolvida) {
        this.resolvida = resolvida;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataResolucao() {
        return dataResolucao;
    }

    public void setDataResolucao(LocalDateTime dataResolucao) {
        this.dataResolucao = dataResolucao;
    }
}
