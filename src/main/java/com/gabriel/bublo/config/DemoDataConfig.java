package com.gabriel.bublo.config;

import com.gabriel.bublo.repository.UsuarioRepository;
import com.gabriel.bublo.repository.ItemAcervoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.ExemplarRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.PagamentoRepository;
import com.gabriel.bublo.repository.ReservaRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import com.gabriel.bublo.model.Usuario;
import com.gabriel.bublo.model.ItemAcervo;
import com.gabriel.bublo.model.Livro;
import com.gabriel.bublo.model.Revista;
import com.gabriel.bublo.model.DVD;
import com.gabriel.bublo.model.PublicacaoDigital;
import com.gabriel.bublo.model.Categoria;
import com.gabriel.bublo.model.Autor;
import com.gabriel.bublo.model.Leitor;
import com.gabriel.bublo.model.Aluno;
import com.gabriel.bublo.model.Professor;
import com.gabriel.bublo.model.Exemplar;
import com.gabriel.bublo.service.ConfiguracaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrega o administrador inicial e dados de demonstração na primeira
 * inicialização (apenas quando bublo.demo.data.enabled=true).
 */
@Configuration
public class DemoDataConfig {

    @Bean
    public CommandLineRunner demoData(UsuarioRepository usuarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      ItemAcervoRepository itemAcervoRepository,
                                      ExemplarRepository exemplarRepository,
                                      LeitorRepository leitorRepository,
                                      EmprestimoRepository emprestimoRepository,
                                      MultaRepository multaRepository,
                                      PagamentoRepository pagamentoRepository,
                                      ReservaRepository reservaRepository,
                                      NotificacaoRepository notificacaoRepository,
                                      ConfiguracaoService configuracaoService,
                                      @Value("${bublo.admin.username:}") String adminUsername,
                                      @Value("${bublo.admin.password:}") String adminPassword,
                                      @Value("${bublo.demo.data.enabled:true}") boolean demoEnabled) {

        return args -> {
            // 1. Administrador inicial (sempre idempotente)
            if (adminUsername != null && !adminUsername.isBlank()
                && adminPassword != null && !adminPassword.isBlank()) {
                if (usuarioRepository.buscarPorUsername(adminUsername).isEmpty()) {
                    Usuario u = new Usuario();
                    u.setUsername(adminUsername);
                    u.setSenhaHash(passwordEncoder.encode(adminPassword));
                    u.setAtivo(true);
                    usuarioRepository.criar(u);
                }
            } else {
                // Fallback: criar admin padrão se não houver variáveis de ambiente
                if (usuarioRepository.buscarPorUsername("admin").isEmpty()) {
                    Usuario u = new Usuario();
                    u.setUsername("admin");
                    u.setSenhaHash(passwordEncoder.encode("admin"));
                    u.setAtivo(true);
                    usuarioRepository.criar(u);
                }
            }

            if (!demoEnabled) return;

            // 2. Idempotência: se já existem materiais, a demo já foi carregada
            if (itemAcervoRepository.listarTodos().stream().findAny().isPresent()) {
                return;
            }

            // 3. Materiais (~30)
            List<ItemAcervo> materiais = new ArrayList<>();

            Object[][] livros = {
                {"Dom Casmurro", "Machado de Assis", 1899, "LIVRO"},
                {"O Cortiço", "Aluísio de Azevedo", 1890, "LIVRO"},
                {"Limpeza de Código", "Robert C. Martin", 2008, "LIVRO"},
                {"Arquitetura Limpa", "Robert C. Martin", 2017, "LIVRO"},
                {"Introdução a Algoritmos", "Thomas H. Cormen", 2009, "LIVRO"},
                {"Domínio Distribuído", "Vaughn Vernon", 2013, "LIVRO"},
                {"Programação Orientada a Objetos", "Lafore", 2002, "LIVRO"},
                {"Engenharia de Software", "Pressman", 2010, "LIVRO"},
                {"Banco de Dados", "Silberschatz", 2011, "LIVRO"},
                {"Redes de Computadores", "Tanenbaum", 2010, "LIVRO"},
            };
            int numeroIsbn = 1;
            for (Object[] l : livros) {
                Livro livro = new Livro();
                livro.setTipo(com.gabriel.bublo.model.TipoMaterial.LIVRO);
                livro.setTitulo((String) l[0]);
                livro.setEditora("Editora Acadêmica");
                livro.setAno((Integer) l[2]);
                livro.setIdioma("Português");
                livro.setIsbn(String.format("978-85-0000-%03d-0", numeroIsbn++));
                Autor a = new Autor();
                a.setNome((String) l[1]);
                a.setNomeNormalizado(((String) l[1]).toLowerCase().trim());
                livro.getAutores().add(a);
                materiais.add(livro);
            }
            Revista r1 = new Revista(); r1.setTipo(com.gabriel.bublo.model.TipoMaterial.REVISTA);
            r1.setTitulo("Pesquisa Acadêmica"); r1.setEditora("Revista Uni"); r1.setAno(2023);
            r1.setIssn("1234-5678"); r1.setVolume(5); r1.setIdioma("Português");
            materiais.add(r1);
            DVD d1 = new DVD(); d1.setTipo(com.gabriel.bublo.model.TipoMaterial.DVD);
            d1.setTitulo("Documentário História"); d1.setDuracaoMinutos(90); d1.setAno(2022);
            d1.setIdioma("Português"); materiais.add(d1);
            PublicacaoDigital pd = new PublicacaoDigital();
            pd.setTipo(com.gabriel.bublo.model.TipoMaterial.DIGITAL);
            pd.setTitulo("Artigo sobre POO"); pd.setDoi("10.1000/xyz"); pd.setUrlExterna("https://exemplo.local/artigo");
            pd.setAno(2023); pd.setIdioma("Português"); materiais.add(pd);

            Categoria literatura = criarCategoria(itemAcervoRepository, "Literatura");
            Categoria tecnologia = criarCategoria(itemAcervoRepository, "Tecnologia");
            Categoria periodicos = criarCategoria(itemAcervoRepository, "Periódicos");
            Categoria multimidia = criarCategoria(itemAcervoRepository, "Multimídia");
            Categoria digital = criarCategoria(itemAcervoRepository, "Publicações digitais");
            int indice = 0;
            for (ItemAcervo m : materiais) {
                m.setCategoria(switch (m.getTipo()) {
                    case LIVRO -> indice++ < 2 ? literatura : tecnologia;
                    case REVISTA -> periodicos;
                    case DVD -> multimidia;
                    case DIGITAL -> digital;
                });
                itemAcervoRepository.salvar(m);
                if (m.getTipo() != com.gabriel.bublo.model.TipoMaterial.DIGITAL) {
                    int qtd = 1 + (int)(Math.random() * 2);
                    exemplarRepository.salvarLote(m, qtd);
                }
            }

            // 4. Leitores (15)
            List<Leitor> leitores = new ArrayList<>();
            String[] nomesAlunos = {"Ana Oliveira", "Bruno Costa", "Carla Dias", "Daniel Souza", "Elisa Ferreira",
                "Fernando Lima", "Giovana Rocha", "Henrique Alves"};
            for (String n : nomesAlunos) {
                Aluno a = new Aluno(); a.setNome(n); a.setTipo(com.gabriel.bublo.model.TipoLeitor.ALUNO);
                a.setEmail(n.toLowerCase().replace(" ", ".") + "@univ.edu.br");
                a.setCursoDepartamento("Ciência da Computação"); a.setAtivo(true);
                leitores.add(leitorRepository.salvar(a));
            }
            String[] nomesProf = {"Felipe Mendes", "Gabriela Lima", "Ricardo Nunes"};
            for (String n : nomesProf) {
                Professor p = new Professor(); p.setNome(n); p.setTipo(com.gabriel.bublo.model.TipoLeitor.PROFESSOR);
                p.setEmail(n.toLowerCase().replace(" ", ".") + "@univ.edu.br");
                p.setCursoDepartamento("Engenharia"); p.setAtivo(true);
                leitores.add(leitorRepository.salvar(p));
            }
            // completar até 15
            while (leitores.size() < 15) {
                int i = leitores.size() + 1;
                Aluno a = new Aluno(); a.setNome("Leitor Demo " + i);
                a.setTipo(com.gabriel.bublo.model.TipoLeitor.ALUNO);
                a.setEmail("demo" + i + "@univ.edu.br"); a.setAtivo(true);
                leitores.add(leitorRepository.salvar(a));
            }

            // 5. Empréstimos de exemplo para alguns leitores (histórico moderado)
            // Busca exemplares disponíveis
            var exemplares = exemplarRepository.listarPorItem(materiais.get(0).getId());
            if (!exemplares.isEmpty() && !leitores.isEmpty()) {
                LocalDateTime prevista = LocalDateTime.now().plusDays(5);
                emprestimoRepository.criar(leitores.get(0).getId(), exemplares.get(0).getId(), prevista);
            }
            var exemplaresAtrasado = exemplarRepository.listarPorItem(materiais.get(1).getId());
            if (!exemplaresAtrasado.isEmpty() && leitores.size() > 1) {
                emprestimoRepository.criar(leitores.get(1).getId(), exemplaresAtrasado.get(0).getId(),
                    LocalDateTime.now().minusDays(3));
            }

            // 6. Configurações iniciais (ON CONFLICT já lida com idempotência)
            configuracaoService.salvar("nome_instituicao", "Biblioteca Acadêmica", "sistema", "Carga inicial");
            configuracaoService.salvar("prefixo_tombamento", "BA", "sistema", "Carga inicial");
            configuracaoService.salvar("limite_alunos", "3", "sistema", "Carga inicial");
            configuracaoService.salvar("limite_professores", "5", "sistema", "Carga inicial");
            configuracaoService.salvar("prazo_alunos", "7", "sistema", "Carga inicial");
            configuracaoService.salvar("prazo_professores", "14", "sistema", "Carga inicial");
            configuracaoService.salvar("taxa_diaria_multa", "2.00", "sistema", "Carga inicial");
            configuracaoService.salvar("prazo_retirada_reserva", "3", "sistema", "Carga inicial");

        };
    }

    private Categoria criarCategoria(ItemAcervoRepository repository, String nome) {
        Categoria categoria = new Categoria();
        categoria.setId(repository.criarCategoria(nome));
        categoria.setNome(nome);
        categoria.setAtivo(true);
        return categoria;
    }
}
