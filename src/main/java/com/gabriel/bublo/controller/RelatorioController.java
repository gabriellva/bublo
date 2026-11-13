package com.gabriel.bublo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de relatórios (páginas; exportação via serviço dedicado).
 */
@Controller
public class RelatorioController {

    @GetMapping("/relatorios")
    public String index() {
        return "relatorios/index";
    }
}
