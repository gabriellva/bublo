# Bublo — Explicação técnica completa (para a apresentação)

> Documento baseado na leitura do código real do repositório `gabriellva/bublo`. Onde o README diz uma coisa e o código faz outra, eu aponto (seção 14).

---

## 0. Resumo em 30 segundos (para abrir a apresentação)

O Bublo é um **sistema web de gerenciamento de biblioteca** feito em **Java 21 + Spring Boot**. O servidor **gera as páginas HTML** (Thymeleaf), o banco é **PostgreSQL** acessado com **JDBC puro** (SQL escrito à mão, sem Hibernate/JPA). Só existe **um tipo de usuário: o administrador**. Ele cadastra livros/revistas/DVDs, leitores (alunos e professores), registra empréstimos, devoluções, reservas e multas, e vê dashboard, relatórios e auditoria.

Fluxo básico de tudo:

```
Navegador → Controller → Service → Repository (SQL) → PostgreSQL
                ↓
           Thymeleaf monta o HTML e devolve ao navegador
```

---

## 1. Tecnologias e o "porquê" de cada uma

| Peça | O que é | Por que está aqui |
|---|---|---|
| **Java 21** | Linguagem | Orientada a objetos (o foco da disciplina) |
| **Spring Boot 3.5** | Framework | Sobe o servidor web, injeta dependências, cuida de segurança e transações |
| **Spring MVC** | Parte do Spring | Recebe as requisições HTTP e chama os métodos dos controllers |
| **Thymeleaf** | Motor de templates | Mistura HTML com dados do Java; o servidor entrega a página já pronta |
| **Spring Security** | Segurança | Login, sessão, CSRF, BCrypt |
| **JDBC puro (JdbcTemplate)** | Acesso ao banco | Você escreve o SQL e o Java executa. É o que o projeto exigiu (sem JPA) |
| **PostgreSQL 16** | Banco de dados | Guarda tudo |
| **Flyway** | Versionamento do banco | Cria/atualiza as tabelas automaticamente por arquivos `V1__...sql`, `V2__...` |
| **Bootstrap 5 + CSS próprio** | Visual | Layout responsivo + `design-system.css` (cores vinho/dourado, tema claro/escuro) |
| **Chart.js** | Gráficos | Gráficos do dashboard |
| **Apache POI** | Planilhas | Gera arquivos `.xlsx` |
| **OpenCSV** | CSV | Lê o CSV de importação de leitores |
| **WebSocket** | Tempo real | Avisa o navegador de novas notificações sem recarregar |
| **JUnit 5 + Mockito** | Testes | Testa as regras de negócio |
| **Docker / Docker Compose** | Infra | Sobe o PostgreSQL (e opcionalmente o app) em contêiner |
| **GitHub Actions** | CI | Roda os testes a cada push |

Bootstrap, Chart.js e ícones são servidos **localmente** (`/lib/...`), sem CDN: funciona sem internet.

---

## 2. Arquitetura em camadas

Pacote raiz: `com.gabriel.bublo`. Cada camada só conversa com a de baixo:

```
controller  →  service  →  repository  →  PostgreSQL
    ↑             ↑
   dto          model
```

| Pacote | Responsabilidade | Exemplo |
|---|---|---|
| `controller` | Recebe a requisição, valida a entrada, chama o service, escolhe qual página mostrar | `AcervoController`, `EmprestimoController` |
| `service` | **Regras de negócio** e transações | `EmprestimoService` (limites, multa, fila de reservas) |
| `repository` | **SQL** com `PreparedStatement`. Só fala com o banco | `ItemAcervoRepository`, `ExemplarRepository` |
| `model` | Classes do domínio com **herança** | `ItemAcervo` → `Livro`, `Revista`, `DVD`, `PublicacaoDigital` |
| `dto` | Objetos para trafegar dados entre tela e service | `ItemAcervoDTO`, `LeitorDTO` |
| `config` | Segurança, WebSocket, agendador, dados de demonstração | `SecurityConfig`, `ReservaScheduler` |
| `exception` | Tratamento global de erros | `GlobalExceptionHandler` |
| `websocket` | Canal de notificações em tempo real | `NotificationWebSocketHandler` |
| `security` | Utilitários de autenticação e auditoria de login | `AuthAuditService` |

