package com.gabriel.bublo.model;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de operações relevantes.
 */
public class Auditoria {

    private Long id;
    private String adminUsuario;
    private LocalDateTime dataHora;
    private String tipoOperacao;
    private String modulo;
    private String registroAfectado;
    private String valoresAntes;
    private String valoresDepois;
    private String justificativa;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminUsuario() {
        return adminUsuario;
    }

    public void setAdminUsuario(String adminUsuario) {
        this.adminUsuario = adminUsuario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getRegistroAfectado() {
        return registroAfectado;
    }

    public void setRegistroAfectado(String registroAfectado) {
        this.registroAfectado = registroAfectado;
    }

    public String getValoresAntes() {
        return valoresAntes;
    }

    public void setValoresAntes(String valoresAntes) {
        this.valoresAntes = valoresAntes;
    }

    public String getValoresDepois() {
        return valoresDepois;
    }

    public void setValoresDepois(String valoresDepois) {
        this.valoresDepois = valoresDepois;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
