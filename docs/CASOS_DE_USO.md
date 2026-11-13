# Casos de Uso e Requisitos — Bublo

## Atores

- **Administrador:** único usuário do sistema, autenticado, responsável por todas as operações.

## Casos de uso

| ID | Caso de uso | Descrição |
|----|-------------|-----------|
| CU01 | Login | Autenticação do administrador com BCrypt e CSRF |
| CU02 | Cadastrar material | Livro, revista, DVD ou publicação digital, com autores |
| CU03 | Editar material | Alteração de metadados do acervo |
| CU04 | Cadastrar exemplar | Individual ou em lote (atômico), tombamento automático |
| CU05 | Buscar acervo | Título, autor, ISBN, categoria, tipo, disponibilidade, filtros combinados, paginação |
| CU06 | Cadastrar leitor | Aluno ou professor, matrícula automática |
| CU07 | Importar leitores CSV | Validação de estrutura, rejeição individual de linhas inválidas, relatório |
| CU08 | Empréstimo | Seleção de leitor, exemplares disponíveis, validação de limites/dívidas, transação única |
| CU09 | Devolução | Registro manual, cálculo automático de multa, fila FIFO de reservas |
| CU10 | Reserva | Fila FIFO por título, prazo de retirada, cancelamento, expiração |
| CU11 | Multa | Pagamento integral/parcial, estorno com justificativa |
| CU12 | Relatório | 9 relatórios, exportação CSV e XLSX |
| CU13 | Configurações | Empréstimos, multas, reservas, notificações, biblioteca; auditoria + reversão |
| CU14 | Notificações | Central, leitura, filtros, WebSocket em tempo real |
| CU15 | Auditoria | Consulta somente leitura de operações |
| CU16 | Alterar senha | Senha atual + nova + confirmação, política de senhas |

## Requisitos funcionais (RF)

- RF01: O sistema deve ter apenas um tipo de usuário (administrador).
- RF02: Senhas devem ser armazenadas com BCrypt.
- RF03: Tombamento de exemplares deve ser `BA-######`, sequencial, único, imutável.
- RF04: Matrícula de leitores é gerada automaticamente.
- RF05: Empréstimo deve apontar a exemplar específico, não ao título.
- RF06: Multa calculada automaticamente na devolução por atraso.
- RF07: Qualquer saldo pendente bloqueia novos empréstimos.
- RF08: Reserva segue fila FIFO e expira automaticamente.
- RF09: Relatórios exportáveis em CSV e XLSX.
- RF10: Tema claro e escuro com preferência persistida.

## Requisitos não funcionais (RNF)

- RNF01: Execução local com PostgreSQL via Docker Compose.
- RNF02: Sem dependência de CDN ou serviços externos.
- RNF03: Interface responsiva (desktop, tablet, celular).
- RNF04: Testes unitários com JUnit 5 + Mockito.
- RNF05: Integração contínua com GitHub Actions.
