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

    @Test
    @DisplayName("Chave desconhecida é recusada")
    void chaveDesconhecida() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("qualquer_coisa", "1", "admin", "teste"));
        verify(configuracaoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Valor numérico inválido ou zero é recusado")
    void valorNumericoInvalido() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("limite_alunos", "abc", "admin", "teste"));
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("prazo_alunos", "0", "admin", "teste"));
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("taxa_diaria_multa", "-1", "admin", "teste"));
    }

    @Test
    @DisplayName("Valor inteiro lido da configuração, com padrão quando ausente ou inválido")
    void buscarInteiroComPadrao() {
        Configuracao c = new Configuracao();
        c.setChave("limite_alunos");
        c.setValor("4");
        when(configuracaoRepository.buscar("limite_alunos")).thenReturn(Optional.of(c));
        when(configuracaoRepository.buscar("prazo_alunos")).thenReturn(Optional.empty());

        assertThat(service.buscarInteiro("limite_alunos", 3)).isEqualTo(4);
        assertThat(service.buscarInteiro("prazo_alunos", 7)).isEqualTo(7);
    }

    @Test
    @DisplayName("Salvar várias configurações grava apenas as que mudaram")
    void salvarVariasGravaSoAlteradas() {
        Configuracao atual = new Configuracao();
        atual.setChave("limite_alunos");
        atual.setValor("3");
        when(configuracaoRepository.buscar("limite_alunos")).thenReturn(Optional.of(atual));
        Configuracao prazo = new Configuracao();
        prazo.setChave("prazo_alunos");
        prazo.setValor("7");
        when(configuracaoRepository.buscar("prazo_alunos")).thenReturn(Optional.of(prazo));

        int alteradas = service.salvarVarias(
            java.util.Map.of("limite_alunos", "3", "prazo_alunos", "10"), "admin", "ajuste");

        assertThat(alteradas).isEqualTo(1);
        verify(configuracaoRepository).salvar(argThat(c -> c.getChave().equals("prazo_alunos") && c.getValor().equals("10")));
    }
}
