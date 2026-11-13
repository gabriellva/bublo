package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.EmprestimoService;
import com.gabriel.bublo.service.LeitorService;
import com.gabriel.bublo.service.AcervoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de devoluções.
 */
@Controller
@RequestMapping("/devolucoes")
public class DevolucaoController {

    private final EmprestimoService emprestimoService;
    private final LeitorService leitorService;
    private final AcervoService acervoService;

    public DevolucaoController(EmprestimoService emprestimoService,
                               LeitorService leitorService,
                               AcervoService acervoService) {
        this.emprestimoService = emprestimoService;
        this.leitorService = leitorService;
        this.acervoService = acervoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo, Model model) {
        var resultado = emprestimoService.listar(termo, "ATIVO", 0, 1000, "data_prevista_devolucao ASC");
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        return "devolucoes/listar";
    }

    @GetMapping("/{id}")
    public String resumo(@PathVariable Long id, Model model) {
        var dto = emprestimoService.buscarDTO(id);
        if (dto == null) return "redirect:/devolucoes";
        model.addAttribute("emprestimo", dto);
        return "devolucoes/resumo";
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id) {
        emprestimoService.devolver(id, "admin");
        return "redirect:/devolucoes?devolvido=1";
    }
}