**Por que separar assim?** Se a regra "aluno pode pegar 3 livros" mudar, mexe-se só no service. Se o banco mudar, só no repository. A tela não sabe nada de SQL.

**Model vs DTO:** o `model` é a entidade do domínio (ex.: `Livro` com objeto `Categoria` dentro). O `DTO` é uma versão "plana" para o formulário/tela (ex.: `categoriaId` como número, mais as mensagens de validação). Assim a tela não expõe as classes internas.

---

## 3. Como o sistema sobe (inicialização)

1. **Variáveis de ambiente** (`.env` / `application.yaml`): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `DEMO_DATA_ENABLED`. **Não há valor padrão**: se faltar alguma, o app **não sobe**. Isso evita senha escrita no código.
2. **Flyway** roda as migrations (`V1` a `V4`) e cria/atualiza o banco.
3. **`DemoDataConfig`** (um `CommandLineRunner`) roda no startup:
   - cria o administrador **se ele ainda não existir** (a senha do `.env` é guardada com **BCrypt**);
   - se `DEMO_DATA_ENABLED=true`, carrega dados de exemplo (13 materiais, 15 leitores, 2 empréstimos).
4. **`ReservaScheduler`** expira reservas vencidas **na partida** e **a cada hora** (`@Scheduled(cron = "0 5 * * * *")` = minuto 5 de toda hora).
5. Servidor sobe na porta 8080.

---

## 4. Banco de dados (modelo de dados)

Criado inteiramente por Flyway (`src/main/resources/db/migration/`).

### Tabelas principais

| Tabela | O que guarda |
|---|---|
| `usuarios` | Administrador (`username`, `senha_hash`, `ativo`) |
| `categorias` | Categorias planas (sem hierarquia) |
| `autores` | Autores, com `nome_normalizado` único (para não duplicar "Machado de Assis" e "machado de assis") |
| `item_acervo` | O **título** (livro, revista, DVD, digital). Não é a cópia física |
| `item_acervo_autor` | Tabela de ligação **muitos-para-muitos** (livro ↔ autores) |
| `exemplares` | A **cópia física**, com tombamento `BA-000001` |
| `leitores` | Alunos e professores |
| `emprestimos` | Empréstimo de **um exemplar** para um leitor |
| `reservas` | Fila de espera por um **título** |
| `multas` / `pagamentos` | Multa por atraso e os pagamentos dela |
| `notificacoes` | Avisos do sino |
| `auditoria` | Log de quem fez o quê |
| `configuracoes` | Pares chave/valor (prazos, limites, taxa) |

### Conceito importante: Título × Exemplar

- `item_acervo` = "Dom Casmurro" (o título, uma linha só).
- `exemplares` = as cópias físicas desse livro (BA-000001, BA-000002...).
- O **empréstimo é do exemplar**, não do título. A **reserva é do título**.

### Como a herança foi guardada no banco

Usa-se **tabela única** (`item_acervo`) com a coluna `tipo` (`LIVRO`, `REVISTA`, `DVD`, `DIGITAL`) e colunas específicas (`isbn`, `issn`, `duracao_minutos`, `doi`, `url_externa`) que ficam vazias quando não se aplicam. É simples para JDBC. Na hora de ler, o `ITEM_MAPPER` olha o `tipo` e cria a classe certa (`new Livro()`, `new DVD()` ...).

### Proteções no próprio banco

- `UNIQUE` em e-mail, matrícula, tombamento, username.
- `CHECK` nos status (ex.: empréstimo só pode ser `ATIVO`, `DEVOLVIDO`, `ATRASADO`, `CANCELADO`).
- Índice único parcial `uix_reserva_ativa_por_leitor_item`: **um leitor não pode ter duas reservas aguardando do mesmo título**.
- ISBN/ISSN único **só entre materiais ativos** (V2), assim um material excluído não bloqueia recadastro.

### Evolução (migrations)

