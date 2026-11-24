# Verificação 2 — regras/validações/cálculos e front (celular, estilo, feedback)

Data: 16/11/2026. Continuação de `docs/AUDITORIA_LOGICA.md`.
Método: releitura de backend e templates; **renderização real** das 19 telas no Chrome headless em 390 px e 320 px
(iframe com largura exata) medindo overflow horizontal, tamanho de alvos de toque e tamanho de fonte dos campos;
capturas de tela; testes contra o app em Docker.

Severidade: **Alta** = quebra uso ou regra; **Média** = erro visível/dado inconsistente; **Baixa** = polimento.

---

## A. Regras de negócio, validações e cálculos

| ID | Sev. | Problema | Onde |
|----|------|----------|------|
| R01 | Média | Filtro **Indisponível** do acervo lista títulos que ainda têm exemplar livre (basta 1 exemplar emprestado). Deveria ser "nenhum exemplar livre". | `ItemAcervoRepository.buscar` |
| R02 | Alta | Autor repetido com outra caixa/espaço (`Machado`/`machado `) vira o mesmo registro e viola a PK de `item_acervo_autor`; o erro é mostrado como "ISBN/ISSN já existe". Nome de autor >150 caracteres estoura no banco (HTTP 500). | `AcervoService.construirItem/salvar`, `bublo` JS do formulário |
| R03 | Média | Material sem limites: ano negativo ou 99999, páginas/volume/duração negativos; ISBN/ISSN e URL sem formato (`javascript:` aceito); o **tipo** pode mudar na edição (LIVRO→DVD perde ISBN; →DIGITAL com exemplares). | `ItemAcervoDTO`, `AcervoService` |
| R04 | Média | Leitor: CPF e telefone sem validação; CPF duplicado aceito; strings vazias gravadas como `""`. A importação CSV não passa pelas validações do formulário (nome >200 vira "Não foi possível gravar esta linha"). O CSV modelo traz um CPF inválido. | `LeitorService`, `LeitorController` |
| R05 | Média | Reserva: prazo de retirada sem teto (2099 segura a fila para sempre) e título **sem exemplares cadastrados** pode ser reservado. | `ReservaService` |
| R06 | Baixa | Forma de pagamento aceita qualquer texto (>50 caracteres → HTTP 500). | `MultaService` |
| R07 | Média | Configurações sem limite superior (limite 2 bilhões, prazo 100000 dias) e taxa com 3+ casas decimais gera multa que o banco arredonda diferente do cálculo. | `ConfiguracaoService` |
| R08 | Média | Gráfico "Empréstimos nos últimos meses" omite meses sem empréstimo (a linha liga pontos não consecutivos). | `EmprestimoRepository.contarPorMes` |
| R09 | Média | `server.servlet.session.timeout: 0` = sessão **nunca expira** (manual diz que expira). | `application.yaml` |
| R10 | Baixa | Prefixo anti-fórmula do CSV não cobre TAB/CR. | `RelatorioService.csv` |
| R11 | Alta | Novo empréstimo: a data sugerida pelo JS usa UTC (`toISOString`); depois das 21h em Brasília sugere um dia a mais, e o servidor agora recusa por passar do prazo. A tela também não mostra o limite do leitor. | `emprestimos/novo.html`, `EmprestimoController.restricao` |

## B. Front: celular, estilos, margens, feedback

| ID | Sev. | Problema (evidência) |
|----|------|----------------------|
| F01 | Alta | Em 320 px a topbar estoura 67 px em **todas** as páginas (rolagem horizontal da página inteira); o nome da instituição quebra em 2 linhas em 390 px. |
| F02 | Alta | "Minha conta" some no celular (`display:none`) e não há outro caminho para trocar a senha. |
| F03 | Alta | Todos os campos de formulário têm 15,2 px (<16 px): o iPhone dá zoom ao focar. Medido em 100 % dos formulários. |
| F04 | Alta | Tabelas viram rolagem horizontal: em 390 px as colunas **Situação** e **Ações** (Renovar, Cancelar, Pagar, Devolver) ficam fora da tela. |
| F05 | Média | Alvos de toque pequenos: botões de ação 28–30 px, topbar 36 px, paginação 39 px, "Sair" 37 px (mínimo recomendado 44 px). |
| F06 | Média | Telas de detalhe (leitor, material) estouram o cartão em 320 px e cortam o e-mail. |
| F07 | Alta | Exemplares do empréstimo em `<select multiple>` (Ctrl/Cmd para vários): inviável no celular. |
| F08 | Média | Paginação lista **todas** as páginas, sem quebra de linha: com muitas páginas estoura a tela. |
| F09 | Média | Nenhum feedback ao enviar formulário e nenhuma proteção contra duplo clique/toque (empréstimo, pagamento, devolução). |
| F10 | Baixa | `:hover` com `translateY` nos cards e botões "gruda" em telas de toque e faz formulários dentro de cards tremerem. |
| F11 | Média | Menu mobile sem fundo escurecido, sem Esc, sem `aria-expanded`; links do menu fechado continuam focáveis por Tab; página rola por trás. |
| F12 | Baixa | `100vh` na sidebar/layout: corta no Safari/Chrome mobile (barra de endereço). |
| F13 | Média | Dashboard: 9 indicadores em coluna única no celular (muita rolagem); botões do cabeçalho com larguras diferentes. |
| F14 | Média | Login: painel editorial espreme o formulário entre 577 e 900 px; texto técnico exposto ("BCrypt", variáveis de ambiente). |
| F15 | Baixa | Toasts com largura mínima fixa e botão × de ~20 px. |
| F16 | Média | Sem foco visível em links/menu/ícones; sem link "pular para o conteúdo"; menu sem `aria-current`. |
| F17 | Baixa | Rodapé da sidebar com nome fixo "Biblioteca Acadêmica" (ignora a configuração); "Minha conta" marca "Configurações" no menu. |
| F18 | Média | Códigos crus na interface: `LIVRO`, `EMPRESTIMO`, `ATENCAO`, `CRIACAO_EMPRESTIMO` (dashboard, notificações, auditoria, acervo). |
| F19 | Baixa | Lista de leitores: botões de ação empilham e cada linha chega a ~170 px; `<input type=file>` sem estilo. |
| F20 | Baixa | `Playfair Display`/`Inter` declaradas mas não empacotadas (README diz "fontes locais"): cai em Georgia/sistema. |
| F21 | Baixa | Indentação/estilo: espaços duplos em `configuracoes/listar.html`, TABs em `BubloApplication.java`, `style=""` repetidos em vez de classes. |
| F22 | Baixa | Cores dos indicadores calculadas no `IndicadorDTO` e ignoradas pelo template. |

