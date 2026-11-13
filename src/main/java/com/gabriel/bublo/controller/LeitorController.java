package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.ImportacaoResultadoDTO;
import com.gabriel.bublo.dto.LeitorDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.TipoLeitor;
import com.gabriel.bublo.security.AuthUtil;
import com.gabriel.bublo.service.EmprestimoService;
import com.gabriel.bublo.service.LeitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

/**
 * Controlador de leitores.
 */
@Controller
@RequestMapping("/leitores")
public class LeitorController {

    private static final String MODELO_CSV =
        "Tipo,Nome,Email,CPF,Endereco,Telefone,CursoDepartamento\n"
        + "ALUNO,Joao Silva,joao@email.com,111.222.333-44,Rua A,11999999999,Engenharia\n"
        + "PROFESSOR,Maria Souza,maria@email.com,,Rua B,11988888888,Matematica\n";

    private final LeitorService leitorService;
    private final EmprestimoService emprestimoService;

    public LeitorController(LeitorService leitorService, EmprestimoService emprestimoService) {
        this.leitorService = leitorService;
        this.emprestimoService = emprestimoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(required = false) TipoLeitor tipo,
                         @RequestParam(required = false) Boolean ativo,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho,
                         @RequestParam(defaultValue = "nome ASC") String ordenacao,
                         Model model) {
        Paginacao<LeitorDTO> resultado = leitorService.listar(termo, tipo, ativo,
            Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100), ordenacao);
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
        LeitorDTO leitor = new LeitorDTO();
        leitor.setTipo(TipoLeitor.ALUNO.name());
        leitor.setAtivo(true);
        model.addAttribute("leitor", leitor);
        return "leitores/formulario";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        LeitorDTO leitor = leitorService.buscarDTO(id);
        if (leitor == null) {
            redirect.addFlashAttribute("erro", "Leitor não encontrado.");
            return "redirect:/leitores";
        }
        model.addAttribute("leitor", leitor);
        model.addAttribute("situacao", emprestimoService.restricaoLeitor(id));
        return "leitores/detalhes";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("leitor") LeitorDTO leitor,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("erro", "Verifique os campos destacados.");
            return "leitores/formulario";
        }
        try {
            leitorService.salvar(leitor, AuthUtil.usuarioAtual());
        } catch (IllegalStateException e) {
            result.rejectValue("email", "duplicado", e.getMessage());
            model.addAttribute("erro", e.getMessage());
            return "leitores/formulario";
        }
        redirect.addFlashAttribute("sucesso", "Leitor salvo com sucesso.");
        return "redirect:/leitores";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        LeitorDTO leitor = leitorService.buscarDTO(id);
        if (leitor == null) {
            redirect.addFlashAttribute("erro", "Leitor não encontrado.");
            return "redirect:/leitores";
        }
        model.addAttribute("leitor", leitor);
        return "leitores/formulario";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            leitorService.inativar(id, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Leitor inativado com sucesso.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/leitores";
    }

    @PostMapping("/importar")
    public String importar(@RequestParam("arquivo") MultipartFile arquivo, RedirectAttributes redirect) {
        if (arquivo.isEmpty()) {
            redirect.addFlashAttribute("erro", "Selecione um arquivo CSV para importar.");
            return "redirect:/leitores";
        }
        try {
            ImportacaoResultadoDTO resultado = leitorService.importarCSV(arquivo, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("importacao", resultado);
            if (resultado.getRejeitados() > 0) {
                redirect.addFlashAttribute("aviso", resultado.getImportados() + " leitor(es) importado(s); "
                    + resultado.getRejeitados() + " linha(s) rejeitada(s). Veja os detalhes abaixo.");
            } else {
                redirect.addFlashAttribute("sucesso", resultado.getImportados() + " leitor(es) importado(s) com sucesso.");
            }
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/leitores";
    }

    @GetMapping("/modelo-csv")
    public ResponseEntity<byte[]> modeloCSV() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modelo-leitores.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(MODELO_CSV.getBytes(StandardCharsets.UTF_8));
    }
}
