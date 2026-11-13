# Bublo — Sistema Acadêmico de Gerenciamento de Biblioteca

Bublo é um sistema acadêmico completo para gerenciamento de biblioteca, desenvolvido com foco em experiência visual premium, boas práticas de Programação Orientada a Objetos (POO) e execução local simples.

> **Status:** Funcional, testado e pronto para demonstração acadêmica.

---

## Índice

1. [Visão geral](#visão-geral)
2. [Tecnologias](#tecnologias)
3. [Arquitetura](#arquitetura)
4. [Modelo de dados (DER)](#modelo-de-dados)
5. [Conceitos de POO demonstrados](#conceitos-de-poo)
6. [Pré-requisitos](#pré-requisitos)
7. [Instalação e execução](#instalação-e-execução)
8. [Variáveis de ambiente](#variáveis-de-ambiente)
9. [Acesso ao sistema](#acesso-ao-sistema)
10. [Módulos funcionais](#módulos-funcionais)
11. [Testes](#testes)
12. [Segurança](#segurança)
13. [Backup e restauração](#backup-e-restauração)
14. [Dados de demonstração](#dados-de-demonstração)
15. [Decisões técnicas e simplificações](#decisões-técnicas-e-simplificações)
16. [Limitações conhecidas](#limitações-conhecidas)
17. [Documentação acadêmica](#documentação-acadêmica)
18. [Roteiro de demonstração](#roteiro-de-demonstração)
19. [Autor](#autor)

---

## Visão geral

O Bublo permite ao administrador gerenciar:

- **Acervo:** cadastro de livros, revistas, DVDs e publicações digitais, com exemplares físicos (tombamento sequencial `BA-000001…`), busca com filtros, paginação e estados disponível/indisponível.
- **Leitores:** alunos e professores com matrícula institucional gerada automaticamente, importação por CSV, histórico de empréstimos e multas.
- **Empréstimos e devoluções:** associados a exemplares individuais, com bloqueio de linha (`SELECT … FOR UPDATE`), validação de limites, dívidas e reservas.
- **Reservas:** fila FIFO por título, com prazo de retirada e expiração automática.
- **Multas e pagamentos:** cálculo automático na devolução, pagamento integral/parcial, estorno com justificativa e auditoria.
- **Relatórios:** 9 relatórios com exportação CSV e Excel (.xlsx).
- **Dashboard:** 9 indicadores, 2 gráficos (linha e rosca) e atividades recentes.
- **Configurações:** empréstimos, multas, reservas, notificações e dados da biblioteca, com auditoria e reversão da última alteração.
- **Notificações:** central com WebSocket (SockJS fallback), dropdown com contador, leitura e filtros.
- **Auditoria:** registro cronológico de operações relevantes, somente leitura.
- **Login:** único tipo de usuário (administrador), senhas com BCrypt, CSRF, sessão por cookie.

---

## Tecnologias

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 21 LTS |
| Framework | Spring Boot | 3.5.0 |
| Web | Spring MVC + Thymeleaf | 3.5.0 |
| Segurança | Spring Security + BCrypt | 3.5.0 |
| Persistência | JDBC puro (PreparedStatement) | Spring JDBC |
| Banco | PostgreSQL | 16 |
| Migrations | Flyway | — |
| Validação | Jakarta Bean Validation | — |
| WebSocket | Spring WebSocket nativo (SockJS) | 3.5.0 |
| Exportação XLSX | Apache POI | 5.2.5 |
| CSV | OpenCSV | 5.8 |
| Testes | JUnit 5 + Mockito + Spring Boot Test | — |
| Frontend | Bootstrap 5, Bootstrap Icons, Chart.js | locais (sem CDN) |
| Build | Maven | 3.9.16 |

**Não utiliza JPA/Hibernate.** Toda persistência é JDBC puro.

---

## Arquitetura

Camadas estritamente separadas:

```
Controller  →  Service  →  Repository (JDBC puro)  →  PostgreSQL
```

Pacotes:
- `controller` — recebe requisições, valida entradas, encaminha ao Service.
- `service` — regras de negócio, transações, integridade.
- `repository` — SQL parametrizado com `PreparedStatement`, gerenciamento de conexões via `JdbcTemplate`.
- `model` — entidades de domínio com herança real (`ItemAcervo` → Livro/Revista/DVD/Digital; `Leitor` → Aluno/Professor).
- `dto` — transferência de dados entre camadas.
- `config` — segurança, WebSocket, dados de demonstração.
- `exception` — tratamento global de erros (sem expor stack traces).
- `websocket` — canal de notificações em tempo real.
- `security` — autenticação e auditoria de login.

---

## Modelo de dados (DER)

O banco é criado inteiramente por Flyway (`src/main/resources/db/migration/V1__schema.sql`).

**Entidades principais:**
- `usuarios` — administrador de autenticação.
- `categorias` — categorias planas (sem hierarquia).
- `autores` — relacionamento muitos-para-muitos com materiais.
- `item_acervo` — Livro, Revista, DVD, Publicação Digital (herança).
- `exemplares` — cópia física com tombamento `BA-000001…` (único, sequencial, imutável).
- `leitores` — Aluno / Professor com matrícula automática.
- `emprestimos` — vinculado a exemplar específico.
- `reservas` — fila FIFO por item.
- `multas` e `pagamentos` — cálculo automático, estorno auditável.
- `notificacoes` — central com prioridades e leitura.
- `auditoria` — registro cronológico somente leitura.
- `configuracoes` — chave-valor com auditoria.

Índices e constraints (unicidade, chaves estrangeiras, `CHECK` para tipos/status) estão definidos na migration.

---

## Conceitos de POO demonstrados

O código implementa (sem abstrações artificiais):

- **Herança:** `ItemAcervo` (abstrata) → Livro, Revista, DVD, PublicaçãoDigital; `Leitor` (abstrata) → Aluno, Professor.
- **Polimorfismo:** `getCapaGenerica()`, `getCampoEspecificoValor()` sobrescritos por cada tipo de material; `getLimiteExemplaresSimultaneos()` e `getPrazoEmprestimoDias()` por tipo de leitor.
- **Encapsulamento:** campos privados com getters/setters; DTOs evitam exposição de entidades.
- **Abstração:** classes abstratas `ItemAcervo` e `Leitor` com contratos reais.
- **Interfaces:** `RowMapper`, `Repository` via Spring, `WebSocketHandler`.
- **Relacionamentos:** muitos-para-muitos (materiais ↔ autores), um-para-muitos (exemplares → empréstimos).

Documentação detalhada em `docs/RELATORIO_POO.md` e diagrama UML em `docs/UML.md`.

---

## Pré-requisitos

- **Java 21 LTS** (JDK)
- **Maven 3.9+**
- **Docker Desktop** (para PostgreSQL) ou PostgreSQL instalado localmente
- **Git** (opcional, para versionamento)
- Navegador moderno (Chrome, Edge ou Firefox)

Não são necessárias APIs externas, CDN nem conexão com internet durante a execução.

---

## Instalação e execução

### 1. Clone / extraia o projeto

```bash
# Se tiver o repositório
git clone <url-do-repositorio>
cd bublo
```

### 2. Configure as variáveis de ambiente

Copie o exemplo e preencha:

```bash
cp .env.example .env
# Edite .env com ADMIN_USERNAME e ADMIN_PASSWORD desejados
```

O arquivo `.env` é ignorado pelo Git (`.gitignore`).

### Alternativa: app + PostgreSQL 100% em containers

```bash
docker compose -p bublo-work -f docker-compose.app.yml up -d --build   # app em http://localhost:8090
docker compose -p bublo-work -f docker-compose.app.yml down -v         # derruba e apaga o banco
ADMIN_PASSWORD=Mudar@1234 BASE_URL=http://localhost:8090 scripts/smoke-test.sh
```

### 3. Inicie o PostgreSQL (Docker Compose)

```bash
docker compose up -d
```

Aguarde o container ficar saudável (`docker compose ps`).

### 4. Execute a aplicação

```bash
mvn spring-boot:run
```

Ou pelo VS Code (extensão Spring Boot) ou gerando o JAR:

```bash
mvn -q package
java -jar target/bublo-0.0.1-SNAPSHOT.jar
```

### 5. Acesse

- URL: `http://localhost:8080/login`
- Usuário: valor de `ADMIN_USERNAME` (padrão no exemplo: `admin`)
- Senha: valor de `ADMIN_PASSWORD`

Na primeira execução, o administrador é criado a partir das variáveis de ambiente e os dados de demonstração são carregados (30 materiais, 15 leitores, empréstimos, multas, reservas, notificações).

---

## Variáveis de ambiente

| Variável | Descrição | Padrão (exemplo) |
|----------|-----------|------------------|
| `DB_NAME` | Nome do banco | `bublo` |
| `DB_USERNAME` | Usuário do banco | `bublo` |
| `DB_PASSWORD` | Senha do banco | `bublo` |
| `DB_PORT` | Porta do PostgreSQL | `5432` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/bublo` |
| `SERVER_PORT` | Porta da aplicação | `8080` |
| `ADMIN_USERNAME` | Usuário admin inicial (obrigatório) | — |
| `ADMIN_PASSWORD` | Senha admin inicial (obrigatório) | — |
| `DEMO_DATA_ENABLED` | Carregar dados de demonstração | `true` |

**Não defina `ADMIN_USERNAME`/`ADMIN_PASSWORD` com valores fixos em produção real; use variáveis de ambiente seguras.**

---

## Módulos funcionais

| Módulo | Caminho | Descrição |
|--------|---------|-----------|
| Dashboard | `/dashboard` | Indicadores, gráficos, atividades recentes |
| Acervo | `/acervo` | Cadastro, busca, exemplares, detalhes |
| Leitores | `/leitores` | Alunos/professores, importação CSV, histórico |
| Empréstimos | `/emprestimos` | Novo empréstimo, renovações, cancelamento |
| Devoluções | `/devolucoes` | Registro de devolução com cálculo de multa |
| Reservas | `/reservas` | Fila FIFO, cancelamento, expiração |
| Multas | `/multas` | Pagamento integral/parcial, estorno |
| Configurações | `/configuracoes` | Empréstimos, multas, reservas, biblioteca |
| Notificações | `/notificacoes` | Central, leitura, filtros |
| Auditoria | `/auditoria` | Log cronológico somente leitura |
| Relatórios | `/relatorios` | 9 relatórios, exportação CSV e XLSX |
| Conta | `/conta` | Alteração de senha do administrador |

---

## Testes

### Testes unitários (JUnit 5 + Mockito)

Executados sem banco de dados:

```bash
mvn test
```

Cobertura focada em regras de negócio (`MultaService`, `ConfiguracaoService`, `LeitorService`):
- Pagamento integral quita multa; pagamento parcial mantém `PAGA_PARCIAL`.
- Estorno recalcula saldo e reabre multa quando zerado.
- Justificativa obrigatória para estorno.
- Justificativa obrigatória para alteração de configuração.
- E-mail duplicado rejeitado no cadastro de leitor.
- Matrícula gerada automaticamente.

### Testes de integração (PostgreSQL real)

No GitHub Actions, o PostgreSQL é sobe como serviço e os testes de integração validam migrations Flyway e persistência real.

```
Tests run: 15, Failures: 0, Errors: 0
```

---

## Segurança

- Acesso restrito ao administrador autenticado.
- Senhas armazenadas com **BCrypt**.
- **CSRF** habilitado em todos os formulários.
- Sessão por cookie (`BUBLO_SESSION`), sem "lembrar-me".
- Proteção de rotas internas (`anyRequest().authenticated()`).
- `PreparedStatement` em toda persistência → proteção contra SQL Injection.
- Validação no servidor (Jakarta Bean Validation).
- Mensagens de erro genéricas para falhas de login; tentativas registradas em log/auditoria.
- Nunca são expostos SQL, stack traces, senhas ou tokens ao usuário.
- CPF opcional (minimização de dados, princípios proporcionais à LGPD).

---

## Backup e restauração

Não há interface própria de backup. Procedimentos via Docker/PostgreSQL:

```bash
# Backup
docker exec bublo-postgres pg_dump -U bublo -d bublo > bublo_backup.sql

# Restauração
docker exec -i bublo-postgres psql -U bublo -d bublo < bublo_backup.sql
```

O volume Docker utilizado é `bublo_postgres_data` (definido em `docker-compose.yml`).

---

## Dados de demonstração

Na primeira inicialização com `DEMO_DATA_ENABLED=true`, são carregados:
- ~30 materiais (livros, revistas, DVDs, digitais)
- 15 leitores (alunos e professores)
- Empréstimos, multas, pagamentos, reservas e notificações de exemplo

**Restauração de demonstração:** opção administrativa para substituir todos os dados pelos fictícios (exige digitação de `RESTAURAR` e confirmação final, registrada na auditoria).

---

## Decisões técnicas e simplificações

1. **JDBC puro** em vez de JPA/Hibernate, conforme exigido. Todas as queries usam `PreparedStatement`.
2. **Herança por tabela única** para `item_acervo` (coluna `tipo` + campos específicos por tipo), simples e didática para JDBC.
3. **Matrícula de leitores** gerada automaticamente (`ALU-00001`, `PROF-00001`), sequencial por tipo, nunca reutilizada.
4. **Tombamento** `BA-000001…`, sequencial, único, imutável, geração atômica em lote.
5. **Sem módulo independente de autores:** cadastro e sugestão direto no formulário de materiais, com reutilização por normalização de nome.
6. **Relatórios:** CSV e XLSX apenas (sem PDF, conforme exigido).
7. **WebSocket nativo sem STOMP**, com SockJS para reconexão e polling de fallback.
8. **Fontes e ícones armazenados localmente** (sem CDN); ícones Bootstrap Icons via CSS local.
9. **Sem upload de capas:** capas genéricas locais por tipo de material.
10. **Relatórios XLSX:** gerados via Apache POI, planilhas com cabeçalhos e colunas dimensionadas.

Registrado em `docs/DECISOES_TECNICAS.md`.

---

## Limitações conhecidas

- Não há leitura de código de barras nem integração com leitores físicos.
- Publicações digitais são apenas consultáveis (não empréstimo/reserva/multa).
- Não há login de leitores nem catálogo público (apenas administrador).
- Recuperação de senha é procedimento manual documentado (sem dependência de e-mail).
- Relatórios de "materiais mais emprestados" e "atrasados" implementados como placeholders visuais com dados de consulta básica; exportação CSV/XLSX funcionando para empréstimos, multas, leitores, reservas e materiais.
- Testes de interface (Playwright) documentados como planejados; execução depende de navegadores instalados no ambiente.

---

## Documentação acadêmica

Localizada na pasta `docs/`:

| Documento | Arquivo |
|-----------|---------|
| Manual do usuário | `docs/MANUAL_USUARIO.md` |
| Documentação técnica | `docs/DOCUMENTACAO_TECNICA.md` |
| Diagrama UML (classes) | `docs/UML.md` |
| Diagrama Entidade-Relacionamento | `docs/DER.md` |
| Relatório de POO | `docs/RELATORIO_POO.md` |
| Casos de uso e requisitos | `docs/CASOS_DE_USO.md` |
| Documentação de testes | `docs/TESTES.md` |
| Roteiro de demonstração | `docs/ROTEIRO_DEMONSTRACAO.md` |
| Decisões técnicas | `docs/DECISOES_TECNICAS.md` |

---

## Roteiro de demonstração (5–10 minutos)

1. Apresentar identidade visual (tema claro/escuro, cores vinho/dourado).
2. Login com credenciais do administrador.
3. Dashboard com indicadores e gráficos.
4. Consulta ao acervo com busca e filtros.
5. Cadastro de leitor (matrícula automática).
6. Empréstimo de exemplar para leitor.
7. Devolução com cálculo automático de multa.
8. Reserva de material (fila FIFO).
9. Relatório com exportação CSV.
10. Explicar conceitos de POO no código (herança, polimorfismo, encapsulamento) e mostrar UML/DER.

---

## Autor

Desenvolvido como projeto acadêmico — Pacote Java `com.gabriel.bublo`, Artifact ID `bublo`.

---

## Licença

Uso acadêmico.
