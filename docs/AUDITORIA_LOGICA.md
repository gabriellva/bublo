# Verificação de lógica — erros encontrados e correções

Data da verificação: 16/11/2026. Escopo: serviços, repositórios, controllers, templates e documentação.
Método: leitura completa do código, execução da suíte de testes (15 testes, todos passando antes das correções),
smoke test e reprodução dos defeitos contra a aplicação rodando em Docker (PostgreSQL real).

Legenda de severidade: **Alta** = viola regra de negócio documentada ou derruba a página; **Média** = dado
inconsistente ou regra furada; **Baixa** = polimento, robustez ou divergência de documentação.

Todos os itens abaixo foram corrigidos e verificados (ver "Resultado da verificação" no final).

---

## Alta

### L01 — Reserva nunca é atendida e a fila é ignorada no empréstimo
- **Onde:** `EmprestimoService.criar`, `ReservaService.atender` (nunca chamado).
- **Problema:** quando um exemplar é devolvido, o primeiro da fila é só notificado. Qualquer leitor pode então
  emprestar o exemplar, "furando" a fila. Quando o leitor da reserva empresta o material, a reserva continua
  `AGUARDANDO` para sempre (e bloqueia a exclusão do material e a renovação de outros empréstimos do título).
- **Regra violada:** manual §6/§7 e RF08 ("reserva segue fila FIFO", "a próxima reserva da fila é atendida").
- **Correção:** o empréstimo só é aceito se houver exemplares livres além dos reservados por quem está à frente
  na fila; ao emprestar, a reserva do próprio leitor passa a `ATENDIDA`. As posições da fila são renumeradas quando
  uma reserva sai (atendida, cancelada ou expirada), e o cancelamento de um empréstimo também avisa o primeiro da
  fila. A reserva de um título só é recusada se houver mais exemplares livres do que reservas na fila.

### L02 — Expiração automática de reservas não existe
- **Onde:** `ReservaRepository.listarExpiradasNaoProcessadas` e `ReservaService.expirar` não têm chamador;
  não há `@Scheduled`.
- **Problema:** reservas vencidas ficam `AGUARDANDO` indefinidamente e travam a fila.
- **Regra violada:** README, manual §7, RF08 ("expiram automaticamente").
- **Correção:** tarefa agendada (a cada hora e na partida) + expiração preventiva antes de validar a fila no
  empréstimo. Reserva vence ao fim do dia do prazo (mesmo critério de data usado nas multas).

### L03 — Estorno de pagamento: sem tela, repetível e sem marca de estornado
- **Onde:** `MultaService.estornar`, `PagamentoRepository`, `MultaController` (sem rota), schema `pagamentos`.
- **Problema:** (a) não existe rota nem tela para estornar; (b) o mesmo pagamento pode ser estornado várias vezes
  — cada chamada subtrai o valor de novo (truncando em zero), deixando a multa `PENDENTE` com outros pagamentos
  válidos ainda registrados; (c) o pagamento não é marcado como estornado (o manual diz que fica no histórico
  "marcado como estornado"); (d) o estorno não vai para a auditoria (README: "estorno auditável").
- **Correção:** migration `V2` (colunas de estorno em `pagamentos`), bloqueio de estorno duplicado, rota
  `POST /multas/pagamentos/{id}/estornar`, histórico de pagamentos na tela da multa com formulário de estorno
  (justificativa obrigatória) e registro na auditoria.

### L04 — Página negativa derruba `/notificacoes` e `/auditoria` com HTTP 500
- **Reprodução:** `GET /notificacoes?pagina=-1` e `GET /auditoria?pagina=-1` → 500 (OFFSET negativo no SQL).
  Também não havia limite superior de `tamanho`.
- **Correção:** normalização de `pagina`/`tamanho` nos dois controllers (igual aos demais).

