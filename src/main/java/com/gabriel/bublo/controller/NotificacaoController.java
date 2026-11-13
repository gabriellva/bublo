package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.NotificacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controlador de notificações.
 */
@Controller
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private static final int RESUMO_TAMANHO = 5;

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

    @GetMapping("/resumo")
    @ResponseBody
    public Map<String, Object> resumo() {
        var recentes = notificacaoService.listar(null, null, false, null, 0, RESUMO_TAMANHO).getConteudo();
        List<Map<String, Object>> itens = recentes.stream()
            .map(n -> Map.<String, Object>of("id", n.getId(), "titulo", n.getTitulo(),
                "mensagem", n.getMensagem() != null ? n.getMensagem() : ""))
            .toList();
        return Map.of("naoLidas", notificacaoService.contarNaoLidas(), "itens", itens);
    }

    @PostMapping("/{id}/ler")
    public String ler(@PathVariable Long id, RedirectAttributes redirect) {
        notificacaoService.marcarLida(id);
        redirect.addFlashAttribute("sucesso", "Notificação marcada como lida.");
        return "redirect:/notificacoes";
    }

    @PostMapping("/ler-todas")
    public String lerTodas(RedirectAttributes redirect) {
        notificacaoService.marcarTodasLidas();
        redirect.addFlashAttribute("sucesso", "Todas as notificações foram marcadas como lidas.");
        return "redirect:/notificacoes";
    }
}
