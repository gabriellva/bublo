package com.gabriel.bublo.dto;

import java.math.BigDecimal;

/**
 * Resumo do leitor para tela de empréstimo/histórico.
 */
public class LeitorResumoDTO {

    private Long id;
    private String nome;
    private String matricula;
    private String tipo;
    private int emprestimosAtivos;
    private int emprestimosAnteriores;
    private int multasPendentes;
    private BigDecimal saldoPendente;
    private boolean bloqueado;
    private int reservas;

    public LeitorResumoDTO() {
        this.saldoPendente = BigDecimal.ZERO;
    }

    public LeitorResumoDTO(Long id, String nome, String matricula, String tipo,
                           int emprestimosAtivos, int emprestimosAnteriores,
                           int multasPendentes, BigDecimal saldoPendente,
                           boolean bloqueado, int reservas) {
        this.id = id;
        this.nome = nome;
        this.matricula = matricula;
        this.tipo = tipo;
        this.emprestimosAtivos = emprestimosAtivos;
        this.emprestimosAnteriores = emprestimosAnteriores;
        this.multasPendentes = multasPendentes;
        this.saldoPendente = saldoPendente;
        this.bloqueado = bloqueado;
        this.reservas = reservas;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getEmprestimosAtivos() { return emprestimosAtivos; }
    public void setEmprestimosAtivos(int emprestimosAtivos) { this.emprestimosAtivos = emprestimosAtivos; }
    public int getEmprestimosAnteriores() { return emprestimosAnteriores; }
    public void setEmprestimosAnteriores(int emprestimosAnteriores) { this.emprestimosAnteriores = emprestimosAnteriores; }
    public int getMultasPendentes() { return multasPendentes; }
    public void setMultasPendentes(int multasPendentes) { this.multasPendentes = multasPendentes; }
    public BigDecimal getSaldoPendente() { return saldoPendente; }
    public void setSaldoPendente(BigDecimal saldoPendente) { this.saldoPendente = saldoPendente; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public int getReservas() { return reservas; }
    public void setReservas(int reservas) { this.reservas = reservas; }
}
