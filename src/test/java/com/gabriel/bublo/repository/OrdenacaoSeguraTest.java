package com.gabriel.bublo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrdenacaoSeguraTest {

    private static final Set<String> COLUNAS = Set.of("nome", "data_cadastro");

    @Test
    @DisplayName("Coluna permitida mantém a direção informada")
    void colunaPermitida() {
        assertThat(OrdenacaoSegura.resolver("nome desc", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome DESC");
    }

    @Test
    @DisplayName("Prefixo de alias enviado pelo cliente é ignorado")
    void aliasDoClienteEhIgnorado() {
        assertThat(OrdenacaoSegura.resolver("x.data_cadastro", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.data_cadastro ASC");
    }

    @Test
    @DisplayName("Tentativa de injeção em ORDER BY cai no padrão")
    void injecaoEhRejeitada() {
        assertThat(OrdenacaoSegura.resolver("nome; DROP TABLE leitores", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome ASC");
        assertThat(OrdenacaoSegura.resolver("(SELECT pg_sleep(5))", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome ASC");
        assertThat(OrdenacaoSegura.resolver("nome ASC, senha_hash", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome ASC");
    }

    @Test
    @DisplayName("Coluna desconhecida ou direção inválida usa o padrão")
    void colunaOuDirecaoInvalida() {
        assertThat(OrdenacaoSegura.resolver("senha_hash", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome ASC");
        assertThat(OrdenacaoSegura.resolver("nome SIDEWAYS", "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome ASC");
        assertThat(OrdenacaoSegura.resolver(null, "l.", COLUNAS, "l.nome ASC")).isEqualTo("l.nome ASC");
    }
}
