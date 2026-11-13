package com.gabriel.bublo.controller;

import com.gabriel.bublo.dto.TrocaSenhaDTO;
import com.gabriel.bublo.repository.UsuarioRepository;
import com.gabriel.bublo.security.AuthAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de autenticação e conta do administrador.
 */
@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuthAuditService authAuditService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authAuditService = authAuditService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String senha,
                        @RequestParam(required = false) String expirada,
                        Authentication authentication,
                        Model model) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        if (error != null) {
            model.addAttribute("erro", "Usuário ou senha inválidos. Tente novamente.");
        }
        if (logout != null) {
            model.addAttribute("sucesso", "Você saiu do sistema.");
        }
        if (expirada != null) {
            model.addAttribute("aviso", "Sua sessão foi encerrada porque a conta entrou em outro navegador ou dispositivo.");
        }
        if (senha != null) {
            model.addAttribute("sucesso", "Senha alterada com sucesso. Entre novamente com a nova senha.");
        }
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/conta")
    public String conta(Model model) {
        model.addAttribute("trocaSenhaDTO", new TrocaSenhaDTO());
        return "conta";
    }

    @PostMapping("/troca-senha")
    public String trocaSenha(@Valid @ModelAttribute("trocaSenhaDTO") TrocaSenhaDTO dto,
                             BindingResult result,
                             Authentication authentication,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             Model model) {
        String username = authentication.getName();
        var usuario = usuarioRepository.buscarPorUsername(username).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!result.hasFieldErrors("senhaAtual") && !passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenhaHash())) {
            result.rejectValue("senhaAtual", "invalida", "Senha atual incorreta.");
        }
        if (!result.hasFieldErrors("novaSenha") && !senhaForte(dto.getNovaSenha())) {
            result.rejectValue("novaSenha", "fraca", "A nova senha deve conter letras e números.");
        }
        if (!result.hasFieldErrors("confirmaNovaSenha") && !dto.getNovaSenha().equals(dto.getConfirmaNovaSenha())) {
            result.rejectValue("confirmaNovaSenha", "diferente", "A confirmação não coincide com a nova senha.");
        }
        if (result.hasErrors()) {
            model.addAttribute("erro", "Verifique os campos informados.");
            return "conta";
        }
        usuarioRepository.alterarSenha(usuario.getId(), passwordEncoder.encode(dto.getNovaSenha()));
        authAuditService.registrarAlteracaoSenha(username);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?senha=alterada";
    }

    private boolean senhaForte(String senha) {
        return senha.chars().anyMatch(Character::isLetter) && senha.chars().anyMatch(Character::isDigit);
    }
}
