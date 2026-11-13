package com.gabriel.bublo.service;

import com.gabriel.bublo.model.Aluno;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.model.Livro;
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
    private ReservaService service;

    @BeforeEach
    void setUp() {
        reservaRepository = mock(ReservaRepository.class);
        itemAcervoRepository = mock(ItemAcervoRepository.class);
        leitorRepository = mock(LeitorRepository.class);
        exemplarRepository = mock(ExemplarRepository.class);
        service = new ReservaService(reservaRepository, itemAcervoRepository, leitorRepository,
            mock(NotificacaoRepository.class), exemplarRepository, mock(EmprestimoRepository.class));

        Aluno aluno = new Aluno();
        aluno.setId(1L);
        aluno.setAtivo(true);
        when(leitorRepository.buscarPorId(1L)).thenReturn(Optional.of(aluno));
        Livro livro = new Livro();
        livro.setId(2L);
        livro.setTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO);
        when(itemAcervoRepository.buscarPorId(2L)).thenReturn(Optional.of(livro));
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
}
