package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.NotificacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de notificações.
 */
@Controller
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String categoria,
                         @RequestParam(required = false) String prioridade,
                         @RequestParam(required = false) Boolean lida,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         Model model) {
        var resultado = notificacaoService.listar(categoria, prioridade, lida, null, pagina, tamanho);
        model.addAttribute("resultado", resultado);
        model.addAttribute("categoria", categoria);
        model.addAttribute("prioridade", prioridade);
        model.addAttribute("lida", lida);
        model.addAttribute("naoLidas", notificacaoService.contarNaoLidas());
        return "notificacoes/listar";
    }

    @PostMapping("/{id}/ler")
    public String ler(@PathVariable Long id) {
        notificacaoService.marcarLida(id);
        return "redirect:/notificacoes";
    }
}
