package com.gabriel.bublo.dto;

import java.time.LocalDateTime;

public class MultaDTO {
    private Long id;
    private Long leitorId;
    private String leitorNome;
    private String leitorMatricula;
    public String getLeitorNome() { return leitorNome; }
    public void setLeitorNome(String leitorNome) { this.leitorNome = leitorNome; }
    public String getLeitorMatricula() { return leitorMatricula; }
    public void setLeitorMatricula(String leitorMatricula) { this.leitorMatricula = leitorMatricula; }
    private java.math.BigDecimal valor;
    private java.math.BigDecimal valorPago;
    private java.math.BigDecimal saldoPendente;
    private String status;
    private java.time.LocalDateTime dataGeracao;
    private Integer diasAtraso;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeitorId() { return leitorId; }
    public void setLeitorId(Long leitorId) { this.leitorId = leitorId; }
    public java.math.BigDecimal getValor() { return valor; }
    public void setValor(java.math.BigDecimal valor) { this.valor = valor; }
    public java.math.BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(java.math.BigDecimal valorPago) { this.valorPago = valorPago; }
    public java.math.BigDecimal getSaldoPendente() { return saldoPendente; }
    public void setSaldoPendente(java.math.BigDecimal saldoPendente) { this.saldoPendente = saldoPendente; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.time.LocalDateTime getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(java.time.LocalDateTime dataGeracao) { this.dataGeracao = dataGeracao; }
    public Integer getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(Integer diasAtraso) { this.diasAtraso = diasAtraso; }
}
