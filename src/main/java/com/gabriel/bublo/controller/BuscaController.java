package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.ItemAcervoDTO;
import com.gabriel.bublo.service.AcervoService;
import com.gabriel.bublo.service.EmprestimoService;
import com.gabriel.bublo.service.LeitorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca global: sugestões agrupadas por módulo e página de resultados completa.
 */
@Controller
@RequestMapping("/busca")
public class BuscaController {

    private static final int SUGESTOES_POR_MODULO = 5;
    private static final int RESULTADOS_POR_MODULO = 25;
    private static final int TERMO_MINIMO = 2;
    private static final String ORDEM_ACERVO = "ia.titulo ASC";
    private static final String ORDEM_LEITORES = "nome ASC";
    private static final String ORDEM_EMPRESTIMOS = "data_emprestimo DESC";

    private final AcervoService acervoService;
    private final LeitorService leitorService;
    private final EmprestimoService emprestimoService;

    public BuscaController(AcervoService acervoService, LeitorService leitorService,
                           EmprestimoService emprestimoService) {
        this.acervoService = acervoService;
        this.leitorService = leitorService;
        this.emprestimoService = emprestimoService;
    }

    @GetMapping
    public String pagina(@RequestParam(required = false) String termo, Model model) {
        String consulta = termo == null ? "" : termo.trim();
        model.addAttribute("termo", consulta);
        model.addAttribute("buscaTermo", consulta);
        if (consulta.length() < TERMO_MINIMO) {
            return "busca";
        }
        model.addAttribute("acervo", acervoService.buscar(consulta, null, null, null, 0, RESULTADOS_POR_MODULO, ORDEM_ACERVO).getConteudo());
        model.addAttribute("leitores", leitorService.listar(consulta, null, null, 0, RESULTADOS_POR_MODULO, ORDEM_LEITORES).getConteudo());
        model.addAttribute("emprestimos", emprestimoService.listar(consulta, null, 0, RESULTADOS_POR_MODULO, ORDEM_EMPRESTIMOS).getConteudo());
        return "busca";
    }

    @GetMapping("/sugestoes")
    @ResponseBody
    public Map<String, Object> sugestoes(@RequestParam String termo) {
        String consulta = termo.trim();
        Map<String, Object> resposta = new LinkedHashMap<>();
        if (consulta.length() < TERMO_MINIMO) {
            resposta.put("acervo", List.of());
            resposta.put("leitores", List.of());
            resposta.put("emprestimos", List.of());
            return resposta;
        }
        resposta.put("acervo", acervoService.buscar(consulta, null, null, null, 0, SUGESTOES_POR_MODULO, ORDEM_ACERVO)
            .getConteudo().stream().map(this::itemDoAcervo).toList());
        resposta.put("leitores", leitorService.listar(consulta, null, null, 0, SUGESTOES_POR_MODULO, ORDEM_LEITORES)
            .getConteudo().stream().map(l -> Map.of("titulo", l.getNome(),
                "detalhe", l.getMatricula(), "url", "/leitores/" + l.getId())).toList());
        resposta.put("emprestimos", emprestimoService.listar(consulta, null, 0, SUGESTOES_POR_MODULO, ORDEM_EMPRESTIMOS)
            .getConteudo().stream().map(e -> Map.of(
                "titulo", "Empréstimo #" + e.getId() + " · " + (e.getTitulo() != null ? e.getTitulo() : "-"),
                "detalhe", e.getLeitorNome() != null ? e.getLeitorNome() : "-",
                "url", "/emprestimos?termo=" + e.getId())).toList());
        return resposta;
    }

    private Map<String, String> itemDoAcervo(ItemAcervoDTO item) {
        return Map.of("titulo", item.getTitulo(),
            "detalhe", item.getAutoresNomes().isEmpty() ? item.getTipo() : String.join(", ", item.getAutoresNomes()),
            "url", "/acervo/" + item.getId());
    }
}
