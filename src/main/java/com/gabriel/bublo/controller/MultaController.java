package com.gabriel.bublo.controller;

import com.gabriel.bublo.service.MultaService;
import com.gabriel.bublo.service.LeitorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de multas e pagamentos.
 */
@Controller
@RequestMapping("/multas")
public class MultaController {

    private final MultaService multaService;
    private final LeitorService leitorService;

    public MultaController(MultaService multaService, LeitorService leitorService) {
        this.multaService = multaService;
        this.leitorService = leitorService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         Model model) {
        var resultado = multaService.listar(termo, status, pagina, tamanho, "data_geracao ASC");
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("status", status);
        return "multas/listar";
    }

    @GetMapping("/{id}/pagar")
    public String pagarForm(@PathVariable Long id, Model model) {
        var dto = multaService.buscarDTO(id);
        if (dto == null) return "redirect:/multas";
        model.addAttribute("multa", dto);
        return "multas/pagar";
    }

    @PostMapping("/{id}/pagar")
    public String pagar(@PathVariable Long id,
                        @RequestParam String valor,
                        @RequestParam String forma,
                        Model model) {
        try {
            multaService.pagar(id, new java.math.BigDecimal(valor), forma, "admin");
            return "redirect:/multas?pago=1";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("multa", multaService.buscarDTO(id));
            return "multas/pagar";
        }
    }

    @PostMapping("/{id}/estornar")
    public String estornar(@PathVariable Long id, @RequestParam String justificativa) {
        // estornar o último pagamento da multa
        // simplificado: redireciona
        return "redirect:/multas?estornado=1";
    }

    @GetMapping("/leitor/{leitorId}/saldo")
    public @ResponseBody Object saldo(@PathVariable Long leitorId) {
        var saldo = multaService.saldoPendente(leitorId);
        return java.util.Collections.singletonMap("saldo", saldo);
    }
}
