CREATE TABLE estoque (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    quantidade_inicial INTEGER NOT NULL,
    quantidade_atual INTEGER NOT NULL,
    custo_unitario DECIMAL(10, 2),
    custo_total DECIMAL(10, 2),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_estoque_produto
        FOREIGN KEY (produto_id)
        REFERENCES produto(id),

    CONSTRAINT chk_estoque_quantidade_inicial
        CHECK (quantidade_inicial > 0),

    CONSTRAINT chk_estoque_quantidade_atual
        CHECK (quantidade_atual >= 0),

    CONSTRAINT chk_estoque_custo_unitario
        CHECK (custo_unitario > 0),

    CONSTRAINT chk_estoque_custo_total
        CHECK (custo_total > 0),

    CONSTRAINT chk_estoque_custo_consistente
        CHECK ((custo_unitario IS NULL) = (custo_total IS NULL))
);