-- Bublo - Esquema inicial do banco de dados
-- Flyway migration V1
-- Toda persistência é feita com JDBC puro (sem JPA/Hibernate)

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE configuracoes (
    chave VARCHAR(100) PRIMARY KEY,
    valor TEXT NOT NULL,
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_por VARCHAR(100)
);

CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE autores (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    nome_normalizado VARCHAR(150) NOT NULL UNIQUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE item_acervo (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('LIVRO','REVISTA','DVD','DIGITAL')),
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    editora VARCHAR(150),
    ano INTEGER,
    data_publicacao DATE,
    paginas INTEGER,
    volume INTEGER,
    idioma VARCHAR(50) NOT NULL DEFAULT 'Português',
    categoria_id BIGINT REFERENCES categorias(id),
    isbn VARCHAR(50),
    issn VARCHAR(50),
    duracao_minutos INTEGER,
    doi VARCHAR(200),
    url_externa VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_item_acervo_isbn ON item_acervo(isbn) WHERE isbn IS NOT NULL;
CREATE UNIQUE INDEX idx_item_acervo_issn ON item_acervo(issn) WHERE issn IS NOT NULL;

CREATE TABLE item_acervo_autor (
    item_acervo_id BIGINT NOT NULL REFERENCES item_acervo(id) ON DELETE CASCADE,
    autor_id BIGINT NOT NULL REFERENCES autores(id) ON DELETE CASCADE,
    PRIMARY KEY (item_acervo_id, autor_id)
);

CREATE TABLE exemplares (
    id BIGSERIAL PRIMARY KEY,
    item_acervo_id BIGINT NOT NULL REFERENCES item_acervo(id) ON DELETE CASCADE,
    tombamento VARCHAR(20) NOT NULL UNIQUE,
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes TEXT,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE leitores (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ALUNO','PROFESSOR')),
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    matricula VARCHAR(50) NOT NULL UNIQUE,
    cpf VARCHAR(20),
    endereco TEXT,
    telefone VARCHAR(30),
    curso_departamento VARCHAR(150),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE emprestimos (
    id BIGSERIAL PRIMARY KEY,
    leitor_id BIGINT NOT NULL REFERENCES leitores(id),
    exemplar_id BIGINT NOT NULL REFERENCES exemplares(id),
    data_emprestimo TIMESTAMP NOT NULL DEFAULT NOW(),
    data_prevista_devolucao TIMESTAMP NOT NULL,
    data_devolucao_real TIMESTAMP,
    renovacoes INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO','DEVOLVIDO','ATRASADO','CANCELADO')),
    observacoes TEXT
);

CREATE TABLE reservas (
    id BIGSERIAL PRIMARY KEY,
    leitor_id BIGINT NOT NULL REFERENCES leitores(id),
    item_acervo_id BIGINT NOT NULL REFERENCES item_acervo(id),
    data_reserva TIMESTAMP NOT NULL DEFAULT NOW(),
    posicao INTEGER NOT NULL,
    prazo_retirada TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO' CHECK (status IN ('AGUARDANDO','ATENDIDA','CANCELADA','EXPIRADA'))
);

CREATE UNIQUE INDEX uix_reserva_ativa_por_leitor_item ON reservas(leitor_id, item_acervo_id) WHERE status = 'AGUARDANDO';

CREATE TABLE multas (
    id BIGSERIAL PRIMARY KEY,
    emprestimo_id BIGINT REFERENCES emprestimos(id),
    leitor_id BIGINT NOT NULL REFERENCES leitores(id),
    valor NUMERIC(10,2) NOT NULL,
    taxa_diaria NUMERIC(10,2) NOT NULL,
    dias_atraso INTEGER NOT NULL,
    data_geracao TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','PAGA_PARCIAL','PAGA','CANCELADA')),
    valor_pago NUMERIC(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE pagamentos (
    id BIGSERIAL PRIMARY KEY,
    multa_id BIGINT NOT NULL REFERENCES multas(id),
    valor NUMERIC(10,2) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    data_pagamento TIMESTAMP NOT NULL DEFAULT NOW(),
    responsavel VARCHAR(100) NOT NULL
);

CREATE TABLE notificacoes (
    id BIGSERIAL PRIMARY KEY,
    categoria VARCHAR(50) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensagem TEXT,
    registro_tipo VARCHAR(50),
    registro_id INTEGER,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    resolvida BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_resolucao TIMESTAMP
);

CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,
    admin_usuario VARCHAR(100) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT NOW(),
    tipo_operacao VARCHAR(50) NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    registro_afetado VARCHAR(100),
    valores_antes TEXT,
    valores_depois TEXT,
    justificativa TEXT
);

-- Índices de apoio a consultas e relatórios
CREATE INDEX idx_item_acervo_titulo ON item_acervo(titulo);
CREATE INDEX idx_item_acervo_tipo ON item_acervo(tipo);
CREATE INDEX idx_item_acervo_categoria ON item_acervo(categoria_id);
CREATE INDEX idx_item_acervo_ativo ON item_acervo(ativo);

CREATE INDEX idx_exemplar_item ON exemplares(item_acervo_id);
CREATE INDEX idx_exemplar_disponivel ON exemplares(disponivel);

CREATE INDEX idx_leitores_tipo ON leitores(tipo);
CREATE INDEX idx_leitores_ativo ON leitores(ativo);

CREATE INDEX idx_emprestimos_leitor ON emprestimos(leitor_id);
CREATE INDEX idx_emprestimos_exemplar ON emprestimos(exemplar_id);
CREATE INDEX idx_emprestimos_status ON emprestimos(status);
CREATE INDEX idx_emprestimos_prevista ON emprestimos(data_prevista_devolucao);

CREATE INDEX idx_reservas_item ON reservas(item_acervo_id);
CREATE INDEX idx_reservas_leitor ON reservas(leitor_id);
CREATE INDEX idx_reservas_status ON reservas(status);

CREATE INDEX idx_multas_leitor ON multas(leitor_id);
CREATE INDEX idx_multas_status ON multas(status);

CREATE INDEX idx_notificacoes_lida ON notificacoes(lida);
CREATE INDEX idx_notificacoes_criacao ON notificacoes(data_criacao);

CREATE INDEX idx_auditoria_data_hora ON auditoria(data_hora);
CREATE INDEX idx_auditoria_modulo ON auditoria(modulo);
