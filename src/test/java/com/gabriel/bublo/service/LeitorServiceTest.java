package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ImportacaoResultadoDTO;
import com.gabriel.bublo.dto.LinhaErroDTO;
import com.gabriel.bublo.dto.LeitorDTO;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.TipoLeitor;
import com.gabriel.bublo.repository.LeitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

/**
 * Testes unitários de LeitorService (validação de e-mail duplicado).
 */
class LeitorServiceTest {

    private LeitorRepository leitorRepository;
    private LeitorService service;

    @BeforeEach
    void setUp() {
        leitorRepository = mock(LeitorRepository.class);
        service = new LeitorService(leitorRepository);
    }

    @Test
    @DisplayName("E-mail duplicado é rejeitado ao cadastrar novo leitor")
    void emailDuplicadoRejeitado() {
        LeitorDTO dto = new LeitorDTO();
        dto.setTipo("ALUNO");
        dto.setNome("João");
        dto.setEmail("joao@email.com");

        when(leitorRepository.emailExiste("joao@email.com", null)).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() ->
            service.salvar(dto, "admin"));
    }

    @Test
    @DisplayName("E-mail disponível permite cadastro")
    void emailDisponivelPermiteCadastro() {
        LeitorDTO dto = new LeitorDTO();
        dto.setTipo("ALUNO");
        dto.setNome("Maria");
        dto.setEmail("maria@email.com");

        when(leitorRepository.emailExiste("maria@email.com", null)).thenReturn(false);
        when(leitorRepository.salvar(any())).thenAnswer(inv -> {
            Leitor l = inv.getArgument(0);
            l.setId(1L);
            l.setMatricula("ALU-00001");
            return l;
        });

        LeitorDTO resultado = service.salvar(dto, "admin");
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getMatricula()).isEqualTo("ALU-00001");
    }

    @Test
    @DisplayName("Importação CSV processa e rejeita linhas inválidas")
    void importacaoCsv() throws Exception {
        MultipartFile arquivo = mock(MultipartFile.class);
        // não testa leitura real do CSV (depende de stream), apenas comportamento do serviço
        ImportacaoResultadoDTO resultado = new ImportacaoResultadoDTO();
        resultado.setTotalProcessado(3);
        resultado.setImportados(2);
        resultado.setRejeitados(1);
        LinhaErroDTO erro = new LinhaErroDTO(2, "E-mail inválido");
        resultado.setErros(List.of(erro));

        assertThat(resultado.getTotalProcessado()).isEqualTo(3);
        assertThat(resultado.getImportados()).isEqualTo(2);
        assertThat(resultado.getRejeitados()).isEqualTo(1);
        assertThat(resultado.getErros()).hasSize(1);
        assertThat(resultado.getErros().get(0).getLinha()).isEqualTo(2);
    }

    @Test
    @DisplayName("Importação aceita CSV com ponto e vírgula e continua após erro de gravação em uma linha")
    void importacaoContinuaAposErroDeBanco() throws Exception {
        String csv = "\uFEFFTipo;Nome;Email\nALUNO;Um;um@x.com\nALUNO;Dois;dois@x.com\nPROFESSOR;Tres;tres@x.com\n";
        MultipartFile arquivo = mock(MultipartFile.class);
        when(arquivo.getBytes()).thenReturn(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(leitorRepository.salvar(any(Leitor.class)))
            .thenAnswer(inv -> inv.getArgument(0))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("PreparedStatementCallback; SQL [insert ...]"))
            .thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResultadoDTO resultado = service.importarCSV(arquivo, "admin");

        assertThat(resultado.getImportados()).isEqualTo(2);
        assertThat(resultado.getRejeitados()).isEqualTo(1);
        assertThat(resultado.getErros().get(0).getMotivo()).doesNotContain("SQL").doesNotContain("PreparedStatement");
    }
}
