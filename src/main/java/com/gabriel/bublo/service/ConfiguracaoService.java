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
        "limite_alunos", "limite_professores",
        "prazo_alunos", "prazo_professores", "taxa_diaria_multa", "prazo_retirada_reserva");

    private static final java.util.Set<String> CHAVES_INTEIRAS = java.util.Set.of(
        "limite_alunos", "limite_professores", "prazo_alunos", "prazo_professores", "prazo_retirada_reserva");

    private static final java.util.Map<String, Integer> MAXIMOS_INTEIROS = java.util.Map.of(
        "limite_alunos", 50, "limite_professores", 50,
        "prazo_alunos", 365, "prazo_professores", 365, "prazo_retirada_reserva", 30);

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

    /** Última alteração que ainda pode ser desfeita, ou null (não há, ou a última já foi uma reversão). */
    @Transactional(readOnly = true)
    public com.gabriel.bublo.model.Auditoria ultimaAlteracaoReversivel() {
        var ultima = auditoriaRepository.ultimaDeConfiguracao();
        if (ultima == null || !"ALTERACAO_CONFIGURACAO".equals(ultima.getTipoOperacao())
            || ultima.getValoresAntes() == null) {
            return null;
        }
        return ultima;
    }

    @Transactional
    public String reverterUltima(String adminUsuario, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória para desfazer uma configuração");
        }
        var ultima = ultimaAlteracaoReversivel();
        if (ultima == null) {
            throw new IllegalArgumentException("Não há alteração de configuração que possa ser desfeita.");
        }
        String chave = ultima.getRegistroAfectado();
        validarValor(chave, ultima.getValoresAntes());
        Configuracao atual = configuracaoRepository.buscar(chave).orElse(null);
        Configuracao c = new Configuracao();
        c.setChave(chave);
        c.setValor(ultima.getValoresAntes());
        c.setAtualizadoPor(adminUsuario);
        configuracaoRepository.salvar(c);
        auditoriaRepository.registrar(adminUsuario, "REVERSAO_CONFIGURACAO", "Configuracoes",
            chave, atual != null ? atual.getValor() : null, ultima.getValoresAntes(), justificativa.trim());
        return chave;
    }

    private void validarValor(String chave, String valor) {
        if (!CHAVES_INTEIRAS.contains(chave) && !"taxa_diaria_multa".equals(chave)) {
            throw new IllegalArgumentException("Configuração desconhecida: " + chave);
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O valor da configuração é obrigatório");
        }
        try {
            if (CHAVES_INTEIRAS.contains(chave)) {
                int numero = Integer.parseInt(valor.trim());
                int maximo = MAXIMOS_INTEIROS.get(chave);
                if (numero < 1 || numero > maximo) {
                    throw new IllegalArgumentException("O valor de " + chave + " deve estar entre 1 e " + maximo);
                }
            }
            if ("taxa_diaria_multa".equals(chave)) {
                java.math.BigDecimal taxa = new java.math.BigDecimal(valor.trim());
                if (taxa.signum() < 0) {
                    throw new IllegalArgumentException("A taxa diária não pode ser negativa");
                }
                if (taxa.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("A taxa diária aceita no máximo duas casas decimais");
                }
                if (taxa.compareTo(new java.math.BigDecimal("1000")) > 0) {
                    throw new IllegalArgumentException("A taxa diária não pode passar de R$ 1.000,00");
                }
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
