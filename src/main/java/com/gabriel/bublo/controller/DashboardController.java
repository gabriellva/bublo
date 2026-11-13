package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.IndicadorDTO;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import com.gabriel.bublo.service.EmprestimoService;
import com.gabriel.bublo.service.NotificacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador do dashboard executivo.
 */
@Controller
public class DashboardController {

    private final ItemAcervoRepository itemAcervoRepository;
    private final ExemplarRepository exemplarRepository;
    private final LeitorRepository leitorRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaRepository reservaRepository;
    private final MultaRepository multaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final EmprestimoService emprestimoService;
    private final NotificacaoService notificacaoService;

    public DashboardController(ItemAcervoRepository itemAcervoRepository,
                               ExemplarRepository exemplarRepository,
                               LeitorRepository leitorRepository,
                               EmprestimoRepository emprestimoRepository,
                               ReservaRepository reservaRepository,
                               MultaRepository multaRepository,
                               NotificacaoRepository notificacaoRepository,
                               EmprestimoService emprestimoService,
                               NotificacaoService notificacaoService) {
        this.itemAcervoRepository = itemAcervoRepository;
        this.exemplarRepository = exemplarRepository;
        this.leitorRepository = leitorRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.reservaRepository = reservaRepository;
        this.multaRepository = multaRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.emprestimoService = emprestimoService;
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<IndicadorDTO> indicadores = new ArrayList<>();
        indicadores.add(new IndicadorDTO("Títulos", String.valueOf(itemAcervoRepository.listarTodos().size()), "Itens do acervo", "#7b1f35"));
        indicadores.add(new IndicadorDTO("Exemplares", String.valueOf(exemplarRepository.totalExemplares()), "Cópias físicas", "#7b1f35"));
        indicadores.add(new IndicadorDTO("Disponíveis", String.valueOf(exemplarRepository.contarDisponiveis()), "Prontos para empréstimo", "#2e7d32"));
        indicadores.add(new IndicadorDTO("Indisponíveis", String.valueOf(exemplarRepository.contarIndisponiveis()), "Fora de circulação", "#c62828"));
        indicadores.add(new IndicadorDTO("Empréstimos ativos", String.valueOf(emprestimoRepository.contarAtivos()), "Em circulação", "#1565c0"));
        indicadores.add(new IndicadorDTO("Atrasados", String.valueOf(emprestimoRepository.contarAtrasados()), "Foram do prazo", "#c62828"));
        indicadores.add(new IndicadorDTO("Multas pendentes", String.valueOf(multaRepository.contarPendentes()), "A receber", "#e65100"));
        indicadores.add(new IndicadorDTO("Reservas", String.valueOf(reservaRepository.contarAguardando()), "Aguardando retirada", "#f9a825"));
        indicadores.add(new IndicadorDTO("Leitores", String.valueOf(leitorRepository.buscar(null, null, null, 0, 1000, "nome ASC").getTotal()), "Cadastrados", "#6a1b9a"));

        model.addAttribute("indicadores", indicadores);
        model.addAttribute("naoLidas", notificacaoRepository.contarNaoLidas());
        return "dashboard";
    }
}
