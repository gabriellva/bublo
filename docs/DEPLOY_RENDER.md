# Deploy do Bublo no Render

Este documento explica o que é o Render, como ele executa a aplicação com Docker, como o Bublo foi configurado nele e quais problemas apareceram durante o deploy.

---

## 1. O que é o Render

O [Render](https://render.com) é uma plataforma de hospedagem (PaaS). Você conecta um repositório do GitHub e ele cuida de:

- baixar o código, compilar e subir a aplicação;
- dar uma URL pública com HTTPS (`https://nome.onrender.com`);
- manter a aplicação rodando e reiniciá-la se ela cair;
- hospedar um banco PostgreSQL gerenciado;
- refazer o deploy automaticamente a cada `git push`.

Não é preciso configurar servidor, instalar Java nem administrar Linux.

### Conceitos usados neste projeto

| Conceito | O que é | No Bublo |
|---|---|---|
| **Workspace / Project** | Agrupamento de recursos na conta | `My Workspace` / `My project` |
| **Web Service** | Aplicação que responde HTTP | O Bublo (Spring Boot) |
| **PostgreSQL** | Banco de dados gerenciado | `bublo-db` |
| **Região** | Datacenter onde o recurso roda | Oregon (a mesma para banco e web service) |
| **Environment Variables** | Configurações injetadas no container | Credenciais do banco e do admin |
| **Deploy** | Uma versão do código sendo construída e publicada | Um por push ou manual |

---

## 2. Como o Render usa Docker

O Render aceita vários tipos de serviço (Node, Python, Java etc.). O Bublo usa o tipo **Docker**, porque o repositório já tem um `Dockerfile` na raiz.

O fluxo de um deploy é:

```
git push  ->  Render clona o repositório  ->  docker build (Dockerfile)
          ->  sobe o container  ->  espera a porta abrir  ->  tráfego liberado
```

1. **Clone:** o Render baixa a branch configurada (aqui, `master`).
2. **Build:** executa o `Dockerfile` e gera a imagem.
3. **Run:** inicia um container dessa imagem com as variáveis de ambiente configuradas no painel.
4. **Port scan:** o Render procura uma porta aberta no container. Se nenhuma abrir, o deploy falha com "No open ports detected".
5. **Troca de versão:** quando o novo container está de pé, o tráfego passa a ir para ele.

### O Dockerfile do projeto

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build      # estágio 1: compila
ENV MAVEN_OPTS=-Xmx512m
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline             # baixa dependências (fica em cache)
COPY src ./src
RUN mvn -q -B -DskipTests package               # gera o .jar

FROM eclipse-temurin:21-jre                     # estágio 2: só o necessário para rodar
WORKDIR /app
COPY --from=build /workspace/target/bublo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Pontos importantes:

- **Multi-stage build:** o primeiro estágio tem Maven e JDK para compilar. O segundo tem só o JRE e o `.jar`, então a imagem final é bem menor.
- **Cache de camadas:** o `pom.xml` é copiado antes do `src`. Se só o código mudar, o Docker reaproveita o download das dependências, e os deploys seguintes ficam mais rápidos. É por isso que o log mostra `CACHED` em vários passos.
- **O Dockerfile não lê variáveis.** Ele apenas executa `java -jar app.jar`. Quem lê as variáveis de ambiente é o Spring, pelo `application.yaml`.

---

## 3. Como a configuração chega na aplicação

O `application.yaml` usa a sintaxe `${VARIAVEL}`, **sem valor padrão**:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

- O valor vem sempre do ambiente: do `.env` localmente e do painel de variáveis no Render. O mesmo código roda nos dois, sem alteração.
- Nenhum endereço, usuário ou senha fica escrito no código.
- Se uma variável estiver com o nome errado ou não existir, o app **não sobe** e o log aponta qual faltou (`Could not resolve placeholder 'DB_URL'`). Isso evita o erro silencioso de cair num valor padrão sem perceber.

---

## 4. Passo a passo da configuração

### 4.1. Criar o banco

1. **New → PostgreSQL**.
2. Nome `bublo-db`, plano **Free**, uma região (Oregon).
3. Aguardar o status **Available**.
4. Em **Info → Connections**, anotar:
   - **Hostname** (interno), por exemplo `dpg-xxxxxxxx-a`
   - **Port**: `5432`
   - **Database**: `bublo`
   - **Username**: `bublo`
   - **Password**

### 4.2. Criar o Web Service

1. **New → Web Service** e conectar o repositório do GitHub.
2. **Language:** Docker (o Render detecta o `Dockerfile` da raiz).
3. **Branch:** `master`.
4. **Região:** a mesma do banco.
5. **Plano:** Free.

### 4.3. Variáveis de ambiente

| Variável | Valor | Para quê |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://<hostname>:5432/<database>` | Endereço do banco |
| `DB_USERNAME` | `bublo` | Usuário do banco |
| `DB_PASSWORD` | senha do banco | Senha do banco |
| `ADMIN_USERNAME` | `admin` | Usuário inicial do sistema |
| `ADMIN_PASSWORD` | senha forte | Senha inicial do sistema |
| `DEMO_DATA_ENABLED` | `true` | Carrega dados de demonstração |
| `PORT` | `8080` | Porta que o Render deve esperar |
| `JAVA_TOOL_OPTIONS` | `-Xmx384m` | Limita a memória do Java |

Exemplo para o hostname `dpg-xxxxxxxx-a` e database `bublo`:

```
DB_URL=jdbc:postgresql://dpg-xxxxxxxx-a:5432/bublo
```

Por que cada uma:

- **`PORT=8080`:** o Render espera, por padrão, a porta 10000. O Spring escuta na 8080, então avisamos o Render.
- **`JAVA_TOOL_OPTIONS=-Xmx384m`:** o plano Free tem 512 MB de RAM. Sem limitar o heap, a JVM pode usar mais do que isso e o container é encerrado por falta de memória.
- **Hostname interno:** só funciona entre recursos da mesma região, mas é mais rápido e não passa pela internet.

### 4.4. Primeiro start

Ao subir, o Spring executa o **Flyway**, que cria as tabelas do banco a partir de `src/main/resources/db/migration`. Com `DEMO_DATA_ENABLED=true`, os dados de demonstração são carregados na primeira inicialização. O deploy deu certo quando o log mostra algo como:

```
Started BubloApplication in ... seconds
```

Depois disso é só abrir a URL `https://<nome>.onrender.com` e entrar com `ADMIN_USERNAME` e `ADMIN_PASSWORD`.

---

## 5. Problemas que apareceram e como resolver

| Erro no log | Causa | Correção |
|---|---|---|
| `Driver org.postgresql.Driver claims to not accept jdbcUrl, postgresql://...` | Foi colada a "Internal Database URL" do Render em `DB_URL`. Ela não começa com `jdbc:` e traz usuário e senha dentro | Usar `jdbc:postgresql://host:5432/database` e colocar usuário e senha em variáveis separadas |
| `password authentication failed for user "bublo"` | Usuário ou senha diferentes dos do banco | Copiar o **Username** e o **Password** da tela do banco e conferir `DB_USERNAME` e `DB_PASSWORD` |
| Mesmo erro, com `DB_USERNAME=bublo-db` | `bublo-db` é o **nome do serviço** no Render, não o usuário do banco | Usar o campo **Username** (`bublo`) |
| Banco não encontrado, ou `database "bublo-db" does not exist` | Mesmo engano: nome do serviço no lugar do campo **Database** | Usar o campo **Database** (`bublo`) no fim do `DB_URL` |
| `No open ports detected` | O app caiu antes de abrir a porta, ou o Render esperava outra porta | Ver o erro real mais acima no log e conferir `PORT=8080` |

Regra prática: **nome do serviço no Render, nome do banco e usuário do banco são três coisas diferentes.** Os valores certos estão sempre em Info → Connections.

---

## 6. Como acompanhar e operar

- **Logs:** no web service, aba **Logs**, em tempo real. É o primeiro lugar para olhar quando algo falha.
- **Redeploy manual:** **Manual Deploy → Deploy latest commit**.
- **Alterar variáveis:** aba **Environment** e salvar. O Render reinicia o serviço com os novos valores (sem rebuild).
- **Deploy automático:** com **Auto-Deploy** ligado, todo push na branch configurada gera um novo deploy.
- **Reiniciar:** **Manual Deploy → Restart** (ou recriar o container a partir da última imagem).

---

## 7. Limites do plano Free

Os valores podem mudar, então confira sempre no painel do Render.

- O web service **dorme após cerca de 15 minutos sem acesso**. O primeiro acesso depois disso demora cerca de 1 minuto para acordar.
- Memória de **512 MB**, por isso o `-Xmx384m`.
- O banco PostgreSQL gratuito **expira depois de um período** (hoje, cerca de 30 dias) e depois disso precisa ser recriado.
- Não há backup automático no plano gratuito.

Dica para apresentações: abra o site uns minutos antes para ele já estar acordado.

---

## 8. Local x Render

| | Local (docker-compose) | Render |
|---|---|---|
| Banco | container `postgres:15-alpine` | PostgreSQL gerenciado |
| Configuração | `environment:` no compose / `.env` | Environment Variables do painel |
| Acesso | `http://localhost:8090` | `https://<nome>.onrender.com` |
| HTTPS | não | sim, automático |
| Como sobe | `docker compose -f docker-compose.app.yml up -d --build` | push no GitHub |

O **mesmo `Dockerfile`** é usado nos dois ambientes. O que muda é só de onde vêm as variáveis de ambiente.

---

## 9. Possível evolução: `render.yaml`

Em vez de criar tudo pelo painel, é possível descrever banco, web service e variáveis em um arquivo `render.yaml` na raiz do repositório (**Blueprint**). O Render lê o arquivo e cria os recursos sozinho, com a conexão do banco ligada automaticamente.

Como o Render entrega host, porta, usuário e senha separados, isso exigiria que o `application.yaml` montasse a URL `jdbc:` a partir de `DB_HOST`, `DB_PORT` e `DB_NAME`. Nesta versão, optou-se por configurar pelo painel, sem alterar o código.

---

## 10. Boas práticas de segurança

- **Nunca** versionar senhas no repositório. As credenciais ficam só nas variáveis de ambiente do Render.
- Trocar `ADMIN_PASSWORD` por uma senha forte se o site ficar acessível publicamente.
- Não colar logs ou prints com senhas em lugares públicos (issues, README, chats).
