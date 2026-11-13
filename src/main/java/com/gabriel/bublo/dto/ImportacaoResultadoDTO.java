package com.gabriel.bublo.dto;

/**
 * DTO de resultado de importação CSV de leitores.
 */
public class ImportacaoResultadoDTO {

    private int totalProcessado;
    private int importados;
    private int rejeitados;
    private java.util.List<LinhaErroDTO> erros;

    public ImportacaoResultadoDTO() {
        this.erros = new java.util.ArrayList<>();
    }

    public int getTotalProcessado() { return totalProcessado; }
    public void setTotalProcessado(int totalProcessado) { this.totalProcessado = totalProcessado; }
    public int getImportados() { return importados; }
    public void setImportados(int importados) { this.importados = importados; }
    public int getRejeitados() { return rejeitados; }
    public void setRejeitados(int rejeitados) { this.rejeitados = rejeitados; }
    public java.util.List<LinhaErroDTO> getErros() { return erros; }
    public void setErros(java.util.List<LinhaErroDTO> erros) { this.erros = erros; }
}
