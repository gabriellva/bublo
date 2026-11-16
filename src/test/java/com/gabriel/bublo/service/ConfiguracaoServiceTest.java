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

    private com.gabriel.bublo.model.Auditoria registro(String tipo, String chave, String antes, String depois) {
        com.gabriel.bublo.model.Auditoria a = new com.gabriel.bublo.model.Auditoria();
        a.setTipoOperacao(tipo);
        a.setRegistroAfectado(chave);
        a.setValoresAntes(antes);
        a.setValoresDepois(depois);
        return a;
    }

    @Test
    @DisplayName("Desfazer restaura o valor anterior e registra a reversão")
    void reverterUltima() {
        when(auditoriaRepository.ultimaDeConfiguracao())
            .thenReturn(registro("ALTERACAO_CONFIGURACAO", "prazo_alunos", "7", "14"));
        Configuracao atual = new Configuracao();
        atual.setChave("prazo_alunos");
        atual.setValor("14");
        when(configuracaoRepository.buscar("prazo_alunos")).thenReturn(Optional.of(atual));

        String chave = service.reverterUltima("admin", "Foi engano");

        assertThat(chave).isEqualTo("prazo_alunos");
        verify(configuracaoRepository).salvar(argThat(c -> c.getValor().equals("7")));
        verify(auditoriaRepository).registrar(eq("admin"), eq("REVERSAO_CONFIGURACAO"), eq("Configuracoes"),
            eq("prazo_alunos"), eq("14"), eq("7"), eq("Foi engano"));
    }

    @Test
    @DisplayName("Uma reversão não pode ser desfeita de novo")
    void naoReverteReversao() {
        when(auditoriaRepository.ultimaDeConfiguracao())
            .thenReturn(registro("REVERSAO_CONFIGURACAO", "prazo_alunos", "14", "7"));

        assertThatIllegalArgumentException().isThrownBy(() -> service.reverterUltima("admin", "x"));
        verify(configuracaoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Desfazer exige justificativa")
    void reverterExigeJustificativa() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.reverterUltima("admin", " "));
    }

    @Test
    @DisplayName("Prefixo de tombamento aceita só letras (1 a 10)")
    void prefixoSoLetras() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("prefixo_tombamento", "B4-", "admin", "teste"));
        assertThatIllegalArgumentException().isThrownBy(() ->
            service.salvar("prefixo_tombamento", "ABCDEFGHIJK", "admin", "teste"));
        service.salvar("prefixo_tombamento", "LIB", "admin", "teste");
        verify(configuracaoRepository).salvar(argThat(c -> c.getValor().equals("LIB")));
    }

    @Test
    @DisplayName("Limites superiores das configurações numéricas")
    void limitesSuperiores() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar("limite_alunos", "51", "admin", "x"));
        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar("prazo_alunos", "366", "admin", "x"));
        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar("prazo_retirada_reserva", "31", "admin", "x"));
        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar("limite_alunos", "99999999999", "admin", "x"));
        service.salvar("limite_alunos", "50", "admin", "x");
    }

    @Test
    @DisplayName("Taxa de multa: no máximo 2 casas decimais e até R$ 1.000")
    void taxaDeMulta() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar("taxa_diaria_multa", "2.005", "admin", "x"));
        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar("taxa_diaria_multa", "1000.01", "admin", "x"));
        service.salvar("taxa_diaria_multa", "2.50", "admin", "x");
        service.salvar("taxa_diaria_multa", "2.500", "admin", "x");
    }
}
