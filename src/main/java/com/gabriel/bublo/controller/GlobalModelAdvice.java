package com.gabriel.bublo.controller;

import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.service.ConfiguracaoService;
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

    private final NotificacaoRepository notificacaoRepository;
    private final ConfiguracaoService configuracaoService;

    public GlobalModelAdvice(NotificacaoRepository notificacaoRepository,
                             ConfiguracaoService configuracaoService) {
        this.notificacaoRepository = notificacaoRepository;
        this.configuracaoService = configuracaoService;
    }

    @ModelAttribute
    public void adicionarGlobais(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
            ? auth.getName() : null;
        model.addAttribute("usuario", usuario);

        try {
            var temaCfg = configuracaoService.buscar("tema_usuario");
            model.addAttribute("temaUsuario", temaCfg != null ? temaCfg.getValor() : "claro");
        } catch (Exception e) {
            model.addAttribute("temaUsuario", "claro");
        }

        try {
            model.addAttribute("naoLidas", notificacaoRepository.contarNaoLidas());
        } catch (Exception e) {
            model.addAttribute("naoLidas", 0);
        }

        try {
            var ni = configuracaoService.buscar("nome_instituicao");
            model.addAttribute("nomeInstituicao", ni != null ? ni.getValor() : "Biblioteca Acadêmica");
        } catch (Exception e) {
            model.addAttribute("nomeInstituicao", "Biblioteca Acadêmica");
        }
    }
}
