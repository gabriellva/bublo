# Diagrama Entidade-Relacionamento (DER) — Bublo

Corresponde ao schema implementado em `src/main/resources/db/migration/V1__schema.sql`.

## Tabelas

```
usuarios (id PK, username UK, senha_hash, ativo, data_criacao)
configuracoes (chave PK, valor, atualizado_em, atualizado_por)
categorias (id PK, nome UK, ativo)
autores (id PK, nome, nome_normalizado UK, data_cadastro)

item_acervo (id PK, tipo, titulo, descricao, editora, ano,
             data_publicacao, paginas, idioma, categoria_id FK,
             isbn UK, issn UK, duracao_minutos, doi, url_externa,
             ativo, data_cadastro)

item_acervo_autor (item_acervo_id FK, autor_id FK, PK composto)

exemplares (id PK, item_acervo_id FK, tombamento UK,
            disponivel, observacoes, data_cadastro)

leitores (id PK, tipo, nome, email UK, matricula UK, cpf,
          endereco, telefone, curso_departamento, ativo, data_cadastro)

emprestimos (id PK, leitor_id FK, exemplar_id FK, data_emprestimo,
             data_prevista_devolucao, data_devolucao_real, renovacoes,
             status, observacoes)

reservas (id PK, leitor_id FK, item_acervo_id FK, data_reserva,
          posicao, prazo_retirada, status)

multas (id PK, emprestimo_id FK, leitor_id FK, valor, taxa_diaria,
        dias_atraso, data_geracao, status, valor_pago)

pagamentos (id PK, multa_id FK, valor, forma_pagamento,
            data_pagamento, responsavel)

notificacoes (id PK, categoria, prioridade, titulo, mensagem,
              registro_tipo, registro_id, lida, resolvida,
              data_criacao, data_resolucao)

auditoria (id PK, admin_usuario, data_hora, tipo_operacao, modulo,
           registro_afetado, valores_antes, valores_depois,
           justificativa)
```

## Constraints de integridade

- Chaves primárias em todas as tabelas (`SERIAL`).
- Chaves estrangeiras com `ON DELETE CASCADE` onde apropriado (ex.: `item_acervo_autor`, `exemplares` → `item_acervo`).
- `UNIQUE`: `usuarios.username`, `categorias.nome`, `autores.nome_normalizado`, `item_acervo.isbn`, `item_acervo.issn`, `exemplares.tombamento`, `leitores.email`, `leitores.matricula`.
- `UNIQUE parcial`: `reservas(leitor_id, item_acervo_id)` WHERE `status='AGUARDANDO'` (evita reserva duplicada ativa).
- `CHECK`: `item_acervo.tipo IN (...)`, `leitores.tipo IN (...)`, `emprestimos.status IN (...)`, `reservas.status IN (...)`, `multas.status IN (...)`.

## Índices de suporte a consultas e relatórios

Índices em `titulo`, `tipo`, `categoria_id`, `ativo`, `disponivel`, `status`, `data_emprestimo`, `data_prevista_devolucao`, `data_criacao`, `data_hora` (auditoria), `leitor_id`, `exemplar_id`, `item_acervo_id`.

## Regras de negócio no modelo

- Exemplar físico vinculado a item do acervo (tombamento imutável).
- Empréstimo nunca aponta apenas ao título: sempre a um exemplar específico.
- Matrícula do leitor gerada pelo sistema, não digitada, única e imutável.
- Categoria em uso não pode ser excluída (apenas desativada).
- Material com empréstimo ativo ou reserva pendente não pode ser excluído.
