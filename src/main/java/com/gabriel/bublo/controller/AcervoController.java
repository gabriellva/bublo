package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.ItemAcervoDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.TipoMaterial;
import com.gabriel.bublo.service.AcervoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador do acervo bibliográfico.
 */
@Controller
@RequestMapping("/acervo")
public class AcervoController {

    private final AcervoService acervoService;

    public AcervoController(AcervoService acervoService) {
        this.acervoService = acervoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) Long categoria,
                         @RequestParam(required = false) TipoMaterial tipo,
                         @RequestParam(required = false) Boolean disponivel,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         @RequestParam(defaultValue = "ia.titulo ASC") String ordenacao,
                         Model model) {
        Paginacao<ItemAcervoDTO> resultado = acervoService.buscar(termo, categoria, tipo, disponivel, pagina, tamanho, ordenacao);
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("categoria", categoria);
        model.addAttribute("tipo", tipo);
        model.addAttribute("disponivel", disponivel);
        model.addAttribute("ordenacao", ordenacao);
        model.addAttribute("tipos", TipoMaterial.values());
        model.addAttribute("categorias", listarCategorias());
        return "acervo/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("item", new ItemAcervoDTO());
        model.addAttribute("tipos", TipoMaterial.values());
        model.addAttribute("categorias", listarCategorias());
        return "acervo/formulario";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        ItemAcervoDTO item = acervoService.buscarDTO(id);
        if (item == null) return "redirect:/acervo";
        model.addAttribute("item", item);
        model.addAttribute("exemplares", acervoService.buscarPorId(id) != null ? java.util.Collections.emptyList() : java.util.Collections.emptyList());
        return "acervo/detalhes";
    }

    @PostMapping
    public String salvar(@ModelAttribute ItemAcervoDTO item, Model model) {
        try {
            acervoService.salvar(item, "admin");
            return "redirect:/acervo?sucesso=1";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("tipos", TipoMaterial.values());
            model.addAttribute("categorias", listarCategorias());
            return "acervo/formulario";
        }
    }

    @PostMapping("/{id}/exemplar")
    public String addExemplar(@PathVariable Long id, @RequestParam(defaultValue = "1") int quantidade) {
        if (quantidade == 1) {
            acervoService.cadastrarExemplar(id, "admin");
        } else {
            acervoService.cadastrarExemplaresLote(id, quantidade, "admin");
        }
        return "redirect:/acervo/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id) {
        acervoService.excluir(id, "admin", null);
        return "redirect:/acervo?excluido=1";
    }

    @GetMapping("/sugestoes-autores")
    @ResponseBody
    public List<String> sugestoesAutores(@RequestParam String termo) {
        return acervoService.sugestoesAutores(termo).stream()
            .map(com.gabriel.bublo.model.Autor::getNome).toList();
    }

    private List<com.gabriel.bublo.model.Categoria> listarCategorias() {
        // retornar categorias ativas - simplificado via repositorio direto não exposto; usar lista vazia por enquanto
        return java.util.Collections.emptyList();
    }
}
