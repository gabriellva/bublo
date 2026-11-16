package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ConfiguracaoDTO;
import com.gabriel.bublo.model.Aluno;
import com.gabriel.bublo.model.Emprestimo;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.Multa;
import com.gabriel.bublo.model.Reserva;
import com.gabriel.bublo.model.Livro;
import com.gabriel.bublo.model.TipoMaterial;
import com.gabriel.bublo.repository.AuditoriaRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
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
    private ItemAcervoRepository itemAcervoRepository;
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
        itemAcervoRepository = mock(ItemAcervoRepository.class);
        when(configuracaoService.buscarInteiro(any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(configuracaoService.buscarDecimal(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        service = new EmprestimoService(emprestimoRepository, leitorRepository, exemplarRepository,
            multaRepository, notificacaoRepository, reservaRepository, configuracaoService,
            itemAcervoRepository, mock(AuditoriaRepository.class));
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

        when(emprestimoRepository.devolver(7L)).thenReturn(true);

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

        when(emprestimoRepository.devolver(7L)).thenReturn(true);

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
        verify(emprestimoRepository, never()).renovar(any(), any(), org.mockito.ArgumentMatchers.anyInt());
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

        when(emprestimoRepository.renovar(7L, prevista.plusDays(10), EmprestimoService.MAXIMO_RENOVACOES)).thenReturn(true);

        service.renovar(7L, "admin");

        verify(emprestimoRepository).renovar(7L, prevista.plusDays(10), EmprestimoService.MAXIMO_RENOVACOES);
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

    // ---- Regras corrigidas na verificação de lógica (docs/AUDITORIA_LOGICA.md) ----

    private Exemplar exemplarDisponivel(long id, long itemId) {
        Exemplar ex = new Exemplar();
        ex.setId(id);
        ex.setItemAcervoId(itemId);
        ex.setTombamento("BA-00000" + id);
        ex.setDisponivel(true);
        when(exemplarRepository.buscarPorId(id)).thenReturn(Optional.of(ex));
        return ex;
    }

    private void itemFisico(long itemId, TipoMaterial tipo) {
        Livro item = new Livro();
        item.setId(itemId);
        item.setTipo(tipo);
        when(itemAcervoRepository.buscarPorId(itemId)).thenReturn(Optional.of(item));
        when(itemAcervoRepository.estaAtivo(itemId)).thenReturn(true);
    }

    private void leitorApto() {
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno(true)));
        when(multaRepository.saldoPendentePorLeitor(1L)).thenReturn(BigDecimal.ZERO);
        when(emprestimoRepository.listarAtivosPorLeitor(1L)).thenReturn(new ArrayList<>());
        when(emprestimoRepository.criar(any(), any(), any())).thenAnswer(inv -> {
            Emprestimo e = new Emprestimo();
            e.setId(50L);
            return e;
        });
    }

    private Reserva reservaDe(long reservaId, long leitorId) {
        Reserva r = new Reserva();
        r.setId(reservaId);
        Aluno a = new Aluno();
        a.setId(leitorId);
        r.setLeitor(a);
        return r;
    }

    @Test
    @DisplayName("Data prevista além do prazo padrão do tipo de leitor é recusada")
    void dataAlemDoPrazoRecusada() {
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno(true)));

        assertThatThrownBy(() -> service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(8), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ultrapassar");
    }

    @Test
    @DisplayName("Publicação digital não pode ser emprestada")
    void digitalNaoEmpresta() {
        leitorApto();
        exemplarDisponivel(10L, 5L);
        itemFisico(5L, TipoMaterial.DIGITAL);

        assertThatThrownBy(() -> service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("digitais");
        verify(emprestimoRepository, never()).criar(any(), any(), any());
    }

    @Test
    @DisplayName("Material excluído do acervo não pode ser emprestado")
    void materialExcluidoNaoEmpresta() {
        leitorApto();
        exemplarDisponivel(10L, 5L);
        itemFisico(5L, TipoMaterial.LIVRO);
        when(itemAcervoRepository.estaAtivo(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("excluído");
    }

    @Test
    @DisplayName("Exemplar devolvido e reservado por outro leitor não pode ser levado por quem está fora da fila")
    void filaTemPrioridade() {
        leitorApto();
        exemplarDisponivel(10L, 5L);
        itemFisico(5L, TipoMaterial.LIVRO);
        when(exemplarRepository.buscarDisponiveisPorItem(5L)).thenReturn(List.of(new Exemplar()));
        when(reservaRepository.listarPendentesPorItem(5L)).thenReturn(List.of(reservaDe(80L, 2L)));

        assertThatThrownBy(() -> service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("reservado");
        verify(emprestimoRepository, never()).criar(any(), any(), any());
    }

    @Test
    @DisplayName("Primeiro da fila empresta e a reserva dele passa a atendida")
    void primeiroDaFilaEmprestaEAtendeReserva() {
        leitorApto();
        exemplarDisponivel(10L, 5L);
        itemFisico(5L, TipoMaterial.LIVRO);
        when(exemplarRepository.buscarDisponiveisPorItem(5L)).thenReturn(List.of(new Exemplar()));
        when(reservaRepository.listarPendentesPorItem(5L))
            .thenReturn(List.of(reservaDe(80L, 1L), reservaDe(81L, 2L)));

        service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(3), "admin");

        verify(emprestimoRepository).criar(any(), any(), any());
        verify(reservaRepository).atender(80L);
        verify(reservaRepository, never()).atender(81L);
    }

    @Test
    @DisplayName("Com exemplares sobrando além da fila, quem está fora dela também pode emprestar")
    void sobraAlemDaFila() {
        leitorApto();
        exemplarDisponivel(10L, 5L);
        itemFisico(5L, TipoMaterial.LIVRO);
        when(exemplarRepository.buscarDisponiveisPorItem(5L)).thenReturn(List.of(new Exemplar(), new Exemplar()));
        when(reservaRepository.listarPendentesPorItem(5L)).thenReturn(List.of(reservaDe(80L, 2L)));

        service.criar(1L, List.of(10L), LocalDateTime.now().plusDays(3), "admin");

        verify(emprestimoRepository).criar(any(), any(), any());
        verify(reservaRepository, never()).atender(any());
    }

    @Test
    @DisplayName("Devolução já processada por outra requisição não gera multa duplicada")
    void devolucaoConcorrenteNaoDuplicaMulta() {
        when(emprestimoRepository.buscarPorId(7L))
            .thenReturn(Optional.of(emprestimoAtivo(LocalDate.now().minusDays(3).atStartOfDay())));
        when(emprestimoRepository.devolver(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.devolver(7L, "admin")).isInstanceOf(IllegalStateException.class);
        verify(multaRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Taxa diária zero não gera multa de R$ 0,00")
    void taxaZeroNaoGeraMulta() {
        when(emprestimoRepository.buscarPorId(7L))
            .thenReturn(Optional.of(emprestimoAtivo(LocalDate.now().minusDays(3).atStartOfDay())));
        when(emprestimoRepository.devolver(7L)).thenReturn(true);
        when(configuracaoService.buscarDecimal("taxa_diaria_multa", EmprestimoService.TAXA_DIARIA_PADRAO))
            .thenReturn(BigDecimal.ZERO);

        BigDecimal valor = service.devolver(7L, "admin");

        verify(multaRepository, never()).salvar(any());
        assertThat(valor).isEqualByComparingTo("0");
    }
}
