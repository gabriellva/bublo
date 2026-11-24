package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ImportacaoResultadoDTO;
import com.gabriel.bublo.dto.LinhaErroDTO;
import com.gabriel.bublo.dto.LeitorDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.TipoLeitor;
import com.gabriel.bublo.repository.AuditoriaRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de leitores com importação CSV.
 */
@Service
public class LeitorService {

    private final LeitorRepository leitorRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaRepository reservaRepository;
    private final AuditoriaRepository auditoriaRepository;

    public LeitorService(LeitorRepository leitorRepository,
                         EmprestimoRepository emprestimoRepository,
                         ReservaRepository reservaRepository,
                         AuditoriaRepository auditoriaRepository) {
        this.leitorRepository = leitorRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.reservaRepository = reservaRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public LeitorDTO buscarDTO(Long id) {
        Leitor l = leitorRepository.buscarPorId(id).orElse(null);
        return l != null ? toDTO(l) : null;
    }

    @Transactional
    public LeitorDTO salvar(LeitorDTO dto, String adminUsuario) {
        // Todas as regras valem também para a importação CSV, que não passa pelo @Valid do formulário.
        dto.setNome(obrigatorio(dto.getNome(), "O nome", 200));
        String email = obrigatorio(dto.getEmail(), "O e-mail", 200).toLowerCase(java.util.Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalStateException("E-mail inválido");
        }
        dto.setCpf(normalizarCpf(dto.getCpf(), dto.getId()));
        dto.setTelefone(normalizarTelefone(dto.getTelefone()));
        dto.setEndereco(opcional(dto.getEndereco(), "O endereço", 1000));
        dto.setCursoDepartamento(opcional(dto.getCursoDepartamento(), "O curso/departamento", 150));
        if (leitorRepository.emailExiste(email, dto.getId())) {
            throw new IllegalStateException("Já existe um leitor cadastrado com este e-mail");
        }
        boolean novo = dto.getId() == null;
        boolean inativando = false;
        Leitor leitor;
        if (!novo) {
            leitor = leitorRepository.buscarPorId(dto.getId())
                .orElseThrow(() -> new IllegalStateException("Leitor não encontrado"));
            // A matrícula (ALU-/PROF-) e os limites dependem do tipo: ele não muda depois do cadastro.
            if (leitor.getTipo() != TipoLeitor.valueOf(dto.getTipo())) {
                throw new IllegalStateException("O tipo do leitor não pode ser alterado depois do cadastro.");
            }
            boolean ativoNovo = dto.getAtivo() != null ? dto.getAtivo() : true;
            inativando = Boolean.TRUE.equals(leitor.getAtivo()) && !ativoNovo;
            if (inativando) {
                exigirSemEmprestimosAtivos(leitor.getId());
            }
        } else {
            TipoLeitor tipo = TipoLeitor.valueOf(dto.getTipo());
            leitor = tipo == TipoLeitor.PROFESSOR
                ? new com.gabriel.bublo.model.Professor()
                : new com.gabriel.bublo.model.Aluno();
        }
        leitor.setTipo(TipoLeitor.valueOf(dto.getTipo()));
        leitor.setNome(dto.getNome() == null ? null : dto.getNome().trim());
        leitor.setEmail(email);
        leitor.setCpf(dto.getCpf());
        leitor.setEndereco(dto.getEndereco());
        leitor.setTelefone(dto.getTelefone());
        leitor.setCursoDepartamento(dto.getCursoDepartamento());
        leitor.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        leitor = leitorRepository.salvar(leitor);
        if (inativando) {
            reservaRepository.cancelarAguardandoDoLeitor(leitor.getId());
        }
        auditoriaRepository.registrar(adminUsuario, novo ? "CADASTRO_LEITOR" : "EDICAO_LEITOR", "Leitores",
            leitor.getMatricula(), null, leitor.getNome(), null);
        return toDTO(leitor);
    }

    @Transactional(readOnly = true)
    public Paginacao<LeitorDTO> listar(String termo, TipoLeitor tipo, Boolean ativo,
                                       int pagina, int tamanho, String ordenacao) {
        return leitorRepository.buscar(termo, tipo, ativo, pagina, tamanho, ordenacao)
            .mapConteudo(this::toDTO);
    }

    @Transactional(readOnly = true)
    public long contar(String termo, TipoLeitor tipo, Boolean ativo) {
        return leitorRepository.buscar(termo, tipo, ativo, 0, 1, "nome ASC").getTotal();
    }

    @Transactional
    public void inativar(Long id, String adminUsuario) {
        Leitor l = leitorRepository.buscarPorId(id).orElseThrow(() -> new IllegalStateException("Leitor não encontrado"));
        if (!Boolean.TRUE.equals(l.getAtivo())) return;
        exigirSemEmprestimosAtivos(id);
        l.setAtivo(false);
        leitorRepository.salvar(l);
        int canceladas = reservaRepository.cancelarAguardandoDoLeitor(id);
        auditoriaRepository.registrar(adminUsuario, "INATIVACAO_LEITOR", "Leitores", l.getMatricula(),
            "ativo", "inativo" + (canceladas > 0 ? " (" + canceladas + " reserva(s) cancelada(s))" : ""), null);
    }

    private String obrigatorio(String valor, String campo, int maximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) throw new IllegalStateException(campo + " é obrigatório");
        if (texto.length() > maximo) throw new IllegalStateException(campo + " deve ter no máximo " + maximo + " caracteres");
        return texto;
    }

