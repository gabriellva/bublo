package com.gabriel.bublo.dto;

import java.time.LocalDateTime;

public class EmprestimoDTO {
    private Long id;
    private Long leitorId;
    private Long exemplarId;
    private LocalDateTime dataEmprestimo;
    private LocalDateTime dataPrevistaDevolucao;
    private LocalDateTime dataDevolucaoReal;
    private Integer renovacoes;
    private String status;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeitorId() { return leitorId; }
    public void setLeitorId(Long leitorId) { this.leitorId = leitorId; }
    public Long getExemplarId() { return exemplarId; }
    public void setExemplarId(Long exemplarId) { this.exemplarId = exemplarId; }
    public LocalDateTime getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDateTime dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public LocalDateTime getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDateTime dataPrevistaDevolucao) { this.dataPrevistaDevolucao = dataPrevistaDevolucao; }
    public LocalDateTime getDataDevolucaoReal() { return dataDevolucaoReal; }
    public void setDataDevolucaoReal(LocalDateTime dataDevolucaoReal) { this.dataDevolucaoReal = dataDevolucaoReal; }
    public Integer getRenovacoes() { return renovacoes; }
    public void setRenovacoes(Integer renovacoes) { this.renovacoes = renovacoes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
