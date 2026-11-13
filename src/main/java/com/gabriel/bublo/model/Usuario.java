package com.gabriel.bublo.model;

import java.time.LocalDateTime;

/**
 * Usuário administrador do sistema (autenticação).
 */
public class Usuario {

    private Long id;
    private String username;
    private String senhaHash;
    private Boolean ativo;
    private LocalDateTime dataCriacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
