package com.gabriel.bublo.security;

import com.gabriel.bublo.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de auditoria de autenticação (tentativas falhas, alteração de senha).
 */
@Service
public class AuthAuditService {

    private final AuditoriaRepository auditoriaRepository;

    public AuthAuditService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public void registrarFalha(String username) {
        auditoriaRepository.registrar(username, "LOGIN_FALHA", "Seguranca", username, null, null, "Tentativa de login falhada");
    }

    @Transactional
    public void registrarSucesso(String username) {
        auditoriaRepository.registrar(username, "LOGIN", "Seguranca", username, null, null, "Login realizado com sucesso");
    }

    @Transactional
    public void registrarAlteracaoSenha(String username) {
        auditoriaRepository.registrar(username, "ALTERACAO_SENHA", "Seguranca", username, null, null, "Senha do administrador alterada");
    }
}
