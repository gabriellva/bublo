package com.gabriel.bublo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Multa gerada por atraso na devolução.
 */
public class Multa {

    private Long id;
    private Long emprestimoId;
    private Long leitorId;
    private BigDecimal valor;
    private BigDecimal taxaDiaria;
    private Integer diasAtraso;
    private LocalDateTime dataGeracao;
    private String status; // PENDENTE, PAGA_PARCIAL, PAGA, CANCELADA
    private BigDecimal valorPago;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmprestimoId() {
        return emprestimoId;
    }

    public void setEmprestimoId(Long emprestimoId) {
        this.emprestimoId = emprestimoId;
    }

    public Long getLeitorId() {
        return leitorId;
    }

    public void setLeitorId(Long leitorId) {
        this.leitorId = leitorId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getTaxaDiaria() {
        return taxaDiaria;
    }

    public void setTaxaDiaria(BigDecimal taxaDiaria) {
        this.taxaDiaria = taxaDiaria;
    }

    public Integer getDiasAtraso() {
        return diasAtraso;
    }

    public void setDiasAtraso(Integer diasAtraso) {
        this.diasAtraso = diasAtraso;
    }

    public LocalDateTime getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(LocalDateTime dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public BigDecimal getSaldoPendente() {
        if (valor == null) return BigDecimal.ZERO;
        return valor.subtract(valorPago != null ? valorPago : BigDecimal.ZERO);
    }
}
