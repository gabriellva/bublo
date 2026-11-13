package com.gabriel.bublo.model;

/**
 * Configuração do sistema (chave-valor).
 */
public class Configuracao {

    private String chave;
    private String valor;
    private java.time.LocalDateTime atualizadoEm;
    private String atualizadoPor;

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public java.time.LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(java.time.LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }

    public void setAtualizadoPor(String atualizadoPor) {
        this.atualizadoPor = atualizadoPor;
    }
}