- **V1** schema inicial.
- **V2** estorno de pagamento registrado no próprio pagamento + índices ISBN/ISSN parciais.
- **V3** "baixa" de exemplar (explicado na seção 8).
- **V4** capa do livro guardada no banco (seção 9).

---

## 5. POO no código (a parte que o professor mais gosta)

### Herança + abstração
- `ItemAcervo` é **abstrata** → `Livro`, `Revista`, `DVD`, `PublicacaoDigital`.
- `Leitor` é **abstrata** → `Aluno`, `Professor`.

### Polimorfismo e sobrescrita (`@Override`)
- `ItemAcervo` declara métodos abstratos `getCapaGenerica()`, `getCampoEspecificoLabel()`, `getCampoEspecificoValor()`. O `Livro` responde `"capa-livro.svg"`, `"ISBN"` e o ISBN; o `DVD` responde coisas diferentes. A tela chama o mesmo método e cada tipo responde do seu jeito.
- `Aluno` devolve limite **3** e prazo **7 dias**; `Professor` devolve **5** e **14**; cada um informa também a chave de configuração (`limite_alunos`, `prazo_professores`...).
- **Ponto-chave:** o `EmprestimoService` usa `leitor.getLimiteExemplaresSimultaneos()` **sem `instanceof`**. Ele não precisa saber se é aluno ou professor.

### Sobrecarga (overload)
`EmprestimoService.diasDeAtraso(prevista)` e `diasDeAtraso(prevista, hoje)`: mesmo nome, parâmetros diferentes (a primeira só chama a segunda com a data de hoje). Também `ReservaService.toDTO(reserva)` / `toDTO(reserva, cache)`.

### Encapsulamento
Campos `private` com getters/setters. DTOs impedem a tela de mexer direto nas entidades.

### Interfaces
Uso de `RowMapper` (transforma linha do banco em objeto), `WebSocketHandler` e `Repository` do Spring.

### Relacionamentos
Muitos-para-muitos (título ↔ autores), um-para-muitos (exemplar → empréstimos).

### Herança em ação na criação de leitor
No `LeitorService.salvar`: `new Professor()` ou `new Aluno()` conforme o tipo escolhido. Depois o resto do código trata ambos como `Leitor`.

---

## 6. Cadastro (como um dado vai da tela até o banco)

Exemplo: **cadastrar um leitor**.

1. **Tela** (`leitores/formulario.html`): formulário Thymeleaf com `th:object="${leitor}"` e `th:field="*{nome}"`. Isso liga cada campo ao `LeitorDTO`. O formulário leva um **token CSRF** automaticamente.
2. **Controller** recebe o `POST /leitores` com `@Valid @ModelAttribute LeitorDTO`. O `@Valid` aplica as regras do DTO (`@NotBlank`, `@Email`, `@Size`...). Se houver erro, volta ao formulário mostrando as mensagens em vermelho (`th:errors`).
3. **Service** (`LeitorService.salvar`), dentro de uma transação:
   - limpa e valida de novo (nome, e-mail com regex, telefone, CPF);
   - **CPF é opcional**, mas se vier precisa ser válido (cálculo dos dígitos verificadores em `cpfValido`) e único; é gravado como `000.000.000-00`;
   - e-mail duplicado é rejeitado;
   - cria `Aluno` ou `Professor`;
   - **o tipo não pode mudar depois** (a matrícula e os limites dependem dele).
4. **Repository** gera a **matrícula automática**: pega o maior número já usado com o prefixo (`ALU` ou `PROF`), soma 1 e formata (`ALU-00001`, `PROF-00001`). Depois faz o `INSERT` com `PreparedStatement`.
5. O service **grava na auditoria** (`CADASTRO_LEITOR`) e o controller redireciona com mensagem de sucesso (flash message, mostrada como toast).

### Importação por CSV
`LeitorService.importarCSV` (OpenCSV): detecta separador `,` ou `;`, ignora BOM do Excel, pula o cabeçalho e **processa linha a linha**, reaproveitando o mesmo `salvar()` (portanto as mesmas regras). Linhas ruins não derrubam as boas: cada erro vira um item `LinhaErroDTO(numeroLinha, mensagem)` mostrado no final ("X importados, Y rejeitados").

