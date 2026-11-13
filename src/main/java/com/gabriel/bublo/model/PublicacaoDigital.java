package com.gabriel.bublo.model;

public class PublicacaoDigital extends ItemAcervo {

    private String doi;
    private String urlExterna;

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrlExterna() {
        return urlExterna;
    }

    public void setUrlExterna(String urlExterna) {
        this.urlExterna = urlExterna;
    }

    @Override
    public String getCapaGenerica() {
        return "capa-digital.svg";
    }

    @Override
    public String getCampoEspecificoLabel() {
        return "DOI";
    }

    @Override
    public String getCampoEspecificoValor() {
        return doi != null ? doi : "";
    }
}
