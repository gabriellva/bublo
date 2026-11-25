# Bublo — Guia de Estudo e Apresentação do Código

> Documento de apoio para a apresentação do projeto. O objetivo é permitir que quem tem pouco contato com Java consiga **mostrar o sistema, abrir o código e explicar o que cada parte faz e por que ela existe**.
>
> Todas as afirmações abaixo foram conferidas diretamente no código-fonte da branch atual. Quando o código difere do que outros documentos do repositório afirmam, isso está registrado na [Seção 17](#17-pontos-de-atenção-o-código-vs-a-documentação-antiga).

## Sumário

1. [Visão geral do sistema](#1-visão-geral-do-sistema)
2. [Glossário mínimo de Java e Spring](#2-glossário-mínimo-de-java-e-spring)
3. [Tecnologias e dependências (`pom.xml`)](#3-tecnologias-e-dependências-pomxml)
4. [Arquitetura em camadas e o caminho de uma requisição](#4-arquitetura-em-camadas-e-o-caminho-de-uma-requisição)
5. [Mapa de pastas e arquivos](#5-mapa-de-pastas-e-arquivos)
6. [Inicialização e configuração](#6-inicialização-e-configuração)
7. [Banco de dados](#7-banco-de-dados)
8. [Modelo de domínio e Programação Orientada a Objetos](#8-modelo-de-domínio-e-programação-orientada-a-objetos)
9. [Acesso a dados com JDBC puro](#9-acesso-a-dados-com-jdbc-puro)
10. [Serviços, regras de negócio e transações](#10-serviços-regras-de-negócio-e-transações)
11. [Caso de estudo: o cadastro de leitor, do clique ao banco](#11-caso-de-estudo-o-cadastro-de-leitor-do-clique-ao-banco)
12. [Como os outros cadastros diferem do cadastro de leitor](#12-como-os-outros-cadastros-diferem-do-cadastro-de-leitor)
13. [Módulos de negócio](#13-módulos-de-negócio)
14. [Segurança](#14-segurança)
15. [Front-end](#15-front-end)
16. [Testes, Docker e CI](#16-testes-docker-e-ci)
17. [Pontos de atenção: o código vs. a documentação antiga](#17-pontos-de-atenção-o-código-vs-a-documentação-antiga)
18. [Perguntas prováveis da banca, com respostas](#18-perguntas-prováveis-da-banca-com-respostas)
19. [Roteiro sugerido de apresentação](#19-roteiro-sugerido-de-apresentação)
20. [Mini quiz](#20-mini-quiz)
21. [Gabarito do quiz](#21-gabarito-do-quiz)

---

## 1. Visão geral do sistema

**Bublo** é um sistema web de gerenciamento de biblioteca acadêmica. Existe **um único tipo de usuário do sistema: o administrador (bibliotecário)**. Alunos e professores são *leitores* — dados cadastrados e gerenciados pelo administrador, mas que **não fazem login**.

O que o sistema faz:

| Módulo | Função |
|---|---|
| Acervo | Cadastrar livros, revistas, DVDs e publicações digitais; controlar os exemplares físicos de cada título |
| Leitores | Cadastrar alunos e professores (manualmente ou por importação CSV) |
| Empréstimos | Emprestar exemplares, renovar, cancelar |
| Devoluções | Registrar a devolução; gerar multa automática em caso de atraso |
| Reservas | Fila de espera (FIFO) por título quando todos os exemplares estão emprestados |
| Multas | Cobrar, receber pagamentos (parciais ou totais) e estornar |
| Relatórios | Exportar CSV/XLSX |
| Notificações | Avisos persistidos no banco e entregues em tempo real por WebSocket |
| Configurações | Parâmetros numéricos do sistema (limites, prazos, taxa de multa) com justificativa e desfazer |
| Auditoria | Registro de quem fez o quê e quando |
| Dashboard | Indicadores e gráficos |

**Características técnicas que merecem destaque na apresentação:**

- Aplicação **monolítica** em **Spring Boot 3.5.0** com **Java 21**.
- Páginas **renderizadas no servidor** (Thymeleaf), não é uma SPA nem uma API REST.
- Persistência com **JDBC puro** (`JdbcTemplate`) — **sem JPA/Hibernate**. Todo SQL está escrito à mão nos repositórios.
- Banco **PostgreSQL**, esquema versionado com **Flyway**.
- **Herança real** no domínio (`ItemAcervo` → 4 tipos; `Leitor` → 2 tipos).
- **Concorrência tratada no banco** (`SELECT ... FOR UPDATE`, *advisory lock*, índices únicos parciais).
- **98 testes unitários** (JUnit 5 + Mockito).

---

## 2. Glossário mínimo de Java e Spring

Esta seção existe para que você tenha o vocabulário necessário para ler qualquer arquivo do projeto sem travar. Os exemplos são do próprio código.

### 2.1 Java básico

| Conceito | O que é | Exemplo no projeto |
|---|---|---|
| **Classe** | Molde que define atributos (dados) e métodos (comportamentos). | `Leitor`, `Emprestimo`, `LeitorService` |
| **Objeto** | Uma instância concreta de uma classe, criada com `new`. | `new Aluno()` |
| **Atributo** | Variável que pertence à classe. | `private String nome;` em `Leitor` |
| **Método** | Função que pertence à classe. | `getNome()`, `salvar(...)` |
| **Construtor** | Método especial, mesmo nome da classe, que prepara o objeto. | `public LeitorService(LeitorRepository ..., ...)` |
| **Encapsulamento** | Atributos `private` + acesso por *getters/setters* públicos. | Todas as classes de `model/` |
| **Getter/Setter** | Métodos `getX()` / `setX()` que leem/gravam um atributo privado. | `leitor.getEmail()` |
| **`static`** | Pertence à classe, não a um objeto. | `private static final RowMapper<Leitor> LEITOR_MAPPER` |
| **`final`** | Não pode ser reatribuído (constante, se `static final`). | `private final LeitorRepository leitorRepository;` |
| **`abstract class`** | Classe que **não pode ser instanciada**; serve de base. | `ItemAcervo`, `Leitor` |
| **Herança (`extends`)** | A subclasse herda atributos e métodos da superclasse. | `class Livro extends ItemAcervo` |
| **`@Override` (sobrescrita)** | A subclasse redefine um método herdado, **com a mesma assinatura**. Decidido em **execução**, pelo tipo real do objeto. | `getLimiteExemplaresSimultaneos()` em `Aluno` (3) e `Professor` (5) |
| **Sobrecarga (*overload*)** | Vários métodos com o **mesmo nome e parâmetros diferentes**. Decidido em **compilação**, pelos argumentos. | `diasDeAtraso(prevista)` e `diasDeAtraso(prevista, hoje)` em `EmprestimoService` |
| **`enum`** | Conjunto fixo de valores nomeados. | `TipoLeitor { ALUNO, PROFESSOR }` |
| **Generics `<T>`** | Tipo parametrizado. | `List<Leitor>`, `Paginacao<ItemAcervoDTO>` |
| **`Optional<T>`** | Caixa que pode ou não conter um valor (evita `null`). | `leitorRepository.buscarPorId(id)` retorna `Optional<Leitor>` |
| **Lambda `->`** | Função anônima curta. | `(rs, rowNum) -> { ... }` |
| **Stream** | Pipeline para processar coleções. | `lista.stream().map(this::toDTO).toList()` |
| **`instanceof` com padrão** | Testa o tipo e já cria a variável. | `if (item instanceof Livro l) l.setIsbn(...)` |
| **`switch` com `->`** | Expressão `switch` moderna. | `switch (tipo) { case LIVRO -> item = new Livro(); ... }` |
| **Exceção** | Erro sinalizado com `throw` e tratado com `try/catch`. | `throw new IllegalStateException("Leitor inativo...")` |
| **`IllegalStateException`** | Exceção de runtime usada neste projeto para **violação de regra de negócio**. | Quase todos os `Service` |

### 2.2 Spring

| Conceito | O que é | Exemplo no projeto |
|---|---|---|
| **Inversão de controle (IoC)** | Em vez de você dar `new` nas dependências, o Spring cria os objetos e os entrega prontos. | — |
| **Bean** | Objeto criado e gerenciado pelo Spring. | Todo `@Service`, `@Repository`, `@Controller` |
| **Injeção de dependência** | O Spring passa os beans pelo **construtor**. | `public LeitorService(LeitorRepository leitorRepository, ...)` |
| **`@SpringBootApplication`** | Liga a configuração automática e a varredura de beans. | `BubloApplication` |
| **`@Controller`** | Recebe requisições HTTP e devolve o nome de uma página. | `LeitorController` |
| **`@Service`** | Camada de regras de negócio. | `LeitorService` |
| **`@Repository`** | Camada de acesso ao banco. | `LeitorRepository` |
| **`@Configuration` / `@Bean`** | Classe de configuração / método que fabrica um bean. | `SecurityConfig` |
| **`@GetMapping` / `@PostMapping`** | Liga um método a uma URL + verbo HTTP. | `@PostMapping("/importar")` |
| **`@RequestParam`** | Lê um parâmetro da URL ou do formulário. | `@RequestParam String termo` |
| **`@PathVariable`** | Lê um pedaço da URL. | `/leitores/{id}` → `@PathVariable Long id` |
| **`@ModelAttribute`** | Preenche um objeto (DTO) com os campos do formulário. | `@ModelAttribute("leitor") LeitorDTO leitor` |
| **`Model`** | "Mochila" de dados que o controller entrega ao template. | `model.addAttribute("resultado", resultado)` |
| **`RedirectAttributes`** | Dados que sobrevivem a **um** redirecionamento (*flash*). | `redirect.addFlashAttribute("sucesso", "...")` |
| **`@Valid` + `BindingResult`** | Dispara a validação do DTO e guarda os erros. | `LeitorController.salvar` |
| **`@Transactional`** | O método roda dentro de **uma transação** do banco: tudo confirma (*commit*) ou tudo desfaz (*rollback*). | Métodos de `Service` |
| **`@ControllerAdvice`** | Código que vale para **todos** os controllers. | `GlobalExceptionHandler`, `GlobalModelAdvice` |
| **`@Scheduled`** | Executa um método em horários agendados. | `ReservaScheduler` |
| **`@Value`** | Injeta uma propriedade do `application.yaml`. | `@Value("${bublo.admin.username:}")` |

---

## 3. Tecnologias e dependências (`pom.xml`)

O `pom.xml` é o arquivo do **Maven**, a ferramenta que baixa dependências e compila o projeto.

| Dependência | Para que serve | Onde aparece |
|---|---|---|
| `spring-boot-starter-web` | Spring MVC: controllers, servidor Tomcat embutido | `controller/` |
| `spring-boot-starter-thymeleaf` | Motor de templates HTML do lado do servidor | `templates/` |
| `spring-boot-starter-security` | Autenticação, sessão, CSRF, BCrypt | `SecurityConfig` |
| `spring-boot-starter-websocket` | WebSocket nativo (**sem STOMP**) | `WebSocketConfig`, `NotificationWebSocketHandler` |
| `spring-boot-starter-jdbc` | `JdbcTemplate` + pool de conexões HikariCP | `repository/` |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Email`...) | `dto/` |
| `flyway-core` + `flyway-database-postgresql` | Versionamento do esquema do banco | `db/migration/` |
| `postgresql` | Driver JDBC do PostgreSQL | — |
| `poi-ooxml` 5.2.5 | Gerar planilhas `.xlsx` | `RelatorioService` |
| `opencsv` 5.8 | Ler CSV na importação de leitores | `LeitorService.importarCSV` |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ | `src/test` |

**Por que JDBC puro e não JPA?** Foi um requisito do trabalho. A consequência prática: **cada SQL é escrito, lido e controlado por nós**, o que dá controle total (locks, índices parciais, `ON CONFLICT`) ao custo de mais código repetitivo.

---

## 4. Arquitetura em camadas e o caminho de uma requisição

```
 Navegador
    │  (1) GET/POST  ex.: POST /leitores
    ▼
 ┌──────────────────────────────────────────────┐
 │ Filtros do Spring Security                   │  autenticação, sessão, CSRF
 └──────────────────────────────────────────────┘
    ▼
 ┌──────────────────────────────────────────────┐
 │ CONTROLLER   (controller/)                   │  lê a requisição, valida o DTO,
 │   LeitorController                           │  chama o Service, escolhe a página
 └──────────────────────────────────────────────┘
    ▼
 ┌──────────────────────────────────────────────┐
 │ SERVICE      (service/)   @Transactional     │  regras de negócio, validações,
 │   LeitorService                              │  auditoria, orquestra repositórios
 └──────────────────────────────────────────────┘
    ▼
 ┌──────────────────────────────────────────────┐
 │ REPOSITORY   (repository/)                   │  SQL com PreparedStatement
 │   LeitorRepository  →  JdbcTemplate          │
 └──────────────────────────────────────────────┘
    ▼
 HikariCP (pool de até 10 conexões)  →  PostgreSQL

 Volta:  Service → Controller → Model → template Thymeleaf → HTML → navegador
```

**Regra de ouro das camadas** (use isso na explicação):

| Camada | Pode | Não deve |
|---|---|---|
| Controller | Receber HTTP, validar formato, escolher a view/redirect | Conter regra de negócio ou SQL |
| Service | Aplicar regras, abrir transação, chamar vários repositórios | Conhecer HTTP/HTML |
| Repository | Executar SQL e converter linhas em objetos | Decidir regras de negócio |
| Model | Representar o domínio | Acessar banco |
| DTO | Transportar dados entre tela e serviço | Ter lógica |

**Por que existem DTOs?** O formulário trabalha com `LeitorDTO` (apenas `String`s e campos de tela, com anotações de validação). O domínio trabalha com `Leitor`/`Aluno`/`Professor`. O `Service` converte um no outro (`toDTO`). Assim a tela nunca "vaza" para o modelo e o modelo nunca é exposto diretamente.

---

## 5. Mapa de pastas e arquivos

```
bublo/
├─ pom.xml                      dependências Maven
├─ Dockerfile                   imagem da aplicação (build em 2 estágios)
├─ docker-compose.yml           só o PostgreSQL (para rodar a app localmente)
├─ docker-compose.app.yml       PostgreSQL + aplicação
├─ .env.example                 variáveis de ambiente de exemplo
├─ .github/workflows/ci.yml     pipeline de CI (GitHub Actions)
├─ docs/                        documentação do projeto
├─ scripts/smoke-test.sh        teste rápido de fumaça
└─ src/
   ├─ main/java/com/gabriel/bublo/
   │   ├─ BubloApplication.java        ponto de entrada (main)
   │   ├─ config/                      configurações do Spring
   │   │    ├─ SecurityConfig          login, logout, sessão, BCrypt
   │   │    ├─ WebSocketConfig         registra o endpoint /ws/notificacoes
   │   │    ├─ ReservaScheduler        expira reservas vencidas (agendado)
   │   │    └─ DemoDataConfig          cria o admin inicial e dados de demonstração
   │   ├─ controller/                  14 controllers + 1 advice
   │   ├─ service/                     8 serviços (regras de negócio)
   │   ├─ repository/                  11 repositórios + OrdenacaoSegura + BaseRepository (não usado)
   │   ├─ model/                       entidades de domínio e enums
   │   ├─ dto/                         objetos de transferência
   │   ├─ exception/                   GlobalExceptionHandler
   │   ├─ security/                    AuthUtil, AuthAuditService
   │   └─ websocket/                   NotificationWebSocketHandler
   ├─ main/resources/
   │   ├─ application.yaml             configuração da aplicação
   │   ├─ db/migration/                V1, V2, V3 (SQL do Flyway)
   │   ├─ templates/                   páginas Thymeleaf
   │   └─ static/                      CSS, JS e bibliotecas locais
   └─ test/                            98 testes unitários
```

### 5.1 Tabela de rotas (URLs)

| Controller | Rotas principais |
|---|---|
| `AuthController` | `GET /login`, `GET /` (redireciona), `GET /conta`, `POST /troca-senha` |
| `DashboardController` | `GET /dashboard` |
| `AcervoController` | `GET /acervo`, `/acervo/novo`, `/acervo/{id}`, `/acervo/{id}/editar`; `POST /acervo`, `/acervo/{id}/exemplar`, `/acervo/{id}/exemplares/{exemplarId}/excluir`, `/acervo/{id}/excluir`; `GET /acervo/{id}/capa` (imagem), `GET /acervo/sugestoes-autores` (JSON) |
| `LeitorController` | `GET /leitores`, `/leitores/novo`, `/leitores/{id}`, `/leitores/{id}/editar`, `/leitores/modelo-csv`; `POST /leitores`, `/leitores/{id}/inativar`, `/leitores/importar` |
| `EmprestimoController` | `GET /emprestimos`, `/emprestimos/novo`, `/emprestimos/restricao/{leitorId}` (JSON); `POST /emprestimos`, `/emprestimos/{id}/renovar`, `/emprestimos/{id}/cancelar` |
| `DevolucaoController` | `GET /devolucoes`; `POST /devolucoes/{id}/devolver` |
| `ReservaController` | `GET /reservas`, `/reservas/novo`; `POST /reservas`, `/reservas/{id}/cancelar` |
| `MultaController` | `GET /multas`, `/multas/{id}/pagar`, `/multas/leitor/{id}/saldo` (JSON); `POST /multas/{id}/pagar`, `/multas/{id}/pagamentos/{pagamentoId}/estornar` |
| `NotificacaoController` | `GET /notificacoes/resumo` (JSON); `POST /notificacoes/{id}/ler`, `/notificacoes/ler-todas` (JSON; sem página própria: tudo pelo sino) |
| `ConfiguracaoController` | `GET /configuracoes`; `POST /configuracoes/salvar`, `/configuracoes/reverter` |
| `AuditoriaController` | `GET /auditoria` |
| `RelatorioController` / `RelatorioExportController` | `GET /relatorios`; `GET /relatorios/{emprestimos,multas,leitores,reservas,materiais}.csv` e `{emprestimos,multas}.xlsx` |
| WebSocket | `/ws/notificacoes` |

---

## 6. Inicialização e configuração

### 6.1 `BubloApplication.java`

```java
@SpringBootApplication
public class BubloApplication {
    public static void main(String[] args) {
        SpringApplication.run(BubloApplication.class, args);
    }
}
```

`main` é onde tudo começa. `SpringApplication.run` sobe o Tomcat embutido, procura todas as classes anotadas (`@Service`, `@Controller`...) no pacote `com.gabriel.bublo` e abaixo, cria os beans e injeta as dependências.

### 6.2 `application.yaml`

| Trecho | Significado |
|---|---|
| `spring.datasource.url: ${DB_URL}` | Lê a variável de ambiente `DB_URL`, **sem valor padrão**: se não existir, o app não sobe. O mesmo vale para usuário, senha, `ADMIN_*` e `DEMO_DATA_ENABLED`. Localmente os valores vêm do `.env`. |
| `hikari.maximum-pool-size: 10` | O pool mantém no máximo 10 conexões abertas com o banco. |
| `flyway.baseline-on-migrate: true` | Se o banco já existir sem histórico do Flyway, ele adota o estado atual como ponto de partida. |
| `thymeleaf.cache: false` | Recarrega templates a cada requisição (útil em desenvolvimento). |
| `web.locale: pt_BR` (fixo) | Datas e números no padrão brasileiro. |
| `multipart.max-file-size: 2MB` | Limite do upload do CSV de leitores. |
| `server.servlet.session.cookie` | Cookie `BUBLO_SESSION`, `http-only` (JavaScript não lê) e `same-site: LAX`. |
| `session.timeout: 60m` | A sessão expira após 60 minutos sem uso. |
| `bublo.admin.username/password` | Credenciais do primeiro administrador (variáveis `ADMIN_USERNAME`/`ADMIN_PASSWORD`). |
| `bublo.demo.data.enabled` | Liga/desliga a carga de dados de demonstração. |

> **Ideia a transmitir:** nenhuma senha está escrita no código. Tudo que é sensível vem de variável de ambiente.

### 6.3 O que acontece na subida (ordem)

1. O **Flyway** executa as migrations pendentes (`V1`, `V2`, `V3`) e cria/atualiza as tabelas.
2. O Spring cria os beans.
3. O `DemoDataConfig` (um `CommandLineRunner`) roda **uma vez** na partida:
   - cria o administrador (`ADMIN_USERNAME`/`ADMIN_PASSWORD`, obrigatórios; no `.env.example` são `admin`/`admin`), **se não existir**. Sem essas variáveis o app não sobe;
   - se `demo.data.enabled=true` **e** o acervo estiver vazio, carrega 13 materiais (10 livros, 1 revista, 1 DVD, 1 publicação digital) com seus exemplares, 15 leitores, 2 empréstimos de exemplo (um deles já atrasado) e as 6 configurações iniciais.
4. O `ReservaScheduler` executa `expirar()` assim que a aplicação fica pronta (`ApplicationReadyEvent`) e depois **a cada hora, no minuto 5** (`@Scheduled(cron = "0 5 * * * *")` — o cron do Spring tem 6 campos: segundo, minuto, hora, dia, mês, dia da semana).

### 6.4 `ReservaScheduler`

Reservas têm um prazo de retirada. Passou do prazo, a reserva deve sair da fila para não segurar o título indefinidamente. O agendador chama `reservaService.expirarVencidas()`, que marca como `EXPIRADA`, gera notificação e registra auditoria com o usuário `"sistema"`.

---

## 7. Banco de dados

### 7.1 Tabelas (14)

```
usuarios            administrador (username, senha_hash BCrypt, ativo)
configuracoes       chave/valor (limites, prazos, taxa...)
categorias          categorias do acervo
autores             autores (nome + nome_normalizado ÚNICO)
item_acervo         TODOS os tipos de material, numa única tabela
item_acervo_autor   ligação N:N entre item_acervo e autores
exemplares          cópias físicas (tombamento ÚNICO)
leitores            alunos e professores, numa única tabela
emprestimos         leitor × exemplar
reservas            leitor × item_acervo (por TÍTULO, não por exemplar)
multas              originadas de empréstimos atrasados
pagamentos          pagamentos de multas (com campos de estorno)
notificacoes        avisos do sistema
auditoria           trilha de auditoria
```

### 7.2 Relacionamentos

```
categorias 1 ───< item_acervo >───< item_acervo_autor >─── autores
                       │ 1
                       │
                       ▼ N
                   exemplares 1 ───< emprestimos >─── 1 leitores
                                          │ 1              │ 1
                                          ▼ N              ├───< reservas >─── item_acervo
                                        multas >───────────┘
                                          │ 1
                                          ▼ N
                                      pagamentos
```

### 7.3 Herança em banco: "tabela única"

`item_acervo` guarda **todos** os tipos. A coluna `tipo` (`LIVRO`, `REVISTA`, `DVD`, `DIGITAL`) diz qual é, e os campos específicos ficam nulos quando não se aplicam:

| Tipo | Campos próprios |
|---|---|
| Livro | `isbn` |
| Revista | `issn`, `volume` |
| DVD | `duracao_minutos` |
| Digital | `doi`, `url_externa` |

`leitores` segue a mesma ideia (`tipo` = `ALUNO` ou `PROFESSOR`).

**Vantagem:** simples, uma só consulta. **Desvantagem:** colunas nulas. É uma escolha consciente e adequada ao JDBC puro.

### 7.4 Restrições importantes (e por que existem)

| Restrição | Efeito |
|---|---|
| `CHECK (tipo IN (...))`, `CHECK (status IN (...))` | O banco rejeita valores inválidos, mesmo que o código erre. |
| `exemplares.tombamento UNIQUE` | Dois exemplares nunca têm o mesmo código. |
| `leitores.email UNIQUE`, `leitores.matricula UNIQUE` | Sem duplicidade. |
| Índice único parcial `uix_reserva_ativa_por_leitor_item ... WHERE status = 'AGUARDANDO'` | Um leitor **não pode ter duas reservas aguardando do mesmo título**, mas pode reservar de novo depois que a anterior for cancelada/atendida. |
| Índice único parcial de ISBN/ISSN `... WHERE isbn IS NOT NULL AND ativo` (V2) | ISBN não repete **entre materiais ativos**; um material excluído não bloqueia um novo cadastro. |
| Chaves estrangeiras | Impedem apagar um exemplar que tem empréstimos (por isso existe a "baixa", ver Seção 12). |

### 7.5 Migrations (Flyway)

| Arquivo | O que faz |
|---|---|
| `V1__schema.sql` | Cria todas as tabelas, restrições e índices. |
| `V2__estorno_e_indices.sql` | Adiciona colunas de estorno em `pagamentos` e transforma os índices de ISBN/ISSN em parciais (só ativos). |
| `V3__baixa_de_exemplares.sql` | Adiciona `baixado`, `baixado_em`, `baixa_motivo` em `exemplares`. |
| `V4__capa_do_item.sql` | Adiciona `capa` (BYTEA), `capa_tipo` e `capa_atualizada_em` em `item_acervo`. |

**Regra de ouro:** uma migration já aplicada **nunca é editada**; mudanças novas viram `V4`, `V5`... O Flyway guarda o histórico em uma tabela própria e compara.

---

## 8. Modelo de domínio e Programação Orientada a Objetos

### 8.1 Hierarquias

```
                ItemAcervo  (abstract)                    Leitor  (abstract)
   ┌──────────┬────────┬───────────────┐                  ┌──────────┐
 Livro     Revista    DVD     PublicacaoDigital          Aluno    Professor
```

### 8.2 Os quatro pilares, com o código do projeto

**Abstração.** `ItemAcervo` e `Leitor` são `abstract`: não existe "um item genérico" nem "um leitor genérico" — existe um livro, um aluno. Elas declaram métodos abstratos que **obrigam** as subclasses a responder:

```java
public abstract class ItemAcervo {
    public abstract String getCapaGenerica();
    public abstract String getCampoEspecificoLabel();
    public abstract String getCampoEspecificoValor();
}
```

**Herança.** `Livro extends ItemAcervo` reaproveita `titulo`, `editora`, `ano`, `autores`, etc., e acrescenta só o `isbn`.

**Polimorfismo.** Cada subclasse responde de forma diferente à mesma chamada:

| Chamada | `Livro` | `Revista` | `DVD` | `PublicacaoDigital` |
|---|---|---|---|---|
| `getCampoEspecificoLabel()` | `"ISBN"` | `"ISSN"` | `"Duração"` | `"DOI"` |
| `getCampoEspecificoValor()` | o ISBN | `"1234-5678 · Vol. 5"` | `"90 min"` | o DOI |

E em `Leitor`:

| Chamada | `Aluno` | `Professor` |
|---|---|---|
| `getLimiteExemplaresSimultaneos()` | 3 | 5 |
| `getPrazoEmprestimoDias()` | 7 | 14 |

**Encapsulamento.** Atributos `private`, acesso por getters/setters. `Multa` tem ainda um comportamento próprio: `getSaldoPendente()` = `valor − valorPago`.

> ✅ **Em uso de verdade:** `EmprestimoService` pergunta ao próprio leitor (`leitor.getLimiteExemplaresSimultaneos()`, `leitor.getPrazoEmprestimoDias()`) qual é o padrão dele, e usa `leitor.getChaveConfiguracaoLimite()`/`getChaveConfiguracaoPrazo()` para achar na tabela `configuracoes` o valor que o administrador definiu. Não há mais `instanceof Professor` nessa regra. Veja a Seção 8.3.
>
> ⚠️ **Ainda demonstrativo:** `getCapaGenerica()` e `getCampoEspecifico*()` (materiais) não são chamados por nenhum template atual. Ver Seção 17, item 2.

### 8.3 Sobrecarga e sobrescrita (com exemplos do projeto)

São duas coisas diferentes que costumam ser confundidas:

| | **Sobrescrita** (*override*) | **Sobrecarga** (*overload*) |
|---|---|---|
| O que é | A **subclasse** redefine um método que herdou | **Mesmo nome**, parâmetros **diferentes**, na mesma classe |
| Assinatura | **Igual** à do método herdado | **Diferente** (quantidade ou tipos de parâmetros) |
| Quem decide qual roda | O **tipo real do objeto**, em **execução** (polimorfismo) | Os **argumentos** da chamada, em **compilação** |
| Marca no código | `@Override` | Nenhuma (só o mesmo nome) |
| Precisa de herança? | Sim | Não |

**Sobrescrita no projeto** — `Leitor` declara o contrato e cada subclasse o cumpre do seu jeito:

```java
// Leitor (abstrata)
public abstract int getLimiteExemplaresSimultaneos();
public abstract int getPrazoEmprestimoDias();
public abstract String getChaveConfiguracaoLimite();
public abstract String getChaveConfiguracaoPrazo();

// Aluno                                         // Professor
@Override getLimiteExemplaresSimultaneos() → 3   @Override getLimiteExemplaresSimultaneos() → 5
@Override getPrazoEmprestimoDias()         → 7   @Override getPrazoEmprestimoDias()         → 14
@Override getChaveConfiguracaoLimite() → "limite_alunos"   → "limite_professores"
```

E o serviço usa isso **sem saber** se o leitor é aluno ou professor:

```java
// EmprestimoService
private int limiteDoLeitor(Leitor leitor) {
    return configuracaoService.buscarInteiro(
        leitor.getChaveConfiguracaoLimite(),        // qual configuração vale para este tipo
        leitor.getLimiteExemplaresSimultaneos());   // padrão do tipo, se não houver configuração
}
```

Se amanhã existir um `Visitante` com limite 1, basta criar a subclasse sobrescrevendo os métodos; o `EmprestimoService` **não muda**. É a vantagem do polimorfismo sobre o `if (leitor instanceof Professor)` que existia antes.

Outras sobrescritas: `Livro`, `Revista`, `DVD` e `PublicacaoDigital` sobrescrevem `getCapaGenerica()`, `getCampoEspecificoLabel()` e `getCampoEspecificoValor()` (ex.: `Revista` devolve `"1234-5678 · Vol. 5"`); `NotificationWebSocketHandler` sobrescreve `afterConnectionEstablished`/`afterConnectionClosed` do Spring; `WebSocketConfig` sobrescreve `registerWebSocketHandlers`.

**Sobrecarga no projeto** — o mesmo nome atende dois jeitos de chamar:

```java
// EmprestimoService
static long diasDeAtraso(LocalDateTime prevista) {                    // usa a data de hoje
    return diasDeAtraso(prevista, LocalDate.now());
}
static long diasDeAtraso(LocalDateTime prevista, LocalDate hoje) {    // data de referência explícita
    if (prevista == null) return 0;
    return Math.max(0, ChronoUnit.DAYS.between(prevista.toLocalDate(), hoje));
}
```

As regras de devolução, renovação e a listagem usam a primeira forma; os testes usam a segunda, com uma data fixa, para não depender do relógio. Outro caso: em `ReservaService`, `toDTO(Reserva)` chama `toDTO(Reserva, Map)`, que recebe um *cache* de exemplares livres por título para não repetir a consulta a cada linha da lista.

> **Para explicar na banca:** "sobrecarga é escolher **pelo que eu passo** (resolvido na compilação); sobrescrita é escolher **por quem eu sou** (resolvido na execução)." Testes: `PolimorfismoTest` (sobrescrita) e `EmprestimoServiceTest.diasDeAtrasoSobrecargaUsaHoje` (sobrecarga).

### 8.4 Onde o polimorfismo também aparece (escolha da subclasse)

O ponto em que o sistema **escolhe a subclasse** é o `RowMapper` (a função que converte uma linha do banco em objeto):

```java
// ItemAcervoRepository
TipoMaterial tipo = TipoMaterial.valueOf(rs.getString("tipo"));
ItemAcervo item;
switch (tipo) {
    case LIVRO   -> item = new Livro();
    case REVISTA -> item = new Revista();
    case DVD     -> item = new DVD();
    case DIGITAL -> item = new PublicacaoDigital();
}
```

Esse trecho é uma **fábrica** informal: lê o texto do banco e devolve a subclasse certa. O mesmo existe em `LeitorRepository` (Aluno/Professor) e em `AcervoService.construirItem`.

A decisão inversa (objeto → coluna) usa `instanceof`:

```java
private String isbnDe(ItemAcervo i) { return i instanceof Livro ? ((Livro) i).getIsbn() : null; }
```

### 8.5 Enums

`TipoLeitor` e `TipoMaterial` têm um `label` legível (`"Revista / Periódico"`). O **nome** (`REVISTA`) vai para o banco; o **rótulo** vai para a tela.

### 8.5 Status como `String`

`Emprestimo.status`, `Reserva.status`, `Multa.status` são `String` (valores controlados pelos `CHECK` do banco), não enums:

| Entidade | Status possíveis |
|---|---|
| Empréstimo | `ATIVO`, `DEVOLVIDO`, `CANCELADO` (`ATRASADO` existe no `CHECK`, mas o código **não grava** esse valor — ver abaixo) |
| Reserva | `AGUARDANDO`, `ATENDIDA`, `CANCELADA`, `EXPIRADA` |
| Multa | `PENDENTE`, `PAGA_PARCIAL`, `PAGA`, `CANCELADA` |

**Atraso é calculado, não gravado:** um empréstimo continua `ATIVO` e é considerado atrasado quando `data_prevista_devolucao::date < CURRENT_DATE`.

---

## 9. Acesso a dados com JDBC puro

### 9.1 As peças

| Peça | O que é |
|---|---|
| `DataSource` / **HikariCP** | Pool de conexões. Abrir conexão com o banco é caro; o pool mantém até 10 abertas e as empresta. |
| `JdbcTemplate` | Classe do Spring que executa SQL e cuida de abrir/fechar recursos e tratar exceções. |
| `PreparedStatement` | SQL com **parâmetros `?`**. Os valores são enviados separados do texto do SQL → impede **injeção de SQL**. |
| `RowMapper<T>` | Função `(ResultSet rs, int rowNum) -> T` que converte **uma linha** em um objeto. |
| `KeyHolder` | Recupera o `id` que o banco gerou após um `INSERT`. |

### 9.2 Anatomia de um repositório (o `LeitorRepository`)

**Inserir** (e recuperar o id gerado):

```java
Long id = insertReturnId(
    "INSERT INTO leitores (tipo, nome, email, matricula, ...) VALUES (?, ?, ?, ?, ..., NOW())",
    leitor.getTipo().name(), leitor.getNome(), leitor.getEmail(), matricula, ...);
```

**Consultar um registro:**

```java
jdbc.queryForObject("SELECT * FROM leitores WHERE id = ?", LEITOR_MAPPER, id);
// se não achar, lança EmptyResultDataAccessException → o repositório devolve Optional.empty()
```

**Salvar = inserir ou atualizar:** se `leitor.getId() == null` faz `INSERT`, senão `UPDATE`. (A matrícula **não** é atualizada no `UPDATE`: é imutável.)

### 9.3 Paginação e filtros dinâmicos

Todo método `buscar(...)` segue o mesmo molde:

1. Monta o `WHERE` com `StringBuilder` **adicionando condições só para os filtros informados** (`AND nome ILIKE ?`), e guarda os valores numa lista de argumentos.
2. Acrescenta `ORDER BY ... LIMIT ? OFFSET ?` (`OFFSET = página × tamanho`).
3. Faz **uma segunda consulta**, `SELECT COUNT(*)` com o mesmo `WHERE`, para saber o total.
4. Devolve um `Paginacao<T>` (conteúdo, total, página, tamanho, totalPaginas).

`ILIKE` é o `LIKE` do PostgreSQL **sem diferenciar maiúsculas/minúsculas**.

### 9.4 Ordenação segura (`OrdenacaoSegura`)

Parâmetros de `?` **não funcionam em `ORDER BY`** (o nome da coluna não é um valor). Para ordenar a partir de um parâmetro vindo da URL (`ordenacao=nome DESC`) seria preciso concatenar texto no SQL — o que abriria **injeção de SQL**. A solução do projeto: `OrdenacaoSegura.resolver(...)` só aceita colunas de uma **lista branca** (`COLUNAS_ORDENAVEIS`) e a direção `ASC`/`DESC`; qualquer outra coisa cai no padrão. Isso é testado em `OrdenacaoSeguraTest`.

### 9.5 Bloqueios de concorrência (o que há de mais sofisticado no acesso a dados)

| Técnica | Onde | Problema que resolve |
|---|---|---|
| `SELECT ... FOR UPDATE` | `EmprestimoRepository.criar` (linha do **exemplar**) | Dois bibliotecários emprestando o mesmo exemplar ao mesmo tempo |
| `SELECT ... FOR UPDATE` | `LeitorRepository.travar` (linha do **leitor**) | Dois empréstimos simultâneos ultrapassando o limite do leitor |
| `SELECT ... FOR UPDATE` | `ReservaRepository.criar` (linha do **título**) | Duas reservas com a mesma posição na fila |
| `SELECT ... FOR UPDATE` | `MultaRepository.buscarPorIdParaAtualizar` | Dois pagamentos simultâneos somando mais que a multa |
| `pg_advisory_xact_lock(7001)` | `ExemplarRepository.proxTombamento` | Dois cadastros gerando o mesmo número de tombamento |
| `UPDATE ... WHERE status = 'ATIVO'` (condicional) | `EmprestimoRepository.devolver/cancelar/renovar`, `PagamentoRepository.marcarEstornado` | Devolver/estornar duas vezes (a segunda não altera nenhuma linha e retorna `false`) |

> **Como explicar `FOR UPDATE`:** "a linha fica travada até o fim da transação; quem tentar travá-la também espera. Isso transforma duas operações concorrentes em duas operações em fila."

### 9.6 O `BaseRepository`

Existe uma classe `BaseRepository` (com `insertReturnId`, `queryForObject`, etc.), mas **nenhum repositório a estende** hoje. Cada repositório recebe o `JdbcTemplate` diretamente e define seu próprio `insertReturnId`. Veja a Seção 17.

### 9.7 "E se um cadastro usar uma conexão diferente?"

**Não usa.** Esta é uma pergunta provável, então a resposta precisa estar clara:

- Existe **um único `DataSource`** (o pool Hikari) para a aplicação inteira.
- **Nenhum repositório abre conexão própria.** Todos usam o `JdbcTemplate` injetado.
- Quando um método `@Transactional` do `Service` começa, o Spring **pega uma conexão do pool e a prende àquela thread** até o fim do método. Todos os repositórios chamados dentro dele **compartilham a mesma conexão e a mesma transação** — é por isso que dá para gravar o empréstimo, a notificação e a auditoria de forma atômica.
- O que **muda de um cadastro para outro** não é a conexão, e sim: (a) **o nível de proteção** (alguns usam `FOR UPDATE` ou *advisory lock*, outros não), (b) **as regras no `Service`**, (c) **a tabela e as restrições** do banco.
- O **WebSocket** é outro canal (protocolo diferente, entre navegador e servidor), mas **não** é uma conexão alternativa com o banco.

---

## 10. Serviços, regras de negócio e transações

### 10.1 O que é uma transação

Uma transação agrupa várias operações no banco em uma **unidade atômica**: ou todas são confirmadas (*commit*) ou, se qualquer uma falhar, **todas são desfeitas** (*rollback*).

Exemplo no `EmprestimoService.devolver`: marcar o empréstimo como devolvido, liberar o exemplar, gerar a multa, criar a notificação e registrar a auditoria. Se a gravação da multa falhar, o exemplar **não** fica liberado.

`@Transactional` em um método do `Service` ativa isso. Para o Spring, **uma exceção de runtime** (como `IllegalStateException`) dentro do método causa *rollback* automático — é por isso que as regras de negócio lançam `IllegalStateException`: além de informar o erro, desfazem o que já tinha sido feito.

`@Transactional(readOnly = true)` é usado nas consultas (otimização e deixa a intenção explícita).

### 10.2 Padrão repetido em todos os serviços

```
1. validar/normalizar a entrada
2. carregar o que for necessário (ex.: leitor, exemplar)
3. verificar as regras → lançar IllegalStateException se violar
4. gravar (repositórios)
5. notificar (opcional)
6. registrar auditoria
7. devolver um DTO
```

### 10.3 Auditoria em tudo

Quase toda operação de escrita chama `auditoriaRepository.registrar(usuario, tipoOperacao, modulo, registroAfetado, antes, depois, justificativa)`. O usuário vem de `AuthUtil.usuarioAtual()` (lê o nome do usuário logado no `SecurityContext`) ou `"sistema"` para o agendador.

---

## 11. Caso de estudo: o cadastro de leitor, do clique ao banco

Este fluxo é o **modelo** de todos os outros cadastros. Aprenda este e os demais viram variações.

### Passo 1 — Abrir o formulário (`GET /leitores/novo`)

```java
// LeitorController
@GetMapping("/novo")
public String novo(Model model) {
    LeitorDTO leitor = new LeitorDTO();
    leitor.setTipo(TipoLeitor.ALUNO.name());
    leitor.setAtivo(true);
    model.addAttribute("leitor", leitor);
    return "leitores/formulario";   // nome do template, não uma URL
}
```

O controller cria um DTO vazio com valores padrão, coloca no `Model` e devolve o **nome** do template (`templates/leitores/formulario.html`).

### Passo 2 — O template (`leitores/formulario.html`)

```html
<form th:action="@{/leitores}" th:object="${leitor}" method="post" novalidate>
  <input type="text" th:field="*{nome}" class="form-control" .../>
  <small class="erro-campo" th:if="${#fields.hasErrors('nome')}" th:errors="*{nome}">Erro</small>
```

- `th:object="${leitor}"` liga o formulário ao DTO.
- `th:field="*{nome}"` gera `name="nome"`, `id="nome"` e preenche o valor.
- `th:action="@{/leitores}"` faz o Thymeleaf **inserir automaticamente o token CSRF** em um campo oculto (veja Seção 14).
- `novalidate` desliga a validação do navegador: **quem valida é o servidor**.
- `#fields.hasErrors('nome')` / `th:errors` mostram a mensagem de erro daquele campo.
- A **matrícula** aparece como campo `disabled` ("Gerada ao salvar"): é gerada pelo sistema.
- Na edição, o **tipo** fica desabilitado, com um campo oculto repetindo o valor (campos `disabled` não são enviados).

### Passo 3 — Enviar (`POST /leitores`)

```java
@PostMapping
public String salvar(@Valid @ModelAttribute("leitor") LeitorDTO leitor,
                     BindingResult result, Model model, RedirectAttributes redirect) {
    if (result.hasErrors()) {
        model.addAttribute("erro", "Verifique os campos destacados.");
        return "leitores/formulario";          // reexibe o formulário com os erros
    }
    try {
        leitorService.salvar(leitor, AuthUtil.usuarioAtual());
    } catch (IllegalStateException e) { ... return "leitores/formulario"; }
    redirect.addFlashAttribute("sucesso", "Leitor salvo com sucesso.");
    return "redirect:/leitores";
}
```

1. **`@ModelAttribute`** pega os campos do formulário e preenche o `LeitorDTO`.
2. **`@Valid`** executa as anotações do DTO (`@NotBlank`, `@Email`, `@Size`, `@Pattern`). Os erros caem em `BindingResult`.
3. Se há erro de formato → volta ao formulário (sem redirect, para manter o que foi digitado).
4. Senão chama o `Service`.
5. Se o `Service` lançar `IllegalStateException` (regra de negócio violada) → volta ao formulário com a mensagem. O controller **decide em qual campo mostrar** o erro olhando o texto da mensagem (e-mail, CPF, telefone).
6. Se deu certo → **redirect** com mensagem *flash*.

> **Padrão Post/Redirect/Get (PRG):** após um `POST` bem-sucedido, o servidor responde com um *redirect* para um `GET`. Se o usuário apertar F5, o navegador repete o `GET` inofensivo e **não reenvia o formulário** (evitando cadastro duplicado). A mensagem de sucesso viaja como *flash attribute*: existe só para a próxima requisição.

### Passo 4 — A regra de negócio (`LeitorService.salvar`)

Em ordem:

| # | Ação | Regra |
|---|---|---|
| 1 | `obrigatorio(nome, 200)` | Nome obrigatório, máx. 200 |
| 2 | e-mail em minúsculas + regex | Formato válido |
| 3 | `normalizarCpf` | CPF **opcional**; se informado, valida os dígitos verificadores (`cpfValido`), exige unicidade e grava como `000.000.000-00` |
| 4 | `normalizarTelefone` | Opcional; aceita só dígitos, espaços, `()+-.` |
| 5 | `emailExiste` | E-mail único (comparação sem maiúsculas/minúsculas) |
| 6 | Se **edição**: carrega o leitor | Lança erro se o **tipo mudou** (a matrícula e os limites dependem dele) |
| 7 | Se **inativando** | Exige **nenhum empréstimo ativo**; ao salvar, **cancela as reservas aguardando** |
| 8 | Cria `Aluno` ou `Professor` | Escolha da subclasse (polimorfismo) |
| 9 | `leitorRepository.salvar` | `INSERT` ou `UPDATE` |
| 10 | `auditoriaRepository.registrar` | `CADASTRO_LEITOR` ou `EDICAO_LEITOR` |

**Validação de CPF (`cpfValido`) — para saber explicar:** rejeita tamanhos diferentes de 11 e sequências repetidas (`111.111.111-11`). Depois calcula os dois dígitos verificadores: soma cada dígito multiplicado por um peso decrescente, multiplica por 10, tira o resto da divisão por 11 e o resto por 10; o resultado precisa bater com o dígito informado.

### Passo 5 — Gravar (`LeitorRepository.salvar`)

- Em **novo cadastro**, `gerarMatricula(tipo)` calcula o próximo número:

```sql
SELECT COALESCE(MAX(CAST(substring(matricula from '\d+') AS INTEGER)), 0) + 1
FROM leitores WHERE matricula LIKE 'ALU-%'
```

  Resultado: `ALU-00001`, `ALU-00002`, … ou `PROF-00001`. O prefixo depende do tipo e a numeração é **independente por tipo**.
- O `INSERT` usa `PreparedStatement` (via `insertReturnId`) e devolve o `id`.

### Passo 6 — Responder

Redirect para `GET /leitores` (a listagem). A página recebe o *flash* `sucesso`; o template o coloca em `<div id="flash-data" data-sucesso="...">` e o `bublo.js` o transforma em um **toast** (Seção 15).

### Resumo do fluxo

```
Formulário ──POST──► Filtro CSRF/Auth ──► LeitorController.salvar
   ▲                                          │ @Valid (formato)
   │ erro                                      ▼
   └───────────────────────────────── LeitorService.salvar (@Transactional)
                                               │ regras: CPF, e-mail, tipo, inativação
                                               ▼
                                       LeitorRepository ──► PostgreSQL
                                               │
                                       AuditoriaRepository ──► PostgreSQL (mesma transação)
                                               ▼
                                   redirect:/leitores  +  flash "sucesso"
```

### 11.1 Edição e inativação

- **Editar** (`GET /leitores/{id}/editar`) reutiliza o mesmo formulário preenchido; o `POST /leitores` com `id` preenchido faz `UPDATE`.
- **Não existe exclusão de leitor.** Existe **inativação** (`POST /leitores/{id}/inativar`): o registro permanece, `ativo = false`. Motivo: o leitor tem histórico (empréstimos, multas); apagar quebraria as chaves estrangeiras e a rastreabilidade. É uma *exclusão lógica* (*soft delete*).

---

## 12. Como os outros cadastros diferem do cadastro de leitor

A estrutura (Controller → Service → Repository → Template) é **a mesma**. Abaixo, **somente o que muda**.

### 12.1 Cadastro de material do acervo (`AcervoController` / `AcervoService`)

| Diferença | Detalhe |
|---|---|
| **Herança de verdade** | `AcervoService.construirItem(dto)` faz `switch (tipo)` e cria `Livro`, `Revista`, `DVD` ou `PublicacaoDigital`, preenchendo só os campos próprios daquele tipo. |
| **Formulário dinâmico** | Uma única página; um JavaScript (`mostrarCamposDoTipo`) mostra/esconde os campos conforme o tipo escolhido e **desabilita** os campos escondidos para que não sejam enviados. |
| **Tipo imutável** | Trocar o tipo apagaria campos próprios (ISBN etc.) e poderia deixar exemplares em material digital. O serviço rejeita. |
| **Autores (N:N)** | Um material tem vários autores e um autor tem vários materiais → tabela de ligação `item_acervo_autor`. Em `salvar`, o repositório **apaga todos os vínculos e recria** (sincronização simples). O autor é identificado por `nome_normalizado` (minúsculas, sem espaços duplicados): "Machado" e "machado " viram **um** autor (`criarOuReutilizarAutor`). |
| **Duplicidade de ISBN/ISSN** | O serviço captura `DuplicateKeyException` e, se o índice violado é o de ISBN/ISSN, converte em mensagem amigável. A regra "só vale entre ativos" está no **índice parcial** (V2). |
| **Exclusão lógica** | `excluir` faz `UPDATE item_acervo SET ativo = FALSE`. Bloqueia se houver empréstimo ativo ou reserva aguardando. |
| **Validações de DTO** | `ItemAcervoDTO` tem `@Min/@Max` para ano, páginas, duração e volume, e `@Pattern` para ISBN/ISSN. |
| **Capa (opcional)** | O formulário é `multipart/form-data`. O arquivo vira `byte[]` no serviço, que descobre o tipo pelos **primeiros bytes** (JPG, PNG ou WebP; SVG é recusado porque pode carregar script) e limita a 2 MB. A imagem fica no próprio banco (coluna `BYTEA`) e é servida por `GET /acervo/{id}/capa`; o `?v=` da URL muda quando a capa é trocada, para o navegador não usar a antiga. Sem foto, a tela mostra uma **capa gerada** com o título (cor por tipo). |
| **Sem matrícula** | O que é gerado automaticamente aqui é o **tombamento** do exemplar (a seguir). |

### 12.2 Cadastro de exemplares (dentro do acervo)

| Diferença | Detalhe |
|---|---|
| **Em lote** | `POST /acervo/{id}/exemplar?quantidade=N` — de 1 a 100. `salvarLote` cria todos **ou nenhum** (a transação do serviço garante isso). |
| **Tombamento** | Formato `PREFIXO-000001` (6 dígitos). Sequencial **global** (não por título), **nunca reaproveitado**. O prefixo é fixo (`BA`, constante `ExemplarRepository.PREFIXO_PADRAO`); não é mais configurável. |
| **Lock explícito** | Antes de calcular `MAX + 1`, executa `pg_advisory_xact_lock(7001)`. Sem isso, dois cadastros simultâneos poderiam calcular o mesmo número. |
| **Material digital não tem exemplares** | O serviço recusa. |
| **Excluir vs. baixar** | Exemplar **sem** histórico de empréstimos é **apagado** (`DELETE`). Exemplar **com** histórico recebe **baixa** (`baixado = TRUE`): some de listas e contagens, mas o histórico e o tombamento permanecem (a chave estrangeira impediria apagar). Exemplar **emprestado** não pode sair. Não se exclui o último exemplar de um título com reservas aguardando. |

### 12.3 Importação em massa de leitores (CSV)

| Diferença | Detalhe |
|---|---|
| **Entrada** | Arquivo `.csv` (até 2 MB). Há um **modelo para download** (`/leitores/modelo-csv`). |
| **Tolerância** | Remove BOM, detecta separador `;` ou `,`, ignora linhas em branco. |
| **Reuso** | Cada linha vira um `LeitorDTO` e passa **pelo mesmo `salvar()`** — por isso as regras de CPF/e-mail valem também aqui (o `@Valid` do formulário não é usado). |
| **Erros por linha** | Linhas inválidas **não derrubam o lote**: o erro vai para uma lista (`LinhaErroDTO`) e a tela exibe "Processados / Importados / Rejeitados" com o motivo de cada linha. |
| **Auditoria** | Um registro `IMPORTACAO_LEITORES` resume o lote. |

### 12.4 Configurações (`ConfiguracaoService`)

| Diferença | Detalhe |
|---|---|
| **Modelo chave-valor** | Tabela `configuracoes(chave, valor)`; **não há uma coluna por parâmetro**. |
| **Justificativa obrigatória** | Toda alteração exige o motivo, que vai para a auditoria. |
| **Validação por chave** | Inteiros entre 1 e o máximo (limites e prazos até 50/365; retirada de reserva até 30); taxa ≥ 0, até 2 casas e ≤ R$ 1.000. |
| **Desfazer a última alteração** | Lê a última linha de auditoria do módulo `Configuracoes`; só reverte se for `ALTERACAO_CONFIGURACAO` com valor "antes". Depois de uma reversão não dá para reverter de novo. |
| **Upsert** | `INSERT ... ON CONFLICT (chave) DO UPDATE` — insere ou atualiza numa instrução. |
| **Vale para operações novas** | O valor é lido na hora de cada operação (`buscarInteiro("limite_alunos", 3)`); não altera o que já foi gravado. |

### 12.5 Cadastros que "não são cadastros"

Empréstimo, reserva, pagamento e estorno **criam registros**, mas são **operações de negócio** com regras próprias — a Seção 13 detalha cada uma.

### 12.6 Quadro comparativo

| | Leitor | Material | Exemplar | Configuração |
|---|---|---|---|---|
| Subclasses | Aluno/Professor | 4 tipos | — | — |
| Identificador gerado | Matrícula `ALU-#####` | — | Tombamento `BA-######` | — |
| Proteção de concorrência na geração | só `UNIQUE` da coluna | — | *advisory lock* | `ON CONFLICT` |
| Exclusão | inativa | `ativo=false` | apaga **ou** baixa | não se exclui |
| Validação extra | CPF, e-mail | ISBN/ISSN | quantidade 1–100 | por chave + justificativa |
| Tipo editável | não | não | — | — |

---

## 13. Módulos de negócio

### 13.1 Empréstimo (`EmprestimoService.criar`)

É a operação com **mais regras**. Ordem de verificação:

1. Pelo menos **um exemplar** foi selecionado (duplicados são ignorados).
2. Leitor **existe e está ativo**.
3. Data prevista **não é anterior a hoje** e **não passa do prazo** do tipo de leitor (`prazo_alunos`/`prazo_professores`).
4. **Trava** a linha do leitor (`FOR UPDATE`).
5. Leitor **sem multa pendente** (`saldoPendentePorLeitor > 0` bloqueia).
6. **Limite de exemplares simultâneos** (`limite_alunos`/`limite_professores`): `ativos + pedidos ≤ limite`.
7. **Expira reservas vencidas** antes de continuar (para que não segurem exemplares).
8. Para cada exemplar: existe, está **disponível**, o material está **ativo** e **não é digital**.
9. **Fila de reservas:** se há leitores à frente na fila daquele título, os exemplares livres já estão "comprometidos"; só o primeiro da fila pode retirar. Se o próprio leitor tem reserva, ela é **atendida** (`atender`) após o empréstimo.
10. Para cada exemplar, `EmprestimoRepository.criar` trava o exemplar (`FOR UPDATE`), **revalida** a disponibilidade, insere o empréstimo e marca o exemplar como **indisponível**.
11. Notificação + auditoria.

**Padrões padrão (se a configuração não existir):** limite 3 (aluno) / 5 (professor); prazo 7 / 14 dias; taxa R$ 2,00; **no máximo 1 renovação**.

**Renovar:** só `ATIVO`; só se `renovacoes < 1`; **não se estiver atrasado**; **não se houver reserva pendente** do título. A nova data = data prevista atual + prazo do tipo. O `UPDATE` é condicional (`WHERE renovacoes < ?`), então duas renovações simultâneas não passam.

**Cancelar:** só `ATIVO`; libera o exemplar e avisa o primeiro da fila.

### 13.2 Devolução (`EmprestimoService.devolver`)

1. Só `ATIVO`.
2. `UPDATE ... WHERE status = 'ATIVO'` — se retornar `false`, outra requisição já devolveu (**evita multa em duplicidade**).
3. Libera o exemplar.
4. `diasDeAtraso = max(0, dias entre a data prevista e hoje)`.
5. Se atrasou: `multa = taxa_diaria_multa × dias` (só cria se o valor for > 0), com status `PENDENTE`, e gera notificação.
6. Auditoria; avisa o primeiro da fila de reservas que o material voltou.

`BigDecimal` é usado para dinheiro (nunca `double`, que acumula erro de arredondamento).

### 13.3 Reserva (`ReservaService`)

- Reserva é **por título**, não por exemplar. Fila **FIFO** (primeiro a reservar, primeiro a ser atendido), com `posicao`.
- **Só se reserva quando não há exemplar livre.** Regra: `livres > naFila` → recusa ("ainda tem exemplares disponíveis").
- Recusa também: leitor inativo, material excluído, **material digital**, prazo de retirada no passado ou > 30 dias, leitor que já está com um exemplar emprestado do título, título sem exemplares.
- `ReservaRepository.criar` trava o título e calcula `MAX(posicao) + 1` entre as reservas aguardando.
- Ao sair da fila (atendida/cancelada/expirada) a fila é **renumerada** sem buracos com `ROW_NUMBER() OVER (...)`.
- O prazo vale **até o fim do dia** (`prazo_retirada::date < CURRENT_DATE` para expirar).
- Se um leitor é inativado, suas reservas aguardando são canceladas.

### 13.4 Multa e pagamento (`MultaService`)

- **Pagar:** trava a multa (`FOR UPDATE`); recusa forma de pagamento vazia (> 50 caracteres), multa já `PAGA`/`CANCELADA`, valor ≤ 0, mais de 2 casas decimais, valor maior que o saldo. Soma ao `valor_pago`: se atingiu o total → `PAGA` (+ notificação); senão → `PAGA_PARCIAL`.
- **Estornar:** exige **justificativa**; marca o pagamento como estornado (`UPDATE ... WHERE estornado = FALSE`, só um estorno por pagamento), **subtrai** o valor de `valor_pago` e recalcula o status (`PENDENTE` ou `PAGA_PARCIAL`). O **pagamento não é apagado** — o histórico se mantém.
- **Efeito sobre o empréstimo:** enquanto houver saldo pendente, o leitor está **bloqueado** para novos empréstimos.

### 13.5 Notificações e WebSocket

```
Service ──► NotificacaoRepository.salvar() ──► INSERT em notificacoes
                         │
                         └─ registra "afterCommit" ──► NotificationWebSocketHandler.enviar()
                                                            │  JSON {tipo, dados, timestamp}
                                                            ▼
                                              todas as sessões WebSocket abertas
                                                            ▼
                                       bublo.js: incrementa o contador + exibe um toast
```

Pontos a destacar:

- A notificação é **persistida** no banco; o WebSocket é apenas o **canal de entrega em tempo real**.
- O envio ocorre **após o commit** (`TransactionSynchronization.afterCommit`). Se a transação for desfeita, **ninguém é avisado** de algo que não aconteceu.
- `NotificationWebSocketHandler` guarda as sessões num `ConcurrentHashMap.newKeySet()` (conjunto seguro para múltiplas threads) e envia a todas.
- O endpoint `/ws/notificacoes` fica atrás do filtro do Spring Security (a rota exige usuário autenticado).
- O navegador tenta **reconectar** automaticamente (até 5 vezes, com espera crescente).
- É WebSocket **nativo**, sem STOMP e sem SockJS.

### 13.6 Relatórios (`RelatorioService`)

- **CSV** com separador `;` e **BOM UTF-8** (para o Excel abrir acentos corretamente).
- **Proteção contra "injeção de fórmula":** valor que começa com `=`, `+`, `-`, `@`, TAB ou CR recebe um apóstrofo `'` na frente, para o Excel não interpretá-lo como fórmula.
- **XLSX** com Apache POI (`XSSFWorkbook`) para empréstimos e multas.
- Limite de 10.000 linhas por exportação.

### 13.7 Dashboard

`DashboardController` consulta diretamente os repositórios e monta 9 indicadores (títulos, exemplares, disponíveis, indisponíveis, empréstimos ativos, atrasados, multas pendentes, reservas, leitores), o gráfico de empréstimos dos últimos 6 meses (`generate_series` garante meses com zero) e a distribuição por tipo, mais as 6 últimas atividades da auditoria. Gráficos com **Chart.js** (arquivo local).

---

## 14. Segurança

### 14.1 `SecurityConfig` linha a linha (conceitual)

| Configuração | Efeito |
|---|---|
| `.requestMatchers("/login", "/css/**", "/js/**", "/lib/**", ...).permitAll()` | Só login e arquivos estáticos são públicos. |
| `.anyRequest().authenticated()` | **Todo o resto exige login** (inclusive `/ws/**`). |
| `.formLogin(...)` | Página própria `/login`; em sucesso grava auditoria `LOGIN` e vai ao dashboard; em falha grava `LOGIN_FALHA` e volta com `?error=true`. |
| `.logout(...)` | `/logout`; invalida a sessão e apaga os cookies. |
| `.sessionManagement().maximumSessions(1)` | **Uma sessão por usuário**: um segundo login derruba o primeiro (com aviso na tela de login). |
| `BCryptPasswordEncoder` | Senhas guardadas como *hash* BCrypt (irreversível, com *salt*). |
| `UserDetailsService` | Ensina o Spring a buscar o usuário no **nosso** banco (`usuarioRepository.buscarPorUsername`); papel único `ADMIN`; `accountLocked` se `ativo = false`. |
| `AuthenticationManager` + `DaoAuthenticationProvider` | Compara a senha digitada com o *hash* usando o `PasswordEncoder`. |

### 14.2 CSRF

**CSRF** (*Cross-Site Request Forgery*) é um ataque em que outro site faz seu navegador (já logado) enviar um `POST` ao sistema. O Spring Security exige um **token secreto** em todo `POST`. O Thymeleaf o insere sozinho nos formulários com `th:action`; o `base.html` também expõe o token em `<meta>` (`_csrf`, `_csrf_header`) para uso em JavaScript.

### 14.3 Outras defesas presentes no código

| Ameaça | Defesa |
|---|---|
| Injeção de SQL | `PreparedStatement` com `?` em todo valor; lista branca em `ORDER BY` |
| XSS | Thymeleaf escapa texto (`th:text`); o JS usa `textContent` em vez de `innerHTML` para dados do usuário |
| Fixação/roubo de cookie | `http-only`, `same-site: LAX` |
| Vazamento de erros | `GlobalExceptionHandler` mostra mensagem amigável e **nunca** o *stack trace* |
| Enumeração de usuário | Mensagem única "Usuário ou senha incorretos." |
| Fórmula em planilha | Prefixo `'` nos CSVs |
| Credencial no código | Nada de senha ou usuário escrito no código nem valor padrão: tudo vem de variável de ambiente (`.env`, ignorado pelo Git). `admin`/`admin` é só o exemplo de desenvolvimento |
| Auditoria de segurança | `LOGIN`, `LOGIN_FALHA`, `ALTERACAO_SENHA` registrados |

### 14.4 Troca de senha

`AuthController.trocaSenha`: nova senha (4 a 72 caracteres — 72 é o limite do BCrypt) + confirmação; grava o novo *hash*, registra auditoria e **encerra a sessão** (o usuário entra de novo).

### 14.5 Tratamento de erros

- `GlobalExceptionHandler` (`@ControllerAdvice`): `IllegalStateException`/`IllegalArgumentException` → página de erro 400 com a mensagem; `NoResourceFoundException` → 404; arquivo grande → 413; qualquer outra → 500 genérico (e o detalhe vai só para o log).
- `ErroController` cobre as rotas de erro do próprio contêiner (`/error`).
- `GlobalModelAdvice` injeta em **todas** as páginas: usuário logado, contagem de notificações não lidas e o nome da instituição (fixo: "Bublo").

---

## 15. Front-end

### 15.1 Thymeleaf — a ideia

O template é HTML comum com atributos `th:*` que o servidor resolve **antes** de enviar a página:

| Atributo | Faz |
|---|---|
| `th:text="${l.nome}"` | Insere texto (escapado) |
| `th:each="l : ${resultado.conteudo}"` | Repete o elemento para cada item |
| `th:if` / `th:unless` | Mostra condicionalmente |
| `th:href="@{/leitores/{id}(id=${l.id})}"` | Monta URL com parâmetros |
| `th:classappend` | Acrescenta classe CSS condicional |
| `th:replace="~{layout/base :: head}"` | **Fragmento**: reaproveita um pedaço de outro arquivo |

### 15.2 Layout reaproveitado (`templates/layout/`)

- `base.html` — fragmento `head` (CSS, tema, CSRF) e fragmento `scripts` (JS + `flash-data`).
- `sidebar.html` — menu lateral; recebe o módulo ativo como parâmetro.
- `topbar.html` — barra superior (notificações, tema, conta).
- `componentes.html` — `abas` (Em andamento/Histórico) e `paginacao` (janela de páginas que preserva os filtros da URL).

Toda página começa com os mesmos três `th:replace` (head, sidebar, topbar) e termina com `scripts`: é o "master page" do projeto.

### 15.3 `bublo.js` (módulo `Bublo`)

| Parte | Função |
|---|---|
| `Tema` | Alterna claro/escuro; guarda a escolha em `localStorage`; um script inline no `<head>` aplica o tema **antes** da primeira pintura (evita "piscar") |
| `Toasts` | Mensagens flutuantes; `exibirFlash()` lê o `<div id="flash-data">` e mostra sucesso/erro/aviso |
| `Confirmacoes` | Qualquer formulário com `data-confirmar="..."` abre um modal Bootstrap de confirmação antes de enviar |
| `Notificacoes` | Busca `/notificacoes/resumo` (JSON) e abre o WebSocket |
| `iniciarMenuMobile` | Menu lateral recolhível, com foco e tecla `Esc` |
| `iniciarEnvios` | Desabilita o botão após o envio (**anti duplo clique**) e trata o botão "voltar" do navegador |

### 15.4 JavaScript específico de página

- `emprestimos/novo.html`: ao escolher o leitor faz `fetch('/emprestimos/restricao/{id}')` (JSON) e mostra, **antes de enviar**, se o leitor está bloqueado por multa ou se passou do limite; ajusta a data máxima ao prazo do tipo de leitor.
- `acervo/formulario.html`: campos por tipo e "chips" de autores.

> **Importante:** o JavaScript melhora a experiência, **mas as regras valem no servidor**. Se o JS for burlado, o `Service` bloqueia do mesmo jeito.

### 15.5 Sem CDN

Bootstrap, Bootstrap Icons e Chart.js estão em `static/lib/`. O sistema funciona sem internet.


---

## 16. Testes, Docker e CI

### 16.1 Testes (91)

| Arquivo | Testes | O que cobre |
|---|---|---|
| `EmprestimoServiceTest` | 19 | multa por atraso, renovação, limites, bloqueio por multa, fila de reservas |
| `AcervoServiceTest` | 24 | cadastro por tipo, exemplares, exclusão/baixa, ISBN duplicado, validação da capa |
| `LeitorServiceTest` | 15 | CPF, e-mail, tipo imutável, inativação, importação CSV |
| `ConfiguracaoServiceTest` | 13 | validação por chave, justificativa, reversão |
| `MultaServiceTest` | 12 | pagamento integral/parcial, excesso, estorno |
| `ReservaServiceTest` | 10 | regras de reserva, expiração |
| `OrdenacaoSeguraTest` | 4 | lista branca do `ORDER BY` |
| `BubloApplicationTests` | 1 | a classe da aplicação carrega |

**Como são feitos:** testes **unitários** com **JUnit 5** + **Mockito** + **AssertJ**. Os repositórios são **simulados** (`mock(EmprestimoRepository.class)`), então testam **somente as regras do `Service`**, rápido e sem banco. Padrão de cada teste: *preparar* (`when(...).thenReturn(...)`) → *executar* (`service.devolver(...)`) → *verificar* (`assertThat(...)`, `verify(...)`).

`src/test/resources/application.properties` desliga DataSource/Flyway/web para que o contexto não precise de banco.

### 16.2 Smoke test

`scripts/smoke-test.sh` faz login real, cria dados pelos formulários e acessa as páginas, para pegar erros de template e de mapeamento SQL que os testes unitários não enxergam (os mocks não executam SQL).

### 16.3 Docker

- **`Dockerfile`** com **build em dois estágios**: o primeiro (Maven + JDK 21) compila e gera o `.jar`; o segundo (apenas JRE 21) só executa — imagem final menor.
- **`docker-compose.yml`**: só o PostgreSQL 15, com *healthcheck* (`pg_isready`) e volume persistente.
- **`docker-compose.app.yml`**: PostgreSQL + aplicação (a aplicação espera o banco ficar saudável: `depends_on ... service_healthy`).
- **`.env.example`**: modelo das variáveis; o `.env` real **não** vai para o Git.

**Para rodar localmente:**
```bash
docker compose up -d            # sobe o PostgreSQL
./mvnw spring-boot:run          # sobe a aplicação (Java 21)
# acessar http://localhost:8080
```

### 16.4 CI (`.github/workflows/ci.yml`)

Em *push*/*pull request* para `master`/`main`: sobe um PostgreSQL de serviço, configura Java 21, compila (`mvn compile`), roda os testes (`mvn test`) e executa uma verificação de segredos no código.

---

## 17. Pontos de atenção: o código vs. a documentação antiga

Esta seção é a mais útil para **não ser pego de surpresa**. São diferenças reais entre o que o código faz e o que outros arquivos do repositório (ou intuições comuns) dizem. Conheça-as antes de apresentar; se perguntarem, responda com segurança.

| # | Observação | Como responder |
|---|---|---|
| 1 | **`BaseRepository` não é usado.** `docs/DOCUMENTACAO_TECNICA.md` diz que "cada repositório estende `BaseRepository`", mas nenhum estende. Cada repositório recebe o `JdbcTemplate` e repete o método `insertReturnId`. | "Existe uma classe base com os utilitários JDBC; os repositórios atuais usam o `JdbcTemplate` diretamente. É uma duplicação que poderia ser removida estendendo a base." |
| 2 | **Parte do polimorfismo ainda é demonstrativa.** `getCapaGenerica()` e `getCampoEspecifico*()` (materiais) **não são chamados** por nenhum template atual. Já `getLimiteExemplaresSimultaneos()`, `getPrazoEmprestimoDias()` e as chaves de configuração (`Leitor` → `Aluno`/`Professor`) **são usados** pelo `EmprestimoService` como valor padrão e para escolher a chave `limite_*`/`prazo_*` na tabela `configuracoes`, que o administrador edita. | "O limite padrão vem da própria subclasse do leitor (sobrescrita) e o administrador pode sobrepô-lo pela configuração, sem recompilar. Na tela de materiais, o tipo é exibido por outros meios; os métodos de capa e campo específico ficam como contrato do modelo." O polimorfismo também é usado no *RowMapper*/`construirItem` (escolha da subclasse). |
| 3 | **SockJS não é usado.** `docs/DOCUMENTACAO_TECNICA.md` e `docs/DECISOES_TECNICAS.md` mencionam SockJS; o `pom.xml` não o inclui e o `bublo.js` usa `new WebSocket(...)` nativo. | "WebSocket nativo do Spring, sem STOMP e sem SockJS; a reconexão é feita no próprio `bublo.js`." |
| 4 | **Tratamento do erro por texto da mensagem.** `LeitorController.salvar` decide em qual campo mostrar o erro procurando "e-mail", "CPF" ou "elefone" dentro do texto da exceção. Funciona, mas é frágil: mudar a mensagem muda o comportamento. | "É uma solução prática; a evolução natural seria uma exceção própria que carregue o nome do campo." |
| 5 | **A matrícula é gerada por `MAX + 1` sem lock** (o tombamento, ao contrário, usa *advisory lock*). A integridade fica por conta da restrição `UNIQUE` em `leitores.matricula`: numa colisão rara, o segundo cadastro falha em vez de duplicar. | "Há dois mecanismos diferentes para o mesmo problema. O tombamento é mais crítico (alto volume em lote), por isso tem lock; a matrícula depende do `UNIQUE`." |
| 6 | **Importação CSV e transação.** `importarCSV` chama `salvar(...)` do **mesmo** objeto. No Spring, a anotação `@Transactional` vale quando a chamada passa pelo *proxy*; uma chamada direta dentro da própria classe (*self-invocation*) **não passa**. Por análise do código, cada gravação da importação tende a confirmar individualmente (sem transação única por linha). Não foi testado em execução. | Só mencione se perguntarem sobre transações na importação; explique o conceito de *proxy* do Spring. |
| 7 | **Os testes são só unitários.** Não há testes de integração contra o PostgreSQL; o SQL é exercitado pelo `smoke-test.sh` e manualmente. O CI sobe um PostgreSQL, mas os testes atuais não o utilizam (`src/test/resources/application.properties` desliga o DataSource). | "A cobertura automatizada é das regras de negócio. O SQL é validado pelo teste de fumaça em ambiente real." |
| 8 | **`ATRASADO` não é gravado.** O `CHECK` do banco permite o valor, mas o código nunca o atribui: o atraso é derivado da data. | "Atraso é um estado calculado, para nunca ficar desatualizado." |
| 9 | **Etapa de CI "ausência de segredos".** Usa `grep` por termos como `senha`, `password`, `token`, que também aparecem em código legítimo (por exemplo, `senhaHash`). Vale conferir no GitHub Actions se essa etapa está passando. | — |
| 10 | **Apenas o administrador autentica.** Leitores não têm login nem catálogo público (decisão de escopo registrada em `DECISOES_TECNICAS.md`). | "Escopo definido: sistema interno do bibliotecário." |

---

## 18. Perguntas prováveis da banca, com respostas

**1. Qual é a arquitetura do sistema?**
Aplicação monolítica em camadas (Controller → Service → Repository → Banco), com páginas renderizadas no servidor via Thymeleaf. Cada camada tem uma responsabilidade; as regras de negócio ficam na camada de serviço.

**2. Por que JDBC puro e não JPA/Hibernate?**
Foi requisito do trabalho. Dá controle total sobre o SQL (locks `FOR UPDATE`, *advisory lock*, `ON CONFLICT`, índices parciais, `ROW_NUMBER`), ao custo de escrever e manter mais código.

**3. Como o sistema evita SQL injection?**
Todos os valores vão como parâmetros de `PreparedStatement` (`?`), nunca concatenados. O único trecho em que parte do SQL vem da requisição, o `ORDER BY`, passa por `OrdenacaoSegura`, que só aceita colunas de uma lista branca.

**4. Onde está a herança no projeto?**
`ItemAcervo` (abstrata) é a base de `Livro`, `Revista`, `DVD` e `PublicacaoDigital`; `Leitor` (abstrata) é a base de `Aluno` e `Professor`. No banco, cada hierarquia fica em **uma tabela** com uma coluna `tipo`.

**5. Onde está o polimorfismo?**
Os métodos abstratos (`getCampoEspecificoLabel`, `getLimiteExemplaresSimultaneos`...) são implementados de forma diferente em cada subclasse; e o repositório escolhe a subclasse certa a partir da coluna `tipo`. O `EmprestimoService` o usa de verdade: chama `leitor.getLimiteExemplaresSimultaneos()` sem saber se é aluno ou professor.

**5-A. Qual a diferença entre sobrecarga e sobrescrita? Onde estão no projeto?**
**Sobrescrita** (*override*): a subclasse redefine um método herdado, com a mesma assinatura (`@Override`); o Java escolhe a versão pelo tipo real do objeto, em execução. Ex.: `Aluno` e `Professor` sobrescrevem `getLimiteExemplaresSimultaneos()` (3 e 5). **Sobrecarga** (*overload*): mesmo nome com parâmetros diferentes; o compilador escolhe pelos argumentos. Ex.: `EmprestimoService.diasDeAtraso(prevista)` e `diasDeAtraso(prevista, hoje)`. Detalhes na Seção 8.3.

**6. O que é uma transação e onde ela está?**
É um conjunto de operações que confirma por inteiro ou desfaz por inteiro. Cada método de escrita dos serviços tem `@Transactional`. Se uma `IllegalStateException` é lançada, tudo o que foi gravado naquele método é desfeito.

**7. Como duas pessoas emprestando o mesmo exemplar ao mesmo tempo são tratadas?**
`EmprestimoRepository.criar` executa `SELECT ... FOR UPDATE` na linha do exemplar. O segundo pedido espera o primeiro terminar e então **revalida** a disponibilidade: encontra o exemplar já indisponível e recebe erro.

**8. Como a multa é calculada?**
Na devolução: `dias de atraso = max(0, dias entre a data prevista e hoje)`; `multa = taxa diária × dias`. A taxa vem da configuração `taxa_diaria_multa` (padrão R$ 2,00). Usa `BigDecimal`.

**9. O que acontece se o leitor tem multa pendente?**
Fica **bloqueado**: o serviço recusa novos empréstimos enquanto o saldo pendente for maior que zero. A tela de empréstimo também avisa antes do envio (consulta `/emprestimos/restricao/{id}`).

**10. Como funciona a fila de reservas?**
FIFO por título. Só se reserva quando todos os exemplares estão emprestados. Ao sair da fila, as posições são renumeradas. Quem está à frente tem prioridade sobre exemplares livres; as reservas vencem no fim do dia do prazo de retirada, e um agendador as expira a cada hora.

**11. Como as senhas são protegidas?**
Com **BCrypt** (função de *hash* lenta, com *salt*): o banco guarda só o *hash*, que não pode ser revertido. A senha digitada é comparada, não descriptografada.

**12. O que é CSRF e como o sistema se protege?**
Ataque em que um site malicioso faz o navegador logado enviar um `POST` ao sistema. O Spring Security exige um token secreto em todo `POST`; o Thymeleaf o coloca nos formulários automaticamente.

**13. Como as notificações em tempo real funcionam?**
A notificação é gravada no banco; **depois do commit** da transação, o servidor a envia por WebSocket a todos os navegadores conectados, onde o `bublo.js` atualiza o contador e mostra um toast. Enviar só após o commit evita avisar algo que foi desfeito.

**14. Por que o leitor e o material não são apagados?**
Têm histórico (empréstimos, multas, reservas) e chaves estrangeiras. Apagar quebraria a integridade e a rastreabilidade. Fazem **exclusão lógica** (`ativo = false`). O exemplar com histórico recebe **baixa** pelo mesmo motivo.

**15. O que é o Flyway e por que usá-lo?**
Ferramenta de versionamento do banco: cada alteração do esquema é um arquivo SQL numerado (`V1`, `V2`, `V3`) aplicado uma única vez e em ordem. Garante que todo ambiente tenha o mesmo esquema e dispensa criar tabelas à mão.

**16. O que é um DTO e por que existe?**
*Data Transfer Object*: objeto simples que trafega entre tela e serviço (com as anotações de validação). Evita expor o modelo de domínio diretamente.

**17. Como a injeção de dependência funciona aqui?**
Cada classe declara suas dependências no **construtor**; o Spring cria os objetos (beans) e os entrega prontos. Por isso nenhuma classe de negócio tem `new` de repositório ou serviço.

**18. Como o sistema é auditado?**
Toda escrita relevante (e login/falha de login/troca de senha) grava uma linha em `auditoria` com usuário, data/hora, tipo, módulo, registro afetado, valor antes/depois e justificativa quando exigida. A tela `/auditoria` consulta e filtra.

**19. Como as configurações podem ser alteradas com segurança?**
Pela tela de configurações, exigindo **justificativa**; cada valor é validado por chave; a mudança é auditada e a **última** alteração pode ser desfeita.

**20. O que você melhoraria?**
(Use a Seção 17.) Por exemplo: remover o `BaseRepository` ou fazer os repositórios o estenderem; criar exceções de domínio com o campo afetado; adicionar testes de integração com banco real (Testcontainers); revisar a transação da importação CSV.

---

## 19. Roteiro sugerido de apresentação

| Etapa | O que mostrar | Ancoragem no código |
|---|---|---|
| 1. Contexto | O problema e os módulos (Seção 1) | — |
| 2. Login e dashboard | Entrar; indicadores | `SecurityConfig`, `DashboardController` |
| 3. **Cadastro de leitor** | Preencher, errar de propósito (CPF inválido), salvar; ver a matrícula gerada | Seção 11 inteira |
| 4. **Cadastro de material** | Trocar o tipo e ver os campos mudarem; adicionar autores; cadastrar exemplares em lote e ver o tombamento | `AcervoService`, `ExemplarRepository.proxTombamento` |
| 5. **Empréstimo** | Escolher um leitor com multa (bloqueado) e outro normal; emprestar | `EmprestimoService.criar` |
| 6. **Devolução com atraso** | Devolver o empréstimo atrasado da demonstração → multa gerada | `EmprestimoService.devolver` |
| 7. **Multa** | Pagamento parcial, total e estorno | `MultaService` |
| 8. **Reserva** | Emprestar todos os exemplares de um título e reservar | `ReservaService` |
| 9. Tempo real | Duas abas abertas: uma notificação aparece na outra | `NotificacaoRepository` + `bublo.js` |
| 10. Auditoria e configurações | Mostrar o registro de tudo que foi feito | `AuditoriaRepository`, `ConfiguracaoService` |
| 11. Código: camadas | Abrir Controller → Service → Repository do mesmo fluxo | Seção 4 |
| 12. Banco | Esquema e restrições (índice parcial, `CHECK`) | `V1__schema.sql` |
| 13. Qualidade | Rodar `mvn test` | Seção 16 |

**Dica:** escolha **um fluxo** (recomendação: empréstimo + devolução com multa) e percorra o código dele de ponta a ponta. Mostrar profundidade em um fluxo costuma ser mais convincente do que passar rápido por todos.

---

## 20. Mini quiz

> Responda **antes** de olhar o gabarito (Seção 21). Parte A: múltipla escolha (uma resposta). Parte B: respostas curtas, com suas palavras.

### Parte A — Múltipla escolha

**1.** Qual anotação faz um método do `Service` rodar dentro de uma transação?
a) `@Service`  b) `@Transactional`  c) `@Valid`  d) `@Scheduled`

**2.** O que significa `ItemAcervo` ser `abstract`?
a) Não tem atributos  b) Não pode ser instanciada com `new`  c) Só existe no banco  d) Não pode ter subclasses

**3.** Por que se usa `PreparedStatement` com `?`?
a) Deixa o SQL mais rápido para escrever  b) Impede injeção de SQL ao separar o SQL dos valores  c) Evita o uso de transações  d) É obrigatório para `SELECT`

**4.** Qual classe impede injeção de SQL no `ORDER BY`?
a) `Paginacao`  b) `BaseRepository`  c) `OrdenacaoSegura`  d) `GlobalExceptionHandler`

**5.** O que acontece se o administrador tentar mudar o **tipo** de um leitor já cadastrado?
a) O tipo é alterado e a matrícula é refeita  b) O sistema lança erro: o tipo não pode ser alterado  c) O leitor é duplicado  d) Nada, o campo é ignorado em silêncio

**6.** Como é calculado o valor da multa?
a) Valor fixo de R$ 2,00  b) Taxa diária × dias de atraso  c) 10% do valor do livro  d) Dias de atraso ÷ taxa

