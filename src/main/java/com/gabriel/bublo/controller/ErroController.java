package com.gabriel.bublo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Página de erro para falhas fora dos controladores (404 de rotas, 403, erros de filtro).
 */
@Controller
public class ErroController implements ErrorController {

    @RequestMapping("/error")
    public String erro(HttpServletRequest request, Model model) {
        Object atributo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int codigo = atributo instanceof Integer valor ? valor : HttpStatus.INTERNAL_SERVER_ERROR.value();
        model.addAttribute("status", codigo);
        model.addAttribute("mensagem", mensagemPara(codigo));
        return "erro";
    }

    private String mensagemPara(int codigo) {
        return switch (codigo) {
            case 400 -> "Requisição inválida. Verifique os dados informados.";
            case 403 -> "Acesso negado. Sua sessão pode ter expirado: recarregue a página e tente novamente.";
            case 404 -> "Página não encontrada.";
            case 405 -> "Operação não permitida para este endereço.";
            default -> "Ocorreu um erro inesperado. Tente novamente.";
        };
    }
}
