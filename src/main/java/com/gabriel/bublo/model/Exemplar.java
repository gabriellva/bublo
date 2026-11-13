package com.gabriel.bublo.model;

import java.time.LocalDateTime;

/**
 * Exemplar físico de um item do acervo.
 * Cada exemplar possui tombamento único, sequencial e imutável.
 */
public class Exemplar {

    private Long id;
    private Long itemAcervoId;
    private ItemAcervo itemAcervo;
    private String tombamento;
    private Boolean disponivel;
    private String observacoes;
    private LocalDateTime dataCadastro;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemAcervoId() {
        return itemAcervoId;
    }

    public void setItemAcervoId(Long itemAcervoId) {
        this.itemAcervoId = itemAcervoId;
    }

    public ItemAcervo getItemAcervo() {
        return itemAcervo;
    }

    public void setItemAcervo(ItemAcervo itemAcervo) {
        this.itemAcervo = itemAcervo;
    }

    public String getTombamento() {
        return tombamento;
    }

    public void setTombamento(String tombamento) {
        this.tombamento = tombamento;
    }

    public Boolean getDisponivel() {
        return disponivel;
    }

    public void setDisponivel(Boolean disponivel) {
        this.disponivel = disponivel;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