**7.** Quantas renovações são permitidas por empréstimo?
a) Nenhuma  b) 1  c) 3  d) Ilimitadas

**8.** Em que situação é possível criar uma reserva?
a) Sempre que o leitor quiser  b) Quando todos os exemplares do título estão emprestados  c) Só para materiais digitais  d) Só para professores

**9.** O que o padrão **Post/Redirect/Get** evita?
a) Injeção de SQL  b) Reenvio do formulário ao apertar F5  c) Ataques CSRF  d) Lentidão do banco

**10.** O que um `SELECT ... FOR UPDATE` faz?
a) Atualiza a linha automaticamente  b) Trava a linha até o fim da transação, fazendo as demais operações esperarem  c) Apaga a linha  d) Cria um índice

**11.** Como o sistema impede que dois cadastros simultâneos gerem o mesmo tombamento?
a) Usando `pg_advisory_xact_lock` antes de calcular o próximo número  b) Usando JavaScript no formulário  c) Não impede  d) Gerando números aleatórios

**12.** Qual algoritmo protege as senhas armazenadas?
a) MD5  b) Texto puro  c) BCrypt  d) Base64

**13.** Quando uma notificação é enviada pelo WebSocket?
a) Antes de gravar no banco  b) Depois do *commit* da transação  c) A cada 5 segundos  d) Só quando o usuário recarrega a página

