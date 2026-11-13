# Containerização do Bublo

O Bublo roda em containers Docker: não é preciso instalar Java 21, Maven nem PostgreSQL na máquina.

## Arquivos

| Arquivo | Função |
|---|---|
| `Dockerfile` | Build multi-stage da aplicação |
| `docker-compose.app.yml` | Sobe a aplicação e o PostgreSQL juntos |
| `docker-compose.yml` | Sobe apenas o PostgreSQL (para rodar a aplicação fora do Docker) |
| `scripts/smoke-test.sh` | Percorre as rotas autenticadas e valida as respostas |

## Dockerfile

Build em dois stages:

1. **build** (`maven:3.9-eclipse-temurin-21`)
   - Copia apenas o `pom.xml` e executa `dependency:go-offline`. As dependências ficam em uma camada de cache e só são baixadas de novo quando o `pom.xml` muda.
   - Copia `src/` e gera o jar com `mvn package -DskipTests`.
   - `MAVEN_OPTS=-Xmx512m` limita a memória do Maven.
2. **runtime** (`eclipse-temurin:21-jre`)
   - Contém apenas o JRE e o jar. Sem Maven e sem código-fonte.
   - Expõe a porta 8080 e executa `java -jar app.jar`.

## docker-compose.app.yml

### Serviço `postgres`
- Imagem `postgres:15-alpine`, volume nomeado `bublo_work_pgdata` e healthcheck com `pg_isready`.
- Publicado em `127.0.0.1:5433`, apenas na máquina local.
- Limite de memória: 256 MB.

### Serviço `app`
- Construído a partir do `Dockerfile`; só inicia depois que o PostgreSQL está saudável.
- Publicado em `127.0.0.1:8090`.
- Limite de memória: 640 MB, com `-Xmx384m` na JVM.
- O Flyway cria o schema na primeira inicialização.

### Variáveis de ambiente

| Variável | Valor no compose | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://postgres:5432/bublo` | Conexão com o banco |
| `DB_USERNAME` / `DB_PASSWORD` | `bublo` / `bublo` | Credenciais do banco |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `Mudar@1234` | Administrador inicial |
| `DEMO_DATA_ENABLED` | `true` | Carrega dados de demonstração |

> As credenciais acima servem apenas para desenvolvimento local. Em qualquer outro ambiente, defina valores próprios e não versione segredos.

## Como usar

Subir (o primeiro build leva cerca de 2,5 minutos):

```bash
docker compose -p bublo-work -f docker-compose.app.yml up -d --build
```

Acessar: <http://localhost:8090> (usuário `admin`, senha `Mudar@1234`).

Derrubar mantendo o banco:

```bash
docker compose -p bublo-work -f docker-compose.app.yml down
```

Derrubar apagando também o banco:

```bash
docker compose -p bublo-work -f docker-compose.app.yml down -v
```

O parâmetro `-p bublo-work` dá um prefixo próprio a containers, rede e volume, evitando colisão com outros projetos Docker na mesma máquina.

## Testes

Os testes não rodam durante o build da imagem (`-DskipTests`). Para executá-los em um container, com cache do Maven em um volume nomeado:

```bash
docker run --rm --memory 1g -e MAVEN_OPTS=-Xmx512m \
  -v bublo-m2:/root/.m2 -v "$PWD":/w -w /w \
  maven:3.9-eclipse-temurin-21 mvn -B -q test
```

Com a aplicação rodando, o smoke test valida as rotas:

```bash
ADMIN_PASSWORD=Mudar@1234 BASE_URL=http://localhost:8090 scripts/smoke-test.sh
```

## Observações

- Em máquinas com pouca memória, execute um processo pesado por vez: build, testes e aplicação.
- Os containers criam arquivos como `root` na pasta `target/` do projeto; ela já está no `.gitignore`.