## Resultado da correção

Todos os itens R01–R11 e F01–F22 foram corrigidos. Decisões que mudam comportamento:

- **Teto de reserva:** 30 dias (constante `PRAZO_RETIRADA_MAXIMO_DIAS`); título sem exemplares cadastrados não pode ser reservado.
- **Limites de configuração:** limites de exemplares 1–50, prazos 1–365 dias, retirada de reserva 1–30 dias, taxa de multa R$ 0–1.000 com até 2 casas.
- **CPF:** validado (dígitos verificadores), único e gravado como `000.000.000-00`; as mesmas regras valem para o CSV. O CSV modelo agora traz um CPF válido.
- **Tipo do material** não muda depois do cadastro (como o tipo do leitor).
- **Sessão** expira após 60 min de inatividade.
- **Front:** `<=768px` as listas viram cartões (CSS `tabela-cartoes` + `data-label` nos templates); `<=992px` campos de 16 px e alvos de 44 px; menu mobile com fundo, Esc, `aria-expanded` e foco; paginação em janela; envios com spinner e proteção contra duplo clique; seleção de exemplares por checkboxes com filtro.

### Como foi verificado

| Verificação | Antes | Depois |
|-------------|-------|--------|
| Overflow horizontal em 320 px (19 telas) | 19 de 19 telas estouravam (topbar +67 px); detalhe do leitor cortava o e-mail | 0 |
| Overflow horizontal em 390 px | 0 (mas tabelas escondiam colunas) | 0 (tabelas em cartões) |
| Campos com fonte < 16 px (390 px) | 15 de 19 telas | 0 |
| Alvos de toque < 40 px | 4 a 49 por tela | 0 (restam só os próprios `<input type=checkbox>` de 18 px, cujo `<label>` tem 44 px, e links de nome em texto corrido) |
| Testes unitários | 69 | **84**, 0 falhas |
| Smoke test (build novo) | passa | passa (ajustado ao seletor de checkbox) |
| Cenários ponta a ponta desta rodada | — | 25 verificações OK (filtro Indisponível, autor duplicado, ano/ISBN inválidos, tipo imutável, CPF, e-mail sem caixa, CSV, reserva >30 dias, limites de configuração, gráfico com 6 meses, paginação em janela, menu, cartões) |

Medição feita no Chrome headless, com cada tela dentro de um iframe de largura exata (o Chrome não aceita janela menor
que ~485 px), injetando um script que mede overflow, fontes e alvos de toque.

### Erros meus durante a correção (corrigidos antes de fechar)

- O regex que adicionou `aria-current` ao menu gravou aspas escapadas e derrubou **todas** as páginas com HTTP 500; pego ao
  reconstruir e abrir o dashboard, corrigido com `th:aria-current`.
- Na primeira remedição, o smoke test derrubou a sessão do proxy (`maximumSessions(1)`) e as medições "limpas" eram da
  tela de login; descartei e refiz com sessão válida.

### Não verificado / limites

- Não testei em iPhone/Android reais nem em Safari; a evidência é Chrome headless. O tema escuro não foi capturado
  (a flag de esquema de cores não surtiu efeito no headless); só revisado no CSS.
- O ajuste final de recuo da tabela de detalhes (CSS) foi feito depois da última captura e não foi renderizado de novo.
- Categorias do acervo não têm tela de cadastro (só as da carga de demonstração); registrado, não alterado.
- Login sem limite de tentativas (já listado na rodada anterior).