**14.** Um empréstimo é considerado **atrasado** quando…
a) O status no banco é `ATRASADO`  b) Está `ATIVO` e a data prevista de devolução já passou  c) O leitor tem multa  d) Foi renovado

**15.** O que o sistema faz ao "excluir" um exemplar que **já foi emprestado** antes?
a) Apaga a linha e os empréstimos  b) Dá **baixa** (marca como baixado), preservando o histórico  c) Impede qualquer exclusão  d) Move para outro título

**16.** Para que serve o Flyway?
a) Enviar e-mails  b) Versionar e aplicar as alterações do esquema do banco  c) Gerar relatórios  d) Fazer o login

**17.** Quantas vezes um mesmo pagamento pode ser **estornado**?
a) Quantas forem necessárias  b) Apenas uma  c) Nenhuma  d) Duas

**18.** Onde ficam as regras de negócio (limite de empréstimos, bloqueio por multa)?
a) No template Thymeleaf  b) No Controller  c) No Service  d) No `application.yaml`

**19.** O que acontece com um leitor ao ser "excluído" pela tela?
a) É apagado do banco  b) É inativado (`ativo = false`), se não tiver empréstimos ativos  c) É transferido  d) Vira professor

**20.** Quantas sessões simultâneas o mesmo administrador pode ter?
a) Ilimitadas  b) 1 — um novo login derruba a sessão anterior  c) 10  d) Nenhuma