### Cadastro de material (acervo)
- Formulário único para os 4 tipos; os campos específicos (ISBN, ISSN, duração, DOI) aparecem conforme o tipo.
- **Autores:** digitados no campo; existe autocomplete (`GET /acervo/sugestoes-autores`, devolve JSON). O sistema **reutiliza** autor já existente pelo `nome_normalizado` (`criarOuReutilizarAutor`) e sincroniza a tabela de ligação (apaga e reinsere).
- **Exemplares:** cadastrados em lote (1 a 100) na página do material; cada um recebe tombamento automático.

### Tombamento (BA-000001...)
`ExemplarRepository.proxTombamento` calcula `MAX(número)+1`. Para duas requisições simultâneas **não gerarem o mesmo número**, usa `pg_advisory_xact_lock(7001)`: um **lock do PostgreSQL** que vale até o fim da transação. Número nunca é reaproveitado.

---

## 7. Resgate de informações (buscas, filtros, paginação)

Exemplo: **tela do acervo** (`GET /acervo?termo=...&categoria=...&tipo=...&disponivel=...&pagina=0&ordenacao=ia.titulo ASC`).

1. O controller recebe os parâmetros e **limita**: página mínima 0, tamanho entre 1 e 100.
2. O service chama o repository, que monta o SQL **dinamicamente**, adicionando só os filtros preenchidos (com `ILIKE` para busca sem diferenciar maiúscula/minúscula) e usando **`?`** para os valores.
3. Retorna um objeto `Paginacao<T>` (conteúdo da página + total + página atual), e a tela desenha a lista e os botões de página (fragmento `componentes :: paginacao`).
4. **Segurança da ordenação:** o nome da coluna do `ORDER BY` **não pode** ser parâmetro `?`. Então a classe `OrdenacaoSegura` só aceita colunas de uma **lista permitida** e direção `ASC`/`DESC`; qualquer outra coisa cai na ordenação padrão. Isso bloqueia SQL Injection pelo parâmetro `ordenacao`.
5. Os autores são carregados separadamente por título (`carregarAutores`).

Outras consultas relevantes:
- **Dashboard:** 9 indicadores (títulos, exemplares, disponíveis, indisponíveis, empréstimos ativos, atrasados, multas pendentes, reservas, leitores) e 2 gráficos. O controller joga as listas no `Model`; a página converte para JavaScript com `th:inline="javascript"` e o **Chart.js** desenha (linha: empréstimos dos últimos 6 meses; rosca: materiais por tipo).
- **Histórico do leitor:** empréstimos, multas e reservas (limitados a 100).

---

## 8. Regras de negócio principais (o coração do sistema)

### Empréstimo (`EmprestimoService.criar`)
Tudo numa **transação** (`@Transactional`): se qualquer coisa falhar, nada é gravado. Validações, em ordem:

1. Pelo menos um exemplar escolhido.
2. Leitor existe e está **ativo**.
3. Data prevista não pode ser anterior a hoje, nem passar do **prazo do tipo de leitor** (aluno 7 dias, professor 14, configurável).
4. **Trava o leitor** (`SELECT ... FOR UPDATE`) para dois empréstimos simultâneos não ultrapassarem o limite.
5. Leitor **com multa pendente é bloqueado**.
6. **Limite de exemplares simultâneos** (aluno 3, professor 5).
7. Para cada exemplar: precisa estar disponível, o material não pode estar excluído, e **publicação digital não pode ser emprestada**.
8. **Fila de reservas:** quem está na fila na frente do leitor tem prioridade; se não sobrar exemplar livre além da fila, bloqueia. O leitor que tem a reserva "usa" a dele (marcada como `ATENDIDA`).
9. Grava o empréstimo, uma notificação e um registro de auditoria.

### Devolução (`devolver`)
- A baixa é **condicional ao status `ATIVO`** (`UPDATE ... WHERE status='ATIVO'`): se duas pessoas clicarem juntas, a segunda não gera multa duplicada.
- Calcula `diasDeAtraso`; se > 0, **multa = taxa diária × dias** (taxa da tabela `configuracoes`, padrão R$ 2,00). Cria a multa, a notificação e a auditoria.
- Avisa que o material voltou para o primeiro da fila de reservas.

