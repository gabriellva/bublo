package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.EmprestimoService;
import com.gabriel.bublo.service.LeitorService;
import com.gabriel.bublo.service.AcervoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de empréstimos.
 */
@Controller
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;
    private final LeitorService leitorService;
    private final AcervoService acervoService;

    public EmprestimoController(EmprestimoService emprestimoService,
                                LeitorService leitorService,
                                AcervoService acervoService) {
        this.emprestimoService = emprestimoService;
        this.leitorService = leitorService;
        this.acervoService = acervoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         @RequestParam(defaultValue = "data_emprestimo DESC") String ordenacao,
                         Model model) {
        var resultado = emprestimoService.listar(termo, status, pagina, tamanho, ordenacao);
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("status", status);
        model.addAttribute("ordenacao", ordenacao);
        return "emprestimos/listar";
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long leitor, Model model) {
        model.addAttribute("leitorId", leitor);
        if (leitor != null) {
            var dto = leitorService.buscarDTO(leitor);
            model.addAttribute("leitor", dto);
            model.addAttribute("bloqueado", dto != null); // simplificado
        }
        model.addAttribute("acervo", acervoService.listarTodos());
        return "emprestimos/novo";
    }

    @PostMapping
    public String criar(@RequestParam Long leitorId,
                        @RequestParam List<Long> exemplares,
                        @RequestParam String dataPrevista,
                        Model model) {
        try {
            LocalDateTime prevista = LocalDateTime.parse(dataPrevista + "T00:00:00");
            emprestimoService.criar(leitorId, exemplares, prevista, "admin");
            return "redirect:/emprestimos?criado=1";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("leitorId", leitorId);
            model.addAttribute("acervo", acervoService.listarTodos());
            return "emprestimos/novo";
        }
    }

    @GetMapping("/{id}/renovar")
    public String renovar(@PathVariable Long id) {
        emprestimoService.renovar(id, "admin");
        return "redirect:/emprestimos?renovado=1";
    }

    @GetMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id) {
        emprestimoService.cancelar(id, "admin");
        return "redirect:/emprestimos?cancelado=1";
    }

    @GetMapping("/restricao/{leitorId}")
    public @ResponseBody Object restricao(@PathVariable Long leitorId) {
        var r = emprestimoService.restricaoLeitor(leitorId);
        return java.util.Collections.singletonMap("bloqueado", r != null && r.isBloqueado());
    }
}
