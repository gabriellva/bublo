package com.gabriel.bublo.controller;

import com.gabriel.bublo.repository.DashboardRepository;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dados em JSON dos gráficos do dashboard para o período escolhido.
 */
@Controller
public class DashboardApiController {

    private static final long DIAS_MAXIMOS_POR_DIA = 62;
    private static final long DIAS_MAXIMOS_DO_PERIODO = 3660;

    private final DashboardRepository dashboardRepository;

    public DashboardApiController(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @GetMapping("/dashboard/dados")
    @ResponseBody
    public Map<String, Object> dados(@RequestParam(defaultValue = "30d") String periodo,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusDays(29);
        LocalDate fim = hoje;
        switch (periodo) {
            case "hoje" -> inicio = hoje;
            case "7d" -> inicio = hoje.minusDays(6);
            case "mes" -> inicio = hoje.withDayOfMonth(1);
            case "ano" -> inicio = hoje.withDayOfYear(1);
            case "custom" -> {
                if (de != null && ate != null) {
                    inicio = de;
                    fim = ate;
                }
            }
            default -> { }
        }
        if (fim.isBefore(inicio)) {
            LocalDate troca = inicio;
            inicio = fim;
            fim = troca;
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > DIAS_MAXIMOS_DO_PERIODO) {
            inicio = fim.minusDays(DIAS_MAXIMOS_DO_PERIODO);
        }
        boolean porMes = ChronoUnit.DAYS.between(inicio, fim) > DIAS_MAXIMOS_POR_DIA;

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("de", inicio.toString());
        resposta.put("ate", fim.toString());
        resposta.put("granularidade", porMes ? "mes" : "dia");
        resposta.put("emprestimosPeriodo", dashboardRepository.emprestimosNoPeriodo(inicio, fim));
        resposta.put("arrecadado", dashboardRepository.arrecadadoNoPeriodo(inicio, fim));
        resposta.put("saldoPendente", dashboardRepository.saldoPendenteTotal());
        resposta.put("emprestimosNoTempo", dashboardRepository.emprestimosNoTempo(inicio, fim, porMes));
        resposta.put("comparacaoMensal", dashboardRepository.comparacaoMensal(fim));
        resposta.put("porCategoria", dashboardRepository.emprestimosPorCategoria(inicio, fim));
        resposta.put("maisEmprestados", dashboardRepository.maisEmprestados(inicio, fim));
        resposta.put("leitoresPorTipo", dashboardRepository.leitoresPorTipo());
        resposta.put("disponibilidade", dashboardRepository.disponibilidade());
        resposta.put("multas", dashboardRepository.multasGeradasEPagas(inicio, fim, porMes));
        return resposta;
    }
}
