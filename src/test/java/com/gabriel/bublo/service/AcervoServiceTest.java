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
    private ExemplarRepository exemplarRepository;
    private ConfiguracaoService configuracaoService;
    private AcervoService service;

    @BeforeEach
    void setUp() {
        itemAcervoRepository = mock(ItemAcervoRepository.class);
        exemplarRepository = mock(ExemplarRepository.class);
        configuracaoService = mock(ConfiguracaoService.class);
        service = new AcervoService(itemAcervoRepository, exemplarRepository, configuracaoService,
            mock(com.gabriel.bublo.repository.AuditoriaRepository.class));
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

    private ItemAcervo itemDoTipo(com.gabriel.bublo.model.TipoMaterial tipo, boolean ativo) {
        Livro l = new Livro();
        l.setId(7L);
        l.setTipo(tipo);
        l.setTitulo("Titulo");
        when(itemAcervoRepository.buscarPorId(7L)).thenReturn(java.util.Optional.of(l));
        when(itemAcervoRepository.estaAtivo(7L)).thenReturn(ativo);
        return l;
    }

    @Test
    @DisplayName("Publicação digital não recebe exemplares")
    void digitalSemExemplares() {
        itemDoTipo(com.gabriel.bublo.model.TipoMaterial.DIGITAL, true);

        assertThatThrownBy(() -> service.cadastrarExemplaresLote(7L, 1, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("digitais");
        org.mockito.Mockito.verifyNoInteractions(exemplarRepository);
    }

    @Test
    @DisplayName("Material excluído não recebe exemplares")
    void excluidoSemExemplares() {
        itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, false);

        assertThatThrownBy(() -> service.cadastrarExemplaresLote(7L, 1, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("excluído");
    }

    @Test
    @DisplayName("Novos exemplares usam o prefixo de tombamento configurado")
    void usaPrefixoConfigurado() {
        ItemAcervo item = itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, true);
        com.gabriel.bublo.dto.ConfiguracaoDTO cfg = new com.gabriel.bublo.dto.ConfiguracaoDTO();
        cfg.setChave("prefixo_tombamento");
        cfg.setValor("lib");
        when(configuracaoService.buscar("prefixo_tombamento")).thenReturn(cfg);

        service.cadastrarExemplaresLote(7L, 2, "admin");

        verify(exemplarRepository).salvarLote(item, 2, "LIB");
    }

    @Test
    @DisplayName("Prefixo inválido na configuração cai no padrão BA")
    void prefixoInvalidoUsaPadrao() {
        ItemAcervo item = itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, true);
        com.gabriel.bublo.dto.ConfiguracaoDTO cfg = new com.gabriel.bublo.dto.ConfiguracaoDTO();
        cfg.setValor("12!");
        when(configuracaoService.buscar("prefixo_tombamento")).thenReturn(cfg);

        service.cadastrarExemplaresLote(7L, 1, "admin");

        verify(exemplarRepository).salvarLote(item, 1, "BA");
    }

    @Test
    @DisplayName("O tipo do material não pode mudar na edição")
    void tipoImutavel() {
        itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, true);
        ItemAcervoDTO dto = new ItemAcervoDTO();
        dto.setId(7L);
        dto.setTipo("DVD");
        dto.setTitulo("Titulo");

        assertThatThrownBy(() -> service.salvar(dto, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("tipo");
    }

    @Test
    @DisplayName("Mesmo autor digitado com outra caixa ou espaços entra uma única vez")
    void autorDuplicadoColapsa() {
        when(itemAcervoRepository.criarOuReutilizarAutor(any())).thenReturn(10L);
        ItemAcervoDTO dto = livro();
        dto.setNovosAutores(new java.util.ArrayList<>(java.util.List.of("Machado de Assis", "machado  de assis ")));

        service.salvar(dto, "admin");

        ArgumentCaptor<ItemAcervo> captor = ArgumentCaptor.forClass(ItemAcervo.class);
        verify(itemAcervoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getAutores()).hasSize(1);
    }

    @Test
    @DisplayName("Nome de autor maior que 150 caracteres é recusado")
    void autorGrande() {
        ItemAcervoDTO dto = livro();
        dto.setNovosAutores(new java.util.ArrayList<>(java.util.List.of("a".repeat(151))));

        assertThatThrownBy(() -> service.salvar(dto, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("150");
    }

    private com.gabriel.bublo.model.Exemplar exemplar(boolean disponivel, boolean baixado) {
        com.gabriel.bublo.model.Exemplar ex = new com.gabriel.bublo.model.Exemplar();
        ex.setId(50L);
        ex.setItemAcervoId(7L);
        ex.setTombamento("BA-000050");
        ex.setDisponivel(disponivel);
        ex.setBaixado(baixado);
        when(exemplarRepository.buscarPorId(50L)).thenReturn(java.util.Optional.of(ex));
        return ex;
    }

    @Test
    @DisplayName("Exemplar nunca emprestado é apagado de vez")
    void excluirExemplarSemHistorico() {
        exemplar(true, false);
        when(exemplarRepository.contarPorItem(7L)).thenReturn(3L);
        when(exemplarRepository.temHistoricoDeEmprestimos(50L)).thenReturn(false);

        boolean apagado = service.excluirExemplar(50L, "admin");

        assertThat(apagado).isTrue();
        verify(exemplarRepository).excluir(50L);
        org.mockito.Mockito.verify(exemplarRepository, org.mockito.Mockito.never()).baixar(any(), any());
    }

    @Test
    @DisplayName("Exemplar com histórico recebe baixa e o histórico é preservado")
    void excluirExemplarComHistoricoDaBaixa() {
        exemplar(true, false);
        when(exemplarRepository.contarPorItem(7L)).thenReturn(3L);
        when(exemplarRepository.temHistoricoDeEmprestimos(50L)).thenReturn(true);

        boolean apagado = service.excluirExemplar(50L, "admin");

        assertThat(apagado).isFalse();
        verify(exemplarRepository).baixar(org.mockito.ArgumentMatchers.eq(50L), any());
        org.mockito.Mockito.verify(exemplarRepository, org.mockito.Mockito.never()).excluir(any());
    }

    @Test
    @DisplayName("Exemplar emprestido não pode ser excluído")
    void exemplarEmprestidoNaoSai() {
        exemplar(false, false);

        assertThatThrownBy(() -> service.excluirExemplar(50L, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("emprestado");
        org.mockito.Mockito.verify(exemplarRepository, org.mockito.Mockito.never()).excluir(any());
    }

    @Test
    @DisplayName("Exemplar já excluído não pode ser excluído de novo")
    void exemplarJaBaixado() {
        exemplar(false, true);

        assertThatThrownBy(() -> service.excluirExemplar(50L, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("já foi excluído");
    }

    @Test
    @DisplayName("Último exemplar com reservas aguardando não pode ser excluído")
    void ultimoExemplarComReservas() {
        exemplar(true, false);
        when(exemplarRepository.contarPorItem(7L)).thenReturn(1L);
        when(itemAcervoRepository.existeReservaPendente(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluirExemplar(50L, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("reservas");
    }
}
