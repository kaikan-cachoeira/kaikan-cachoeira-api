CREATE TABLE venda (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id BIGINT NOT NULL,
    desconto DECIMAL(10, 2) NOT NULL DEFAULT 0,
    preco_total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    cancelada_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_venda_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento(id),

    CONSTRAINT chk_venda_desconto
        CHECK (desconto >= 0),

    CONSTRAINT chk_venda_preco_total
        CHECK (preco_total >= 0)
);