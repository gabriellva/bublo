package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.LeitorDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.TipoLeitor;
import com.gabriel.bublo.service.LeitorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

/**
 * Controlador de leitores.
 */
@Controller
@RequestMapping("/leitores")
public class LeitorController {

    private final LeitorService leitorService;

    public LeitorController(LeitorService leitorService) {
        this.leitorService = leitorService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) TipoLeitor tipo,
                         @RequestParam(required = false) Boolean ativo,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         @RequestParam(defaultValue = "nome ASC") String ordenacao,
                         Model model) {
        Paginacao<LeitorDTO> resultado = leitorService.listar(termo, tipo, ativo, pagina, tamanho, ordenacao);
        model.addAttribute("resultado", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("tipo", tipo);
        model.addAttribute("ativo", ativo);
        model.addAttribute("ordenacao", ordenacao);
        model.addAttribute("tipos", TipoLeitor.values());
        return "leitores/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("leitor", new LeitorDTO());
        model.addAttribute("tipos", TipoLeitor.values());
        return "leitores/formulario";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        LeitorDTO leitor = leitorService.buscarDTO(id);
        if (leitor == null) return "redirect:/leitores";
        model.addAttribute("leitor", leitor);
        return "leitores/detalhes";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute LeitorDTO leitor, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tipos", TipoLeitor.values());
            return "leitores/formulario";
        }
        try {
            leitorService.salvar(leitor, "admin");
            return "redirect:/leitores?sucesso=1";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("tipos", TipoLeitor.values());
            return "leitores/formulario";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        LeitorDTO leitor = leitorService.buscarDTO(id);
        if (leitor == null) return "redirect:/leitores";
        model.addAttribute("leitor", leitor);
        model.addAttribute("tipos", TipoLeitor.values());
        return "leitores/formulario";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id) {
        leitorService.inativar(id, "admin");
        return "redirect:/leitores?inativado=1";
    }

    @PostMapping("/importar")
    public String importar(@RequestParam("arquivo") MultipartFile arquivo, RedirectAttributes redirect) {
        try {
            var resultado = leitorService.importarCSV(arquivo, "admin");
            redirect.addFlashAttribute("importacao", resultado);
        } catch (Exception e) {
            redirect.addFlashAttribute("erroImportacao", e.getMessage());
        }
        return "redirect:/leitores";
    }

    @GetMapping("/modelo-csv")
    public @ResponseBody byte[] modeloCSV() {
        String csv = "Tipo,Nome,Email,CPF,Endereco,Telefone,CursoDepartamento\nALUNO,Joao Silva,joao@email.com,111.222.333-44,Rua A,11999999999,Engenharia\nPROFESSOR,Maria Souza,maria@email.com,,Rua B,11988888888,Matematica\n";
        return csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
