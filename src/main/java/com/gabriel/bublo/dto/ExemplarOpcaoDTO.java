package com.gabriel.bublo.dto;

/**
 * Exemplar disponível, com dados mínimos para seleção em formulários.
 */
public record ExemplarOpcaoDTO(Long id, String tombamento, String titulo, String tipo) {

    public String getRotulo() {
        return tombamento + " — " + titulo;
    }

    public Long getId() {
        return id;
    }
}
