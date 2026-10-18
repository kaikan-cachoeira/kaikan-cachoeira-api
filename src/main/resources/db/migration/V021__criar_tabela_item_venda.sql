CREATE TABLE item_venda (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venda_id BIGINT NOT NULL,
    produto_evento_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(10, 2) NOT NULL,
    desconto_unitario DECIMAL(10, 2) NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_item_venda_venda
        FOREIGN KEY (venda_id)
        REFERENCES venda(id),

    CONSTRAINT fk_item_venda_produto_evento
        FOREIGN KEY (produto_evento_id)
        REFERENCES produto_evento(id),

    CONSTRAINT chk_item_venda_quantidade
        CHECK (quantidade > 0),

    CONSTRAINT uq_item_venda_produto
        UNIQUE (venda_id, produto_evento_id),

    CONSTRAINT chk_item_venda_desconto_unitario
        CHECK (desconto_unitario >= 0 AND desconto_unitario <= preco_unitario)
);