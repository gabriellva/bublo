package com.gabriel.bublo.controller;

import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.AcervoService;
import com.gabriel.bublo.service.EmprestimoService;
import com.gabriel.bublo.service.LeitorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        var resultado = emprestimoService.listar(termo, status, Math.max(pagina, 0),
            Math.min(Math.max(tamanho, 1), 100), ordenacao);
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("status", status);
        model.addAttribute("ordenacao", ordenacao);
        return "emprestimos/listar";
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long leitor, Model model) {
        model.addAttribute("leitorId", leitor);
        carregarFormulario(model);
        return "emprestimos/novo";
    }

    @PostMapping
    public String criar(@RequestParam(required = false) Long leitorId,
                        @RequestParam(required = false) List<Long> exemplares,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPrevista,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            if (leitorId == null) throw new IllegalStateException("Selecione o leitor.");
            var criado = emprestimoService.criar(leitorId, exemplares,
                dataPrevista != null ? dataPrevista.atStartOfDay() : null, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Empréstimo registrado com sucesso (#" + criado.getId() + ").");
            return "redirect:/emprestimos";
        } catch (IllegalStateException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("leitorId", leitorId);
            model.addAttribute("exemplaresSelecionados", exemplares);
            model.addAttribute("dataSelecionada", dataPrevista);
            carregarFormulario(model);
            return "emprestimos/novo";
        }
    }

    @PostMapping("/{id}/renovar")
    public String renovar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            emprestimoService.renovar(id, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Empréstimo renovado com sucesso.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/emprestimos";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            emprestimoService.cancelar(id, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Empréstimo cancelado. O exemplar voltou a ficar disponível.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/emprestimos";
    }

    @GetMapping("/restricao/{leitorId}")
    @ResponseBody
    public Map<String, Object> restricao(@PathVariable Long leitorId) {
        var restricao = emprestimoService.restricaoLeitor(leitorId);
        if (restricao == null) return Map.of("encontrado", false, "bloqueado", false);
        return Map.of("encontrado", true, "bloqueado", restricao.isBloqueado(),
            "ativos", restricao.getEmprestimosAtivos(), "saldo", restricao.getSaldoPendente());
    }

    private void carregarFormulario(Model model) {
        model.addAttribute("leitores", leitorService.listar(null, null, true, 0, 1000, "nome ASC").getConteudo());
        model.addAttribute("exemplaresDisponiveis", acervoService.listarExemplaresDisponiveis());
        model.addAttribute("prazoAlunos", emprestimoService.prazoAlunosEmDias());
        model.addAttribute("prazoProfessores", emprestimoService.prazoProfessoresEmDias());
        model.addAttribute("hoje", LocalDate.now());
    }
}
