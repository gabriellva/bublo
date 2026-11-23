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
    private AcervoService service;

    @BeforeEach
    void setUp() {
        itemAcervoRepository = mock(ItemAcervoRepository.class);
        exemplarRepository = mock(ExemplarRepository.class);
        service = new AcervoService(itemAcervoRepository, exemplarRepository,
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
    @DisplayName("Novos exemplares cadastrados em lote usam o prefixo de tombamento padrão")
    void usaPrefixoPadrao() {
        ItemAcervo item = itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, true);

        service.cadastrarExemplaresLote(7L, 2, "admin");

        verify(exemplarRepository).salvarLote(item, 2);
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

    // ---------- Capa ----------

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private org.springframework.mock.web.MockMultipartFile arquivo(String nome, String tipo, byte[] dados) {
        return new org.springframework.mock.web.MockMultipartFile("capa", nome, tipo, dados);
    }

    @Test
    @DisplayName("Capa PNG válida é gravada com o tipo detectado pelos bytes")
    void capaValidaEhGravada() {
        service.salvar(livro(), arquivo("capa.png", "image/png", PNG), false, "admin");

        verify(itemAcervoRepository).salvarCapa(org.mockito.ArgumentMatchers.eq(1L), any(),
            org.mockito.ArgumentMatchers.eq("image/png"));
    }

    @Test
    @DisplayName("Arquivo que não é imagem é recusado mesmo com nome .png, e nada é gravado")
    void arquivoQueNaoEImagemEhRecusado() {
        byte[] texto = "isto nao e uma imagem".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.salvar(livro(), arquivo("capa.png", "image/png", texto), false, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JPG, PNG ou WebP");
        verify(itemAcervoRepository, org.mockito.Mockito.never()).salvar(any());
        verify(itemAcervoRepository, org.mockito.Mockito.never()).salvarCapa(any(), any(), any());
    }

    @Test
    @DisplayName("SVG é recusado como capa (pode carregar script)")
    void svgEhRecusado() {
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.salvar(livro(), arquivo("capa.svg", "image/svg+xml", svg), false, "admin"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Capa acima de 2 MB é recusada")
    void capaGrandeEhRecusada() {
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        assertThatThrownBy(() -> service.salvar(livro(), arquivo("capa.png", "image/png", grande), false, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("2 MB");
    }

    @Test
    @DisplayName("Sem arquivo e sem pedido de remoção, a capa atual não é tocada")
    void semArquivoNaoMexeNaCapa() {
        service.salvar(livro(), null, false, "admin");

        verify(itemAcervoRepository, org.mockito.Mockito.never()).salvarCapa(any(), any(), any());
        verify(itemAcervoRepository, org.mockito.Mockito.never()).removerCapa(any());
    }

    @Test
    @DisplayName("Remover capa na edição apaga a imagem atual")
    void removerCapaNaEdicao() {
        itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, true);
        ItemAcervoDTO dto = livro();
        dto.setId(7L);

        service.salvar(dto, null, true, "admin");

        verify(itemAcervoRepository).removerCapa(1L);
    }

    @Test
    @DisplayName("Arquivo novo vence o pedido de remoção")
    void arquivoNovoVenceRemocao() {
        itemDoTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO, true);
        ItemAcervoDTO dto = livro();
        dto.setId(7L);

        service.salvar(dto, arquivo("capa.png", "image/png", PNG), true, "admin");

        verify(itemAcervoRepository).salvarCapa(any(), any(), any());
        verify(itemAcervoRepository, org.mockito.Mockito.never()).removerCapa(any());
    }

    @Test
    @DisplayName("Detecção de tipo pelos primeiros bytes: JPEG, PNG e WebP")
    void deteccaoDeTipo() {
        assertThat(AcervoService.tipoDaImagem(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("image/jpeg");
        assertThat(AcervoService.tipoDaImagem(PNG)).isEqualTo("image/png");
        assertThat(AcervoService.tipoDaImagem("RIFF\0\0\0\0WEBPVP8 ".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)))
            .isEqualTo("image/webp");
    }
}