### Renovação
Só se estiver ativo, **no máximo 1 renovação**, sem atraso e **sem reserva pendente** para o título. Soma o prazo do tipo de leitor à data prevista.

### Reserva (`ReservaService.criar`)
- Só faz sentido **quando todos os exemplares estão emprestados** (se há livre, o sistema manda emprestar).
- Não pode reservar digital, nem material sem exemplares, nem quem já está com um exemplar do mesmo título.
- **Fila FIFO** por título, com `posicao` e prazo de retirada (máx. definido em constante).
- **Expiração automática** pelo `ReservaScheduler` (a cada hora).

### Multas e pagamentos (`MultaService`)
- Pagamento **integral ou parcial**. Status: `PENDENTE` → `PAGA_PARCIAL` → `PAGA`.
- A multa é **travada** (`FOR UPDATE`) durante o pagamento, então dois pagamentos simultâneos não passam do valor devido.
- Valor com no máximo 2 casas decimais, nunca acima do saldo (`BigDecimal`, nunca `double`, por causa de dinheiro).
- **Estorno:** exige **justificativa**, marca o pagamento como estornado (não apaga: o histórico fica) e recalcula a multa. Só um estorno por pagamento.

### Exclusões seguras
- **Material:** não exclui com empréstimo ativo ou reserva pendente. É **exclusão lógica** (`ativo = false`), não apaga a linha.
- **Exemplar:** se nunca foi emprestado, é apagado; se tem histórico, recebe **"baixa"** (`baixado = true`) para não quebrar a chave estrangeira e preservar o histórico. O tombamento nunca é reutilizado.
- **Leitor:** só pode ser inativado se não tiver empréstimo ativo; ao inativar, as reservas dele são canceladas.

### Configurações
Limites e prazos de aluno/professor, taxa diária e prazo de retirada ficam na tabela `configuracoes`. Alterar exige **justificativa**, é auditado e pode ser revertido.

---

## 9. Imagens (capas dos materiais)

**Onde ficam:** no **próprio banco**, na coluna `capa` do tipo `BYTEA` (bytes da imagem) na tabela `item_acervo` (+ `capa_tipo` e `capa_atualizada_em`). Vantagem: vai junto no backup e não depende de pasta de arquivos no contêiner.

**Upload (`AcervoService.salvar`):**
1. O formulário é `multipart`; o arquivo chega como `MultipartFile`.
2. Limite de **2 MB** (configurado no `application.yaml` e conferido no service).
3. **O tipo é descoberto pelos primeiros bytes do arquivo** (`tipoDaImagem`), e não pela extensão ou pelo `Content-Type`, porque o usuário pode forjar esses dois. Só aceita **JPG, PNG, WebP** (assinaturas: `FF D8 FF`, `89 PNG...`, `RIFF....WEBP`). **SVG foi barrado de propósito**: ele pode conter script (risco de XSS).
4. Salva com `UPDATE item_acervo SET capa = ?, ...`. Existe opção de remover a capa.

**Exibição:**
- A tela usa `<img src="/acervo/{id}/capa?v={versão}">`.
- O endpoint `GET /acervo/{id}/capa` lê os bytes do banco e responde com o `Content-Type` correto, cache de 30 dias (`cachePrivate`) e `X-Content-Type-Options: nosniff`.
- O parâmetro `v` (versão = data de atualização em milissegundos) faz o navegador **buscar de novo** quando a capa muda, apesar do cache longo.
- **Sem foto:** o fragmento `capa(item)` desenha uma **capa gerada** com título e autor, com cor por tipo (CSS).
- Se não há capa, o endpoint responde 404 e a tela já usa a capa gerada.

---

## 10. Conexão front ↔ back

O Bublo **não é uma SPA** (não é React/Vue com API JSON). É **renderização no servidor**:

| Como a tela fala com o servidor | Onde |
|---|---|
| **Formulários HTML** (`POST` + redirect) | Quase tudo: cadastros, empréstimos, devoluções, pagamentos |
| **Links `GET` com parâmetros** | Busca, filtros, paginação, ordenação |
| **`fetch` (JSON) em `bublo.js`** | Só o sino de notificações: `GET /notificacoes/resumo`, `POST /notificacoes/{id}/ler`, `POST /notificacoes/ler-todas` e o autocomplete de autores |
| **WebSocket** | Aviso em tempo real de nova notificação |

### Padrão Post/Redirect/Get
Depois de um `POST` bem-sucedido, o controller **redireciona** (`redirect:/acervo`) e manda uma mensagem por `RedirectAttributes.addFlashAttribute("sucesso", ...)`. Isso evita reenviar o formulário ao apertar F5. A mensagem vira um **toast** (um `<div id="flash-data">` oculto é lido pelo `bublo.js`).

### Templates (Thymeleaf)
- `layout/base.html` (head e scripts comuns), `sidebar`, `topbar`, `componentes` (paginação, capa) são **fragmentos reutilizados** via `th:replace`.
- Expressões: `th:text`, `th:each` (loops), `th:if`/`th:unless`, `th:href="@{/acervo/{id}(id=${x})}"`.
- Nenhum template tem "regra de negócio": só mostra o que o controller colocou no `Model`.

### `bublo.js` (≈470 linhas, JavaScript puro)
Organizado em módulos: **Tema** (claro/escuro salvo no `localStorage`, aplicado antes da página aparecer para não "piscar"), **Toasts**, **Confirmações** (modal), **Notificações** (dropdown + WebSocket), menu mobile, painéis recolhíveis, bloqueio de duplo clique em envios.

### CSRF no `fetch`
O `base.html` coloca o token em `<meta name="_csrf">`. O JS lê e manda no cabeçalho em todo `POST` feito por `fetch`. Sem isso o Spring Security recusaria (403).

### Tratamento de erros
- Erros de regra de negócio são lançados como `IllegalStateException("mensagem em português")` no service. O controller captura e mostra a mensagem na tela.
- Os que escapam são pegos pelo `GlobalExceptionHandler`, que mostra uma página de erro **sem stack trace**; erros inesperados só vão para o log.

---

## 11. Tempo real (WebSocket)

Fluxo completo de uma notificação:

1. Qualquer service chama `notificacaoRepository.salvar(...)` (ex.: "Multa gerada por atraso").
2. O repository faz o `INSERT` na tabela `notificacoes` e agenda `publicarAposCommit`.
3. **Só depois que a transação confirma (`afterCommit`)** o `NotificationWebSocketHandler.enviar("notificacao", ...)` manda um JSON para **todos** os navegadores conectados. Isso evita avisar de algo que depois sofreu rollback.
4. No navegador, `bublo.js` abriu `new WebSocket("ws://host/ws/notificacoes")`. Ao receber, soma 1 no contador do sino e mostra um toast. Se a conexão cair, reconecta com espera crescente (até 5 tentativas).

O handler guarda as conexões num `ConcurrentHashMap.newKeySet()` (conjunto seguro para várias threads).

---

## 12. Segurança

- **Login** (`SecurityConfig`): formulário em `/login`; todas as rotas exigem autenticação, exceto `/login`, `/css`, `/js`, `/lib`, `/images`, `/error`.
- **Senhas com BCrypt** (hash com salt; não é reversível). Troca de senha em `/conta`, que depois **desloga** o usuário.
- **CSRF** ligado em todos os formulários (token escondido gerado pelo Thymeleaf).
- **Sessão por cookie** `BUBLO_SESSION`: `http-only` (JavaScript não lê), `SameSite=LAX`, expira em 60 min, **máximo 1 sessão por usuário** (logar em outro lugar derruba a anterior, com aviso).
- **SQL Injection:** todos os valores usam `PreparedStatement` (`?`); ordenação protegida por lista permitida (`OrdenacaoSegura`).
- **Validação no servidor** (Jakarta Bean Validation + validações no service, valendo também para o CSV).
- **Upload seguro** (seção 9).
- **Erros genéricos:** login errado diz "Usuário ou senha incorretos", sem revelar qual campo errou.
- **Auditoria de login:** sucessos e falhas vão para a tabela `auditoria`.
- **Concorrência:** `FOR UPDATE` (leitor, multa), `UPDATE ... WHERE status='ATIVO'` (devolução), advisory lock (tombamento), índice único parcial (reserva).
- **LGPD:** CPF é opcional (minimização de dados).

