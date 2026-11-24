-- Bublo - Migration V2
-- 1) Estorno de pagamento passa a ser registrado no próprio pagamento (histórico preservado).
-- 2) ISBN/ISSN únicos apenas entre materiais ativos: material excluído (inativo) não bloqueia novo cadastro.

ALTER TABLE pagamentos
    ADD COLUMN estornado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estornado_em TIMESTAMP,
    ADD COLUMN estornado_por VARCHAR(100),
    ADD COLUMN estorno_justificativa TEXT;

DROP INDEX IF EXISTS idx_item_acervo_isbn;
DROP INDEX IF EXISTS idx_item_acervo_issn;
CREATE UNIQUE INDEX idx_item_acervo_isbn ON item_acervo(isbn) WHERE isbn IS NOT NULL AND ativo;
CREATE UNIQUE INDEX idx_item_acervo_issn ON item_acervo(issn) WHERE issn IS NOT NULL AND ativo;
