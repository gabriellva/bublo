package com.gabriel.bublo.controller;

import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.AcervoService;
import com.gabriel.bublo.service.ConfiguracaoService;
import com.gabriel.bublo.service.LeitorService;
import com.gabriel.bublo.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Controlador de reservas.
 */
@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private static final int PRAZO_RETIRADA_PADRAO_DIAS = 3;

    private final ReservaService reservaService;
    private final AcervoService acervoService;
    private final LeitorService leitorService;
    private final ConfiguracaoService configuracaoService;

    public ReservaController(ReservaService reservaService, AcervoService acervoService,
                             LeitorService leitorService, ConfiguracaoService configuracaoService) {
        this.reservaService = reservaService;
        this.acervoService = acervoService;
        this.leitorService = leitorService;
        this.configuracaoService = configuracaoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         Model model) {
        var resultado = reservaService.listar(termo, status, Math.max(pagina, 0),
            Math.min(Math.max(tamanho, 1), 100), "data_reserva DESC");
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("status", status);
        return "reservas/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        carregarFormulario(model);
        model.addAttribute("prazoSelecionado", LocalDate.now().plusDays(
            configuracaoService.buscarInteiro("prazo_retirada_reserva", PRAZO_RETIRADA_PADRAO_DIAS)));
        return "reservas/novo";
    }

    @PostMapping
    public String criar(@RequestParam(required = false) Long leitorId,
                        @RequestParam(required = false) Long itemId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            if (leitorId == null) throw new IllegalStateException("Selecione o leitor.");
            if (itemId == null) throw new IllegalStateException("Selecione o material.");
            var reserva = reservaService.criar(leitorId, itemId,
                prazo != null ? prazo.atStartOfDay() : null, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Reserva criada. Posição na fila: " + reserva.getPosicao() + ".");
            return "redirect:/reservas";
        } catch (IllegalStateException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("leitorSelecionado", leitorId);
            model.addAttribute("itemSelecionado", itemId);
            model.addAttribute("prazoSelecionado", prazo);
            carregarFormulario(model);
            return "reservas/novo";
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservaService.cancelar(id, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Reserva cancelada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/reservas";
    }

    private void carregarFormulario(Model model) {
        model.addAttribute("leitores", leitorService.listar(null, null, true, 0, 1000, "nome ASC").getConteudo());
        model.addAttribute("acervo", acervoService.listarTodos());
        model.addAttribute("hoje", LocalDate.now());
    }
}
