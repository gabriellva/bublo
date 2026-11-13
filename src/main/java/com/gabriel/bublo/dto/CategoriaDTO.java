package com.gabriel.bublo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de cadastro/edição de categoria.
 */
public class CategoriaDTO {

    private Long id;

    @NotBlank(message = "Nome da categoria é obrigatório")
    @Size(max = 100)
    private String nome;

    private Boolean ativo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
