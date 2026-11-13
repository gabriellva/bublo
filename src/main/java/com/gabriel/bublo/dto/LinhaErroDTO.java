package com.gabriel.bublo.dto;

/**
 * Erro de uma linha na importação CSV.
 */
public class LinhaErroDTO {

    private int linha;
    private String motivo;

    public LinhaErroDTO() {}

    public LinhaErroDTO(int linha, String motivo) {
        this.linha = linha;
        this.motivo = motivo;
    }

    public int getLinha() { return linha; }
    public void setLinha(int linha) { this.linha = linha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
