package com.gabriel.bublo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tratador global de exceções.
 * Nunca exibe stack traces ou dados internos ao usuário.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleIllegalState(IllegalStateException ex) {
        ModelAndView mav = new ModelAndView("erro");
        mav.addObject("mensagem", ex.getMessage() != null ? ex.getMessage() : "Erro ao processar a operação.");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        ModelAndView mav = new ModelAndView("erro");
        mav.addObject("mensagem", "Ocorreu um erro inesperado. Tente novamente.");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
