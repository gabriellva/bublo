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

**No formulário (front):**
- O `<form>` usa `enctype="multipart/form-data"` (obrigatório para enviar arquivo) e o campo `<input type="file" name="capa" accept="image/jpeg,image/png,image/webp">`.
- Um JavaScript no formulário confere **tipo e tamanho antes de enviar** (se passar de 2 MB, mostra um toast de erro e limpa o campo) e mostra uma **prévia** da imagem escolhida. Isso é só conveniência: a validação que vale é a do servidor (bytes mágicos), porque o JS do navegador pode ser burlado.
- Ao editar, há a caixa "Remover a capa atual".

**Fotos dos dados de demonstração:**
- Ficam na pasta `src/main/resources/demo/capas/` (JPGs do Open Library, usados só para ilustrar a demonstração).
- Na primeira carga, o `DemoDataConfig.carregarCapaDemo` procura um arquivo cujo nome é o **título sem acentos e com hífens** (ex.: "Dom Casmurro" → `dom-casmurro.jpg`) e grava os bytes no banco com o mesmo `salvarCapa` do upload. Se não houver arquivo, o item fica sem foto e aparece a capa gerada.
- Num banco já existente, o startup também completa as capas que faltam, sem recriar nada.

**Resumo para falar:** upload (validado) → bytes no PostgreSQL (`BYTEA`) → endpoint `/acervo/{id}/capa` entrega a imagem → `<img>` na tela; sem foto, capa desenhada em CSS.

**Detalhe a saber:** `ItemAcervo` tem o método polimórfico `getCapaGenerica()` (`Livro` devolve `"capa-livro.svg"`), mas **nenhuma tela usa esse retorno**: a capa gerada de verdade é feita em CSS (`.capa-gerada`, `.capa-livro` etc.). O método existe como exemplo de polimorfismo, não alimenta a imagem exibida.

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
- Detalhes completos de como a página é montada: veja a subseção **10.1** logo abaixo.

### 10.1. Como uma página é renderizada (passo a passo)

**Ideia central:** o navegador nunca recebe o template. Quem lê o `.html` com as marcações `th:...` e entrega HTML comum, já com os dados preenchidos, é o **servidor**. Isso se chama *Server-Side Rendering* (SSR). O JavaScript só entra depois, para detalhes (tema, toasts, sino, gráficos).

**Exemplo: abrir `/acervo` no navegador**

```
1. Navegador:  GET /acervo?termo=dom
2. Spring Security: está logado? (se não, redireciona para /login)
3. DispatcherServlet do Spring MVC acha o método do controller (@GetMapping("/acervo"))
4. GlobalModelAdvice (@ControllerAdvice) roda ANTES e coloca no Model:
      usuario, naoLidas (contador do sino), nomeInstituicao
5. AcervoController.listar(...) roda: pede ao service, e coloca no Model:
      resultado (página de itens), termo, categorias, tipos...
6. O método devolve só uma String: "acervo/listar"  (o NOME da view)
7. O Thymeleaf procura  classpath:/templates/acervo/listar.html
      (application.yaml: prefix=/templates/, suffix=.html)
8. Thymeleaf percorre o HTML, avalia cada th:* usando o Model e gera HTML puro
9. O HTML pronto volta como resposta; o navegador desenha a página
10. Depois carrega CSS/JS de /css, /js e /lib, e o bublo.js "ativa" a página
```

O controller **não monta HTML**: ele devolve o nome da view e o `Model` (um "saco" de atributos). Essa é a separação MVC: **M**odel (dados), **V**iew (template), **C**ontroller (decide qual view e quais dados).

