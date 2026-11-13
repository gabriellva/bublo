package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controlador de exportação de relatórios (CSV e XLSX).
 */
@Controller
public class RelatorioExportController {

    private final RelatorioService relatorioService;

    public RelatorioExportController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/relatorios/emprestimos.csv")
    public void emprestimosCsv(HttpServletResponse response,
                               @RequestParam(required = false) String termo,
                               @RequestParam(required = false) String status) throws Exception {
        relatorioService.emprestimosCsv(response, termo, status);
    }

    @GetMapping("/relatorios/emprestimos.xlsx")
    public void emprestimosXlsx(HttpServletResponse response,
                                @RequestParam(required = false) String termo,
                                @RequestParam(required = false) String status) throws Exception {
        byte[] dados = relatorioService.emprestimosXlsx(termo, status);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=emprestimos.xlsx");
        response.getOutputStream().write(dados);
    }

    @GetMapping("/relatorios/multas.csv")
    public void multasCsv(HttpServletResponse response,
                          @RequestParam(required = false) String termo,
                          @RequestParam(required = false) String status) throws Exception {
        relatorioService.multasCsv(response, termo, status);
    }

    @GetMapping("/relatorios/multas.xlsx")
    public void multasXlsx(HttpServletResponse response,
                           @RequestParam(required = false) String termo,
                           @RequestParam(required = false) String status) throws Exception {
        byte[] dados = relatorioService.multasXlsx(termo, status);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=multas.xlsx");
        response.getOutputStream().write(dados);
    }

    @GetMapping("/relatorios/leitores.csv")
    public void leitoresCsv(HttpServletResponse response,
                            @RequestParam(required = false) String termo) throws Exception {
        relatorioService.leitoresCsv(response, termo);
    }

    @GetMapping("/relatorios/reservas.csv")
    public void reservasCsv(HttpServletResponse response,
                            @RequestParam(required = false) String termo,
                            @RequestParam(required = false) String status) throws Exception {
        relatorioService.reservasCsv(response, termo, status);
    }

    @GetMapping("/relatorios/materiais.csv")
    public void materiaisCsv(HttpServletResponse response,
                             @RequestParam(required = false) String termo) throws Exception {
        relatorioService.materiaisCsv(response, termo);
    }
}
