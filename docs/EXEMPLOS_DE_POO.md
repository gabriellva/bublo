# Exemplos de POO no Bublo

Cada conceito abaixo tem: **definição curta**, **onde está no código** e **um exemplo que você consegue contar passo a passo**. Todos os trechos são do projeto (pacote `com.gabriel.bublo`).

Complementa `RELATORIO_POO.md` (visão geral) e `GUIA_DE_ESTUDO_E_APRESENTACAO.md` (Seção 8).

---

## 1. Herança

**O que é:** uma classe (filha) reaproveita atributos e métodos de outra (mãe) com `extends`, e acrescenta o que é só dela.

**Onde:** `ItemAcervo` → `Livro`, `Revista`, `DVD`, `PublicacaoDigital`; `Leitor` → `Aluno`, `Professor`.

```
ItemAcervo (abstrata)                     Leitor (abstrata)
 ├─ Livro               + isbn             ├─ Aluno
 ├─ Revista             + issn, volume     └─ Professor
 ├─ DVD                 + duracaoMinutos
 └─ PublicacaoDigital   + doi, urlExterna
```

```java
public abstract class ItemAcervo {            // título, editora, ano, autores... (comum a todos)
    private String titulo;
    private String editora;
    // getters e setters
}

public class Livro extends ItemAcervo {       // herda tudo acima
    private String isbn;                      // e acrescenta só o que é de livro
}
```

**Exemplo para contar:** "`Livro` não declara `titulo` nem `editora`, mas `livro.getTitulo()` funciona, porque herdou de `ItemAcervo`. O ISBN existe só no livro; o DOI só na publicação digital."

**Por que ajuda:** não repetimos título/editora/ano quatro vezes. Mudou um campo comum, muda em um lugar só.

---

## 2. Abstração (classe e método abstratos)

**O que é:** uma classe `abstract` **não pode ser instanciada** (`new ItemAcervo()` não compila): ela só define o contrato. Um método `abstract` não tem corpo e **obriga** cada filha a implementá-lo.

```java
public abstract class Leitor {
    public abstract int getLimiteExemplaresSimultaneos();   // sem corpo: cada filha responde
    public abstract int getPrazoEmprestimoDias();
}
```

**Exemplo para contar:** "Não existe 'um leitor genérico' na biblioteca: existe um aluno ou um professor. Por isso `Leitor` é abstrata. Se alguém criar uma nova subclasse e esquecer de definir o limite, o código nem compila."

---

## 3. Sobrescrita (*override*)

**O que é:** a filha **redefine** um método herdado, com a **mesma assinatura** (mesmo nome e mesmos parâmetros). Usa-se `@Override`. Quem decide qual versão roda é o **tipo real do objeto, em execução**.

```java
// Aluno                                          // Professor
@Override                                         @Override
public int getLimiteExemplaresSimultaneos() {     public int getLimiteExemplaresSimultaneos() {
    return 3;                                         return 5;
}                                                 }
@Override                                         @Override
public int getPrazoEmprestimoDias() {             public int getPrazoEmprestimoDias() {
    return 7;                                         return 14;
}                                                 }
```

Outro exemplo, nos materiais (`Revista` monta o texto de um jeito, `Livro` de outro):

```java
// Livro
@Override public String getCampoEspecificoLabel() { return "ISBN"; }
@Override public String getCampoEspecificoValor() { return isbn; }

// Revista
@Override public String getCampoEspecificoLabel() { return "ISSN"; }
@Override public String getCampoEspecificoValor() {
    return (issn != null ? issn : "") + (volume != null ? " · Vol. " + volume : "");
}
```

**Também sobrescrevemos métodos de framework:** `NotificationWebSocketHandler` sobrescreve `afterConnectionEstablished` e `afterConnectionClosed` (do Spring) para guardar/remover as conexões do sino em tempo real.

**Exemplo para contar:** "`Aluno` e `Professor` sobrescrevem o mesmo método e devolvem valores diferentes (3 e 5)."

**Teste:** `PolimorfismoTest`.

---

## 4. Polimorfismo

**O que é:** tratar objetos de classes diferentes **pelo tipo da mãe** e cada um responder do seu jeito. É a sobrescrita **em ação**: quem chama não sabe (e não precisa saber) qual é a filha.

**Onde é usado de verdade:** `EmprestimoService`.

```java
private int limiteDoLeitor(Leitor leitor) {              // recebe "um Leitor" (aluno OU professor)
    return configuracaoService.buscarInteiro(
        leitor.getChaveConfiguracaoLimite(),             // "limite_alunos" ou "limite_professores"
        leitor.getLimiteExemplaresSimultaneos());        // 3 ou 5, conforme o objeto real
}
```

Passo a passo:

1. O bibliotecário registra um empréstimo para **Bruno** (um `Aluno`).
2. O serviço chama `limiteDoLeitor(bruno)`. O parâmetro é do tipo `Leitor`.
3. Na execução, o Java vê que o objeto é um `Aluno` e roda a versão do `Aluno`: devolve 3.
4. Para uma `Professora`, a **mesma linha de código** devolve 5.

**Sem polimorfismo** ficaria assim (o que existia antes), e cada novo tipo de leitor obrigaria a mexer no serviço:

```java
return leitor instanceof Professor ? limiteProfessor : limiteAluno;
```

**Outro uso:** o repositório escolhe a subclasse a partir da coluna `tipo` do banco (`new Professor()` ou `new Aluno()`), e o resto do sistema trata o resultado como `Leitor`.

