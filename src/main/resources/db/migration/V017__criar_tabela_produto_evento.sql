CREATE TABLE produto_evento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id BIGINT,
    evento_id BIGINT NOT NULL,
    origem VARCHAR(10) NOT NULL,
    preco_venda DECIMAL(10, 2) NOT NULL,
    quantidade_prevista INTEGER,
    custo_total_ingredientes DECIMAL(10, 2),
    desconto DECIMAL(10, 2) NOT NULL DEFAULT 0,
    desconto_aplicado_em TIMESTAMPTZ,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT,
    categoria VARCHAR(100),
    foto_url VARCHAR(64),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_produto_evento_produto
        FOREIGN KEY (produto_id)
        REFERENCES produto(id),

    CONSTRAINT fk_produto_evento_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento(id),

    CONSTRAINT uq_produto_evento
        UNIQUE (produto_id, evento_id),

    CONSTRAINT chk_produto_evento_preco_venda
        CHECK (preco_venda >= 0),

    CONSTRAINT chk_produto_evento_quantidade_prevista
        CHECK (quantidade_prevista > 0),

    CONSTRAINT chk_produto_evento_custo_total_ingredientes
        CHECK (custo_total_ingredientes >= 0),

    CONSTRAINT chk_produto_evento_desconto
        CHECK (desconto >= 0 AND desconto <= preco_venda),

    CONSTRAINT chk_produto_evento_desconto_aplicado
        CHECK ((desconto > 0) = (desconto_aplicado_em IS NOT NULL)),

    CONSTRAINT uq_produto_evento_nome
        UNIQUE (evento_id, nome),

    CONSTRAINT chk_produto_evento_origem
        CHECK (
            origem = 'ESTOQUE'
                AND produto_id IS NOT NULL
                AND quantidade_prevista IS NOT NULL
                AND custo_total_ingredientes IS NULL
            OR origem = 'PRODUZIDO'
                AND produto_id IS NULL
                AND quantidade_prevista IS NULL
                AND custo_total_ingredientes IS NOT NULL
        )

);