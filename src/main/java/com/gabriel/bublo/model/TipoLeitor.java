package com.gabriel.bublo.model;

/**
 * Tipo de leitor.
 */
public enum TipoLeitor {
    ALUNO("Aluno"),
    PROFESSOR("Professor");

    private final String label;

    TipoLeitor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
