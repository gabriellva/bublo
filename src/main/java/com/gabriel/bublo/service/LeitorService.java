package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.ImportacaoResultadoDTO;
import com.gabriel.bublo.dto.LinhaErroDTO;
import com.gabriel.bublo.dto.LeitorDTO;
import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.TipoLeitor;
import com.gabriel.bublo.repository.LeitorRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
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

    public LeitorService(LeitorRepository leitorRepository) {
        this.leitorRepository = leitorRepository;
    }

    @Transactional(readOnly = true)
    public LeitorDTO buscarDTO(Long id) {
        Leitor l = leitorRepository.buscarPorId(id).orElse(null);
        return l != null ? toDTO(l) : null;
    }

    @Transactional
    public LeitorDTO salvar(LeitorDTO dto, String adminUsuario) {
        if (leitorRepository.emailExiste(dto.getEmail(), dto.getId())) {
            throw new IllegalStateException("Já existe um leitor cadastrado com este e-mail");
        }
        Leitor leitor;
        if (dto.getId() != null) {
            leitor = leitorRepository.buscarPorId(dto.getId())
                .orElseThrow(() -> new IllegalStateException("Leitor não encontrado"));
        } else {
            TipoLeitor tipo = TipoLeitor.valueOf(dto.getTipo());
            leitor = tipo == TipoLeitor.PROFESSOR
                ? new com.gabriel.bublo.model.Professor()
                : new com.gabriel.bublo.model.Aluno();
        }
        leitor.setTipo(TipoLeitor.valueOf(dto.getTipo()));
        leitor.setNome(dto.getNome());
        leitor.setEmail(dto.getEmail());
        leitor.setCpf(dto.getCpf());
        leitor.setEndereco(dto.getEndereco());
        leitor.setTelefone(dto.getTelefone());
        leitor.setCursoDepartamento(dto.getCursoDepartamento());
        leitor.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        leitor = leitorRepository.salvar(leitor);
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
        l.setAtivo(false);
        leitorRepository.salvar(l);
    }

    @Transactional
    public ImportacaoResultadoDTO importarCSV(MultipartFile arquivo, String adminUsuario) {
        ImportacaoResultadoDTO resultado = new ImportacaoResultadoDTO();
        List<LinhaErroDTO> erros = new ArrayList<>();
        int importados = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(
                arquivo.getInputStream(), StandardCharsets.UTF_8))) {
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
                } catch (Exception e) {
                    erros.add(new LinhaErroDTO(numeroLinha, e.getMessage()));
                }
            }
        } catch (CsvValidationException | java.io.IOException e) {
            throw new IllegalStateException("Erro ao ler arquivo CSV: " + e.getMessage());
        }

        resultado.setImportados(importados);
        resultado.setRejeitados(erros.size());
        resultado.setErros(erros);
        return resultado;
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
