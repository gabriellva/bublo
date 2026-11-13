package com.gabriel.bublo.dto;

import java.time.LocalDateTime;

public class ReservaDTO {
    private Long id;
    private Long leitorId;
    private Long itemId;
    private Integer posicao;
    private String leitorNome;
    private String itemTitulo;
    public String getLeitorNome() { return leitorNome; }
    public void setLeitorNome(String leitorNome) { this.leitorNome = leitorNome; }
    public String getItemTitulo() { return itemTitulo; }
    public void setItemTitulo(String itemTitulo) { this.itemTitulo = itemTitulo; }
    private java.time.LocalDateTime prazoRetirada;
    private String status;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeitorId() { return leitorId; }
    public void setLeitorId(Long leitorId) { this.leitorId = leitorId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Integer getPosicao() { return posicao; }
    public void setPosicao(Integer posicao) { this.posicao = posicao; }
    public java.time.LocalDateTime getPrazoRetirada() { return prazoRetirada; }
    public void setPrazoRetirada(java.time.LocalDateTime prazoRetirada) { this.prazoRetirada = prazoRetirada; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
