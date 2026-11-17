package com.gabriel.bublo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de pagamento de uma multa.
 */
public class Pagamento {

    private Long id;
    private Long multaId;
    private BigDecimal valor;
    private String formaPagamento;
    private LocalDateTime dataPagamento;
    private String responsavel;
    private boolean estornado;
    private LocalDateTime estornadoEm;
    private String estornadoPor;
    private String estornoJustificativa;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMultaId() {
        return multaId;
    }

    public void setMultaId(Long multaId) {
        this.multaId = multaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public LocalDateTime getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDateTime dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public boolean isEstornado() {
        return estornado;
    }

    public void setEstornado(boolean estornado) {
        this.estornado = estornado;
    }

    public LocalDateTime getEstornadoEm() {
        return estornadoEm;
    }

    public void setEstornadoEm(LocalDateTime estornadoEm) {
        this.estornadoEm = estornadoEm;
    }

    public String getEstornadoPor() {
        return estornadoPor;
    }

    public void setEstornadoPor(String estornadoPor) {
        this.estornadoPor = estornadoPor;
    }

    public String getEstornoJustificativa() {
        return estornoJustificativa;
    }

    public void setEstornoJustificativa(String estornoJustificativa) {
        this.estornoJustificativa = estornoJustificativa;
    }
}
