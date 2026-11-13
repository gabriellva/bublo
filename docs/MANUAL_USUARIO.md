# Manual do Usuário — Bublo

Sistema Acadêmico de Gerenciamento de Biblioteca. Destinado ao administrador da biblioteca.

---

## 1. Primeiro acesso

1. Inicie o PostgreSQL (`docker compose up -d`).
2. Inicie a aplicação (`mvn spring-boot:run`).
3. Acesse `http://localhost:8080/login`.
4. Informe o usuário e senha definidos nas variáveis de ambiente (`ADMIN_USERNAME`, `ADMIN_PASSWORD`).
5. Após o login, você será redirecionado ao Dashboard.

Não há login de leitores nem catálogo público. Todo o acesso é restrito ao administrador autenticado.

---

## 2. Dashboard

Exibe:
- **Indicadores:** títulos, exemplares, disponíveis/indisponíveis, empréstimos ativos/atrasados, multas pendentes, valores arrecadados, reservas aguardando, leitores cadastrados.
- **Gráficos:** empréstimos ao longo do tempo e materiais por tipo.
- **Atividades recentes** e **ações rápidas** (cadastrar material, leitor, empréstimo, devolução, reservas, pagamento de multa, relatórios).

Use o seletor de tema (ícone de lua/sol no topo) para alternar claro/escuro. A preferência é mantida.

---

## 3. Acervo

### Cadastrar material
Acesse **Acervo → Cadastrar material**. Escolha o tipo (Livro, Revista, DVD, Publicação Digital). Os campos específicos aparecem conforme o tipo:
- Livro: ISBN
- Revista: ISSN e Volume
- DVD: Duração
- Publicação Digital: DOI e URL externa

Autores podem ser digitados e adicionados; nomes já existentes são reaproveitados automaticamente.

### Exemplares
Em **Detalhes do material**, cadastre exemplares individuais ou em lote. Cada exemplar recebe tombamento automático (`BA-000001`, `BA-000002`…). O tombamento não pode ser editado.

### Busca
Use o painel de filtros para buscar por título, autor, ISBN/ISSN, categoria, tipo e disponibilidade. Os filtros são mantidos durante a navegação. Visualização em cards (padrão) ou tabela.

---

## 4. Leitores

### Cadastrar
**Leitores → Cadastrar leitor.** Informe nome, e-mail, endereço e opcionalmente CPF, telefone e curso/departamento. A matrícula institucional é gerada automaticamente (`ALU-00001` para alunos, `PROF-00001` para professores).

### Importar CSV
Baixe o modelo CSV, preencha e importe em **Leitores → Importar CSV**. A importação é imediata: linhas válidas são importadas e inválidas são rejeitadas com motivo e número da linha. Não sobrescreve leitores existentes.

### Histórico
Ao clicar em um leitor, você vê empréstimos ativos, anteriores, multas, saldo pendente e reservas.

---

## 5. Empréstimos

**Empréstimos → Novo empréstimo:**
1. Selecione o leitor. O sistema exibe se há bloqueio por dívida.
2. Escolha um ou mais exemplares disponíveis.
3. Informe a data prevista de devolução.
4. Confirme. Todos os empréstimos são registrados em uma única transação.

Regras:
- Alunos: até 3 exemplares, prazo 7 dias.
- Professores: até 5 exemplares, prazo 14 dias.
- Uma renovação por empréstimo.
- Saldo de multa pendente bloqueia novos empréstimos.

---

## 6. Devoluções

**Devoluções:** busque por tombamento, matrícula ou nome do leitor. Selecione o empréstimo ativo, confira o resumo (exemplar, situação, possível multa) e registre a devolução. A multa é calculada automaticamente se houver atraso (a partir do dia seguinte ao vencimento). O exemplar volta ao estado disponível e a próxima reserva da fila é atendida.

---

## 7. Reservas

**Reservas → Nova reserva:** informe leitor, material e prazo de retirada. A reserva entra na fila FIFO. Quando um exemplar é devolvido, o primeiro da fila é atendido. Reservas podem ser canceladas pelo administrador e expiram automaticamente se não retiradas no prazo.

---

## 8. Multas e pagamentos

Multas são geradas automaticamente na devolução atrasada.
- **Multas:** lista de pendentes, pagas parciais e pagas.
- **Pagar:** informe valor (integral ou parcial) e forma (dinheiro ou outro meio). Pagamento parcial não remove o bloqueio enquanto houver saldo.
- **Estornar:** exige justificativa obrigatória. O pagamento original permanece no histórico, marcado como estornado, e o saldo é recalculado.

---

## 9. Relatórios

Acesse **Relatórios**. Cada relatório pode ser exportado em **CSV** (UTF-8, delimitador vírgula) ou **Excel (.xlsx)**. Os relatórios incluem empréstimos por período, atrasados, multas pendentes, materiais mais emprestados, disponíveis/indisponíveis, leitores cadastrados e reservas.

---

## 10. Configurações

**Configurações:** aba com empréstimos, multas, reservas, notificações e dados da biblioteca. Alterações valem apenas para novas operações (não modificam empréstimos/multas/reservas existentes). Toda alteração exige justificativa e é registrada na auditoria. É possível desfazer a última alteração.

---

## 11. Notificações

O ícone de sino no topo mostra o contador de não lidas. Clique para ver notificações recentes. Acesse a central completa para filtrar por categoria, prioridade e data, e marcar como lidas.

---

## 12. Auditoria

**Auditoria:** tabela cronológica somente leitura com operações, responsável, data/hora, módulo, registro afetado e justificativa. Mantida por 2 anos.

---

## 13. Alterar senha (conta)

Acesse **Minha conta** no topo. Informe senha atual, nova senha (mín. 8 caracteres, letras e números) e confirmação. Após a alteração, você será desconectado e deverá entrar novamente.

---

## 14. Saída

Clique em **Sair** no topo para encerrar a sessão. A sessão expira ao fechar o navegador ou ao fazer logout.

---

## 15. Dados de demonstração

A primeira inicialização carrega dados fictícios (~30 materiais, 15 leitores, empréstimos, multas, reservas, notificações) para demonstração imediata. Existe opção administrativa para restaurar esses dados (exige digitar `RESTAURAR`).