### Auditoria
Toda operação relevante chama `auditoriaRepository.registrar(usuario, tipoOperacao, módulo, registro, antes, depois, justificativa)`. A tela `/auditoria` é **somente leitura**. É assim que se responde "quem fez o quê e quando".

---

## 13. Relatórios, testes e implantação

### Relatórios
`RelatorioExportController` + `RelatorioService`. Exportação em **CSV** (escrito direto na resposta HTTP em UTF-8) e **XLSX** (Apache POI: `XSSFWorkbook`, linhas e células montadas em memória e devolvidas como bytes com `Content-Disposition: attachment`). Há exportação de empréstimos, multas, leitores, reservas e materiais.

### Testes
- ~100 métodos `@Test` em `src/test`: serviços (`EmprestimoServiceTest`, `MultaServiceTest`, `ReservaServiceTest`...), `PolimorfismoTest`, `OrdenacaoSeguraTest`.
- Usam **Mockito** para simular os repositórios (testam a regra sem banco).
- Há também teste de integração com PostgreSQL real (no CI).
- **CI** (`.github/workflows/ci.yml`): sobe um PostgreSQL 16 no GitHub Actions e roda `mvn` a cada push.

### Docker
- `docker-compose.yml` sobe só o PostgreSQL.
- `docker-compose.app.yml` + `Dockerfile` sobem **app + banco** em contêineres. O Dockerfile tem 2 estágios: compila com Maven e depois copia só o `.jar` para uma imagem enxuta só com o JRE.
- Há guia de deploy no Render (`docs/DEPLOY_RENDER.md`).

---

## 14. Divergências que encontrei entre README e código (importante)

Para você não ser pego de surpresa numa pergunta:

1. **SockJS:** o README diz "WebSocket com SockJS fallback". No código **não há SockJS em lugar nenhum** (nem no `WebSocketConfig`, nem no JS, nem no `pom.xml`). É **WebSocket nativo** com reconexão automática. Se perguntarem, responda isso.
2. **Número da matrícula** é gerado com `MAX+1` **sem lock** (diferente do tombamento, que usa advisory lock). Duas criações exatamente simultâneas poderiam colidir; o `UNIQUE` do banco impediria a duplicata, mas uma das duas daria erro. Pouco provável numa demo, mas é uma limitação real.
3. **Relatórios:** o README fala em "9 relatórios", mas a própria seção de limitações admite que dois são "placeholders". O que existe de exportação real: empréstimos (CSV+XLSX), multas (CSV+XLSX), leitores, reservas e materiais (CSV).
4. **Contagem de testes:** README diz 91; encontrei ~103 anotações `@Test`. (Diferença possível por testes parametrizados/de integração. Não rodei a suíte.)
5. **Ressalva:** eu **li** o código, mas **não executei** o sistema. As descrições vêm do código-fonte, não de uma execução.

---

## 15. Perguntas que o professor pode fazer (com resposta curta)

**1. Por que JDBC e não JPA?** O requisito do trabalho. Dá controle total do SQL, e o `JdbcTemplate` cuida das conexões e do `PreparedStatement`.

**2. Como evitou SQL Injection?** Valores sempre como `?` em `PreparedStatement`; o `ORDER BY` (que não aceita `?`) passa por lista permitida na `OrdenacaoSegura`.

**3. Onde está a herança?** `ItemAcervo` (abstrata) → 4 tipos; `Leitor` (abstrata) → `Aluno`/`Professor`.

**4. Onde está o polimorfismo?** `leitor.getLimiteExemplaresSimultaneos()` no `EmprestimoService`: o mesmo código funciona para aluno e professor sem `if`/`instanceof`.

**5. Como sabe quem fez cada ação?** Tabela `auditoria`, preenchida pelos services com o usuário logado (`AuthUtil.usuarioAtual()`).

