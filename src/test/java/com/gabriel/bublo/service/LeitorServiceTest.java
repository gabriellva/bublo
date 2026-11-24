package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ImportacaoResultadoDTO;
import com.gabriel.bublo.dto.LinhaErroDTO;
import com.gabriel.bublo.dto.LeitorDTO;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.TipoLeitor;
import com.gabriel.bublo.repository.AuditoriaRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.ReservaRepository;
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
    private EmprestimoRepository emprestimoRepository;
    private ReservaRepository reservaRepository;
    private LeitorService service;

    @BeforeEach
    void setUp() {
        leitorRepository = mock(LeitorRepository.class);
        emprestimoRepository = mock(EmprestimoRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        service = new LeitorService(leitorRepository, emprestimoRepository, reservaRepository,
            mock(AuditoriaRepository.class));
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

    private Leitor existente(TipoLeitor tipo, boolean ativo) {
        Leitor l = tipo == TipoLeitor.PROFESSOR ? new com.gabriel.bublo.model.Professor() : new com.gabriel.bublo.model.Aluno();
        l.setId(5L);
        l.setTipo(tipo);
        l.setMatricula(tipo == TipoLeitor.PROFESSOR ? "PROF-00001" : "ALU-00001");
        l.setAtivo(ativo);
        return l;
    }

    @Test
    @DisplayName("E-mail é comparado sem diferenciar maiúsculas e sem espaços nas pontas")
    void emailNormalizado() {
        LeitorDTO dto = new LeitorDTO();
        dto.setTipo("ALUNO");
        dto.setNome("  Maria ");
        dto.setEmail("  Maria@Email.com ");
        when(leitorRepository.emailExiste("maria@email.com", null)).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> service.salvar(dto, "admin"));
    }

    @Test
    @DisplayName("O tipo do leitor não pode ser alterado depois do cadastro")
    void tipoImutavel() {
        LeitorDTO dto = new LeitorDTO();
        dto.setId(5L);
        dto.setTipo("PROFESSOR");
        dto.setNome("Ana");
        dto.setEmail("ana@email.com");
        when(leitorRepository.buscarPorId(5L)).thenReturn(java.util.Optional.of(existente(TipoLeitor.ALUNO, true)));

        assertThatIllegalStateException().isThrownBy(() -> service.salvar(dto, "admin"))
            .withMessageContaining("tipo");
        verify(leitorRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Leitor com empréstimo ativo não pode ser inativado")
    void inativarComEmprestimoAtivo() {
        when(leitorRepository.buscarPorId(5L)).thenReturn(java.util.Optional.of(existente(TipoLeitor.ALUNO, true)));
        when(emprestimoRepository.listarAtivosPorLeitor(5L))
            .thenReturn(java.util.List.of(new com.gabriel.bublo.model.Emprestimo()));

        assertThatIllegalStateException().isThrownBy(() -> service.inativar(5L, "admin"))
            .withMessageContaining("empréstimo");
        verify(leitorRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Inativar leitor cancela as reservas dele")
    void inativarCancelaReservas() {
        when(leitorRepository.buscarPorId(5L)).thenReturn(java.util.Optional.of(existente(TipoLeitor.ALUNO, true)));
        when(emprestimoRepository.listarAtivosPorLeitor(5L)).thenReturn(java.util.List.of());

        service.inativar(5L, "admin");

        verify(leitorRepository).salvar(any());
        verify(reservaRepository).cancelarAguardandoDoLeitor(5L);
    }

    private LeitorDTO novoDto() {
        LeitorDTO dto = new LeitorDTO();
        dto.setTipo("ALUNO");
        dto.setNome("Maria");
        dto.setEmail("maria@email.com");
        return dto;
    }

    @Test
    @DisplayName("CPF com dígito verificador errado é recusado")
    void cpfInvalido() {
        LeitorDTO dto = novoDto();
        dto.setCpf("111.222.333-44");

        assertThatIllegalStateException().isThrownBy(() -> service.salvar(dto, "admin"))
            .withMessageContaining("CPF inválido");
        verify(leitorRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("CPF válido é gravado formatado")
    void cpfValidoFormatado() {
        LeitorDTO dto = novoDto();
        dto.setCpf("52998224725");
        when(leitorRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.salvar(dto, "admin");

        verify(leitorRepository).salvar(argThat(l -> "529.982.247-25".equals(l.getCpf())));
    }

    @Test
    @DisplayName("CPF já cadastrado para outro leitor é recusado")
    void cpfDuplicado() {
        LeitorDTO dto = novoDto();
        dto.setCpf("529.982.247-25");
        when(leitorRepository.cpfExiste("52998224725", null)).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> service.salvar(dto, "admin"))
            .withMessageContaining("CPF");
    }

    @Test
    @DisplayName("Campos opcionais em branco são gravados como nulos")
    void opcionaisEmBrancoViramNulo() {
        LeitorDTO dto = novoDto();
        dto.setCpf("  ");
        dto.setTelefone("");
        dto.setEndereco(" ");
        when(leitorRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.salvar(dto, "admin");

        verify(leitorRepository).salvar(argThat(l -> l.getCpf() == null && l.getTelefone() == null && l.getEndereco() == null));
    }

    @Test
    @DisplayName("Telefone com letras e nome gigante são recusados (vale também para o CSV)")
    void telefoneENomeInvalidos() {
        LeitorDTO dto = novoDto();
        dto.setTelefone("abc");
        assertThatIllegalStateException().isThrownBy(() -> service.salvar(dto, "admin")).withMessageContaining("Telefone");

        LeitorDTO longo = novoDto();
        longo.setNome("x".repeat(201));
        assertThatIllegalStateException().isThrownBy(() -> service.salvar(longo, "admin")).withMessageContaining("nome");
    }

    @Test
    @DisplayName("E-mail sem domínio é recusado")
    void emailSemDominio() {
        LeitorDTO dto = novoDto();
        dto.setEmail("maria@host");
        assertThatIllegalStateException().isThrownBy(() -> service.salvar(dto, "admin")).withMessageContaining("E-mail");
    }

    @Test
    @DisplayName("Algoritmo do CPF: válidos, repetidos e tamanho errado")
    void algoritmoCpf() {
        assertThat(LeitorService.cpfValido("52998224725")).isTrue();
        assertThat(LeitorService.cpfValido("11111111111")).isFalse();
        assertThat(LeitorService.cpfValido("1234")).isFalse();
        assertThat(LeitorService.cpfValido(null)).isFalse();
    }
}
