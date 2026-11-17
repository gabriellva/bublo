package com.gabriel.bublo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de troca de senha do administrador: nova senha e confirmação.
 * Mínimo de 4 caracteres; o máximo de 72 é o limite do BCrypt.
 */
public class TrocaSenhaDTO {

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 4, message = "A nova senha deve ter pelo menos 4 caracteres")
    @Size(max = 72, message = "A nova senha deve ter no máximo 72 caracteres")
    private String novaSenha;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    private String confirmaNovaSenha;

    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    public String getConfirmaNovaSenha() { return confirmaNovaSenha; }
    public void setConfirmaNovaSenha(String confirmaNovaSenha) { this.confirmaNovaSenha = confirmaNovaSenha; }
}