**6. O que é `@Transactional`?** Faz um conjunto de operações valer "tudo ou nada". Se a criação de empréstimo falhar na 3ª validação, nenhum dado é salvo.

**7. Como duas pessoas não pegam o mesmo livro?** Travas no banco (`FOR UPDATE`), `UPDATE` condicional por status e checagem de disponibilidade dentro da transação.

**8. Por que BCrypt?** É hash lento, com salt, não reversível; mesmo vazando o banco, as senhas não aparecem.

**9. Como a imagem chega e aparece?** Upload multipart → valida tamanho e **bytes mágicos** → grava como `BYTEA` → `<img>` aponta para `/acervo/{id}/capa` → controller devolve os bytes com o content-type certo.

**10. Como o front conversa com o back?** Formulários e links (HTML renderizado pelo Thymeleaf), `fetch` JSON só para o sino, WebSocket para tempo real, CSRF em tudo.

**11. Por que exclusão lógica?** Para não perder histórico e não quebrar chaves estrangeiras (empréstimos antigos continuam apontando para o material/exemplar).

**12. O que o Flyway faz?** Aplica os scripts SQL versionados na ordem, registrando o que já rodou; todo ambiente chega ao mesmo schema.

**13. Por que `BigDecimal` para multa?** `double` tem erro de arredondamento; dinheiro exige precisão exata.

**14. Como os atrasos viram multa?** Na devolução: dias entre a data prevista e hoje × taxa diária da configuração.

**15. O que acontece quando uma reserva vence?** O `ReservaScheduler` (a cada hora e na partida) marca como `EXPIRADA`, cria notificação e auditoria.

---

## 16. Sugestão de roteiro para apresentar (10–15 min)

1. **Visão geral** (seção 0 e 1) – 1 min
2. **Arquitetura em camadas** (seção 2) com um exemplo: "vou seguir o cadastro de um leitor" – 2 min
3. **Banco e herança** (seções 4–5): mostre `ItemAcervo`/`Livro` e a tabela `item_acervo` – 2 min
4. **Demonstração ao vivo:** login → dashboard → cadastrar leitor → emprestar → devolver atrasado (multa) → pagar – 4 min
5. **Regras de negócio** (seção 8): foque em empréstimo e fila de reservas – 2 min
6. **Imagens, WebSocket, segurança** (seções 9, 11, 12) – 2 min
7. **Testes, Docker, CI** (seção 13) – 1 min

**Dica:** abra no IDE, durante a fala, estes 5 arquivos, pois cobrem quase tudo:
`model/ItemAcervo.java`, `model/Aluno.java`, `service/EmprestimoService.java`, `repository/ExemplarRepository.java` (tombamento) e `config/SecurityConfig.java`.

---

## Anexo — Mapa de pastas

```
bublo/
├── pom.xml                      dependências Maven
├── Dockerfile                   imagem do app (build em 2 estágios)
├── docker-compose.yml           só o PostgreSQL
├── docker-compose.app.yml       app + PostgreSQL
├── .env.example                 modelo das variáveis de ambiente
├── .github/workflows/ci.yml     testes automáticos
├── docs/                        documentação acadêmica já existente (UML, DER, POO, manual...)
└── src/
    ├── main/java/com/gabriel/bublo/
    │   ├── controller/   (15 controllers)
    │   ├── service/      (8 services)
    │   ├── repository/   (13 classes, JDBC)
    │   ├── model/        (domínio com herança)
    │   ├── dto/          (objetos de transferência)
    │   ├── config/       (segurança, websocket, scheduler, demo)
    │   ├── security/ exception/ websocket/
    ├── main/resources/
    │   ├── application.yaml
    │   ├── db/migration/ (V1–V4)
    │   ├── templates/    (páginas Thymeleaf)
    │   └── static/       (css, js, bootstrap, chart.js)
    └── test/             (JUnit + Mockito)
```

> O repositório já traz documentos de apoio em `docs/` (por exemplo `GUIA_DE_ESTUDO_E_APRESENTACAO.md`, `MAPA_DO_CODIGO_POR_CAMADAS.md`, `RELATORIO_POO.md`, `UML.md`, `DER.md`). Vale ler junto com este.
