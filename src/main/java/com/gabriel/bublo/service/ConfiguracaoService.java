package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ConfiguracaoDTO;
import com.gabriel.bublo.model.Configuracao;
import com.gabriel.bublo.repository.ConfiguracaoRepository;
import com.gabriel.bublo.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de configurações do sistema.
 * Regras: alterações valem para novas operações; auditoria com justificativa;
 * reversão apenas da última alteração.
 */
@Service
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final AuditoriaRepository auditoriaRepository;

    public ConfiguracaoService(ConfiguracaoRepository configuracaoRepository,
                               AuditoriaRepository auditoriaRepository) {
        this.configuracaoRepository = configuracaoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoDTO> listarTodas() {
        return configuracaoRepository.listarTodas().stream()
            .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConfiguracaoDTO buscar(String chave) {
        return configuracaoRepository.buscar(chave).map(this::toDTO).orElse(null);
    }

    @Transactional
    public void salvar(String chave, String valor, String adminUsuario, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória para alterar configuração");
        }
        Configuracao anterior = configuracaoRepository.buscar(chave).orElse(null);
        Configuracao c = new Configuracao();
        c.setChave(chave);
        c.setValor(valor);
        c.setAtualizadoPor(adminUsuario);
        configuracaoRepository.salvar(c);
        auditoriaRepository.registrar(adminUsuario, "ALTERACAO_CONFIGURACAO", "Configuracoes",
            chave, anterior != null ? anterior.getValor() : null, valor, justificativa);
    }

    private ConfiguracaoDTO toDTO(Configuracao c) {
        ConfiguracaoDTO dto = new ConfiguracaoDTO();
        dto.setChave(c.getChave());
        dto.setValor(c.getValor());
        return dto;
    }
}
