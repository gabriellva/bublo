package com.gabriel.bublo.dto;

import java.math.BigDecimal;

/**
 * DTO de indicador do dashboard.
 */
public class IndicadorDTO {

    private String rotulo;
    private String valor;
    private String subtitulo;
    private String cor;

    public IndicadorDTO() {}

    public IndicadorDTO(String rotulo, String valor, String subtitulo, String cor) {
        this.rotulo = rotulo;
        this.valor = valor;
        this.subtitulo = subtitulo;
        this.cor = cor;
    }

    public String getRotulo() { return rotulo; }
    public void setRotulo(String rotulo) { this.rotulo = rotulo; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getSubtitulo() { return subtitulo; }
    public void setSubtitulo(String subtitulo) { this.subtitulo = subtitulo; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
}
