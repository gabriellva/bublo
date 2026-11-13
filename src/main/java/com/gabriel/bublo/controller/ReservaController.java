package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.ReservaService;
import com.gabriel.bublo.service.AcervoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controlador de reservas.
 */
@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final AcervoService acervoService;

    public ReservaController(ReservaService reservaService, AcervoService acervoService) {
        this.reservaService = reservaService;
        this.acervoService = acervoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         Model model) {
        var resultado = reservaService.listar(termo, status, pagina, tamanho, "posicao ASC");
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("status", status);
        return "reservas/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("acervo", acervoService.listarTodos());
        return "reservas/novo";
    }

    @PostMapping
    public String criar(@RequestParam Long leitor,
                        @RequestParam Long item,
                        @RequestParam String prazo,
                        Model model) {
        try {
            LocalDateTime prazoRetirada = LocalDateTime.parse(prazo + "T00:00:00");
            reservaService.criar(leitor, item, prazoRetirada, "admin");
            return "redirect:/reservas?criado=1";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("acervo", acervoService.listarTodos());
            return "reservas/novo";
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id) {
        reservaService.cancelar(id, "admin");
        return "redirect:/reservas?cancelado=1";
    }
}
