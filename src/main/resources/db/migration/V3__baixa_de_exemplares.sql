-- Bublo - Migration V3
-- Exemplar com histórico de empréstimos não pode ser apagado (chave estrangeira): recebe "baixa",
-- some das listas e contagens, mas o histórico e o tombamento (que nunca é reaproveitado) permanecem.
ALTER TABLE exemplares
    ADD COLUMN baixado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN baixado_em TIMESTAMP,
    ADD COLUMN baixa_motivo TEXT;

CREATE INDEX idx_exemplar_baixado ON exemplares(baixado);
