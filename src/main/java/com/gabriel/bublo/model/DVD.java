package com.gabriel.bublo.model;

public class DVD extends ItemAcervo {

    private Integer duracaoMinutos;

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    @Override
    public String getCapaGenerica() {
        return "capa-dvd.svg";
    }

    @Override
    public String getCampoEspecificoLabel() {
        return "Duração";
    }

    @Override
    public String getCampoEspecificoValor() {
        return duracaoMinutos != null ? duracaoMinutos + " min" : "";
    }
}
