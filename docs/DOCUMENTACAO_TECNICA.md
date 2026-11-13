# Documentação Técnica — Bublo

## Visão geral da implementação

Bublo é uma aplicação Spring Boot 3.5.0 com Java 21, Thymeleaf, Spring Security, JDBC puro, Flyway e PostgreSQL 16. Toda persistência é implementada com `PreparedStatement` (sem JPA/Hibernate).

## Estrutura de pastas

```
src/main/java/com/gabriel/bublo/
  controller/   - Controllers Thymeleaf
  service/      - Regras de negócio e transações
  repository/   - Repositórios JDBC puro
  model/        - Entidades de domínio
  dto/          - Objetos de transferência de dados
  config/       - Segurança, WebSocket, dados de demonstração
  exception/    - Tratamento global de erros
  websocket/    - Canal de notificações nativo
  security/     - Autenticação e auditoria de login
src/main/resources/
  templates/    - Templates Thymeleaf (login, dashboard, módulos, layouts)
  static/       - CSS, JS, libs locais (Bootstrap, Icons, Chart.js)
  db/migration/ - Migrations Flyway (V1__schema.sql)
  application.yaml
src/test/java/com/gabriel/bublo/service/ - Testes unitários (JUnit 5 + Mockito)
```

## Camadas

### Controller
Recebe requisições HTTP, valida entradas, chama serviços e retorna páginas Thymeleaf ou redirecionamentos. Não concentra regras de negócio.

### Service
Concentra regras de negócio, validações e transações (`@Transactional`). Exemplo: `EmprestimoService.criar()` valida limite do leitor, dívidas, disponibilidade e cria empréstimos em uma única transação.

### Repository
Executa SQL parametrizado com `PreparedStatement`. Cada repositório estende `BaseRepository` que encapsula `JdbcTemplate` e `GeneratedKeyHolder`.

### Model
Entidades de domínio com herança real:
- `ItemAcervo` (abstrata) → Livro, Revista, DVD, PublicacaoDigital
- `Leitor` (abstrata) → Aluno, Professor

### DTO
Transportam dados entre camadas e evitam exposição de entidades. Validados com Jakarta Bean Validation.

## Segurança

- `SecurityConfig`: formulário de login, CSRF, sessão por cookie, rotas protegidas.
- Senhas com BCrypt (`PasswordEncoder`).
- `AuthAuditService`: registra tentativas falhas e alterações de senha na auditoria.
- `GlobalExceptionHandler`: exibe mensagens amigáveis, nunca exibe stack traces ou dados internos.

## Banco de dados

Migration `V1__schema.sql` cria todas as tabelas, constraints, chaves estrangeiras, índices e unique constraints. Flyway gerencia a versão do esquema. Não há geração automática de tabelas.

## WebSocket

`NotificationWebSocketHandler` + `WebSocketConfig` (SockJS) entregam notificações em tempo real sem recarregar a página. Notificações são persistidas em `notificacoes`.

## Exportação

`RelatorioService` gera CSV (UTF-8, vírgula) e XLSX (Apache POI). `RelatorioExportController` expõe endpoints de download.

## Testes

- Unitários: `src/test/java/com/gabriel/bublo/service/` (JUnit 5 + Mockito).
- Integração: CI do GitHub Actions com PostgreSQL serviço.
- `mvn test` executa os testes unitários.

## Variáveis de ambiente

Veja `.env.example` e `README.md` (seção Variáveis de ambiente).

## Execução local

```bash
docker compose up -d        # PostgreSQL
mvn spring-boot:run         # Aplicação
# Acessar http://localhost:8080/login
```

## CI/CD

`.github/workflows/ci.yml`: compila, roda testes e sobe PostgreSQL como serviço a cada push/PR.
