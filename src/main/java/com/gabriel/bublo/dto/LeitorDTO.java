package com.gabriel.bublo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de cadastro/edição de leitor.
 */
public class LeitorDTO {

    private Long id;

    @NotBlank(message = "Tipo de leitor é obrigatório")
    private String tipo; // ALUNO, PROFESSOR

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(max = 200)
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 200)
    private String email;

    private String matricula;

    @Size(max = 20)
    private String cpf;

    @Size(max = 1000)
    private String endereco;

    @Size(max = 30)
    private String telefone;

    @Size(max = 150)
    private String cursoDepartamento;

    private Boolean ativo;

    // Getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCursoDepartamento() { return cursoDepartamento; }
    public void setCursoDepartamento(String cursoDepartamento) { this.cursoDepartamento = cursoDepartamento; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