**Montagem por peças (layout com fragmentos)**
Cada página começa assim:
```html
<th:block th:replace="~{layout/base :: head}"></th:block>      <!-- <head> comum -->
<th:block th:replace="~{layout/sidebar :: sidebar('acervo')}"></th:block>
<th:block th:replace="~{layout/topbar :: topbar}"></th:block>
... conteúdo próprio da página ...
<th:block th:replace="~{layout/base :: scripts}"></th:block>   <!-- JS comum -->
```
- `base :: head` leva as `<meta>` (inclusive o **token CSRF**), o script que aplica o tema antes de pintar a tela, e o CSS.
- `sidebar('acervo')` é um fragmento **com parâmetro**: o menu compara `modulo == 'acervo'` e usa `th:classappend` para destacar o item da página atual.
- `topbar` mostra o nome do usuário e o contador de notificações (vem do `GlobalModelAdvice`, por isso aparece em **todas** as páginas sem cada controller precisar repetir).
- `componentes :: paginacao(resultado, '/acervo')` e `componentes :: capa(item)` são fragmentos reutilizados em várias telas.

**Alguns usos reais de Thymeleaf no projeto**

| Recurso | Para quê | Exemplo do projeto |
|---|---|---|
| `th:text` | Escreve texto **já escapado** (protege contra XSS) | `th:text="${item.titulo}"` |
| `th:each` | Repete um bloco por item da lista | Os cards de livros do acervo |
| `th:if` / `th:unless` | Mostra ou esconde | Estado vazio ("Nenhum material encontrado") |
| `th:classappend` | Troca classe CSS conforme o dado | Badge verde/vermelho de disponibilidade |
| `th:href="@{/acervo/{id}(id=${item.id})}"` | Monta URLs corretas | Link para os detalhes |
| `th:object` + `th:field` | Liga o formulário ao DTO (preenche e devolve os valores) | Formulário de leitor |
| `th:errors` / `#fields.hasErrors` | Mostra erros de validação do `@Valid` | "Nome completo é obrigatório" |
| `th:with` | Variáveis locais no template | Cálculo das páginas visíveis na paginação |
| `th:inline="javascript"` | Passa dados do Java para dentro do JS | Dados dos gráficos do dashboard |
| `?:` (Elvis) | Valor padrão | `${usuario ?: 'Administrador'}` |

**Dashboard (dados do servidor para o gráfico):** o controller coloca no Model listas como `rotulosMes` e `valoresMes`. No template, `/*[[${rotulosMes}]]*/ []` faz o Thymeleaf escrever essas listas como JavaScript (JSON) dentro da página, e o **Chart.js** as usa para desenhar. O gráfico é desenhado no navegador, mas os números já chegam prontos do servidor.

**Formulários e o ciclo GET → POST → redirect**
1. `GET /leitores/novo`: o controller coloca um `LeitorDTO` vazio no Model; o Thymeleaf renderiza o formulário vazio.
2. O usuário envia: `POST /leitores`. O Spring preenche um `LeitorDTO` com os campos e valida (`@Valid`).
3. **Com erro:** o controller devolve de novo o nome da view do formulário (`return "leitores/formulario"`). O Thymeleaf renderiza a página **com os valores digitados e as mensagens de erro** (`th:errors`).
4. **Sem erro:** salva e devolve `redirect:/leitores` (o navegador faz um novo `GET`); a mensagem de sucesso vai por *flash attribute*.

**Páginas de erro:** o `GlobalExceptionHandler` também devolve uma *view* (`erro.html`) com `status` e `mensagem` no Model, em vez do erro cru do servidor.

**Pontos de configuração (`application.yaml`)**
- `thymeleaf.prefix/suffix`: onde ficam os templates (`/templates/` e `.html`).
- `thymeleaf.cache: false`: o servidor relê o template a cada acesso (bom para desenvolver; em produção costuma-se ligar o cache).
- `web.locale: pt_BR` com `locale-resolver: fixed`: datas e números em português independentemente do navegador.
- `static-locations: classpath:/static/`: pasta de CSS, JS e bibliotecas, servida direto sem passar pelo Thymeleaf.

**SSR × SPA (pergunta provável):**
- Aqui cada ação navega para uma página nova (ou recarrega a atual) com HTML pronto vindo do servidor.
- Numa SPA (React/Vue) o navegador baixa um app em JavaScript que pede dados em JSON a uma API e monta a tela sozinho.
- O Bublo escolheu SSR porque é mais simples, não precisa de API separada, e o Thymeleaf já escapa o HTML e integra com validação e CSRF. A parte "dinâmica" é pequena e tratada com JavaScript puro (`bublo.js`).

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

### 12.1. Como funciona o login (passo a passo)

