package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.NotificacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Notificações: consumidas pelo sino da barra superior (sem página própria).
 */
@Controller
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private static final int RESUMO_TAMANHO = 30;

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
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
    @ResponseBody
    public Map<String, Object> ler(@PathVariable Long id) {
        notificacaoService.marcarLida(id);
        return Map.of("naoLidas", notificacaoService.contarNaoLidas());
    }

    @PostMapping("/ler-todas")
    @ResponseBody
    public Map<String, Object> lerTodas() {
        notificacaoService.marcarTodasLidas();
        return Map.of("naoLidas", notificacaoService.contarNaoLidas());
    }
}
