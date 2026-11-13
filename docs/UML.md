# Diagrama de Classes UML — Bublo

Este diagrama corresponde ao código realmente implementado no pacote `com.gabriel.bublo.model`.

## Hierarquia de herança

```
ItemAcervo (abstrata)
│── Livro
│── Revista
│── DVD
└── PublicacaoDigital

Leitor (abstrata)
│── Aluno
└── Professor
```

### ItemAcervo (abstrata)
- id: Long
- tipo: TipoMaterial
- titulo: String
- descricao: String
- editora: String
- ano: Integer
- dataPublicacao: LocalDate
- paginas: Integer
- idioma: String
- categoria: Categoria
- autores: List<Autor>
+ salvar()
+ getCapaGenerica(): String      <<abstrato>>
+ getCampoEspecificoLabel(): String  <<abstrato>>
+ getCampoEspecificoValor(): String  <<abstrato>>

### Livro
- isbn: String

### Revista
- issn: String
- volume: Integer

### DVD
- duracaoMinutos: Integer

### PublicacaoDigital
- doi: String
- urlExterna: String

### Leitor (abstrata)
- id: Long
- tipo: TipoLeitor
- nome: String
- email: String
- matricula: String
- cpf: String
- endereco: String
- telefone: String
- cursoDepartamento: String
- ativo: Boolean
- dataCadastro: LocalDateTime
+ getLimiteExemplaresSimultaneos(): int   <<abstrato>>
+ getPrazoEmprestimoDias(): int            <<abstrato>>

### Aluno
+ getLimiteExemplaresSimultaneos(): int = 3
+ getPrazoEmprestimoDias(): int = 7

### Professor
+ getLimiteExemplaresSimultaneos(): int = 5
+ getPrazoEmprestimoDias(): int = 14

## Outras entidades

- **Autor**: id, nome, nomeNormalizado
- **Categoria**: id, nome, ativo
- **Exemplar**: id, itemAcervoId, itemAcervo, tombamento, disponivel, observacoes, dataCadastro
- **Emprestimo**: id, leitor, exemplar, dataEmprestimo, dataPrevistaDevolucao, dataDevolucaoReal, renovacoes, status, observacoes
- **Reserva**: id, leitor, itemAcervo, dataReserva, posicao, prazoRetirada, status
- **Multa**: id, emprestimoId, leitorId, valor, taxaDiaria, diasAtraso, dataGeracao, status, valorPago, + getSaldoPendente()
- **Pagamento**: id, multaId, valor, formaPagamento, dataPagamento, responsavel
- **Notificacao**: id, categoria, prioridade, titulo, mensagem, registroTipo, registroId, lida, resolvida, dataCriacao, dataResolucao
- **Auditoria**: id, adminUsuario, dataHora, tipoOperacao, modulo, registroAfectado, valoresAntes, valoresDepois, justificativa
- **Configuracao**: chave, valor, atualizadoEm, atualizadoPor
- **Usuario**: id, username, senhaHash, ativo, dataCriacao

## Relacionamentos

| Relacionamento | Cardinalidade | Observação |
|----------------|---------------|------------|
| ItemAcervo — Autor | N:M | tabela `item_acervo_autor`, cadastro direto no formulário |
| ItemAcervo — Categoria | N:1 | categoria plana, desativação em vez de exclusão |
| ItemAcervo — Exemplar | 1:N | cada exemplar tem tombamento único `BA-######` |
| Exemplar — Emprestimo | 1:N | empréstimo aponta sempre a exemplar específico |
| Leitor — Emprestimo | 1:N | histórico de empréstimos por leitor |
| Leitor — Reserva | 1:N | reserva por leitor e item |
| Leitor — Multa | 1:N | multas vinculadas ao leitor |
| Emprestimo — Multa | 1:1 | multa gerada na devolução |
| Multa — Pagamento | 1:N | pagamentos parciais permitidos |

## Enumeradores

- `TipoMaterial`: LIVRO, REVISTA, DVD, DIGITAL
- `TipoLeitor`: ALUNO, PROFESSOR

## Pacotes de camadas

- `controller` — Controllers Thymeleaf
- `service` — Regras de negócio e transações
- `repository` — JDBC puro (PreparedStatement)
- `model` — Entidades de domínio (UML acima)
- `dto` — Objetos de transferência
- `config` — Segurança, WebSocket, dados demo
- `exception` — Tratamento global de erros
