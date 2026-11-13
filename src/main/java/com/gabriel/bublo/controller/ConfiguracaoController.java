package com.gabriel.bublo.controller;

import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.ConfiguracaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador de configurações do sistema.
 */
@Controller
@RequestMapping("/configuracoes")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    @GetMapping
    public String listar(Model model) {
        Map<String, String> valores = new LinkedHashMap<>();
        configuracaoService.listarTodas().forEach(c -> valores.put(c.getChave(), c.getValor()));
        model.addAttribute("valores", valores);
        return "configuracoes/listar";
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam Map<String, String> parametros,
                         @RequestParam(required = false) String justificativa,
                         RedirectAttributes redirect) {
        if (justificativa == null || justificativa.isBlank()) {
            redirect.addFlashAttribute("erro", "Informe a justificativa da alteração.");
            return "redirect:/configuracoes";
        }
        int alteradas;
        try {
            alteradas = configuracaoService.salvarVarias(parametros, AuthUtil.usuarioAtual(), justificativa.trim());
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/configuracoes";
        }
        if (alteradas == 0) {
            redirect.addFlashAttribute("aviso", "Nenhuma configuração foi alterada.");
            return "redirect:/configuracoes";
        }
        redirect.addFlashAttribute("sucesso", alteradas + " configuração(ões) atualizada(s) com sucesso.");
        return "redirect:/configuracoes";
    }
}
