CREATE TABLE produto (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    categoria_id INTEGER,
    nome VARCHAR(150) NOT NULL UNIQUE,
    descricao TEXT,
    foto_url VARCHAR(64),
    quantidade_atual INTEGER NOT NULL DEFAULT 0,
    estoque_minimo INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_produto_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES categoria(id),

    CONSTRAINT chk_produto_estoque_minimo
        CHECK (estoque_minimo >= 0)
);