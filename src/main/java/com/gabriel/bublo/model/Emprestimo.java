package com.gabriel.bublo.model;

import java.time.LocalDateTime;

/**
 * Empréstimo de um exemplar específico para um leitor.
 */
public class Emprestimo {

    private Long id;
    private Leitor leitor;
    private Exemplar exemplar;
    private LocalDateTime dataEmprestimo;
    private LocalDateTime dataPrevistaDevolucao;
    private LocalDateTime dataDevolucaoReal;
    private Integer renovacoes;
    private String status; // ATIVO, DEVOLVIDO, ATRASADO, CANCELADO
    private String observacoes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public void setLeitor(Leitor leitor) {
        this.leitor = leitor;
    }

    public Exemplar getExemplar() {
        return exemplar;
    }

    public void setExemplar(Exemplar exemplar) {
        this.exemplar = exemplar;
    }

    public LocalDateTime getDataEmprestimo() {
        return dataEmprestimo;
    }

    public void setDataEmprestimo(LocalDateTime dataEmprestimo) {
        this.dataEmprestimo = dataEmprestimo;
    }

    public LocalDateTime getDataPrevistaDevolucao() {
        return dataPrevistaDevolucao;
    }

    public void setDataPrevistaDevolucao(LocalDateTime dataPrevistaDevolucao) {
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
    }

    public LocalDateTime getDataDevolucaoReal() {
        return dataDevolucaoReal;
    }

    public void setDataDevolucaoReal(LocalDateTime dataDevolucaoReal) {
        this.dataDevolucaoReal = dataDevolucaoReal;
    }

    public Integer getRenovacoes() {
        return renovacoes;
    }

    public void setRenovacoes(Integer renovacoes) {
        this.renovacoes = renovacoes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
