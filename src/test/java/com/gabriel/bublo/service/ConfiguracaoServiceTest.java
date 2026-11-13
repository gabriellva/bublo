package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ConfiguracaoDTO;
import com.gabriel.bublo.model.Configuracao;
import com.gabriel.bublo.repository.ConfiguracaoRepository;
import com.gabriel.bublo.repository.AuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de ConfiguracaoService.
 */
class ConfiguracaoServiceTest {

    private ConfiguracaoRepository configuracaoRepository;
    private AuditoriaRepository auditoriaRepository;
    private ConfiguracaoService service;

    @BeforeEach
    void setUp() {
        configuracaoRepository = mock(ConfiguracaoRepository.class);
        auditoriaRepository = mock(AuditoriaRepository.class);
        service = new ConfiguracaoService(configuracaoRepository, auditoriaRepository);
    }

    @Test
    @DisplayName("Salvar configuração registra auditoria com antes e depois")
    void salvarRegistraAuditoria() {
        Configuracao anterior = new Configuracao();
        anterior.setChave("prazo_alunos");
        anterior.setValor("7");

        when(configuracaoRepository.buscar("prazo_alunos")).thenReturn(Optional.of(anterior));

        service.salvar("prazo_alunos", "14", "admin", "Ajuste de prazo");

        verify(configuracaoRepository).salvar(argThat(c ->
            c.getChave().equals("prazo_alunos") && c.getValor().equals("14")));
        verify(auditoriaRepository).registrar(eq("admin"), eq("ALTERACAO_CONFIGURACAO"),
            eq("Configuracoes"), eq("prazo_alunos"), eq("7"), eq("14"), eq("Ajuste de prazo"));
    }

    @Test
    @DisplayName("Justificativa obrigatória")
    void justificativaObrigatoria() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("prazo_alunos", "14", "admin", ""));
    }

    @Test
    @DisplayName("Listar todas as configurações")
    void listarTodas() {
        Configuracao c = new Configuracao();
        c.setChave("taxa_diaria_multa");
        c.setValor("2.00");
        when(configuracaoRepository.listarTodas()).thenReturn(List.of(c));

        List<ConfiguracaoDTO> resultado = service.listarTodas();
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getChave()).isEqualTo("taxa_diaria_multa");
    }
}
