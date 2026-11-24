package com.gabriel.bublo.controller;

import com.gabriel.bublo.repository.NotificacaoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos globais para todas as views Thymeleaf.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final String NOME_INSTITUICAO = "Biblioteca Acadêmica";

    private final NotificacaoRepository notificacaoRepository;

    public GlobalModelAdvice(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @ModelAttribute
    public void adicionarGlobais(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
            ? auth.getName() : null;
        model.addAttribute("usuario", usuario);

        try {
            model.addAttribute("naoLidas", notificacaoRepository.contarNaoLidas());
        } catch (Exception e) {
            model.addAttribute("naoLidas", 0);
        }

        model.addAttribute("nomeInstituicao", NOME_INSTITUICAO);
    }
}