### Parte B — Respostas curtas

**21.** Descreva, em ordem, as camadas por onde passa um `POST /leitores` até gravar no banco e o que cada uma faz.

**22.** Explique a diferença entre **herança** e **polimorfismo** usando `ItemAcervo` e suas subclasses.

**23.** Por que o sistema usa `BigDecimal` para valores em dinheiro?

**24.** Explique por que existe a **baixa de exemplar** em vez de simplesmente apagar a linha.

**25.** Como o sistema impede que a mesma devolução gere duas multas se dois cliques chegarem quase juntos?

**26.** O que é CSRF e qual é a defesa usada no projeto?

**27.** Qual é a diferença entre `@Controller`, `@Service` e `@Repository`?

**28.** Por que a notificação é enviada **depois** do commit e não logo após o `INSERT`?

**29.** O que é um DTO e por que o formulário não usa diretamente a classe `Leitor`?

**30.** Se alguém perguntar "o que acontece se eu usar outra conexão para um cadastro específico?", o que você responde?

---

## 21. Gabarito do quiz

### Parte A

| Q | R | Q | R | Q | R | Q | R |
|---|---|---|---|---|---|---|---|
| 1 | b | 6 | b | 11 | a | 16 | b |
| 2 | b | 7 | b | 12 | c | 17 | b |
| 3 | b | 8 | b | 13 | b | 18 | c |
| 4 | c | 9 | b | 14 | b | 19 | b |
| 5 | b | 10 | b | 15 | b | 20 | b |

