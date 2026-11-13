package com.gabriel.bublo.model;

/**
 * Tipo de material do acervo.
 */
public enum TipoMaterial {
    LIVRO("Livro"),
    REVISTA("Revista / Periódico"),
    DVD("DVD"),
    DIGITAL("Publicação Digital");

    private final String label;

    TipoMaterial(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
