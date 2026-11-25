package com.gabriel.bublo.model;

public class Aluno extends Leitor {

    @Override
    public int getLimiteExemplaresSimultaneos() {
        return 3;
    }

    @Override
    public int getPrazoEmprestimoDias() {
        return 7;
    }

    @Override
    public String getChaveConfiguracaoLimite() {
        return "limite_alunos";
    }

    @Override
    public String getChaveConfiguracaoPrazo() {
        return "prazo_alunos";
    }
}
