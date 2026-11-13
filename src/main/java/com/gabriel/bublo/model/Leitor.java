package com.gabriel.bublo.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Classe base abstrata para leitores.
 * Demonstra herança: aluno e professor compartilham dados comuns
 * e regras próprias (limite de exemplares, prazo de empréstimo).
 */
public abstract class Leitor {

    private Long id;
    private TipoLeitor tipo;
    private String nome;
    private String email;
    private String matricula;
    private String cpf;
    private String endereco;
    private String telefone;
    private String cursoDepartamento;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
    private List<Emprestimo> emprestimosAtivos;
    private List<Emprestimo> emprestimosAnteriores;
    private List<Reserva> reservas;
    private List<Multa> multas;

    public abstract int getLimiteExemplaresSimultaneos();

    public abstract int getPrazoEmprestimoDias();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoLeitor getTipo() {
        return tipo;
    }

    public void setTipo(TipoLeitor tipo) {
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCursoDepartamento() {
        return cursoDepartamento;
    }

    public void setCursoDepartamento(String cursoDepartamento) {
        this.cursoDepartamento = cursoDepartamento;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public List<Emprestimo> getEmprestimosAtivos() {
        return emprestimosAtivos;
    }

    public void setEmprestimosAtivos(List<Emprestimo> emprestimosAtivos) {
        this.emprestimosAtivos = emprestimosAtivos;
    }

    public List<Emprestimo> getEmprestimosAnteriores() {
        return emprestimosAnteriores;
    }

    public void setEmprestimosAnteriores(List<Emprestimo> emprestimosAnteriores) {
        this.emprestimosAnteriores = emprestimosAnteriores;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }

    public List<Multa> getMultas() {
        return multas;
    }

    public void setMultas(List<Multa> multas) {
        this.multas = multas;
    }
}