**Quem existe:** só **um tipo de usuário**, o administrador, guardado na tabela `usuarios` (`username`, `senha_hash`, `ativo`). Não há cadastro público nem login de leitor.

**1. De onde vem o primeiro administrador**
No startup, o `DemoDataConfig` lê `ADMIN_USERNAME` e `ADMIN_PASSWORD` do ambiente. Se esse usuário **ainda não existe**, ele é criado com a senha já em **BCrypt** (`passwordEncoder.encode`). Se as duas variáveis faltarem, o app **lança erro e não sobe**. A senha nunca fica escrita no código.
> Detalhe: como só cria se não existir, **mudar `ADMIN_PASSWORD` depois não altera a senha** de um admin já criado. Para trocar, use a tela `/conta`.

**2. O que é BCrypt**
Função de hash feita para senhas: é **lenta de propósito** (dificulta força bruta), usa **salt** aleatório (duas senhas iguais geram hashes diferentes) e **não é reversível**. O sistema nunca guarda nem consegue recuperar a senha original; ele só compara.

**3. O fluxo de entrar**

```
1. Usuário abre qualquer página protegida → Spring Security vê que não há login
   → redireciona para GET /login
2. AuthController.login mostra login.html
   (se já estiver logado, redireciona direto para /dashboard)
3. Formulário envia POST /login com os campos "username" e "password"
   (o token CSRF vai junto, escondido, porque o form usa th:action)
4. O Spring Security (formLogin) intercepta o POST, antes de qualquer controller:
   a) chama o UserDetailsService definido no SecurityConfig
      → UsuarioRepository.buscarPorUsername("SELECT ... WHERE username = ?")
   b) monta um UserDetails com o hash do banco, papel ADMIN e
      accountLocked = !ativo   (usuário inativo não entra)
   c) o DaoAuthenticationProvider compara a senha digitada com o hash
      usando o BCryptPasswordEncoder
5. SUCESSO → successHandler: grava "LOGIN" na auditoria e redireciona para /dashboard
   FALHA   → failureHandler: grava "LOGIN_FALHA" na auditoria e redireciona para /login?error=true
6. O AuthController, ao ver ?error, coloca no Model a mensagem
   "Usuário ou senha incorretos."  (a mesma mensagem para usuário inexistente e senha errada)
```

Quem faz a checagem real é o **framework**; o código do projeto só informa *onde achar o usuário* (`userDetailsService`) e *qual hash usar* (`passwordEncoder`), e o que fazer no sucesso/falha.

**4. Depois de logado: a sessão**
- O servidor cria uma **sessão** e manda o navegador guardar um cookie chamado `BUBLO_SESSION`. A cada requisição o cookie volta e o Spring sabe quem é você, sem pedir senha de novo.
- O cookie é `http-only` (JavaScript não consegue ler, o que reduz o roubo por XSS) e `SameSite=LAX` (não é enviado em requisições vindas de outros sites, o que ajuda contra CSRF).
- A sessão expira após **60 minutos** de inatividade.
- `maximumSessions(1)`: um segundo login do mesmo usuário **encerra a sessão anterior**. Quem estava no outro navegador é mandado para `/login?expirada=true` com o aviso "a conta entrou em outro navegador ou dispositivo".
- Todas as rotas, exceto `/login`, `/css/**`, `/js/**`, `/lib/**`, `/images/**`, `/error` e `/favicon.ico`, exigem estar autenticado (`anyRequest().authenticated()`). Todo usuário logado é `ROLE_ADMIN`; **não há níveis de permissão diferentes**.
- O nome do usuário logado fica disponível em qualquer service por `AuthUtil.usuarioAtual()` e é o que vai para a coluna `admin_usuario` da auditoria.

**5. Sair (logout)**
- O botão "Sair" é um **formulário `POST /logout`** (com CSRF), não um link. Assim um site malicioso não consegue deslogar o usuário só com uma imagem ou link.
- O Spring invalida a sessão e apaga os cookies `BUBLO_SESSION` e `JSESSIONID`; depois redireciona para `/login?logout=true` com "Você saiu do sistema."

