# Relatório de Programação Orientada a Objetos — Bublo

Este relatório explica os conceitos de POO utilizados no código-fonte do Bublo, com exemplos reais extraídos do pacote `com.gabriel.bublo`.

---

## 1. Encapsulamento

Os campos das entidades são privados e expostos por getters/setters, protegendo o estado interno.

Exemplo (`model/Exemplar.java`):
```java
private String tombamento;
private Boolean disponivel;

public String getTombamento() { return tombamento; }
public void setTombamento(String tombamento) { this.tombamento = tombamento; }
```

O tombamento não pode ser editado manualmente na interface: é gerado pelo repositório. Os DTOs (`dto/LeitorDTO`, `dto/ItemAcervoDTO`) evitam expor as entidades diretamente às camadas de apresentação.

---

## 2. Herança

Hierarquias reais modelam o domínio da biblioteca:

```
ItemAcervo (abstrata)
├── Livro
├── Revista
├── DVD
└── PublicacaoDigital

Leitor (abstrata)
├── Aluno
└── Professor
```

Exemplo (`model/Leitor.java`): `Leitor` é abstrata e define campos comuns (nome, email, matrícula, endereço). `Aluno` e `Professor` herdam e acrescentam comportamento próprio.

---

## 3. Polimorfismo

Cada subclasse sobrescreve métodos da classe abstrata com comportamento próprio:

- `ItemAcervo.getCapaGenerica()` retorna `capa-livro.svg`, `capa-revista.svg`, `capa-dvd.svg` ou `capa-digital.svg` conforme o tipo.
- `ItemAcervo.getCampoEspecificoValor()` retorna ISBN (livro), ISSN (revista), duração (DVD) ou DOI (digital).
- `Leitor.getLimiteExemplaresSimultaneos()` retorna 3 (aluno) ou 5 (professor).
- `Leitor.getPrazoEmprestimoDias()` retorna 7 (aluno) ou 14 (professor).

No serviço de empréstimos (`EmprestimoService`), o limite é determinado dinamicamente pelo tipo do leitor, sem necessidade de `if` sobre classes concretas em todo o sistema.

---

## 4. Abstração

`ItemAcervo` e `Leitor` são classes abstratas com responsabilidades reais: não podem ser instanciadas diretamente e definem um contrato que todas as subclasses devem cumprir. Não existem classes abstratas criadas apenas para cumprir requisito acadêmico.

---

## 5. Interfaces

O sistema utiliza interfaces do Spring e do Java como contratos:

- `org.springframework.jdbc.core.RowMapper<T>` — mapeamento de linhas JDBC para entidades.
- `org.springframework.web.socket.WebSocketHandler` — canal de notificações.
- Repositórios Spring (`@Repository`) encapsulam o acesso a dados.

---

## 6. Relacionamentos entre classes

- **Muitos-para-muitos:** Material ↔ Autor (tabela `item_acervo_autor`).
- **Um-para-muitos:** Categoria → Materiais; Item → Exemplares; Leitor → Empréstimos/Reservas/Multas; Exemplar → Empréstimos; Empréstimo → Multa; Multa → Pagamentos.

---

## 7. DTOs e separação de camadas

Controllers trabalham com DTOs (`ItemAcervoDTO`, `LeitorDTO`, `LoginDTO`, `TrocaSenhaDTO`), que transportam dados entre camadas sem expor entidades de domínio. Isso mantém o modelo interno protegido e facilita a validação com Jakarta Bean Validation.

---

## 8. Conclusão

Os conceitos de POO estão presentes no código de forma justificada pelo domínio da biblioteca, não de forma artificial. Herança, polimorfismo, encapsulamento e abstração são usados para modelar materiais, leitores e operações de empréstimo de maneira coesa e extensível.
