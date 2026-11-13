package com.gabriel.bublo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tratador global de exceções.
 * Nunca exibe stack traces ou dados internos ao usuário.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ModelAndView handleRegraDeNegocio(RuntimeException ex) {
        String mensagem = ex.getMessage() != null ? ex.getMessage() : "Erro ao processar a operação.";
        return erro(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNaoEncontrado(NoResourceFoundException ex) {
        return erro(HttpStatus.NOT_FOUND, "Página não encontrada.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ModelAndView handleRequisicaoInvalida(Exception ex) {
        return erro(HttpStatus.BAD_REQUEST, "Requisição inválida. Verifique os dados informados.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMetodoNaoPermitido(HttpRequestMethodNotSupportedException ex) {
        return erro(HttpStatus.METHOD_NOT_ALLOWED, "Operação não permitida para este endereço.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleArquivoGrande(MaxUploadSizeExceededException ex) {
        return erro(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo enviado excede o tamanho máximo permitido.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente.");
    }

    private ModelAndView erro(HttpStatus status, String mensagem) {
        ModelAndView mav = new ModelAndView("erro");
        mav.addObject("mensagem", mensagem);
        mav.addObject("status", status.value());
        mav.setStatus(status);
        return mav;
    }
}