**6. Trocar a senha (`/conta`)**
1. `TrocaSenhaDTO` valida: nova senha entre **4 e 72** caracteres (72 é o limite que o BCrypt considera) e confirmação obrigatória.
2. O controller confere se "nova senha" e "confirmação" são iguais.
3. Grava o novo hash (`UPDATE usuarios SET senha_hash = ?`), registra `ALTERACAO_SENHA` na auditoria, **faz logout** e manda para `/login?senha=alterada`, obrigando a entrar com a senha nova.

**7. CSRF no login**
O Spring gera um token por sessão. O Thymeleaf o insere como campo escondido em todo `<form th:action=... method="post">` e também na `<meta name="_csrf">` para o `fetch`. Um POST sem o token válido é recusado (403). Por isso um site externo não consegue enviar formulários em nome do administrador.

**Pontos para citar como limites (honestidade técnica):**
- **Não encontrei bloqueio por tentativas** (rate limit / lockout): as falhas são só registradas na auditoria. Um atacante poderia tentar muitas senhas.
- A política de senha é fraca (mínimo de 4 caracteres) e o `admin`/`admin` do `.env.example` serve **só para desenvolvimento**; no Render o próprio guia manda usar senha forte.
- O cookie não tem a flag `secure` configurada no `application.yaml`. Em produção com HTTPS o ideal é ativá-la.
- Não há recuperação de senha por e-mail (o README diz que é procedimento manual).
- O `SecurityConfig` declara um bean `AuthenticationManager` e existe `AuthUtil.autenticar`, mas **nada no código chama** esses dois; o login usa o mecanismo padrão do `formLogin`. É código que sobra.
- Eu li o código, mas **não testei** o comportamento da sessão única na prática.

---

## 13. Relatórios, testes e implantação

### Relatórios
`RelatorioExportController` + `RelatorioService`. Exportação em **CSV** (escrito direto na resposta HTTP em UTF-8) e **XLSX** (Apache POI: `XSSFWorkbook`, linhas e células montadas em memória e devolvidas como bytes com `Content-Disposition: attachment`). Há exportação de empréstimos, multas, leitores, reservas e materiais.

### Testes
- 103 métodos `@Test` em `src/test`, distribuídos assim: `AcervoServiceTest` 24, `EmprestimoServiceTest` 20, `LeitorServiceTest` 15, `ConfiguracaoServiceTest` 13, `ReservaServiceTest` 12, `MultaServiceTest` 12, `OrdenacaoSeguraTest` 4, `PolimorfismoTest` 2, `BubloApplicationTests` 1.
- Usam **Mockito** para simular os repositórios (testam a regra sem banco). Exemplos em `EmprestimoServiceTest`: devolução atrasada gera multa com a taxa da configuração, devolução no prazo não gera multa, leitor inativo não empresta, data no passado é recusada, renovação bloqueada por reserva ou por atraso.
- **Não encontrei teste de integração com banco real** em `src/test`: nenhum teste sobe o Spring ou o PostgreSQL. O `BubloApplicationTests` só confere que a classe existe, não carrega o contexto.
- **CI** (`.github/workflows/ci.yml`): sobe um PostgreSQL 16 no GitHub Actions e roda `mvn` a cada push.

### Docker
- `docker-compose.yml` sobe só o PostgreSQL.
- `docker-compose.app.yml` + `Dockerfile` sobem **app + banco** em contêineres. O Dockerfile tem 2 estágios: compila com Maven e depois copia só o `.jar` para uma imagem enxuta só com o JRE.
- O deploy em produção está descrito na seção 13.1 (Render).

### 13.1. Deploy no Render (colocar o sistema online)

**O que é o Render:** uma plataforma de hospedagem (PaaS). Você conecta o repositório do GitHub e ele baixa o código, compila, sobe a aplicação, dá uma URL pública com **HTTPS** (`https://nome.onrender.com`), reinicia se cair e refaz o deploy a cada `git push`. Não precisa administrar servidor nem instalar Java.

**Duas peças criadas no painel:**
1. **PostgreSQL gerenciado** (`bublo-db`, plano Free, região Oregon).
2. **Web Service tipo Docker** (usa o `Dockerfile` da raiz, branch `master`, mesma região do banco).

