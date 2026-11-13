# Documentação de Testes — Bublo

## Estratégia de testes

- **Testes unitários:** JUnit 5 + Mockito, focados em Services e regras de negócio.
- **Testes de integração:** Spring Boot Test com PostgreSQL real (via Docker no CI).
- **Testes de interface:** Playwright para Java (planejado; execução depende de navegadores instalados).

## Testes unitários implementados

| Classe testada | Cenários |
|----------------|----------|
| `MultaServiceTest` | Pagamento integral quita multa; pagamento parcial; valor maior que saldo rejeitado; estorno volta multa para pendente; saldo pendente do leitor; bloqueio por saldo; justificativa obrigatória para estorno. |
| `ConfiguracaoServiceTest` | Salvamento registra auditoria com antes/depois; justificativa obrigatória; listagem de configurações. |
| `LeitorServiceTest` | E-mail duplicado rejeitado; e-mail disponível permite cadastro com matrícula automática; resultado de importação CSV. |
| `BubloApplicationTests` | Classe da aplicação instanciável. |

## Execução

```bash
mvn test
```

Resultado esperado: `Tests run: 15, Failures: 0, Errors: 0`.

## Cobertura

Meta de pelo menos 70% nas regras de negócio (Services e validações). Não há cobertura artificial em código sem relevância.

## Testes de integração

No GitHub Actions (`.github/workflows/ci.yml`), o PostgreSQL sobe como serviço e as migrations Flyway são validadas na inicialização da aplicação. Testes de integração exercitam persistência real no PostgreSQL.

## Testes de interface (planejados)

Fluxos E2E obrigatórios (conforme prompt mestre):
- Login
- Consulta e cadastro de materiais
- Cadastro de leitores
- Importação CSV
- Empréstimos
- Devoluções
- Reservas
- Multas
- Pagamentos
- Relatórios

Executados em Chrome, Edge e Firefox, em temas claro e escuro, e em responsividade desktop/celular.
