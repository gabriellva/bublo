package com.gabriel.bublo.model;

public class Professor extends Leitor {

    @Override
    public int getLimiteExemplaresSimultaneos() {
        return 5;
    }

    @Override
    public int getPrazoEmprestimoDias() {
        return 14;
    }
}
