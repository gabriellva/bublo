package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ConfiguracaoDTO;
import com.gabriel.bublo.model.Aluno;
import com.gabriel.bublo.model.Emprestimo;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.Multa;
import com.gabriel.bublo.model.Reserva;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmprestimoServiceTest {

    private EmprestimoRepository emprestimoRepository;
    private LeitorRepository leitorRepository;
    private ExemplarRepository exemplarRepository;
    private MultaRepository multaRepository;
    private NotificacaoRepository notificacaoRepository;
    private ReservaRepository reservaRepository;
    private ConfiguracaoService configuracaoService;
    private EmprestimoService service;

    @BeforeEach
    void setUp() {
        emprestimoRepository = mock(EmprestimoRepository.class);
        leitorRepository = mock(LeitorRepository.class);
        exemplarRepository = mock(ExemplarRepository.class);
        multaRepository = mock(MultaRepository.class);
        notificacaoRepository = mock(NotificacaoRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        configuracaoService = mock(ConfiguracaoService.class);
        when(configuracaoService.buscarInteiro(any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(configuracaoService.buscarDecimal(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        service = new EmprestimoService(emprestimoRepository, leitorRepository, exemplarRepository,
            multaRepository, notificacaoRepository, reservaRepository, configuracaoService);
    }

    private Leitor aluno(boolean ativo) {
        Aluno aluno = new Aluno();
        aluno.setId(1L);
        aluno.setNome("Ana");
        aluno.setAtivo(ativo);
        return aluno;
    }

    private Emprestimo emprestimoAtivo(LocalDateTime prevista) {
        Exemplar exemplar = new Exemplar();
        exemplar.setId(9L);
        exemplar.setItemAcervoId(5L);
        Emprestimo e = new Emprestimo();
        e.setId(7L);
        e.setLeitor(aluno(true));
        e.setExemplar(exemplar);
        e.setStatus("ATIVO");
        e.setRenovacoes(0);
        e.setDataPrevistaDevolucao(prevista);
        return e;
    }

    @Test
    @DisplayName("Devolução com 3 dias de atraso gera multa com a taxa configurada")
    void devolucaoAtrasadaGeraMultaComTaxaDaConfiguracao() {
        when(emprestimoRepository.buscarPorId(7L))
            .thenReturn(Optional.of(emprestimoAtivo(LocalDate.now().minusDays(3).atStartOfDay())));
        when(configuracaoService.buscarDecimal("taxa_diaria_multa", EmprestimoService.TAXA_DIARIA_PADRAO))
            .thenReturn(new BigDecimal("1.50"));

        BigDecimal valor = service.devolver(7L, "admin");

        ArgumentCaptor<Multa> captor = ArgumentCaptor.forClass(Multa.class);
        verify(multaRepository).salvar(captor.capture());
        assertThat(captor.getValue().getDiasAtraso()).isEqualTo(3);
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("4.50");
        assertThat(captor.getValue().getLeitorId()).isEqualTo(1L);
        assertThat(valor).isEqualByComparingTo("4.50");
        verify(emprestimoRepository).devolver(7L);
    }

    @Test
    @DisplayName("Devolução na data prevista não gera multa")
    void devolucaoNoPrazoNaoGeraMulta() {
        when(emprestimoRepository.buscarPorId(7L))
            .thenReturn(Optional.of(emprestimoAtivo(LocalDate.now().atStartOfDay())));

        BigDecimal valor = service.devolver(7L, "admin");

        verify(multaRepository, never()).salvar(any());
        assertThat(valor).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Empréstimo respeita o limite configurado para alunos")
    void limiteVemDaConfiguracao() {
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno(true)));
        when(multaRepository.saldoPendentePorLeitor(1L)).thenReturn(BigDecimal.ZERO);
        when(emprestimoRepository.listarAtivosPorLeitor(1L)).thenReturn(new ArrayList<>(List.of(new Emprestimo())));
        when(configuracaoService.buscarInteiro("limite_alunos", EmprestimoService.LIMITE_ALUNOS_PADRAO)).thenReturn(2);

        assertThatThrownBy(() -> service.criar(1L, List.of(10L, 11L), LocalDateTime.now().plusDays(7), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Limite de exemplares");
    }

    @Test
    @DisplayName("Leitor inativo não pode emprestar")
    void leitorInativoNaoEmpresta() {
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno(false)));

        assertThatThrownBy(() -> service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(7), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("inativo");
    }

    @Test
    @DisplayName("Data prevista anterior a hoje é recusada")
    void dataNoPassadoEhRecusada() {
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno(true)));

        assertThatThrownBy(() -> service.criar(1L, List.of(10L), LocalDateTime.now().minusDays(1), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("anterior a hoje");
    }

    @Test
    @DisplayName("Sem exemplares selecionados o empréstimo é recusado")
    void semExemplares() {
        assertThatThrownBy(() -> service.criar(1L, List.of(), LocalDateTime.now().plusDays(7), "admin"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Renovação é bloqueada quando há reserva pendente para o material")
    void renovacaoBloqueadaPorReserva() {
        when(emprestimoRepository.buscarPorId(7L))
            .thenReturn(Optional.of(emprestimoAtivo(LocalDate.now().plusDays(2).atStartOfDay())));
        when(reservaRepository.listarPendentesPorItem(5L)).thenReturn(List.of(new Reserva()));

        assertThatThrownBy(() -> service.renovar(7L, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("reserva");
        verify(emprestimoRepository, never()).renovar(any(), any());
    }

    @Test
    @DisplayName("Renovação de empréstimo em atraso é recusada")
    void renovacaoEmAtraso() {
        when(emprestimoRepository.buscarPorId(7L))
            .thenReturn(Optional.of(emprestimoAtivo(LocalDate.now().minusDays(1).atStartOfDay())));

        assertThatThrownBy(() -> service.renovar(7L, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("atraso");
    }

    @Test
    @DisplayName("Renovação estende a data pelo prazo configurado")
    void renovacaoEstendePrazo() {
        LocalDateTime prevista = LocalDate.now().plusDays(2).atStartOfDay();
        when(emprestimoRepository.buscarPorId(7L)).thenReturn(Optional.of(emprestimoAtivo(prevista)));
        when(reservaRepository.listarPendentesPorItem(5L)).thenReturn(List.of());
        when(configuracaoService.buscarInteiro("prazo_alunos", EmprestimoService.PRAZO_ALUNOS_PADRAO)).thenReturn(10);

        service.renovar(7L, "admin");

        verify(emprestimoRepository).renovar(7L, prevista.plusDays(10));
    }

    @Test
    @DisplayName("Somente empréstimos ativos podem ser cancelados")
    void cancelarSoAtivo() {
        Emprestimo devolvido = emprestimoAtivo(LocalDateTime.now());
        devolvido.setStatus("DEVOLVIDO");
        when(emprestimoRepository.buscarPorId(7L)).thenReturn(Optional.of(devolvido));

        assertThatThrownBy(() -> service.cancelar(7L, "admin")).isInstanceOf(IllegalStateException.class);
        verify(emprestimoRepository, never()).cancelar(any());
    }

    @Test
    @DisplayName("Dias de atraso contam por data, não por hora")
    void diasDeAtrasoPorData() {
        LocalDate hoje = LocalDate.of(2026, 11, 13);
        assertThat(EmprestimoService.diasDeAtraso(LocalDate.of(2026, 11, 13).atStartOfDay(), hoje)).isZero();
        assertThat(EmprestimoService.diasDeAtraso(LocalDate.of(2026, 11, 12).atStartOfDay(), hoje)).isEqualTo(1);
        assertThat(EmprestimoService.diasDeAtraso(LocalDate.of(2026, 11, 20).atStartOfDay(), hoje)).isZero();
    }
}
