package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.LoginDTO;
import com.gabriel.bublo.dto.TrocaSenhaDTO;
import com.gabriel.bublo.repository.UsuarioRepository;
import com.gabriel.bublo.security.AuthAuditService;
import com.gabriel.bublo.service.ConfiguracaoService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Controlador de autenticação e conta do administrador.
 */
@Controller
@RequestMapping
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;
    private final ConfiguracaoService configuracaoService;

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuthAuditService authAuditService,
                          ConfiguracaoService configuracaoService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authAuditService = authAuditService;
        this.configuracaoService = configuracaoService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String logout, Model model) {
        if (error != null) {
            model.addAttribute("error", "Usuário ou senha inválidos.");
            authAuditService.registrarFalha("desconhecido");
        }
        if (logout != null) {
            model.addAttribute("logout", "Você saiu do sistema.");
        }
        model.addAttribute("loginDTO", new LoginDTO());
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @PostMapping("/troca-senha")
    public String trocaSenha(@Valid @ModelAttribute TrocaSenhaDTO dto, BindingResult result,
                             Authentication authentication, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("erroSenha", "Verifique os campos informados.");
            return "redirect:/conta";
        }
        String username = authentication.getName();
        usuarioRepository.buscarPorUsername(username).ifPresent(u -> {
            if (!passwordEncoder.matches(dto.getSenhaAtual(), u.getSenhaHash())) {
                throw new IllegalArgumentException("Senha atual inválida");
            }
            if (!dto.getNovaSenha().equals(dto.getConfirmaNovaSenha())) {
                throw new IllegalArgumentException("Nova senha e confirmação não coincidem");
            }
            usuarioRepository.alterarSenha(u.getId(), passwordEncoder.encode(dto.getNovaSenha()));
            authAuditService.registrarAlteracaoSenha(username);
        });
        redirect.addFlashAttribute("sucesso", "Senha alterada com sucesso. Faça login novamente.");
        return "redirect:/logout";
    }

    @RequestMapping("/conta")
    public String conta(Model model, Authentication authentication) {
        model.addAttribute("usuario", authentication.getName());
        model.addAttribute("trocaSenhaDTO", new TrocaSenhaDTO());
        model.addAttribute("nomeInstituicao", carregarNomeInstituicao());
        return "conta";
    }

    private String carregarNomeInstituicao() {
        try {
            var c = configuracaoService.buscar("nome_instituicao");
            return c != null ? c.getValor() : "Biblioteca Acadêmica";
        } catch (Exception e) {
            return "Biblioteca Acadêmica";
        }
    }
}
