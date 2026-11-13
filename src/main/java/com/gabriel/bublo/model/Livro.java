package com.gabriel.bublo.model;

public class Livro extends ItemAcervo {

    private String isbn;

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @Override
    public String getCapaGenerica() {
        return "capa-livro.svg";
    }

    @Override
    public String getCampoEspecificoLabel() {
        return "ISBN";
    }

    @Override
    public String getCampoEspecificoValor() {
        return isbn;
    }
}
