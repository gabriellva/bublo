package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.ItemAcervoDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Autor;
import com.gabriel.bublo.model.TipoMaterial;
import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.AcervoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                         @RequestParam(defaultValue = "12") int tamanho,
                         @RequestParam(defaultValue = "ia.titulo ASC") String ordenacao,
                         Model model) {
        Paginacao<ItemAcervoDTO> resultado = acervoService.buscar(termo, categoria, tipo, disponivel,
            Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100), ordenacao);
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("categoria", categoria);
        model.addAttribute("tipo", tipo);
        model.addAttribute("disponivel", disponivel);
        model.addAttribute("ordenacao", ordenacao);
        model.addAttribute("tipos", TipoMaterial.values());
        model.addAttribute("categorias", acervoService.listarCategorias());
        return "acervo/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        ItemAcervoDTO item = new ItemAcervoDTO();
        item.setTipo(TipoMaterial.LIVRO.name());
        item.setIdioma("Português");
        model.addAttribute("item", item);
        carregarFormulario(model);
        return "acervo/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        ItemAcervoDTO item = acervoService.buscarDTO(id);
        if (item == null) {
            redirect.addFlashAttribute("erro", "Material não encontrado.");
            return "redirect:/acervo";
        }
        item.setNovosAutores(item.getAutoresNomes());
        model.addAttribute("item", item);
        carregarFormulario(model);
        return "acervo/formulario";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        ItemAcervoDTO item = acervoService.buscarDTO(id);
        if (item == null) {
            redirect.addFlashAttribute("erro", "Material não encontrado.");
            return "redirect:/acervo";
        }
        model.addAttribute("item", item);
        model.addAttribute("exemplares", acervoService.listarExemplares(id));
        return "acervo/detalhes";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("item") ItemAcervoDTO item,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("erro", "Verifique os campos destacados.");
            carregarFormulario(model);
            return "acervo/formulario";
        }
        try {
            acervoService.salvar(item, AuthUtil.usuarioAtual());
        } catch (IllegalStateException e) {
            model.addAttribute("erro", e.getMessage());
            carregarFormulario(model);
            return "acervo/formulario";
        }
        redirect.addFlashAttribute("sucesso", "Material salvo com sucesso.");
        return "redirect:/acervo";
    }

    @PostMapping("/{id}/exemplar")
    public String addExemplar(@PathVariable Long id,
                              @RequestParam(defaultValue = "1") int quantidade,
                              RedirectAttributes redirect) {
        try {
            acervoService.cadastrarExemplaresLote(id, quantidade, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", quantidade + " exemplar(es) cadastrado(s) com sucesso.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/acervo/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            acervoService.excluir(id, AuthUtil.usuarioAtual(), null);
            redirect.addFlashAttribute("sucesso", "Material excluído com sucesso.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/acervo/" + id;
        }
        return "redirect:/acervo";
    }

    @GetMapping("/sugestoes-autores")
    @ResponseBody
    public List<String> sugestoesAutores(@RequestParam String termo) {
        return acervoService.sugestoesAutores(termo).stream().map(Autor::getNome).toList();
    }

    private void carregarFormulario(Model model) {
        model.addAttribute("tipos", TipoMaterial.values());
        model.addAttribute("categorias", acervoService.listarCategorias());
    }
}