**Observações:**
- **Q4**: `OrdenacaoSegura` aceita só colunas de uma lista branca e `ASC`/`DESC`.
- **Q14**: o `CHECK` do banco permite `ATRASADO`, mas o código nunca grava esse status; o atraso é calculado pela data.
- **Q17**: `UPDATE pagamentos SET estornado = TRUE ... WHERE id = ? AND estornado = FALSE` só altera uma vez.

### Parte B (respostas-modelo)

**21.** *Filtros de segurança* (autenticação e CSRF) → **Controller** (`LeitorController.salvar`: recebe o formulário, valida o DTO com `@Valid`, chama o serviço e decide a resposta) → **Service** (`LeitorService.salvar`: normaliza, valida CPF/e-mail/tipo, escolhe `Aluno` ou `Professor`, grava e audita, tudo em transação) → **Repository** (`LeitorRepository.salvar`: gera a matrícula e executa o `INSERT` com `PreparedStatement`) → **JdbcTemplate/HikariCP** → **PostgreSQL**. Depois, *redirect* para a listagem com a mensagem de sucesso.

**22.** **Herança**: `Livro`, `Revista`, `DVD` e `PublicacaoDigital` herdam de `ItemAcervo` os atributos comuns (título, editora, autores...) e acrescentam os próprios (ISBN, ISSN/volume, duração, DOI/URL). **Polimorfismo**: o mesmo método (`getCampoEspecificoLabel()`) devolve respostas diferentes conforme a subclasse real ("ISBN", "ISSN", "Duração", "DOI"); e o repositório decide qual subclasse instanciar a partir da coluna `tipo`.