---

## 5. Sobrecarga (*overload*)

**O que é:** **vários métodos com o mesmo nome e parâmetros diferentes** (quantidade ou tipos), na mesma classe. Quem decide qual chamar é o **compilador, pelos argumentos**. Não precisa de herança nem de `@Override`.

```java
// EmprestimoService
static long diasDeAtraso(LocalDateTime prevista) {                    // versão 1: usa a data de hoje
    return diasDeAtraso(prevista, LocalDate.now());
}

static long diasDeAtraso(LocalDateTime prevista, LocalDate hoje) {    // versão 2: data explícita
    if (prevista == null) return 0;
    return Math.max(0, ChronoUnit.DAYS.between(prevista.toLocalDate(), hoje));
}
```

Uso:

```java
diasDeAtraso(e.getDataPrevistaDevolucao());                 // no sistema: compara com hoje
diasDeAtraso(LocalDate.of(2026,11,12).atStartOfDay(),
             LocalDate.of(2026,11,13));                     // no teste: data fixa, resultado previsível
```

Segundo exemplo, em `ReservaService`:

```java
private ReservaDTO toDTO(Reserva r)                                  // converte uma reserva
private ReservaDTO toDTO(Reserva r, Map<Long,Integer> livresPorItem) // idem, reaproveitando um cache
```

A de um parâmetro chama a de dois. A lista de reservas usa a de dois para consultar os exemplares livres **uma vez por título**, e não uma vez por linha.

**Teste:** `EmprestimoServiceTest.diasDeAtrasoSobrecargaUsaHoje`.

---

## 6. Sobrescrita × sobrecarga (a pergunta clássica)

| | **Sobrescrita** (*override*) | **Sobrecarga** (*overload*) |
|---|---|---|
| Onde | Entre **mãe e filha** | Na **mesma classe** |
| Assinatura | **Igual** | **Diferente** |
| Quem escolhe | Tipo real do objeto, em **execução** | Argumentos, em **compilação** |
| Marca | `@Override` | Nenhuma |
| No projeto | `Aluno`/`Professor.getLimiteExemplaresSimultaneos()` | `diasDeAtraso(prevista)` e `diasDeAtraso(prevista, hoje)` |

Frase para a banca: **"sobrescrita é escolher por quem eu sou; sobrecarga é escolher pelo que eu passo."**

---

## 7. Encapsulamento

**O que é:** esconder o estado interno (`private`) e expor só o necessário por métodos.

```java
public class Exemplar {
    private String tombamento;                           // ninguém mexe direto
    public String getTombamento() { return tombamento; }
}
```

Na regra de negócio: o tombamento (`BA-000016`) não é editável na tela; é gerado pelo repositório. `Multa` tem comportamento próprio: `getSaldoPendente()` = `valor − valorPago`.

---

## 8. Interfaces

Contratos que o projeto **implementa ou usa**: `RowMapper<T>` (converte linha do banco em objeto), `WebSocketHandler`/`WebSocketConfigurer` (canal do sino), `CommandLineRunner` (roda o `DemoDataConfig` na subida). A classe se compromete a ter certos métodos; quem a usa só conhece o contrato.

---

## 9. Model × DTO (por que existem duas classes para o "mesmo" leitor)

| | **Model** (`model/Leitor`) | **DTO** (`dto/LeitorDTO`) |
|---|---|---|
| Papel | O **conceito do domínio**: o que é um leitor e como ele se comporta | Uma **caixa de dados** para viajar entre tela e serviço |
| Tem regras/comportamento? | Sim (limite, prazo, herança, `Aluno`/`Professor`) | Não, só campos, getters/setters e validações de formulário |
| Tipo dos campos | Tipos ricos: `TipoLeitor` (enum), `LocalDateTime`, listas de empréstimos/multas | Simples: `String tipo`, `Boolean ativo` |
| Quem usa | `service/` e `repository/` | `controller/`, templates (HTML) e `service/` |
| Hierarquia | Abstrata, com `Aluno` e `Professor` | Uma classe só, com um campo `tipo` |

Caminho de um cadastro de leitor:

```
Formulário HTML ──► LeitorDTO ──► LeitorService ──► Leitor (Aluno/Professor) ──► Repository ──► banco
   (textos)        (validado)    (regras)           (domínio)                   (SQL)
```

No código (`LeitorService.salvar`): o serviço recebe o `LeitorDTO`, valida e normaliza, escolhe a subclasse (`new Aluno()` ou `new Professor()`), copia os campos para o model e manda salvar. Na volta, `toDTO(Leitor)` copia o model para um DTO e a tela o exibe.

**Por que não usar o model direto na tela?**

1. **Separa tela e domínio:** mudar um campo do formulário não mexe nas regras, e vice-versa.
2. **Valida na entrada:** o DTO carrega `@NotBlank`, `@Email`, `@Size`. O model fica sem anotações de tela.
3. **Expõe só o necessário:** o model tem listas de empréstimos, multas e reservas; o DTO não leva nada disso para o HTML.
4. **Resolve o abstrato:** o formulário não consegue fabricar um `Leitor` (abstrato). Ele preenche um DTO com `tipo = "ALUNO"`, e o serviço decide qual subclasse criar.
5. **Dado calculado só para a tela:** `ReservaDTO.prontaParaRetirada` (o selo "Pode retirar") não existe no model `Reserva`; é calculado pelo serviço só para exibir.
