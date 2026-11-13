package com.gabriel.bublo.dto;

import java.time.LocalDateTime;

public class EmprestimoResumoDTO {
    private Long id;
    private String status;
    private boolean atrasado;
    private Integer renovacoes;
    private Long leitorId;
    private String leitorNome;
    private String leitorMatricula;
    private Long exemplarId;
    private String tombamento;
    private String titulo;
    private LocalDateTime dataEmprestimo;
    private LocalDateTime dataPrevistaDevolucao;
    private LocalDateTime dataDevolucaoReal;
    public boolean isAtrasado() { return atrasado; }
    public void setAtrasado(boolean atrasado) { this.atrasado = atrasado; }
    public Integer getRenovacoes() { return renovacoes; }
    public void setRenovacoes(Integer renovacoes) { this.renovacoes = renovacoes; }
    public Long getLeitorId() { return leitorId; }
    public void setLeitorId(Long leitorId) { this.leitorId = leitorId; }
    public String getLeitorNome() { return leitorNome; }
    public void setLeitorNome(String leitorNome) { this.leitorNome = leitorNome; }
    public String getLeitorMatricula() { return leitorMatricula; }
    public void setLeitorMatricula(String leitorMatricula) { this.leitorMatricula = leitorMatricula; }
    public Long getExemplarId() { return exemplarId; }
    public void setExemplarId(Long exemplarId) { this.exemplarId = exemplarId; }
    public String getTombamento() { return tombamento; }
    public void setTombamento(String tombamento) { this.tombamento = tombamento; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public LocalDateTime getDataDevolucaoReal() { return dataDevolucaoReal; }
    public void setDataDevolucaoReal(LocalDateTime dataDevolucaoReal) { this.dataDevolucaoReal = dataDevolucaoReal; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDateTime dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public LocalDateTime getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDateTime dataPrevistaDevolucao) { this.dataPrevistaDevolucao = dataPrevistaDevolucao; }
}
