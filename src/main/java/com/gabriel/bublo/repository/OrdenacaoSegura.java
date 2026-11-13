package com.gabriel.bublo.repository;

import java.util.Locale;
import java.util.Set;

/**
 * Valida o parâmetro de ordenação vindo da requisição antes de concatená-lo em um ORDER BY.
 * Apenas colunas explicitamente permitidas e a direção ASC/DESC são aceitas.
 */
final class OrdenacaoSegura {

    private OrdenacaoSegura() {}

    static String resolver(String pedido, String alias, Set<String> colunasPermitidas, String padrao) {
        if (pedido == null || pedido.isBlank()) return padrao;
        String[] partes = pedido.trim().split("\\s+");
        if (partes.length > 2) return padrao;
        String coluna = partes[0].contains(".") ? partes[0].substring(partes[0].indexOf('.') + 1) : partes[0];
        if (!colunasPermitidas.contains(coluna)) return padrao;
        String direcao = partes.length == 2 ? partes[1].toUpperCase(Locale.ROOT) : "ASC";
        if (!direcao.equals("ASC") && !direcao.equals("DESC")) return padrao;
        return alias + coluna + " " + direcao;
    }
}
