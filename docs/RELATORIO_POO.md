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

No serviço de empréstimos (`EmprestimoService`), o limite e o prazo são obtidos chamando os métodos do próprio leitor, sem `if`/`instanceof` sobre classes concretas:

```java
private int limiteDoLeitor(Leitor leitor) {
    return configuracaoService.buscarInteiro(
        leitor.getChaveConfiguracaoLimite(),        // "limite_alunos" ou "limite_professores"
        leitor.getLimiteExemplaresSimultaneos());   // padrão do tipo (3 ou 5)
}
```

A configuração do administrador sobrepõe o padrão do tipo; se não houver configuração, vale o que a subclasse define.

### 3.1 Sobrescrita (*override*)

A subclasse redefine um método herdado, com a **mesma assinatura** (`@Override`). A versão executada é escolhida pelo **tipo real do objeto**, em execução. Exemplos: `Aluno`/`Professor` sobrescrevem `getLimiteExemplaresSimultaneos()`, `getPrazoEmprestimoDias()`, `getChaveConfiguracaoLimite()` e `getChaveConfiguracaoPrazo()`; `Livro`, `Revista`, `DVD` e `PublicacaoDigital` sobrescrevem `getCapaGenerica()` e `getCampoEspecifico*()`. Teste: `PolimorfismoTest`.

### 3.2 Sobrecarga (*overload*)

Métodos com o **mesmo nome e parâmetros diferentes**, escolhidos pelo compilador conforme os argumentos. Exemplos:

- `EmprestimoService.diasDeAtraso(LocalDateTime prevista)` (usa hoje) e `diasDeAtraso(LocalDateTime prevista, LocalDate hoje)` (data explícita, usada nos testes);
- `ReservaService.toDTO(Reserva)` e `toDTO(Reserva, Map<Long,Integer>)` (com cache de exemplares livres).

Teste: `EmprestimoServiceTest.diasDeAtrasoSobrecargaUsaHoje`.

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
