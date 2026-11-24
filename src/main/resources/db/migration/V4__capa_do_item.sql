-- Bublo - Migration V4
-- Capa opcional do item do acervo: a imagem fica no próprio banco (BYTEA), assim acompanha o backup
-- e não depende de volume de arquivos no contêiner. A versão (capa_atualizada_em) serve para o
-- navegador buscar de novo quando a capa é trocada.
ALTER TABLE item_acervo
    ADD COLUMN capa BYTEA,
    ADD COLUMN capa_tipo VARCHAR(30),
    ADD COLUMN capa_atualizada_em TIMESTAMP;