**23.** `double` representa números em binário e acumula erros de arredondamento (0,1 + 0,2 ≠ 0,3). `BigDecimal` representa decimais exatos e permite controlar a escala (2 casas), o que é essencial para dinheiro.

**24.** O exemplar com histórico de empréstimos é referenciado por chave estrangeira; apagá-lo é impossível (ou apagaria o histórico). A **baixa** marca `baixado = TRUE`, remove o exemplar das listas e contagens, mas mantém o histórico e o tombamento (que nunca é reaproveitado). Exemplar nunca emprestado pode ser apagado de fato.

**25.** O `UPDATE emprestimos SET status = 'DEVOLVIDO' ... WHERE id = ? AND status = 'ATIVO'` é **condicional**: só a primeira requisição altera uma linha. A segunda recebe `0` linhas alteradas, o método devolve `false` e o serviço lança "Empréstimo não está ativo" **antes** de calcular multa. Além disso o botão é desabilitado no front-end após o primeiro clique.

**26.** *Cross-Site Request Forgery*: um site malicioso induz o navegador já autenticado a enviar um `POST` ao sistema. A defesa é o **token CSRF** do Spring Security: exigido em todo `POST`, inserido automaticamente nos formulários pelo Thymeleaf; um site externo não consegue conhecê-lo.

