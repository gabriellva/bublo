# Bublo — Mapa do código por camadas

> **Para quê serve este documento:** entender e explicar, na apresentação, **onde cada coisa mora no código, o que cada pasta e cada arquivo faz, e por onde o dado passa** quando alguém usa o sistema. Cada camada vem com exemplos reais do projeto.
>
> Complementa o `GUIA_DE_ESTUDO_E_APRESENTACAO.md` (que aprofunda Java/Spring, banco e POO). Aqui o foco é o **mapa** e os **fluxos de ponta a ponta**.

## Sumário

1. [O sistema em uma página](#1-o-sistema-em-uma-página)
2. [O caminho de uma requisição](#2-o-caminho-de-uma-requisição)
3. [Raiz do projeto](#3-raiz-do-projeto)
4. [Camada `config/` e `security/`: ligar tudo e proteger](#4-camada-config-e-security-ligar-tudo-e-proteger)
5. [Camada `controller/`: a porta de entrada](#5-camada-controller-a-porta-de-entrada)
6. [Camada `dto/`: os dados que viajam](#6-camada-dto-os-dados-que-viajam)
7. [Camada `service/`: as regras de negócio](#7-camada-service-as-regras-de-negócio)
8. [Camada `repository/`: o SQL](#8-camada-repository-o-sql)
9. [Camada `model/`: o domínio](#9-camada-model-o-domínio)
10. [`exception/` e `websocket/`](#10-exception-e-websocket)
11. [`resources/`: telas, estilos, banco e configuração](#11-resources-telas-estilos-banco-e-configuração)
12. [`test/`: os testes](#12-test-os-testes)
13. [Fluxos de ponta a ponta (com exemplos)](#13-fluxos-de-ponta-a-ponta-com-exemplos)
14. [Perguntas que podem aparecer na apresentação](#14-perguntas-que-podem-aparecer-na-apresentação)

---

## 1. O sistema em uma página

**Bublo** é um sistema web para uma biblioteca acadêmica. Existe **um único usuário do sistema: o administrador (bibliotecário)**. Alunos e professores são *leitores*: dados cadastrados pelo administrador, que **não fazem login**.

Stack: **Java 21 + Spring Boot** (MVC), **Thymeleaf** (HTML renderizado no servidor), **JDBC puro** (`JdbcTemplate`, sem JPA/Hibernate), **PostgreSQL**, **Flyway** (versiona o banco), **Spring Security** (login), **WebSocket** (notificações em tempo real), **Bootstrap** + CSS próprio.

As camadas, de fora para dentro:

```
        NAVEGADOR (HTML + CSS + JS)
              │  requisição HTTP
              ▼
   ┌────────────────────────────┐
   │ Spring Security (filtros)  │  config/SecurityConfig  → "está logado? o CSRF é válido?"
   └────────────────────────────┘
              ▼
   ┌────────────────────────────┐
   │ CONTROLLER                 │  controller/  → recebe a requisição, valida o formato,
   │                            │                 chama o service, escolhe a tela
   └────────────────────────────┘
              ▼        (usa DTOs para trazer/levar dados da tela)
   ┌────────────────────────────┐
   │ SERVICE   @Transactional   │  service/     → REGRAS DE NEGÓCIO, auditoria, notificações
   └────────────────────────────┘
              ▼
   ┌────────────────────────────┐
   │ REPOSITORY                 │  repository/  → SQL (PreparedStatement via JdbcTemplate)
   └────────────────────────────┘
              ▼
        PostgreSQL  (tabelas criadas pelo Flyway: db/migration/)

   Em paralelo: MODEL (objetos do domínio) circula entre service e repository;
                WebSocket empurra notificações ao navegador sem ele pedir.
```

**Regra de ouro** (diga isto na apresentação): cada camada só conversa com a vizinha de baixo.

| Camada | Faz | Não faz |
|---|---|---|
| Controller | recebe HTTP, valida formato, escolhe a tela | regra de negócio, SQL |
| Service | aplica as regras, abre a transação, orquestra repositórios | conhece HTML/HTTP |
| Repository | SQL e conversão de linha → objeto | decide regra de negócio |
| Model | representa o domínio (Leitor, Exemplar, Reserva…) | acessa banco |
| DTO | transporta dados entre tela e service | tem lógica |

---

## 2. O caminho de uma requisição

Exemplo curto: o administrador abre **Reservas** (`GET /reservas`).

1. **Navegador** pede `GET /reservas`.
2. **Spring Security** confere a sessão (cookie `BUBLO_SESSION`). Sem login, redireciona para `/login`.
3. **`ReservaController.listar`** lê os filtros da URL (`termo`, `aba`, `pagina`), decide o filtro (`AGUARDANDO` ou `HISTORICO`) e chama `reservaService.listar(...)`.
4. **`ReservaService.listar`** pede ao `ReservaRepository.buscar(...)` e converte cada `Reserva` em `ReservaDTO` (nesse ponto calcula se está "pronta para retirada").
5. **`ReservaRepository.buscar`** monta o SQL com os filtros, executa no PostgreSQL e devolve a página.
6. O controller coloca o resultado no `Model` (`model.addAttribute("resultado", ...)`) e devolve o nome da tela: `"reservas/listar"`.
7. **Thymeleaf** renderiza `templates/reservas/listar.html` com esses dados, gerando o HTML.
8. O navegador recebe o HTML; `bublo.js` e `design-system.css` cuidam do comportamento e do visual.

No **POST** (ex.: criar reserva) muda o final: depois de gravar, o controller faz `redirect:/reservas` (padrão *Post/Redirect/Get*, para o F5 não repetir a operação) e deixa uma mensagem de sucesso/erro em `flash`, que o `bublo.js` mostra como *toast*.

---

## 3. Raiz do projeto

| Arquivo / pasta | O que faz |
|---|---|
| `pom.xml` | Dependências e build (Maven): Spring Boot (web, thymeleaf, security, websocket, jdbc, validation), Flyway, driver PostgreSQL, **Apache POI** (exporta XLSX), **OpenCSV** (importa CSV). |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven Wrapper: roda o Maven sem precisar instalá-lo. |
| `Dockerfile` | Imagem da aplicação, build em dois estágios (compila com Maven, roda só o `.jar`). |
| `docker-compose.yml` | Sobe **só o PostgreSQL** (para desenvolver rodando a app na sua máquina). |
| `docker-compose.app.yml` | Sobe **PostgreSQL + aplicação**. |
| `.env.example` | Modelo das variáveis de ambiente (`DB_URL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `DEMO_DATA_ENABLED`…). |
| `.github/workflows/ci.yml` | Integração contínua: a cada push/PR, sobe um PostgreSQL e roda os testes. |
| `scripts/smoke-test.sh` | Teste rápido de fumaça: confere se a aplicação subida responde. |
| `docs/` | Toda a documentação (manual, UML, DER, decisões, deploy no Render…). |
| `README.md`, `HELP.md` | Apresentação do projeto e ajuda gerada pelo Spring Initializr. |
| `src/` | O código (explicado abaixo). |
| `target/` | Saída da compilação (gerada, não é código-fonte). |

Estrutura de `src/`:

```
src/
├─ main/
│  ├─ java/com/gabriel/bublo/
│  │   ├─ BubloApplication.java   ← ponto de entrada
│  │   ├─ config/                 ← configurações do Spring
│  │   ├─ security/               ← apoio à autenticação
│  │   ├─ controller/             ← 14 controllers + 1 advice
│  │   ├─ dto/                    ← objetos de transferência
│  │   ├─ service/                ← 8 serviços (regras)
│  │   ├─ repository/             ← SQL
│  │   ├─ model/                  ← domínio
│  │   ├─ exception/              ← tratamento global de erros
│  │   └─ websocket/              ← notificações em tempo real
│  └─ resources/
│      ├─ application.yaml        ← configuração
│      ├─ db/migration/           ← SQL do Flyway (V1…V4)
│      ├─ templates/              ← telas Thymeleaf
│      ├─ static/                 ← CSS, JS, bibliotecas
│      └─ demo/capas/             ← imagens da carga de demonstração
└─ test/                          ← testes unitários
```

**`BubloApplication.java`** — o `main`. A anotação `@SpringBootApplication` faz o Spring varrer os pacotes, criar os objetos (controllers, services, repositories) e ligar um ao outro. É daqui que tudo começa.

---

## 4. Camada `config/` e `security/`: ligar tudo e proteger

Pacotes de **infraestrutura**: não têm regra de biblioteca, só configuram *como* o sistema funciona.

### `config/`

| Arquivo | O que faz | Exemplo |
|---|---|---|
| `SecurityConfig` | Define **quem entra**: tudo exige login, exceto `/login` e arquivos estáticos (`/css`, `/js`, `/lib`). Configura o formulário de login, logout, **sessão única** (`maximumSessions(1)`) e senhas com **BCrypt**. Registra no log de auditoria cada login (sucesso e falha). | Tentar abrir `/emprestimos` sem login → redireciona para `/login`. Errar a senha → grava `LOGIN_FALHA` na auditoria. |
| `WebSocketConfig` | Registra o endereço `/ws/notificacoes`, onde o navegador abre a conexão de tempo real. | O sino da barra superior se conecta aqui. |
| `ReservaScheduler` | Tarefa agendada: **expira reservas vencidas** na partida do sistema e a cada hora (`@Scheduled(cron = "0 5 * * * *")`). | Reserva com prazo 20/11 vira `EXPIRADA` no dia 21. |
| `DemoDataConfig` | Na subida, **cria o administrador** (a partir de `ADMIN_USERNAME/ADMIN_PASSWORD`; sem elas, gera uma senha aleatória e mostra uma vez no log) e, se `DEMO_DATA_ENABLED=true`, carrega ~30 materiais, leitores, empréstimos, multas e reservas de exemplo. É **idempotente**: se já existe dado, não recria. | Primeira subida no Render: já abre com acervo para demonstrar. |

### `security/`

| Arquivo | O que faz |
|---|---|
| `AuthUtil` | Atalhos estáticos. O mais usado: `AuthUtil.usuarioAtual()`, que devolve o nome do admin logado — passado para os services registrarem **quem** fez cada ação na auditoria. |
| `AuthAuditService` | Grava na auditoria os eventos de segurança: `LOGIN`, `LOGIN_FALHA`, `ALTERACAO_SENHA`. |

---

## 5. Camada `controller/`: a porta de entrada

**Papel:** mapear URLs (`@GetMapping`, `@PostMapping`) para métodos Java. Lê parâmetros, valida o formato, chama **um service** e escolhe a **tela** (string com o nome do template) ou um **redirect**. Não tem regra de negócio.

Padrão que se repete em quase todos:

```java
@Controller
@RequestMapping("/reservas")
public class ReservaController {

    @PostMapping                                   // POST /reservas
    public String criar(@RequestParam Long leitorId, @RequestParam Long itemId, ...,
                        RedirectAttributes redirect) {
        try {
            var reserva = reservaService.criar(leitorId, itemId, prazo, AuthUtil.usuarioAtual());
            redirect.addFlashAttribute("sucesso", "Reserva criada. Posição na fila: " + reserva.getPosicao() + ".");
            return "redirect:/reservas";           // deu certo → volta para a lista
        } catch (IllegalStateException e) {        // o service recusou por regra de negócio
            model.addAttribute("erroForm", e.getMessage());
            return "reservas/novo";                // reabre o formulário com o motivo
        }
    }
}
```

> **Ideia-chave:** quando o service quebra uma regra, ele lança `IllegalStateException("mensagem em português")`. O controller captura e mostra a mensagem. Por isso as mensagens de erro vistas na tela nascem nos services.

| Controller | Rotas principais | Função |
|---|---|---|
| `AuthController` | `GET /login`, `GET /` (→ dashboard), `GET /conta`, `POST /troca-senha` | Tela de login (com mensagens "senha incorreta", "sessão encerrada") e troca de senha do admin. |
| `DashboardController` | `GET /dashboard` | Monta os 9 indicadores (títulos, exemplares, disponíveis, empréstimos ativos, atrasados, multas pendentes, reservas, leitores…), os gráficos (empréstimos por mês, acervo por tipo) e as atividades recentes (vindas da auditoria). |
| `AcervoController` | `/acervo`, `/acervo/novo`, `/acervo/{id}`, `/acervo/{id}/editar`; `POST` salvar/excluir; `/acervo/{id}/exemplar` (adiciona cópia); `/acervo/{id}/capa` (imagem); `/acervo/sugestoes-autores` (JSON p/ autocompletar) | Catálogo: livros, revistas, DVDs, publicações digitais, e seus **exemplares**. |
| `LeitorController` | `/leitores`, `/leitores/novo`, `/{id}`, `/{id}/editar`; `POST` salvar, `/{id}/inativar`, `/importar` (CSV); `/modelo-csv` | Cadastro de alunos e professores; detalhes com histórico de empréstimos, reservas e multas. |
| `EmprestimoController` | `/emprestimos`, `/emprestimos/novo`, `/emprestimos/restricao/{leitorId}` (JSON); `POST` criar, `/{id}/renovar`, `/{id}/cancelar` | Registro e acompanhamento de empréstimos. O endpoint `restricao` alimenta o formulário (mostra se o leitor está bloqueado ou no limite). |
| `DevolucaoController` | `GET /devolucoes`; `POST /devolucoes/{id}/devolver` | Tela do balcão para devolver; informa se gerou multa por atraso. |
| `ReservaController` | `/reservas`, `/reservas/novo`; `POST` criar, `/{id}/cancelar` | Fila de reservas. |
| `MultaController` | `/multas`, `/multas/{id}/pagar`, `/multas/leitor/{id}/saldo` (JSON); `POST` pagar, estornar | Multas, pagamento (total ou parcial) e estorno com justificativa. |
| `NotificacaoController` | `GET /notificacoes/resumo` (JSON); `POST /notificacoes/{id}/ler`, `/notificacoes/ler-todas` (JSON) | Alimenta o **sino**: lista as não lidas e marca como lidas. Não há página própria. |
| `ConfiguracaoController` | `GET /configuracoes`; `POST /salvar`, `/reverter` | Edita limites, prazos, taxa de multa e prazo de retirada de reserva; permite reverter a última alteração. |
| `AuditoriaController` | `GET /auditoria` | Log cronológico, somente leitura, com filtros. |
| `RelatorioController` | `GET /relatorios` | Página com os relatórios. |
| `RelatorioExportController` | `/relatorios/*.csv`, `/relatorios/*.xlsx` | Baixa os relatórios em CSV/XLSX (usa `RelatorioService`). |
| `ErroController` | `/error` | Página de erro para o que acontece fora dos controllers (404 de rota, 403). |
| `GlobalModelAdvice` | (não tem rota) | `@ControllerAdvice`: coloca em **todas** as telas o usuário logado, o nome "Bublo" e a contagem de notificações não lidas (o número do sino). |

---

## 6. Camada `dto/`: os dados que viajam

**DTO** = *Data Transfer Object*. É um objeto simples (campos + getters/setters) criado **para a tela**, não para o banco. Motivos: a tela trabalha com textos de formulário e validações (`@NotBlank`, `@Email`), e o domínio (`Leitor`, `Aluno`…) não deve ser exposto direto.

Exemplo: o formulário de leitor usa `LeitorDTO` (tudo `String`, com validação). O `LeitorService` converte `LeitorDTO` ⇄ `Leitor/Aluno/Professor`.

| Arquivo | Usado para |
|---|---|
| `LeitorDTO` | Formulário/lista de leitores (com validações). |
| `LeitorResumoDTO` | Situação do leitor no empréstimo: ativos, limite, saldo de multa, **bloqueado?** |
| `ItemAcervoDTO` | Formulário/lista de materiais (campos de livro, revista, DVD, digital). |
| `CategoriaDTO` | Cadastro de categoria. |
| `EmprestimoDTO` | Empréstimo "cru" retornado ao criar. |
| `EmprestimoResumoDTO` | Linha da lista de empréstimos (leitor, título, tombamento, **atrasado?**). |
| `ExemplarOpcaoDTO` | Opção de exemplar disponível no `<select>` de novo empréstimo (`BA-000016 — Título`). |
| `ReservaDTO` | Linha da lista de reservas, incluindo **`prontaParaRetirada`**. |
| `MultaDTO` | Linha de multa (valor, pago, saldo, dias de atraso). |
| `NotificacaoDTO` | Notificação exibida/serializada para o sino. |
| `ConfiguracaoDTO` | Linha da tela de configurações. |
| `IndicadorDTO` | Cartão numérico do dashboard (título, valor, legenda, cor). |
| `LoginDTO`, `TrocaSenhaDTO` | Login e troca de senha (nova senha + confirmação, mínimo 4 e máximo 72 — limite do BCrypt). |
| `ImportacaoResultadoDTO`, `LinhaErroDTO` | Resultado da importação CSV: quantos entraram e **quais linhas foram rejeitadas e por quê**. |
| `Paginacao<T>` | Resultado paginado genérico (`conteudo`, `total`, `pagina`, `tamanho`). Todas as listas usam. |

---

## 7. Camada `service/`: as regras de negócio

**É a camada mais importante para explicar.** Aqui moram as regras da biblioteca. Cada método de escrita é `@Transactional`: **ou tudo acontece, ou nada** (se uma regra falha no meio, o banco volta ao estado anterior).

Padrão repetido nos serviços de escrita:

1. validar entradas e regras (lançar `IllegalStateException` com a mensagem);
2. alterar via repositórios;
3. gerar **notificação** quando faz sentido;
4. registrar na **auditoria** (quem fez o quê).

| Serviço | Responsabilidade | Regras principais |
|---|---|---|
| `LeitorService` | Cadastro, edição, inativação, **importação CSV** | E-mail único e válido; CPF e telefone normalizados; **tipo não muda** depois do cadastro; não inativa leitor com empréstimo ativo; ao inativar, cancela as reservas aguardando. |
| `AcervoService` | Materiais e exemplares | Cria o material e suas cópias; guarda capa; exclui exemplar só se não tiver histórico (senão **dá baixa**); impede remover o último exemplar com reserva pendente. |
| `EmprestimoService` | Empréstimo, renovação, devolução, cancelamento | Detalhado nos fluxos (seção 13). Limite de exemplares, prazo por tipo de leitor, bloqueio por multa, **fila de reservas tem prioridade**. |
| `ReservaService` | Fila de reservas | Só reserva material **com todos os exemplares emprestados**; não reserva digital; prazo de retirada máximo de 30 dias; calcula **"pronta para retirada"**; expira vencidas. |
| `MultaService` | Multas e pagamentos | Pagamento parcial ou total; valor com no máximo 2 casas e nunca acima do saldo; **estorno exige justificativa** e só ocorre uma vez por pagamento. |
| `NotificacaoService` | Notificações | Listar não lidas, contar, marcar uma/todas como lidas. |
| `ConfiguracaoService` | Parâmetros do sistema | Só 6 chaves editáveis (`limite_alunos`, `limite_professores`, `prazo_alunos`, `prazo_professores`, `taxa_diaria_multa`, `prazo_retirada_reserva`) com valores máximos; **reverte** a última alteração. |
| `RelatorioService` | Relatórios | Gera CSV (com BOM UTF-8 para o Excel abrir acentos e proteção contra "injeção de fórmula") e XLSX (POI). |

**Valores padrão** (alteráveis em Configurações): aluno 3 exemplares / 7 dias; professor 5 exemplares / 14 dias; multa R$ 2,00 por dia de atraso; reserva retira em 3 dias.

---

## 8. Camada `repository/`: o SQL

**Papel:** executar SQL com `JdbcTemplate` (que usa `PreparedStatement`, protegendo contra *SQL injection*) e converter linhas em objetos via `RowMapper`. Não decide nada; só lê e grava.

Exemplo, `ReservaRepository.listarPendentesPorItem`:

```java
jdbc.query(
  "SELECT * FROM reservas WHERE item_acervo_id = ? AND status = 'AGUARDANDO' ORDER BY posicao ASC",
  MAPPER, itemId);
```

Por que **JDBC puro e não JPA?** Controle total do SQL e das travas de concorrência, e o projeto mostra o que acontece "por baixo". (Decisão registrada em `DECISOES_TECNICAS.md`.)

| Arquivo | Tabela(s) | Destaques |
|---|---|---|
| `LeitorRepository` | `leitores` | Gera matrícula (`ALU-…`/`PROF-…`); `travar(id)` faz `SELECT … FOR UPDATE` para serializar empréstimos simultâneos do mesmo leitor. |
| `ItemAcervoRepository` | `item_acervo`, `item_acervo_autor`, `autores`, `categorias` | Persiste o material com suas subclasses em **tabela única**; liga autores (muitos-para-muitos); guarda a capa. |
| `ExemplarRepository` | `exemplares` | **Tombamento** sequencial e imutável (`BA-000001`…), com *advisory lock* para dois cadastros não pegarem o mesmo número; "disponíveis por item". |
| `EmprestimoRepository` | `emprestimos` | `criar` trava o exemplar (`FOR UPDATE`) e o marca indisponível; `devolver` só atualiza se ainda está `ATIVO` (evita devolver duas vezes). |
| `ReservaRepository` | `reservas` | Fila FIFO por material: posição = `MAX(posicao)+1`; ao sair da fila, **renumera** (1, 2, 3…). |
| `MultaRepository` | `multas` | Saldo pendente por leitor; trava a multa para pagamentos simultâneos. |
| `PagamentoRepository` | `pagamentos` | Registra e **marca estorno** (o histórico é preservado, nada é apagado). |
| `NotificacaoRepository` | `notificacoes` | Ao salvar, **depois do commit** envia pelo WebSocket (para não avisar de algo que não gravou). |
| `AuditoriaRepository` | `auditoria` | Só insere e consulta: o log não é editável. |
| `ConfiguracaoRepository` | `configuracoes` | Chave-valor. |
| `UsuarioRepository` | `usuarios` | Busca o admin pelo login. |
| `OrdenacaoSegura` | — | Valida o parâmetro `ordenacao` da URL contra uma lista de colunas permitidas antes de entrar no `ORDER BY` (não dá para parametrizar `ORDER BY`, então se valida). |
| `BaseRepository` | — | Utilitários JDBC genéricos (não usado pelos demais). |

---

## 9. Camada `model/`: o domínio

Classes que representam os conceitos da biblioteca. Mostram **POO**: herança, abstração, polimorfismo, sobrescrita e sobrecarga.

```
Leitor (abstrata)                       ItemAcervo (abstrata)
 ├─ Aluno      (3 exemplares, 7 dias)     ├─ Livro
 └─ Professor  (5 exemplares, 14 dias)    ├─ Revista
                                          ├─ DVD
                                          └─ PublicacaoDigital   (só consulta; não empresta nem reserva)
```

- `Leitor` declara métodos abstratos (`getLimiteExemplaresSimultaneos()`, `getPrazoEmprestimoDias()`, `getChaveConfiguracaoLimite()`, `getChaveConfiguracaoPrazo()`); `Aluno` e `Professor` os **sobrescrevem** (`@Override`) com valores próprios — **polimorfismo**. O `EmprestimoService` chama esses métodos sem saber o tipo do leitor.
- **Sobrescrita × sobrecarga** (ver quadro abaixo): sobrescrever é redefinir na subclasse, com a mesma assinatura (decidido em execução); sobrecarregar é repetir o nome com parâmetros diferentes (decidido em compilação).
- No banco, cada hierarquia vive numa **tabela única** (`leitores` com coluna `tipo`; `item_acervo` com coluna `tipo`); o repositório decide qual subclasse instanciar ao ler.

### Sobrescrita e sobrecarga: onde estão

| Conceito | Onde | Exemplo |
|---|---|---|
| **Sobrescrita** (`@Override`) | `Aluno`, `Professor` | `getLimiteExemplaresSimultaneos()` devolve 3 no aluno e 5 no professor; `getPrazoEmprestimoDias()` devolve 7 e 14. |
| **Sobrescrita** | `Livro`, `Revista`, `DVD`, `PublicacaoDigital` | `getCampoEspecificoLabel()` devolve "ISBN", "ISSN", "Duração", "DOI". |
| **Sobrescrita** (do framework) | `NotificationWebSocketHandler`, `WebSocketConfig` | `afterConnectionEstablished`, `afterConnectionClosed`, `registerWebSocketHandlers`. |
| **Sobrecarga** | `EmprestimoService` | `diasDeAtraso(prevista)` usa hoje; `diasDeAtraso(prevista, hoje)` recebe a data (usada nos testes). |
| **Sobrecarga** | `ReservaService` | `toDTO(Reserva)` chama `toDTO(Reserva, Map)`, que recebe o cache de exemplares livres por título. |

Uso real da sobrescrita (`EmprestimoService.limiteDoLeitor`):

```java
configuracaoService.buscarInteiro(
    leitor.getChaveConfiguracaoLimite(),        // "limite_alunos" ou "limite_professores"
    leitor.getLimiteExemplaresSimultaneos());   // 3 ou 5, se não houver configuração
```

Antes era `leitor instanceof Professor ? ... : ...`; agora um novo tipo de leitor não exige mexer no serviço.

| Arquivo | Representa |
|---|---|
| `Leitor`, `Aluno`, `Professor`, `TipoLeitor` | Quem pega material emprestado. |
| `ItemAcervo`, `Livro`, `Revista`, `DVD`, `PublicacaoDigital`, `TipoMaterial` | O **título** no catálogo ("Dom Casmurro"). |
| `Exemplar` | A **cópia física** de um título, com tombamento único (`BA-000016`). Empréstimos são de **exemplar**; reservas são de **título**. |
| `Autor`, `Categoria` | Autores e categorias do acervo. |
| `Emprestimo` | Um exemplar com um leitor: datas, renovações, status (`ATIVO`, `DEVOLVIDO`, `CANCELADO`; "em atraso" é **calculado** pela data prevista). |
| `Reserva` | Um leitor na fila de um título: posição, prazo de retirada, status (`AGUARDANDO`, `ATENDIDA`, `CANCELADA`, `EXPIRADA`). |
| `Multa`, `Pagamento` | Dívida por atraso e os pagamentos (e estornos) dela. Status da multa: `PENDENTE`, `PAGA_PARCIAL`, `PAGA`, `CANCELADA`. |
| `Notificacao` | Aviso do sistema (categoria, prioridade, lida/não lida). |
| `Auditoria` | Registro de "quem, quando, o quê". |
| `Configuracao` | Parâmetro chave-valor. |
| `Usuario` | O administrador (login + hash BCrypt). |

---

## 10. `exception/` e `websocket/`

### `exception/GlobalExceptionHandler`

`@ControllerAdvice` que captura erros que escapam dos controllers e mostra a página `erro.html` **sem expor detalhes técnicos**: regra de negócio → 400 com a mensagem; página inexistente → 404; método não permitido → 405; arquivo grande (>2 MB) → 413; qualquer outro → 500 com mensagem genérica (o detalhe vai só para o log).

### `websocket/NotificationWebSocketHandler`

Guarda as conexões abertas dos navegadores e, quando chamado (`enviar`), manda uma mensagem JSON a todas. É o que faz o **sino atualizar sozinho**, sem recarregar a página.

---

## 11. `resources/`: telas, estilos, banco e configuração

### `application.yaml`

Configuração: conexão com o banco (variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), pool com até 10 conexões (HikariCP), Flyway ligado, Thymeleaf em UTF-8, upload máximo de 2 MB, cookie de sessão `BUBLO_SESSION` (HttpOnly, SameSite=Lax, 60 min) e as propriedades `bublo.admin.*` e `bublo.demo.data.enabled`.

### `db/migration/` (Flyway)

O banco é **construído por scripts versionados**: ao subir, o Flyway aplica os que ainda não rodaram, em ordem.

| Script | O que faz |
|---|---|
| `V1__schema.sql` | Cria as 14 tabelas (`usuarios`, `configuracoes`, `categorias`, `autores`, `item_acervo`, `item_acervo_autor`, `exemplares`, `leitores`, `emprestimos`, `reservas`, `multas`, `pagamentos`, `notificacoes`, `auditoria`), restrições (`CHECK` de status) e índices. Ex.: o índice único `uix_reserva_ativa_por_leitor_item` impede o mesmo leitor de ter duas reservas ativas do mesmo título. |
| `V2__estorno_e_indices.sql` | Estorno registrado no próprio pagamento + índices extras. |
| `V3__baixa_de_exemplares.sql` | Coluna `baixado` em `exemplares` (exemplar retirado de circulação sem perder o histórico). |
| `V4__capa_do_item.sql` | Colunas para a imagem de capa do material. |

> Regra: **nunca editar um script já aplicado**; mudança de banco = novo `V5__…`.

### `templates/` (Thymeleaf)

HTML com marcações `th:` que recebem os dados do controller. Uma pasta por módulo:

| Pasta / arquivo | Telas |
|---|---|
| `login.html`, `conta.html`, `dashboard.html`, `erro.html` | Entrada, conta do admin, painel, página de erro. |
| `acervo/` | `listar`, `formulario` (cadastro/edição), `detalhes` (com exemplares). |
| `leitores/` | `listar`, `formulario`, `detalhes` (histórico). |
| `emprestimos/` | `listar` (abas Em andamento/Histórico), `novo`. |
| `devolucoes/listar.html` | Balcão de devolução. |
| `reservas/` | `listar` (abas Aguardando/Histórico), `novo`. |
| `multas/` | `listar`, `pagar`. |
| `configuracoes/listar.html`, `auditoria/listar.html`, `relatorios/index.html` | Parâmetros, log e relatórios. |
| `layout/base.html` | Fragmentos `head` (CSS, tema, token CSRF) e `scripts` (JS e dados de *flash*). |
| `layout/sidebar.html`, `layout/topbar.html` | Menu lateral e barra superior (nome, tema claro/escuro, **sino de notificações**, conta, sair). |
| `layout/componentes.html` | Peças reutilizáveis: `capa`, `abas`, `paginacao`. |

Exemplo de uso de fragmento (toda tela começa assim, reaproveitando o layout):

```html
<th:block th:replace="~{layout/base :: head}"></th:block>
<th:block th:replace="~{layout/sidebar :: sidebar('reservas')}"></th:block>
```

Exemplo de dado vindo do controller (selo da reserva pronta):

```html
<span class="badge-bublo badge-disponivel" th:if="${r.prontaParaRetirada}">Pode retirar</span>
```

### `static/`

| Arquivo | Função |
|---|---|
| `css/design-system.css` | Visual do sistema: cores, tema claro/escuro, cartões, tabelas que viram cartões no celular, selos (`badge-*`), dropdown do sino. |
| `js/bublo.js` | Comportamento no navegador, em módulos: `Tema` (claro/escuro, salvo no navegador), `Toasts` (avisos), `Confirmacoes` (modal "Cancelar esta reserva?"), `Notificacoes` (sino + WebSocket), `Utils` (moeda, CSRF), menu mobile e painéis recolhíveis. |
| `lib/` | Bootstrap, Bootstrap Icons e Chart.js **locais** (o sistema não depende de CDN). |

### `demo/capas/`

Imagens de capa usadas só na carga de demonstração; o nome do arquivo é o título sem acento e com hífens (`dom-casmurro.jpg`).

---

## 12. `test/`: os testes

Testes unitários com **JUnit 5 + Mockito**: os repositórios são *mocks*, então testam as **regras dos services** sem banco.

| Arquivo | Cobre |
|---|---|
| `LeitorServiceTest` | Validações de cadastro, CSV, inativação. |
| `AcervoServiceTest` | Material, exemplares, baixa/exclusão. |
| `EmprestimoServiceTest` | Limites, bloqueio por multa, fila de reservas, renovação, devolução com multa. |
| `ReservaServiceTest` | Quando se pode reservar, prazos, e **reserva pronta para retirada**. |
| `MultaServiceTest` | Pagamento parcial/total, estorno. |
| `ConfiguracaoServiceTest` | Limites das configurações e reversão. |
| `OrdenacaoSeguraTest` | Proteção do `ORDER BY`. |
| `BubloApplicationTests` | A aplicação inicia. |
| `test/resources/application.properties` | Desliga banco e Flyway nos testes. |

Rodar: `./mvnw test` (ou `mvn test`).

---

## 13. Fluxos de ponta a ponta (com exemplos)

Cada fluxo mostra **qual arquivo entra em cada etapa**. Use estes para narrar a demonstração.

### Fluxo A — Registrar um empréstimo

**Cenário:** o bibliotecário empresta o exemplar `BA-000016` ("Teste de DVD") para Ana Oliveira.

1. `GET /emprestimos/novo` → `EmprestimoController.novo` lista leitores e exemplares disponíveis (`AcervoService.listarExemplaresDisponiveis`) e abre `emprestimos/novo.html`.
2. Ao escolher a leitora, o JS chama `GET /emprestimos/restricao/{id}` (JSON) e mostra "Sem pendências. Empréstimos ativos: 1 de 3." — ou bloqueia o botão se houver multa pendente.
3. O envio vai a `POST /emprestimos` → `EmprestimoController.criar` → **`EmprestimoService.criar`**, que valida nesta ordem:
   - leitor ativo; data prevista não passa do prazo do tipo (7 dias aluno / 14 professor);
   - **trava o leitor** (`leitorRepository.travar`) para dois empréstimos simultâneos não furarem o limite;
   - leitor **sem multa pendente**; dentro do **limite de exemplares**;
   - exemplar disponível, material ativo e **não digital**;
   - **fila de reservas**: se há reservas à frente, só o primeiro da fila pode retirar.
4. `EmprestimoRepository.criar` trava o exemplar (`FOR UPDATE`), insere em `emprestimos` e marca o exemplar **indisponível**.
5. Se o leitor tinha reserva desse título, ela vira `ATENDIDA`.
6. O service grava **notificação** ("Novo empréstimo registrado") e **auditoria** (`CRIACAO_EMPRESTIMO`).
7. Redireciona para `/emprestimos` com o toast "Empréstimo registrado com sucesso (#3)".

Se qualquer regra falhar, vira `IllegalStateException` e a tela reabre com a mensagem; **nada é gravado** (transação).

### Fluxo B — Devolver (com multa) e a reserva que fica pronta

**Cenário:** o exemplar de "Dom Casmurro" volta com 2 dias de atraso, e Bruno Costa estava na fila.

1. Em `/devolucoes`, o admin clica em devolver → `POST /devolucoes/{id}/devolver` → `DevolucaoController` → **`EmprestimoService.devolver`**.
2. `EmprestimoRepository.devolver` marca `DEVOLVIDO` **somente se ainda está `ATIVO`** (devolver duas vezes não gera multa dupla) e libera o exemplar.
3. Calcula atraso: `dias = hoje − data prevista`. Se > 0, cria a **multa** = `taxa_diaria × dias` (padrão R$ 2,00/dia → R$ 4,00), status `PENDENTE`, e uma notificação de atenção. O leitor passa a ficar **bloqueado para novos empréstimos** enquanto houver saldo.
4. `avisarPrimeiroDaFila` cria a notificação **"Material reservado disponível"** para a 1ª reserva do título.
5. O controller mostra o toast: "Devolução registrada com atraso. Multa gerada: R$ 4,00."
6. Agora, em **Reservas**, `ReservaService.toDTO` compara a posição da reserva com os exemplares livres: reserva na **posição 1** com **1 exemplar livre** ⇒ `prontaParaRetirada = true`. A tela mostra o selo verde **"Pode retirar"** e o botão **"Emprestar"**.
7. "Emprestar" abre `/emprestimos/novo?leitor=ID&item=ID` já com o leitor e **um exemplar livre do título marcados**. Ao confirmar, é o Fluxo A, e a reserva vira `ATENDIDA` automaticamente.

> Regra da fila: com 2 reservas e 1 exemplar devolvido, só a **posição 1** fica pronta; a 2 continua "Aguardando" até outro exemplar voltar. A conversão em empréstimo é **manual** (o bibliotecário confirma), porque envolve limite do leitor, multas e escolha do exemplar.

### Fluxo C — Reservar (fila FIFO)

**Cenário:** todos os exemplares de "Dom Casmurro" estão emprestados; Bruno quer reservar.

1. `POST /reservas` → **`ReservaService.criar`** valida: leitor ativo; material ativo e **não digital**; prazo de retirada entre hoje e 30 dias; leitor **não** já está com um exemplar desse título; o material **tem exemplares**; e **não há exemplar livre sobrando** (`livres > naFila` ⇒ "ainda tem exemplares disponíveis").
2. `ReservaRepository.criar` trava o título (`FOR UPDATE`), impede duplicidade e define a **posição** = próxima da fila.
3. Notificação "Nova reserva" + auditoria `CRIACAO_RESERVA`.
4. A fila se mantém sem buracos: se a posição 1 é cancelada/expira, `reordenarFila` renumera (2 → 1…).
5. Todo início de hora, `ReservaScheduler` expira as reservas cujo prazo passou (`EXPIRADA`), liberando a fila.

**Renovar** também respeita a fila: `EmprestimoService.renovar` recusa se há reserva pendente do título, se já usou a única renovação, ou se está em atraso.

### Fluxo D — Pagar e estornar uma multa

1. `GET /multas/{id}/pagar` abre `multas/pagar.html` com o saldo.
2. `POST /multas/{id}/pagar` → `MultaService.pagar`: trava a multa; exige forma de pagamento; valor > 0, ≤ saldo, no máximo 2 casas. Cria o `Pagamento`; se `pago ≥ valor` → `PAGA` (+ notificação "Multa quitada"), senão `PAGA_PARCIAL`.
3. Quando o saldo do leitor zera, ele **volta a poder pegar emprestado**.
4. Estorno: `POST /multas/{id}/pagamentos/{pid}/estornar` exige **justificativa**, marca o pagamento como estornado (não apaga) e recalcula o status (`PENDENTE`/`PAGA_PARCIAL`). Tudo vai para a auditoria.

### Fluxo E — Notificação em tempo real (o sino)

1. Qualquer service chama `notificacaoRepository.salvar(...)`.
2. O repositório grava e, **só após o commit**, chama `NotificationWebSocketHandler.enviar("notificacao", {...})`.
3. No navegador, `bublo.js` (módulo `Notificacoes`) recebe pelo WebSocket: **soma 1 ao contador** do sino e mostra um toast "Nova notificação".
4. Ao clicar no sino, o JS busca `GET /notificacoes/resumo` e lista as **não lidas**.
5. O ✓ de cada item chama `POST /notificacoes/{id}/ler`; **"Marcar todas como lidas"** chama `POST /notificacoes/ler-todas`. As duas respondem a nova contagem, e o JS remove o item e atualiza o número.
6. O número inicial do sino, ao carregar qualquer tela, vem do `GlobalModelAdvice` (`naoLidas`).

### Fluxo F — Login e segurança

1. Qualquer URL sem sessão → redireciona para `/login`.
2. `POST /login`: o Spring Security busca o usuário (`UsuarioRepository` via `userDetailsService`) e compara a senha com o hash **BCrypt**.
3. Sucesso → grava `LOGIN` na auditoria e vai para `/dashboard`. Falha → grava `LOGIN_FALHA` e volta com "Usuário ou senha incorretos." (sem dizer qual dos dois errou).
4. **CSRF:** todo formulário POST (e os `fetch` do JS) envia o token que o `base.html` coloca nas `<meta>`; sem ele a requisição é recusada.
5. **Sessão única:** entrar em outro navegador derruba a anterior ("sua sessão foi encerrada…").
6. **Troca de senha** (`/conta`) exige a nova senha e a confirmação, e exige novo login.

### Fluxo G — Cadastrar leitor (resumo)

`GET /leitores/novo` → `leitores/formulario.html` → `POST /leitores` → `@Valid LeitorDTO` (formato) → `LeitorService.salvar` (regras: e-mail único, CPF/telefone normalizados) → `LeitorRepository.salvar` (gera matrícula `ALU-…`) → auditoria `CADASTRO_LEITOR` → `redirect:/leitores` + toast. Passo a passo detalhado em `GUIA_DE_ESTUDO_E_APRESENTACAO.md`, seção 11.

---

## 14. Perguntas que podem aparecer na apresentação

**Por que separar em camadas?** Cada camada tem uma responsabilidade; dá para trocar a tela sem mexer nas regras, e testar as regras sem banco nem navegador.

**Onde ficam as regras de negócio?** Só nos `service/`. O controller recebe e responde; o repository só faz SQL.

**O que acontece se dois bibliotecários emprestam o mesmo exemplar ao mesmo tempo?** O `EmprestimoRepository.criar` usa `SELECT … FOR UPDATE` no exemplar: o segundo espera o primeiro terminar e então encontra o exemplar indisponível.

**Como evitam multa duplicada em devolução repetida?** O `UPDATE … WHERE status = 'ATIVO'` só altera uma vez; a segunda tentativa retorna "não está ativo" e nenhuma multa é gerada.

**Por que a reserva não vira empréstimo sozinha?** Empréstimo depende de limite do leitor, multa pendente, prazo por tipo e escolha do exemplar. O sistema **avisa e deixa pronto** ("Pode retirar" + botão "Emprestar"); o bibliotecário confirma.

**Qual a diferença entre sobrescrita e sobrecarga?** Sobrescrita: a subclasse redefine o método herdado (mesma assinatura, `@Override`), e o Java escolhe pelo **tipo real do objeto** em execução; ex.: `Aluno` e `Professor` e o limite de exemplares (3 e 5). Sobrecarga: mesmo nome, parâmetros diferentes, escolhido na **compilação**; ex.: `diasDeAtraso(prevista)` e `diasDeAtraso(prevista, hoje)`.

**Por que JDBC e não JPA/Hibernate?** Controle do SQL e das travas de concorrência, e transparência didática do que vai ao banco.

**Como o banco é versionado?** Flyway: scripts `V1…V4` em `db/migration/`, aplicados em ordem na subida; mudança nova = novo script.

**Como o sistema se protege?** Login obrigatório, senha com BCrypt, CSRF, sessão única, cookies HttpOnly, `PreparedStatement` contra SQL injection, ordenação validada por lista permitida, CSV protegido contra injeção de fórmula, erros sem stack trace na tela.

**Como o tempo real funciona?** WebSocket nativo: o servidor guarda as conexões e envia JSON quando uma notificação é salva (após o commit); o `bublo.js` atualiza o sino.

**Como mostrar que está testado?** `./mvnw test` roda os testes unitários dos serviços (empréstimo, reserva, multa, leitor, acervo, configuração) e o CI do GitHub repete isso a cada PR.
