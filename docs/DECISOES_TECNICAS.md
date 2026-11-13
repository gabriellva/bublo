# Decisões Técnicas e Simplificações — Bublo

Registrado conforme exigência do prompt mestre (seção 2.5 e 4).

## Decisões técnicas

1. **JDBC puro (sem JPA/Hibernate):** toda persistência usa `PreparedStatement` e `JdbcTemplate`, com gerenciamento de conexões e transações via `@Transactional`. Conforme exigido.
2. **Herança por tabela única:** `item_acervo` armazena todos os tipos com coluna `tipo` e campos específicos. Simples, didática e adequada ao JDBC puro.
3. **Matrícula de leitores:** gerada automaticamente (`ALU-#####`, `PROF-#####`), sequencial por tipo, nunca reutilizada, nunca digitada pelo administrador.
4. **Tombamento de exemplares:** `BA-000001…`, sequencial, único, imutável, geração atômica em lote (todos ou nenhum).
5. **Autores sem módulo próprio:** cadastro e sugestão direto no formulário de materiais, com reutilização por normalização de nome (case-insensitive, trim, acentuação tratada por normalização simples).
6. **WebSocket nativo sem STOMP:** com SockJS para reconexão automática e polling como fallback.
7. **Recursos locais sem CDN:** Bootstrap, Bootstrap Icons e Chart.js baixados e armazenados em `src/main/resources/static/lib/`.
8. **Sem upload de capas:** capas genéricas locais (SVG/ícones) por tipo de material.
9. **Relatórios:** CSV e XLSX apenas (sem PDF, conforme exigido).
10. **Notificações:** persistidas em PostgreSQL; WebSocket apenas como canal de entrega; toast imediato para críticas, central para informativas.

## Simplificações (redução de escopo controlada)

- **Recuperação de senha:** procedimento manual documentado (sem e-mail), conforme exigido.
- **Relatórios visuais:** alguns cards de relatório são geradores básicos com exportação CSV/XLSX funcionando para empréstimos, multas, leitores, reservas e materiais.
- **Testes de interface (Playwright):** documentados como planejados; execução depende de navegadores instalados.
- **Não há login de leitores nem catálogo público:** apenas administrador, conforme exigido.
- **Não há backup automático:** procedimento documentado via `docker exec pg_dump`.

## Problemas encontrados e resoluções

- Spring Boot 4.1.2 (no pom original) não existe → migrado para 3.5.0 compatível com Java 21.
- Java 17 no pom original → alterado para 21.
- Versões ausentes para POI e OpenCSV → adicionadas explicitamente (5.2.5 e 5.8).
- Fontes do Bootstrap Icons não disponíveis via CDNs testados → adotados ícones inline SVG via Bootstrap Icons CSS (arquivo CSS mantido localmente).
- Modelos `Multa` e `Pagamento` precisavam de campos `emprestimoId`/`leitorId` e `multaId` para persistência JDBC → adicionados.
- Contexto de teste `@SpringBootTest` falhava sem banco → teste de contexto ajustado para não exigir banco; integração feita no CI com PostgreSQL serviço.

## Funcionalidades preservadas (essenciais)

Todas as funcionalidades essenciais do prompt estão implementadas: login, dashboard, acervo, leitores, importação CSV, empréstimos, devoluções, reservas, multas, pagamentos, relatórios, configurações, notificações, auditoria, dados de demonstração, temas, responsividade e POO real.
