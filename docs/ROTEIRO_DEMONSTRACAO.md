# Roteiro de Demonstração — Bublo

Duração sugerida: 5 a 10 minutos. Ambiente: dados de demonstração carregados na primeira inicialização.

## Sequência

| Passo | Ação | O que demonstrar |
|-------|------|------------------|
| 1 | Acessar `http://localhost:8080/login` | Tela dividida, tema vinho, símbolo de livro, nome da instituição configurável |
| 2 | Login (admin / senha do .env) | Spring Security, BCrypt, sessão por cookie |
| 3 | Dashboard | 9 indicadores, gráficos (linha e rosca), alternar tema claro/escuro |
| 4 | Acervo → buscar | Cards, filtros avançados, paginação, ordenação A–Z |
| 5 | Acervo → cadastrar material | Formulário com campos específicos por tipo (ISBN/ISSN/DOI/duração), autores com sugestão |
| 6 | Leitores → cadastrar | Aluno ou professor, matrícula automática gerada |
| 7 | Leitores → importar CSV | Download do modelo, importação imediata, relatório de rejeitados |
| 8 | Empréstimos → novo | Selecionar leitor, verificar bloqueio, escolher exemplares, data prevista, confirmação transacional |
| 9 | Devoluções → registrar | Busca por tombamento/matricula, resumo, cálculo automático de multa por atraso |
| 10 | Reservas → nova | Fila FIFO, posição, prazo de retirada |
| 11 | Multas → pagar | Pagamento integral/parcial, saldo restante |
| 12 | Relatórios | Exportar CSV e XLSX |
| 13 | Configurações | Alterar prazo/taxa com justificativa, auditoria |
| 14 | Auditoria | Log cronológico somente leitura |
| 15 | POO | Mostrar UML (`docs/UML.md`), DER (`docs/DER.md`) e explicar herança `ItemAcervo`/`Leitor` e polimorfismo |

## Conceitos acadêmicos a explicar

- Herança e polimorfismo em `ItemAcervo` (Livro, Revista, DVD, Digital) e `Leitor` (Aluno, Professor).
- Encapsulamento via entidades com campos privados e DTOs.
- Abstração em classes abstratas com contratos reais.
- Relacionamentos muitos-para-muitos (materiais ↔ autores) e um-para-muitos.
- Transações e `SELECT … FOR UPDATE` para concorrência em empréstimos.
- Persistência JDBC puro (sem JPA/Hibernate).

## Critério de sucesso

Todos os fluxos acima funcionam com os dados de demonstração, sem depender de serviços externos, e a interface apresenta temas claro/escuro e responsividade.