    private String opcional(String valor, String campo, int maximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.length() > maximo) throw new IllegalStateException(campo + " deve ter no máximo " + maximo + " caracteres");
        return texto.isEmpty() ? null : texto;
    }

    private String normalizarTelefone(String telefone) {
        String texto = opcional(telefone, "O telefone", 30);
        if (texto != null && !texto.matches("^[0-9()+\\-.\\s]{8,30}$")) {
            throw new IllegalStateException("Telefone inválido (use apenas números, espaços, parênteses, + e -)");
        }
        return texto;
    }

    /** CPF é opcional; quando informado precisa ser válido e único. Grava no formato 000.000.000-00. */
    private String normalizarCpf(String cpf, Long excluirId) {
        String texto = opcional(cpf, "O CPF", 20);
        if (texto == null) return null;
        String digitos = texto.replaceAll("\\D", "");
        if (!cpfValido(digitos)) throw new IllegalStateException("CPF inválido");
        if (leitorRepository.cpfExiste(digitos, excluirId)) {
            throw new IllegalStateException("Já existe um leitor cadastrado com este CPF");
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    static boolean cpfValido(String d) {
        if (d == null || d.length() != 11 || d.chars().distinct().count() == 1) return false;
        for (int posicao = 9; posicao < 11; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) soma += (d.charAt(i) - '0') * (posicao + 1 - i);
            int digito = (soma * 10) % 11 % 10;
            if (digito != d.charAt(posicao) - '0') return false;
        }
        return true;
    }

    private void exigirSemEmprestimosAtivos(Long leitorId) {
        int ativos = emprestimoRepository.listarAtivosPorLeitor(leitorId).size();
        if (ativos > 0) {
            throw new IllegalStateException("Não é possível inativar: o leitor possui " + ativos
                + " empréstimo(s) ativo(s). Registre as devoluções antes.");
        }
    }

    public ImportacaoResultadoDTO importarCSV(MultipartFile arquivo, String adminUsuario) {
        ImportacaoResultadoDTO resultado = new ImportacaoResultadoDTO();
        List<LinhaErroDTO> erros = new ArrayList<>();
        int importados = 0;

        try (CSVReader reader = abrirLeitorCsv(arquivo)) {
            String[] linha;
            int numeroLinha = 1;
            boolean cabecalho = true;
            while ((linha = reader.readNext()) != null) {
                if (cabecalho) {
                    cabecalho = false;
                    numeroLinha++;
                    continue;
                }
                numeroLinha++;
                if (linhaEmBranco(linha)) continue;
                resultado.setTotalProcessado(resultado.getTotalProcessado() + 1);
                try {
                    validarLinha(linha);
                    LeitorDTO dto = new LeitorDTO();
                    dto.setTipo(linha[0].trim());
                    dto.setNome(linha[1].trim());
                    dto.setEmail(linha[2].trim());
                    dto.setCpf(linha.length > 3 ? linha[3].trim() : null);
                    dto.setEndereco(linha.length > 4 ? linha[4].trim() : null);
                    dto.setTelefone(linha.length > 5 ? linha[5].trim() : null);
                    dto.setCursoDepartamento(linha.length > 6 ? linha[6].trim() : null);
                    dto.setAtivo(true);
                    salvar(dto, adminUsuario);
                    importados++;
                } catch (IllegalArgumentException | IllegalStateException e) {
                    erros.add(new LinhaErroDTO(numeroLinha, e.getMessage()));
                } catch (RuntimeException e) {
                    erros.add(new LinhaErroDTO(numeroLinha, "Não foi possível gravar esta linha."));
                }
            }
        } catch (CsvValidationException | java.io.IOException e) {
            throw new IllegalStateException("Erro ao ler arquivo CSV: " + e.getMessage());
        }

        auditoriaRepository.registrar(adminUsuario, "IMPORTACAO_LEITORES", "Leitores", arquivo.getOriginalFilename(),
            null, importados + " importado(s), " + erros.size() + " rejeitado(s)", null);
        resultado.setImportados(importados);
        resultado.setRejeitados(erros.size());
        resultado.setErros(erros);
        return resultado;
    }

    private boolean linhaEmBranco(String[] linha) {
        for (String campo : linha) {
            if (campo != null && !campo.isBlank()) return false;
        }
        return true;
    }

    private CSVReader abrirLeitorCsv(MultipartFile arquivo) throws java.io.IOException {
        String texto = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        if (texto.startsWith("\uFEFF")) texto = texto.substring(1);
        String primeiraLinha = texto.lines().findFirst().orElse("");
        char separador = primeiraLinha.contains(";") && !primeiraLinha.contains(",") ? ';' : ',';
        return new com.opencsv.CSVReaderBuilder(new java.io.StringReader(texto))
            .withCSVParser(new com.opencsv.CSVParserBuilder().withSeparator(separador).build())
            .build();
    }

    private void validarLinha(String[] linha) {
        if (linha.length < 3) {
            throw new IllegalArgumentException("Linha incompleta (tipo, nome, email obrigatórios)");
        }
        String tipo = linha[0].trim();
        if (!tipo.equals("ALUNO") && !tipo.equals("PROFESSOR")) {
            throw new IllegalArgumentException("Tipo inválido (use ALUNO ou PROFESSOR)");
        }
        if (linha[1].trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        String email = linha[2].trim();
        if (email.isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        if (leitorRepository.emailExiste(email, null)) {
            throw new IllegalStateException("E-mail já cadastrado: " + email);
        }
    }

    private LeitorDTO toDTO(Leitor l) {
        LeitorDTO dto = new LeitorDTO();
        dto.setId(l.getId());
        dto.setTipo(l.getTipo().name());
        dto.setNome(l.getNome());
        dto.setEmail(l.getEmail());
        dto.setMatricula(l.getMatricula());
        dto.setCpf(l.getCpf());
        dto.setEndereco(l.getEndereco());
        dto.setTelefone(l.getTelefone());
        dto.setCursoDepartamento(l.getCursoDepartamento());
        dto.setAtivo(l.getAtivo());
        return dto;
    }
}