**Fluxo de um deploy:**
```
git push → Render clona o repo → docker build (Dockerfile) → sobe o container
        → espera uma porta abrir → passa o tráfego para a nova versão
```
Se nenhuma porta abrir, o deploy falha com "No open ports detected".

**Por que o Dockerfile é bom:**
- **Multi-stage:** o 1º estágio (Maven + JDK) compila; o 2º (só JRE + `.jar`) roda. A imagem final fica pequena.
- **Cache de camadas:** o `pom.xml` é copiado antes do `src`, então se só o código mudar, as dependências não são baixadas de novo (deploys mais rápidos).

**Como a configuração chega no app:** o `application.yaml` usa `${DB_URL}`, `${DB_USERNAME}` etc. **sem valor padrão**. No computador vêm do `.env`; no Render vêm do painel **Environment Variables**. O **mesmo código e o mesmo Dockerfile** rodam nos dois lugares. Nenhuma senha fica no repositório. Se faltar uma variável, o app não sobe e o log diz qual (`Could not resolve placeholder 'DB_URL'`).

**Variáveis configuradas no Render:**

| Variável | Para quê |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-interno>:5432/<database>` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Administrador criado no primeiro start |
| `DEMO_DATA_ENABLED` | Carregar dados de demonstração |
| `PORT=8080` | O Render espera a porta 10000 por padrão; o Spring usa 8080 |
| `JAVA_TOOL_OPTIONS=-Xmx384m` | O plano Free tem 512 MB de RAM; limita o heap da JVM para o container não ser encerrado por falta de memória |

**No primeiro start:** o Flyway cria as tabelas, o `DemoDataConfig` cria o admin e os dados de exemplo, e o log mostra `Started BubloApplication in ... seconds`.

**Problemas reais que apareceram (bom para contar na apresentação, mostra que você entende o que foi feito):**
- Colar a "Internal Database URL" do Render no `DB_URL` dá erro, porque ela não começa com `jdbc:` e traz usuário/senha embutidos. Correção: montar `jdbc:postgresql://host:5432/database` e passar usuário e senha em variáveis separadas.
- Usar `bublo-db` (nome do **serviço** no Render) como usuário ou como nome do banco dá `password authentication failed` / `database does not exist`. Nome do serviço, nome do banco e usuário do banco são três coisas diferentes: os valores certos estão em *Info → Connections*.

**Limites do plano Free:** o site **dorme após ~15 min sem acesso** e o primeiro acesso leva cerca de 1 min para acordar; o banco gratuito expira depois de um período (~30 dias) e não tem backup automático. Dica: abra o site uns minutos antes da apresentação.

**Local × Render:**

| | Local (`docker-compose.app.yml`) | Render |
|---|---|---|
| Banco | contêiner `postgres:15-alpine` | PostgreSQL gerenciado |
| Acesso | `http://localhost:8090` | `https://<nome>.onrender.com` |
| HTTPS | não | sim, automático |
| Como sobe | `docker compose -f docker-compose.app.yml up -d --build` | `git push` |

**Evolução possível (não feita):** um arquivo `render.yaml` ("Blueprint") criaria banco, serviço e variáveis automaticamente. Exigiria o `application.yaml` montar a URL a partir de `DB_HOST`/`DB_PORT`/`DB_NAME`; nesta versão optou-se por configurar pelo painel, sem alterar o código.

> Observação: essas informações vêm do guia `docs/DEPLOY_RENDER.md` e dos arquivos Docker do repositório. Eu não consigo ver o painel do Render, então **não sei se o site está no ar agora**, nem a URL real. Confirme isso antes de apresentar.

---

## 14. Divergências que encontrei entre README e código (importante)

Para você não ser pego de surpresa numa pergunta:

