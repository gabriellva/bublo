package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ItemAcervoDTO;
import com.gabriel.bublo.model.ItemAcervo;
import com.gabriel.bublo.model.Livro;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcervoServiceTest {

    private ItemAcervoRepository itemAcervoRepository;
    private AcervoService service;

    @BeforeEach
    void setUp() {
        itemAcervoRepository = mock(ItemAcervoRepository.class);
        service = new AcervoService(itemAcervoRepository, mock(ExemplarRepository.class));
        when(itemAcervoRepository.salvar(any())).thenAnswer(inv -> {
            ItemAcervo item = inv.getArgument(0);
            item.setId(1L);
            return item;
        });
    }

    private ItemAcervoDTO livro() {
        ItemAcervoDTO dto = new ItemAcervoDTO();
        dto.setTipo("LIVRO");
        dto.setTitulo("Livro");
        return dto;
    }

    @Test
    @DisplayName("ISBN em branco é gravado como nulo para não colidir no índice único")
    void isbnEmBrancoViraNulo() {
        ItemAcervoDTO dto = livro();
        dto.setIsbn("  ");

        service.salvar(dto, "admin");

        ArgumentCaptor<ItemAcervo> captor = ArgumentCaptor.forClass(ItemAcervo.class);
        verify(itemAcervoRepository).salvar(captor.capture());
        assertThat(((Livro) captor.getValue()).getIsbn()).isNull();
    }

    @Test
    @DisplayName("Idioma ausente assume Português (coluna é NOT NULL)")
    void idiomaPadrao() {
        service.salvar(livro(), "admin");

        ArgumentCaptor<ItemAcervo> captor = ArgumentCaptor.forClass(ItemAcervo.class);
        verify(itemAcervoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getIdioma()).isEqualTo("Português");
    }

    @Test
    @DisplayName("ISBN duplicado vira mensagem de negócio, não erro 500")
    void isbnDuplicado() {
        org.mockito.Mockito.doThrow(new DuplicateKeyException("idx_item_acervo_isbn")).when(itemAcervoRepository).salvar(any());

        assertThatThrownBy(() -> service.salvar(livro(), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ISBN");
    }

    @Test
    @DisplayName("Tipo de material inválido é recusado")
    void tipoInvalido() {
        ItemAcervoDTO dto = livro();
        dto.setTipo("CD");

        assertThatThrownBy(() -> service.salvar(dto, "admin")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Cadastro de exemplares em lote limita a quantidade")
    void loteLimitado() {
        assertThatThrownBy(() -> service.cadastrarExemplaresLote(1L, 101, "admin"))
            .isInstanceOf(IllegalStateException.class);
    }
}