### L05 — Auditoria só registra login e configurações
- **Evidência:** na base de teste, `auditoria` continha apenas `LOGIN` e `ALTERACAO_CONFIGURACAO`.
- **Regra violada:** README/manual §12 ("registro cronológico de operações relevantes").
- **Correção:** registro de auditoria em empréstimo (criar/devolver/renovar/cancelar), multas (pagar/estornar),
  reservas (criar/cancelar/atender/expirar), acervo (salvar/excluir/exemplares) e leitores
  (cadastrar/editar/inativar/importar).

---

## Média

### L06 — "Atrasado" calculado de duas formas diferentes
- **Onde:** SQL usa `data_prevista_devolucao < NOW()` (instante); Java e manual usam data ("a partir do dia
  seguinte ao vencimento"). A data prevista é gravada às 00:00.
- **Efeito:** empréstimo que vence **hoje** aparece como atrasado no dashboard, no filtro "Atrasado" e na lista de
  atrasados, mas a devolução não gera multa e a tela de devolução mostra "No prazo". O mesmo vale para reservas
  com prazo de retirada hoje.
- **Correção:** SQL passa a comparar por data (`::date < CURRENT_DATE`).

### L07 — Publicações digitais aceitam exemplares e aparecem para empréstimo
- **Regra:** README "Publicações digitais são apenas consultáveis (não empréstimo/reserva/multa)".
- **Problema:** `cadastrarExemplar(es)` não bloqueia o tipo `DIGITAL`; `listarDisponiveisParaEmprestimo` não o
  exclui; `criar` não revalida.
- **Correção:** bloqueio nos três pontos.

### L08 — Material "excluído" (inativo) continua operável e contado
- **Problema:** (a) exemplares podem ser cadastrados em item inativo; (b) um POST direto empresta exemplar de item
  inativo; (c) reserva em item inativo; (d) dashboard conta exemplares/disponíveis/indisponíveis de itens
  excluídos, enquanto "Títulos" só conta os ativos; (e) o índice único de ISBN/ISSN continua valendo para itens
  excluídos, então recadastrar o mesmo livro falha com "já existe".
- **Correção:** validações de item ativo; contadores filtram por item ativo; índices únicos parciais
  (`WHERE ativo`) na migration `V2`.

### L09 — Configuração "Prefixo dos tombamentos" não tem efeito
- **Problema:** a tela grava `prefixo_tombamento`, mas `ExemplarRepository` usa `BA-` fixo.
- **Correção:** novos exemplares usam o prefixo configurado (somente letras, até 10); a numeração continua global
  e nunca reaproveitada; tombamentos existentes não mudam.

### L10 — Trocar o tipo do leitor deixa a matrícula incoerente
- **Problema:** editar um `ALUNO` para `PROFESSOR` mantém `ALU-xxxxx` mas muda limites e prazos.
- **Correção:** tipo imutável após o cadastro (campo desabilitado na edição e validação no serviço).

### L11 — Leitor pode ser inativado com empréstimos ativos
- **Problema:** `inativar` e o checkbox "ativo" do formulário não conferem pendências; reservas do leitor
  inativado continuam na fila.
- **Correção:** bloqueio se houver empréstimo ativo; reservas aguardando são canceladas ao inativar.

### L12 — Multa de valor zero
- **Problema:** com `taxa_diaria_multa = 0` (permitido), a devolução atrasada cria multa `PENDENTE` de R$ 0,00 que
  infla "Multas pendentes" e não pode ser paga (valor deve ser > 0).
- **Correção:** só gera multa se o valor for maior que zero.

### L13 — Concorrência em devolução, cancelamento, pagamento e limite do leitor
- **Problema:** duas requisições simultâneas de devolução (duplo clique) veem o empréstimo `ATIVO` e geram duas
  multas; dois pagamentos simultâneos podem somar acima do valor; dois empréstimos simultâneos ultrapassam o limite.
- **Correção:** `UPDATE ... WHERE status = 'ATIVO'` com checagem de linhas afetadas (multa só depois);
  `SELECT ... FOR UPDATE` na multa e no leitor.

### L14 — Data prevista de devolução sem limite superior
- **Problema:** aceita qualquer data futura (ex.: 2099), anulando prazo e multa. Manual: aluno 7 dias, professor 14.
- **Correção:** a data prevista não pode passar do prazo padrão do tipo de leitor contado a partir de hoje.

### L15 — Reserva de título que o leitor já está com emprestado
- **Correção:** bloqueada, com mensagem clara.

### L16 — "Desfazer a última alteração de configuração" documentado e inexistente
- **Correção:** `ConfiguracaoService.reverterUltima` + botão na tela, com auditoria (`REVERSAO_CONFIGURACAO`).
  Só reverte a última alteração; não reverte uma reversão.

---

## Baixa

| ID | Problema | Correção |
|----|----------|----------|
| L17 | E-mail duplicado é sensível a maiúsculas e a espaços (`Ana@x.com` ≠ `ana@x.com`) | comparação `lower()`, e-mail gravado normalizado |
| L18 | Importação CSV conta linha em branco como "Linha incompleta" | linhas vazias são ignoradas |
| L19 | Filtros de busca não removem espaços das pontas (multas, leitores, acervo, auditoria) | `trim` |
| L20 | `materiais.csv` ignora o parâmetro `termo` | filtro aplicado |
| L21 | Dados demo usam `Math.random()` (commit "determinísticos" não cobriu) | quantidade determinística |
| L22 | Sem `ADMIN_*` criava `admin`/`admin` | senha aleatória gerada e registrada uma vez no log |
| L23 | Pagamento aceita mais de 2 casas decimais (compara sem arredondar, grava arredondado) | rejeita >2 casas |
| L24 | Documentação divergente do código | README e manual ajustados (ver abaixo) |

Divergências de documentação tratadas em L24: delimitador do CSV exportado é `;` (não vírgula); dados de demo
contêm 2 empréstimos (sem multas/pagamentos/reservas prontos); "Restauração de demonstração" não existe;
"valores arrecadados" não aparece no dashboard; contagem de testes.

## Observações não alteradas

- Login sem limite de tentativas (apenas auditado). Recomendável para produção; fora do escopo desta rodada.
- `maximumSessions(1)` funciona (testado: novo login derruba a sessão anterior; logout/login repetido OK).

## Resultado da verificação

| Item | Status | Como foi verificado |
|------|--------|---------------------|
| L01–L24 | Corrigidos | testes unitários + cenários ponta a ponta contra o app e o PostgreSQL reais |

- **Testes unitários:** 69 testes, 0 falhas (eram 15). Novos testes cobrem fila de reservas, empréstimo de digital e de
  material excluído, prazo máximo, devolução concorrente, taxa zero, estorno duplicado, pagamento com 3 casas,
  tipo imutável do leitor, inativação com empréstimo/reservas, prefixo de tombamento e reversão de configuração.
- **Migration V2:** aplicada sobre um banco já existente na versão 1 (Flyway: `1 -> 2`), sem perda de dados.
- **Smoke test (`scripts/smoke-test.sh`):** passa; agora é repetível (cancela o empréstimo que cria) e cobre
  `pagina=-1` em notificações e auditoria.
- **Cenários ponta a ponta (33 verificações, todas OK):** fila 1,2 → só o primeiro empresta → reserva `ATENDIDA` →
  fila renumerada; data +8 dias recusada; digital sem exemplares; vence-hoje fora de "atrasados"; multa de 3 dias a
  R$ 2,00 = R$ 6,00 bloqueia o leitor; pagamento parcial/total; estorno com justificativa, uma vez só, saldo
  recalculado; tipo do leitor imutável; inativar bloqueado com empréstimo; prefixo `LIB` aplicado e revertido;
  12 tipos de operação na auditoria; reserva vencida ontem expira na partida e a que vence hoje permanece;
  ISBN de material excluído pode ser recadastrado.

Arquivos principais alterados: `EmprestimoService`, `ReservaService`, `MultaService`, `LeitorService`,
`AcervoService`, `ConfiguracaoService`, repositórios de empréstimo/reserva/exemplar/multa/pagamento/leitor,
`ReservaScheduler` (novo), migration `V2__estorno_e_indices.sql` (nova), telas de multas, leitores e configurações.
