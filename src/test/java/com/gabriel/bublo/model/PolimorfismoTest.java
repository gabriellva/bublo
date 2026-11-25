package com.gabriel.bublo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobrescrita: a mesma chamada, vista como Leitor/ItemAcervo, responde diferente em cada subclasse. */
class PolimorfismoTest {

    @Test
    @DisplayName("Sobrescrita: Aluno e Professor respondem diferente à mesma chamada de Leitor")
    void leitoresSobrescrevemRegras() {
        Leitor aluno = new Aluno();
        Leitor professor = new Professor();

        assertThat(aluno.getLimiteExemplaresSimultaneos()).isEqualTo(3);
        assertThat(professor.getLimiteExemplaresSimultaneos()).isEqualTo(5);
        assertThat(aluno.getPrazoEmprestimoDias()).isEqualTo(7);
        assertThat(professor.getPrazoEmprestimoDias()).isEqualTo(14);
        assertThat(aluno.getChaveConfiguracaoLimite()).isEqualTo("limite_alunos");
        assertThat(professor.getChaveConfiguracaoPrazo()).isEqualTo("prazo_professores");
    }

    @Test
    @DisplayName("Sobrescrita: cada tipo de material informa seu campo específico")
    void itensSobrescrevemCampoEspecifico() {
        Livro livro = new Livro();
        livro.setIsbn("978-85-359-0277-5");
        ItemAcervo item = livro;

        assertThat(item.getCampoEspecificoLabel()).isEqualTo("ISBN");
        assertThat(item.getCampoEspecificoValor()).isEqualTo("978-85-359-0277-5");
        assertThat(item.getCapaGenerica()).isEqualTo("capa-livro.svg");
    }
}