1. **SockJS:** o README diz "WebSocket com SockJS fallback". No código **não há SockJS em lugar nenhum** (nem no `WebSocketConfig`, nem no JS, nem no `pom.xml`). É **WebSocket nativo** com reconexão automática. Se perguntarem, responda isso.
2. **Número da matrícula** é gerado com `MAX+1` **sem lock** (diferente do tombamento, que usa advisory lock). Duas criações exatamente simultâneas poderiam colidir; o `UNIQUE` do banco impediria a duplicata, mas uma das duas daria erro. Pouco provável numa demo, mas é uma limitação real.
3. **Relatórios:** o README fala em "9 relatórios", mas a própria seção de limitações admite que dois são "placeholders". O que existe de exportação real: empréstimos (CSV+XLSX), multas (CSV+XLSX), leitores, reservas e materiais (CSV).
4. **Testes:** README diz "Tests run: 91" e fala em testes de integração com PostgreSQL real. Em `src/test` contei **103** `@Test`, todos de unidade com Mockito, e **nenhum** sobe o Spring ou o banco (o `BubloApplicationTests` só confere que a classe existe). Não rodei a suíte, então não sei dizer qual número a execução real mostra.
5. **Capas:** o README (decisão técnica 9) diz "sem upload de capas, capas genéricas locais por tipo". O código **tem upload** (migration V4, `BYTEA`, endpoint `/acervo/{id}/capa`) e a capa genérica é desenhada em CSS, não com os `.svg`. Vale o código.
6. **Versão do PostgreSQL:** o README e o CI dizem PostgreSQL 16, mas o `docker-compose.app.yml` usa `postgres:15-alpine`. Pequena inconsistência; se perguntarem, diga a versão que você de fato usa.
7. **Ressalva:** eu **li** o código, mas **não executei** o sistema. As descrições vêm do código-fonte, não de uma execução.

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

### 15.1. Perguntas extras que podem aparecer (e a resposta)

**Injeção de dependência (muito provável).** Nenhuma classe faz `new` de outra camada. Cada service/repository declara o que precisa **no construtor** (ex.: `EmprestimoService(EmprestimoRepository, LeitorRepository, ...)`) e o **Spring cria e entrega** os objetos. Isso se chama **injeção de dependência / inversão de controle**. As anotações `@Controller`, `@Service`, `@Repository` e `@Component` dizem ao Spring quais classes ele deve gerenciar (esses objetos são *beans*, por padrão únicos na aplicação). Vantagem: nos testes, troca-se o repositório por um *mock* sem mexer na classe.

**O que o `@Transactional` faz por baixo?** O Spring cria um *proxy* em volta do service: abre a transação antes do método, **confirma** (commit) se terminar bem e **desfaz** (rollback) se uma exceção sem tratamento (`RuntimeException`, como `IllegalStateException`) escapar. Por isso as validações lançam exceção: ela cancela tudo que já foi gravado no método. `readOnly = true` nas consultas indica que não há escrita.

**Padrões de projeto usados:** MVC (controller/view/model), Repository (isola o SQL), DTO, Dependency Injection, Template Method/polimorfismo (`ItemAcervo`, `Leitor`), e *fragments* de layout.

**Por que `RuntimeException` e não checked?** `IllegalStateException` é *unchecked*: não obriga `throws` em toda a cadeia. O controller ou o `GlobalExceptionHandler` captura e mostra a mensagem.

**Status "ATRASADO" existe?** Está permitido no `CHECK` do banco, mas **o código não grava esse status**. "Atrasado" é **calculado**: empréstimo `ATIVO` com data prevista anterior a hoje (`diasDeAtraso > 0`). O filtro "Atrasados" da tela traduz isso em condição SQL.

**Como funciona a paginação?** O SQL termina em `LIMIT ? OFFSET ?` (tamanho da página e deslocamento), mais um `COUNT(*)` para o total. O resultado vai num `Paginacao<T>` (conteúdo, total, página, tamanho, total de páginas). O controller limita o tamanho entre 1 e 100.

**Há N+1 queries?** Há um caso: `MultaService.toDTO` busca o leitor **uma vez por multa** da página. Com páginas pequenas funciona bem, mas é o tipo de ponto que se resolveria com um `JOIN`. Já a `ReservaService` usa um cache (`livresPorItem`) por página justamente para evitar isso. Se perguntarem, assuma como melhoria conhecida.

**Por que `BigDecimal` e `LocalDateTime`?** `BigDecimal` evita erro de arredondamento em dinheiro; `LocalDateTime`/`LocalDate` são a API moderna de datas (imutável, sem os problemas do `Date`).

