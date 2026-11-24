package com.gabriel.bublo.controller;

import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.MultaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controlador de multas e pagamentos.
 */
@Controller
@RequestMapping("/multas")
public class MultaController {

    private final MultaService multaService;

    public MultaController(MultaService multaService) {
        this.multaService = multaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "andamento") String aba,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         Model model) {
        boolean historico = "historico".equals(aba);
        // Aba "Em aberto" = pendentes e pagas parcialmente; "Histórico" = quitadas e canceladas.
        String filtro = (status == null || status.isBlank()) ? (historico ? "HISTORICO" : "ABERTAS") : status;
        var resultado = multaService.listar(termo, filtro, Math.max(pagina, 0),
            Math.min(Math.max(tamanho, 1), 100), "data_geracao DESC");
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("status", status);
        model.addAttribute("aba", historico ? "historico" : "andamento");
        return "multas/listar";
    }

    @GetMapping("/{id}/pagar")
    public String pagarForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        var dto = multaService.buscarDTO(id);
        if (dto == null) {
            redirect.addFlashAttribute("erro", "Multa não encontrada.");
            return "redirect:/multas";
        }
        carregarTela(model, dto);
        return "multas/pagar";
    }

    @PostMapping("/{id}/pagar")
    public String pagar(@PathVariable Long id,
                        @RequestParam String valor,
                        @RequestParam String forma,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            multaService.pagar(id, parseValor(valor), forma, AuthUtil.usuarioAtual());
        } catch (IllegalStateException e) {
            model.addAttribute("erro", e.getMessage());
            carregarTela(model, multaService.buscarDTO(id));
            return "multas/pagar";
        }
        redirect.addFlashAttribute("sucesso", "Pagamento registrado com sucesso.");
        return "redirect:/multas";
    }

    @PostMapping("/{id}/pagamentos/{pagamentoId}/estornar")
    public String estornar(@PathVariable Long id,
                           @PathVariable Long pagamentoId,
                           @RequestParam(required = false) String justificativa,
                           RedirectAttributes redirect) {
        try {
            multaService.estornar(pagamentoId, justificativa, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Pagamento estornado. O saldo da multa foi recalculado.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/multas/" + id + "/pagar";
    }

    private void carregarTela(Model model, com.gabriel.bublo.dto.MultaDTO multa) {
        model.addAttribute("multa", multa);
        model.addAttribute("pagamentos", multa != null ? multaService.listarPagamentos(multa.getId()) : java.util.List.of());
    }

    @GetMapping("/leitor/{leitorId}/saldo")
    @ResponseBody
    public Map<String, BigDecimal> saldo(@PathVariable Long leitorId) {
        return Map.of("saldo", multaService.saldoPendente(leitorId));
    }

    private BigDecimal parseValor(String texto) {
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Valor de pagamento inválido.");
        }
    }
}