**27.** São *estereótipos* que marcam o papel da classe e a registram como bean: `@Controller` recebe HTTP e escolhe a página; `@Service` concentra as regras de negócio e as transações; `@Repository` acessa o banco com SQL.

**28.** Porque a transação ainda pode ser **desfeita**. Se a notificação saísse antes do commit e houvesse *rollback*, o usuário seria avisado de algo que não aconteceu. Por isso o envio é registrado em `afterCommit`.

**29.** DTO (*Data Transfer Object*) é um objeto simples para trafegar dados entre a tela e o serviço, com as anotações de validação do formulário. Usá-lo evita acoplar a tela ao modelo de domínio (que é abstrato e tem listas de relacionamentos) e permite expor apenas os campos necessários.

**30.** "Todos os cadastros usam o **mesmo** `DataSource` (pool HikariCP) e o mesmo `JdbcTemplate`; não há conexão alternativa. Dentro de um método `@Transactional`, todos os repositórios compartilham **uma** conexão/transação, o que garante a atomicidade. O que difere entre cadastros são as regras do serviço e o tipo de proteção de concorrência (`FOR UPDATE`, *advisory lock*, índices únicos), não a conexão."

### Como pontuar

| Acertos (A, 20 questões) | Leitura |
|---|---|
| 18–20 | Pronto para a banca |
| 14–17 | Revise as seções onde errou |
| < 14 | Releia as Seções 4, 9, 10 e 11 e refaça |

Para a Parte B, confira se a sua resposta contém os **termos-chave** do gabarito (por exemplo: *transação*, *PreparedStatement*, *FOR UPDATE*, *BCrypt*, *token CSRF*, *exclusão lógica*, *commit*).