**Recursos modernos de Java que aparecem no código:** `record` (`ItemAcervoRepository.Capa`), `switch` com `->` e `case LIVRO -> new Livro()` no mapper, `Optional` nos repositórios, *streams* (`.stream().map(...).toList()`), `enum` com campo (`TipoMaterial` com `label`) e lambdas (`RowMapper`, handlers de login).

**Por que PostgreSQL?** Banco relacional robusto e gratuito, com os recursos que o sistema usa: `SELECT ... FOR UPDATE`, índices únicos parciais, advisory locks, `BYTEA`, `ILIKE`. Também é o oferecido pelo Render.

**Por que Spring Boot?** Já traz servidor web embutido (Tomcat), configuração automática, segurança, WebSocket e testes. Gera um único `.jar` executável (`java -jar`), que casa com o Docker.

**O que é Maven e o `pom.xml`?** Ferramenta de build: o `pom.xml` lista as dependências (spring-boot-starter-web, -thymeleaf, -security, -websocket, JDBC, PostgreSQL, Flyway, POI, OpenCSV...) e o Maven baixa, compila, roda os testes e empacota o `.jar`.

**E o que fica de fora (limitações):** leitor não tem login nem catálogo público; sem código de barras; digitais só para consulta; sem recuperação de senha por e-mail; sem bloqueio por tentativas de login; sem integração com sistemas externos.

**Git/GitHub:** o histórico tem 94 commits, de 11 a 25/11/2026, com mensagens no padrão `feat:`, `fix:`, `docs:`, `chore:`, e *pull requests* de branches (`dev`, `docs/...`) para a principal. Mostra um fluxo de trabalho organizado.

**Pergunta de ouro nesse trabalho: "o que a IA fez e o que você decidiu?"** Isso **eu não consigo responder por você**. Como o enunciado libera IA mas exige que você saiba explicar, prepare uma resposta honesta e curta: o que você pediu, que decisões foram suas (ex.: usar JDBC puro, só um perfil de usuário, regras de multa e reserva) e que partes você revisou e entende. Uma boa saída é conseguir **abrir qualquer arquivo e explicar linha a linha**, e saber dizer onde mexer se o professor pedir uma alteração ao vivo.

### 15.2. "Onde eu mexo para...?" (alterações ao vivo)

| Pedido do professor | Onde mudar |
|---|---|
| Mudar o limite de livros do aluno | Pela tela **Configurações** (tabela `configuracoes`, chave `limite_alunos`); o padrão do código está em `Aluno.getLimiteExemplaresSimultaneos()` |
| Mudar prazo de empréstimo | Configurações (`prazo_alunos` / `prazo_professores`); padrão em `Aluno`/`Professor.getPrazoEmprestimoDias()` |
| Mudar o valor da multa por dia | Configurações (`taxa_diaria_multa`); padrão `TAXA_DIARIA_PADRAO` em `EmprestimoService` |
| Permitir mais de uma renovação | `MAXIMO_RENOVACOES` em `EmprestimoService` |
| Novo tipo de material | Nova subclasse de `ItemAcervo`, novo valor em `TipoMaterial`, e incluir o tipo no `CHECK` e no mapper (`ITEM_MAPPER`) do `ItemAcervoRepository` (nova migration Flyway) |
| Novo campo no leitor | Nova migration (`ALTER TABLE leitores ...`), campo em `Leitor`, `LeitorDTO`, `LeitorRepository` e `formulario.html` |
| Mudar cores/tema | `static/css/design-system.css` (variáveis como `--bublo-vinho`) |
| Mudar tempo de sessão | `server.servlet.session.timeout` no `application.yaml` |
| Aumentar o tamanho máximo da capa | `spring.servlet.multipart.max-file-size` e `TAMANHO_MAXIMO_CAPA` em `AcervoService` |
| Mudar a frequência da expiração de reservas | `@Scheduled(cron = ...)` em `ReservaScheduler` |

> Atenção: eu li esses pontos no código, mas **não executei nenhuma dessas alterações**. Teste antes de depender delas na apresentação.

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
