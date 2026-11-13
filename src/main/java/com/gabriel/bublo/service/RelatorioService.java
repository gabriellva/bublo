package com.gabriel.bublo.service;

import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de relatórios gerenciais com exportação CSV e XLSX.
 */
@Service
public class RelatorioService {

    private static final char BOM_UTF8 = '\uFEFF';
    private static final String FORMULA_PREFIXOS = "=+-@";

    private final EmprestimoRepository emprestimoRepository;
    private final MultaRepository multaRepository;
    private final ItemAcervoRepository itemAcervoRepository;
    private final LeitorRepository leitorRepository;
    private final ReservaRepository reservaRepository;
    private final ExemplarRepository exemplarRepository;

    public RelatorioService(EmprestimoRepository emprestimoRepository,
                            MultaRepository multaRepository,
                            ItemAcervoRepository itemAcervoRepository,
                            LeitorRepository leitorRepository,
                            ReservaRepository reservaRepository,
                            ExemplarRepository exemplarRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.multaRepository = multaRepository;
        this.itemAcervoRepository = itemAcervoRepository;
        this.leitorRepository = leitorRepository;
        this.reservaRepository = reservaRepository;
        this.exemplarRepository = exemplarRepository;
    }

    // ===================== CSV =====================
    public void emprestimosCsv(HttpServletResponse response, String termo, String status) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=emprestimos.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            out.print(BOM_UTF8);
            out.println("Id;Leitor;Exemplar/Tombamento;Data empréstimo;Prevista;Status");
            var lista = emprestimoRepository.buscar(termo, status, 0, 10000, "data_emprestimo DESC").getConteudo();
            for (var e : lista) {
                String leitor = e.getLeitor() != null ? e.getLeitor().getNome() : "";
                String exemplar = e.getExemplar() != null ? e.getExemplar().getTombamento() : "";
                out.println(csv(e.getId()) + ";" + csv(leitor) + ";" + csv(exemplar) + ";"
                    + csv(fmt(e.getDataEmprestimo())) + ";" + csv(fmt(e.getDataPrevistaDevolucao())) + ";" + csv(e.getStatus()));
            }
        }
    }

    public void multasCsv(HttpServletResponse response, String termo, String status) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=multas.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            out.print(BOM_UTF8);
            out.println("Id;Leitor;Valor;Pago;Saldo;Status;Data geração");
            var lista = multaRepository.buscar(termo, status, 0, 10000, "data_geracao ASC").getConteudo();
            Map<Long, String> nomes = new HashMap<>();
            for (var m : lista) {
                out.println(csv(m.getId()) + ";" + csv(nomeDoLeitor(m.getLeitorId(), nomes)) + ";" + csv(m.getValor()) + ";"
                    + csv(m.getValorPago()) + ";" + csv(m.getSaldoPendente()) + ";" + csv(m.getStatus()) + ";"
                    + csv(fmt(m.getDataGeracao())));
            }
        }
    }

    public void leitoresCsv(HttpServletResponse response, String termo) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=leitores.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            out.print(BOM_UTF8);
            out.println("Matrícula;Nome;Tipo;E-mail;CPF;Endereço;Telefone;Curso/Dept.;Ativo");
            var lista = leitorRepository.buscar(termo, null, null, 0, 10000, "nome ASC").getConteudo();
            for (var l : lista) {
                out.println(csv(l.getMatricula()) + ";" + csv(l.getNome()) + ";" + csv(l.getTipo()) + ";"
                    + csv(l.getEmail()) + ";" + csv(l.getCpf()) + ";" + csv(l.getEndereco()) + ";"
                    + csv(l.getTelefone()) + ";" + csv(l.getCursoDepartamento()) + ";" + csv(l.getAtivo()));
            }
        }
    }

    public void reservasCsv(HttpServletResponse response, String termo, String status) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=reservas.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            out.print(BOM_UTF8);
            out.println("Id;Leitor;Material;Posição;Prazo retirada;Status");
            var lista = reservaRepository.buscar(termo, status, 0, 10000, "posicao ASC").getConteudo();
            for (var r : lista) {
                String leitor = r.getLeitor() != null ? r.getLeitor().getNome() : "";
                String item = r.getItemAcervo() != null ? r.getItemAcervo().getTitulo() : "";
                out.println(csv(r.getId()) + ";" + csv(leitor) + ";" + csv(item) + ";"
                    + csv(r.getPosicao()) + ";" + csv(fmt(r.getPrazoRetirada())) + ";" + csv(r.getStatus()));
            }
        }
    }

    public void materiaisCsv(HttpServletResponse response, String termo) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=materiais.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            out.print(BOM_UTF8);
            out.println("Título;Tipo;Editora;Ano;Categoria;ISBN/ISSN");
            var lista = itemAcervoRepository.listarTodos();
            for (var m : lista) {
                out.println(csv(m.getTitulo()) + ";" + csv(m.getTipo().name()) + ";" + csv(m.getEditora()) + ";"
                    + csv(m.getAno()) + ";" + csv(m.getCategoria() != null ? m.getCategoria().getNome() : "") + ";"
                    + csv(isbnIssn(m)));
            }
        }
    }

    // ===================== XLSX =====================
    public byte[] emprestimosXlsx(String termo, String status) throws IOException {
        var lista = emprestimoRepository.buscar(termo, status, 0, 10000, "data_emprestimo DESC").getConteudo();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Empréstimos");
            String[] cabecalho = {"Id", "Leitor", "Exemplar/Tombamento", "Data empréstimo", "Prevista", "Status"};
            Row row = sheet.createRow(0);
            for (int i = 0; i < cabecalho.length; i++) row.createCell(i).setCellValue(cabecalho[i]);
            int r = 1;
            for (var e : lista) {
                Row linha = sheet.createRow(r++);
                linha.createCell(0).setCellValue(e.getId() != null ? e.getId() : 0);
                linha.createCell(1).setCellValue(e.getLeitor() != null ? e.getLeitor().getNome() : "");
                linha.createCell(2).setCellValue(e.getExemplar() != null ? e.getExemplar().getTombamento() : "");
                linha.createCell(3).setCellValue(fmt(e.getDataEmprestimo()));
                linha.createCell(4).setCellValue(fmt(e.getDataPrevistaDevolucao()));
                linha.createCell(5).setCellValue(e.getStatus());
            }
            for (int i = 0; i < cabecalho.length; i++) sheet.autoSizeColumn(i);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] multasXlsx(String termo, String status) throws IOException {
        var lista = multaRepository.buscar(termo, status, 0, 10000, "data_geracao ASC").getConteudo();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Multas");
            String[] cabecalho = {"Id", "Leitor", "Valor", "Pago", "Saldo", "Status", "Data geração"};
            Row row = sheet.createRow(0);
            for (int i = 0; i < cabecalho.length; i++) row.createCell(i).setCellValue(cabecalho[i]);
            int r = 1;
            Map<Long, String> nomes = new HashMap<>();
            for (var m : lista) {
                Row linha = sheet.createRow(r++);
                linha.createCell(0).setCellValue(m.getId() != null ? m.getId() : 0);
                linha.createCell(1).setCellValue(nomeDoLeitor(m.getLeitorId(), nomes));
                linha.createCell(2).setCellValue(m.getValor() != null ? m.getValor().doubleValue() : 0);
                linha.createCell(3).setCellValue(m.getValorPago() != null ? m.getValorPago().doubleValue() : 0);
                linha.createCell(4).setCellValue(m.getSaldoPendente() != null ? m.getSaldoPendente().doubleValue() : 0);
                linha.createCell(5).setCellValue(m.getStatus());
                linha.createCell(6).setCellValue(fmt(m.getDataGeracao()));
            }
            for (int i = 0; i < cabecalho.length; i++) sheet.autoSizeColumn(i);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    // ===================== Helpers =====================
    private String nomeDoLeitor(Long leitorId, Map<Long, String> cache) {
        if (leitorId == null) return "";
        return cache.computeIfAbsent(leitorId,
            id -> leitorRepository.buscarPorId(id).map(l -> l.getNome()).orElse(""));
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (!s.isEmpty() && FORMULA_PREFIXOS.indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String fmt(LocalDateTime d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    private String isbnIssn(com.gabriel.bublo.model.ItemAcervo m) {
        if (m instanceof com.gabriel.bublo.model.Livro) return ((com.gabriel.bublo.model.Livro) m).getIsbn();
        if (m instanceof com.gabriel.bublo.model.Revista) return ((com.gabriel.bublo.model.Revista) m).getIssn();
        return "";
    }
}
