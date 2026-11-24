package com.gabriel.bublo.service;

import com.gabriel.bublo.model.Aluno;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.Livro;
import com.gabriel.bublo.model.Reserva;
import com.gabriel.bublo.repository.AuditoriaRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservaServiceTest {

    private ReservaRepository reservaRepository;
    private ItemAcervoRepository itemAcervoRepository;
    private LeitorRepository leitorRepository;
    private ExemplarRepository exemplarRepository;
    private EmprestimoRepository emprestimoRepository;
    private ReservaService service;

    @BeforeEach
    void setUp() {
        reservaRepository = mock(ReservaRepository.class);
        itemAcervoRepository = mock(ItemAcervoRepository.class);
        leitorRepository = mock(LeitorRepository.class);
        exemplarRepository = mock(ExemplarRepository.class);
        emprestimoRepository = mock(EmprestimoRepository.class);
        service = new ReservaService(reservaRepository, itemAcervoRepository, leitorRepository,
            mock(NotificacaoRepository.class), exemplarRepository, emprestimoRepository,
            mock(AuditoriaRepository.class));

        Aluno aluno = new Aluno();
        aluno.setId(1L);
        aluno.setAtivo(true);
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno));
        Livro livro = new Livro();
        livro.setId(2L);
        livro.setTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO);
        when(itemAcervoRepository.buscarPorId(2L)).thenReturn(Optional.of(livro));
        when(itemAcervoRepository.estaAtivo(2L)).thenReturn(true);
        when(exemplarRepository.contarPorItem(2L)).thenReturn(1L);
    }

    @Test
    @DisplayName("Material com exemplar disponível não pode ser reservado")
    void materialDisponivelNaoEhReservavel() {
        when(exemplarRepository.buscarDisponiveisPorItem(2L)).thenReturn(List.of(new Exemplar()));

        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("disponíveis");
        verify(reservaRepository, never()).criar(any(), any(), any());
    }

    @Test
    @DisplayName("Prazo de retirada no passado é recusado")
    void prazoNoPassado() {
        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().minusDays(2), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("anterior a hoje");
    }

    @Test
    @DisplayName("Exemplar livre já comprometido com a fila não impede nova reserva")
    void exemplarComprometidoComAFilaPermiteReservar() {
        when(exemplarRepository.buscarDisponiveisPorItem(2L)).thenReturn(List.of(new Exemplar()));
        when(reservaRepository.listarPendentesPorItem(2L)).thenReturn(List.of(new Reserva()));
        Reserva criada = new Reserva();
        criada.setId(9L);
        criada.setPosicao(2);
        when(reservaRepository.criar(any(), any(), any())).thenReturn(criada);

        service.criar(1L, 2L, LocalDateTime.now().plusDays(3), "admin");

        verify(reservaRepository).criar(any(), any(), any());
    }

    @Test
    @DisplayName("Leitor que já está com o título emprestado não pode reservá-lo")
    void jaEmprestadoNaoReserva() {
        when(emprestimoRepository.leitorTemEmprestimoAtivoDoItem(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("já está com");
    }

    @Test
    @DisplayName("Material excluído não pode ser reservado")
    void materialExcluidoNaoReserva() {
        when(itemAcervoRepository.estaAtivo(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("excluído");
    }

    @Test
    @DisplayName("Reservas vencidas são expiradas")
    void expiraVencidas() {
        Reserva vencida = new Reserva();
        vencida.setId(33L);
        when(reservaRepository.listarExpiradasNaoProcessadas()).thenReturn(List.of(vencida));

        int total = service.expirarVencidas();

        org.assertj.core.api.Assertions.assertThat(total).isEqualTo(1);
        verify(reservaRepository).expirar(33L);
    }

    @Test
    @DisplayName("Título sem exemplares cadastrados não pode ser reservado")
    void semExemplaresCadastrados() {
        when(exemplarRepository.contarPorItem(2L)).thenReturn(0L);

        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("não tem exemplares cadastrados");
    }

    @Test
    @DisplayName("Prazo de retirada acima do teto de 30 dias é recusado")
    void prazoAcimaDoTeto() {
        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().plusDays(31), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("30 dias");
    }

    @Test
    @DisplayName("Mensagem explica que só se reserva quando todos os exemplares estão emprestados")
    void mensagemDeExemplarDisponivelEhClara() {
        when(exemplarRepository.buscarDisponiveisPorItem(2L)).thenReturn(List.of(new Exemplar()));

        assertThatThrownBy(() -> service.criar(1L, 2L, LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("todos os exemplares estão emprestados")
            .hasMessageContaining("empréstimo");
    }

    @Test
    @DisplayName("Mensagem de publicação digital explica o motivo")
    void mensagemDeDigitalEhClara() {
        com.gabriel.bublo.model.PublicacaoDigital digital = new com.gabriel.bublo.model.PublicacaoDigital();
        digital.setId(9L);
        digital.setTipo(com.gabriel.bublo.model.TipoMaterial.DIGITAL);
        when(itemAcervoRepository.buscarPorId(9L)).thenReturn(Optional.of(digital));
        when(itemAcervoRepository.estaAtivo(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(1L, 9L, LocalDateTime.now().plusDays(3), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("digitais não podem ser reservadas")
            .hasMessageContaining("apenas para consulta");
    }
}
