package com.gabriel.bublo.controller;

import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.EmprestimoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Controlador de devoluções.
 */
@Controller
@RequestMapping("/devolucoes")
public class DevolucaoController {

    private static final int LIMITE_LISTAGEM = 1000;

    private final EmprestimoService emprestimoService;

    public DevolucaoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo, Model model) {
        var resultado = emprestimoService.listar(termo, "ATIVO", 0, LIMITE_LISTAGEM, "data_prevista_devolucao ASC");
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        return "devolucoes/listar";
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            BigDecimal multa = emprestimoService.devolver(id, AuthUtil.usuarioAtual());
            if (multa.signum() > 0) {
                String valor = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(multa);
                redirect.addFlashAttribute("aviso", "Devolução registrada com atraso. Multa gerada: " + valor + ".");
            } else {
                redirect.addFlashAttribute("sucesso", "Devolução registrada com sucesso.");
            }
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/devolucoes";
    }
}
