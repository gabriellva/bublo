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

    public static final java.util.List<String> CHAVES_EDITAVEIS = java.util.List.of(
        "nome_instituicao", "prefixo_tombamento", "limite_alunos", "limite_professores",
        "prazo_alunos", "prazo_professores", "taxa_diaria_multa", "prazo_retirada_reserva");

    private static final java.util.Set<String> CHAVES_INTEIRAS = java.util.Set.of(
        "limite_alunos", "limite_professores", "prazo_alunos", "prazo_professores", "prazo_retirada_reserva");

    private static final java.util.Set<String> CHAVES_TEXTO = java.util.Set.of("nome_instituicao", "prefixo_tombamento");

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

    @Transactional(readOnly = true)
    public int buscarInteiro(String chave, int padrao) {
        ConfiguracaoDTO cfg = buscar(chave);
        if (cfg == null) return padrao;
        try {
            return Integer.parseInt(cfg.getValor().trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal buscarDecimal(String chave, java.math.BigDecimal padrao) {
        ConfiguracaoDTO cfg = buscar(chave);
        if (cfg == null) return padrao;
        try {
            return new java.math.BigDecimal(cfg.getValor().trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    @Transactional
    public int salvarVarias(java.util.Map<String, String> novosValores, String adminUsuario, String justificativa) {
        int alteradas = 0;
        for (String chave : CHAVES_EDITAVEIS) {
            String novo = novosValores.get(chave);
            if (novo == null) continue;
            ConfiguracaoDTO atual = buscar(chave);
            if (atual != null && novo.trim().equals(atual.getValor())) continue;
            salvar(chave, novo.trim(), adminUsuario, justificativa);
            alteradas++;
        }
        return alteradas;
    }

    @Transactional
    public void salvar(String chave, String valor, String adminUsuario, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória para alterar configuração");
        }
        validarValor(chave, valor);
        Configuracao anterior = configuracaoRepository.buscar(chave).orElse(null);
        Configuracao c = new Configuracao();
        c.setChave(chave);
        c.setValor(valor);
        c.setAtualizadoPor(adminUsuario);
        configuracaoRepository.salvar(c);
        auditoriaRepository.registrar(adminUsuario, "ALTERACAO_CONFIGURACAO", "Configuracoes",
            chave, anterior != null ? anterior.getValor() : null, valor, justificativa);
    }

    private void validarValor(String chave, String valor) {
        if (!CHAVES_INTEIRAS.contains(chave) && !CHAVES_TEXTO.contains(chave) && !"taxa_diaria_multa".equals(chave)) {
            throw new IllegalArgumentException("Configuração desconhecida: " + chave);
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O valor da configuração é obrigatório");
        }
        try {
            if (CHAVES_INTEIRAS.contains(chave) && Integer.parseInt(valor.trim()) < 1) {
                throw new IllegalArgumentException("O valor deve ser um número inteiro maior que zero");
            }
            if ("taxa_diaria_multa".equals(chave) && new java.math.BigDecimal(valor.trim()).signum() < 0) {
                throw new IllegalArgumentException("A taxa diária não pode ser negativa");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico inválido para " + chave);
        }
    }

    private ConfiguracaoDTO toDTO(Configuracao c) {
        ConfiguracaoDTO dto = new ConfiguracaoDTO();
        dto.setChave(c.getChave());
        dto.setValor(c.getValor());
        return dto;
    }
}
