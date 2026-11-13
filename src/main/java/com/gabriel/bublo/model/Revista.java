package com.gabriel.bublo.model;

public class Revista extends ItemAcervo {

    private String issn;
    private Integer volume;

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    @Override
    public String getCapaGenerica() {
        return "capa-revista.svg";
    }

    @Override
    public String getCampoEspecificoLabel() {
        return "ISSN";
    }

    @Override
    public String getCampoEspecificoValor() {
        return (issn != null ? issn : "") + (volume != null ? " · Vol. " + volume : "");
    }
}
