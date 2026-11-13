package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.ConfiguracaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("configuracoes", configuracaoService.listarTodas());
        return "configuracoes/listar";
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam String chave,
                         @RequestParam String valor,
                         @RequestParam String justificativa,
                         RedirectAttributes redirect) {
        try {
            configuracaoService.salvar(chave, valor, "admin", justificativa);
            redirect.addFlashAttribute("sucesso", "Configuração atualizada com sucesso.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/configuracoes";
    }
}
