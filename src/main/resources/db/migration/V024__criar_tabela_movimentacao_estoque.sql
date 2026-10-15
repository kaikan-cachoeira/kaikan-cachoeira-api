CREATE TABLE movimentacao_estoque (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estoque_id BIGINT NOT NULL,
    tipo_movimentacao_id INTEGER NOT NULL,
    evento_id BIGINT,
    doacao_id BIGINT,
    item_venda_id BIGINT,
    quantidade INTEGER NOT NULL,
    custo_unitario DECIMAL(10, 2),
    observacoes TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimentacao_estoque_estoque
        FOREIGN KEY (estoque_id)
        REFERENCES estoque(id),

    CONSTRAINT fk_movimentacao_estoque_tipo_movimentacao
        FOREIGN KEY (tipo_movimentacao_id)
        REFERENCES tipo_movimentacao(id),

    CONSTRAINT fk_movimentacao_estoque_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento(id),

    CONSTRAINT fk_movimentacao_estoque_doacao
        FOREIGN KEY (doacao_id)
        REFERENCES doacao(id),

    CONSTRAINT fk_movimentacao_estoque_item_venda
        FOREIGN KEY (item_venda_id)
        REFERENCES item_venda(id),

    CONSTRAINT chk_mov_quantidade
        CHECK (quantidade != 0),

    CONSTRAINT chk_mov_custo_unitario
        CHECK (custo_unitario > 0),

    CONSTRAINT chk_mov_origem_unica
        CHECK (doacao_id IS NULL OR item_venda_id IS NULL),

    CONSTRAINT uq_mov_item_venda_tipo_lote
        UNIQUE (item_venda_id, tipo_movimentacao_id, estoque_id),

    CONSTRAINT uq_mov_doacao_tipo
        UNIQUE (doacao_id, tipo_movimentacao_id)
);