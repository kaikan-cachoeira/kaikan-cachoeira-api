CREATE TABLE tipo_movimentacao (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descricao VARCHAR(255),
    direcao VARCHAR(7) NOT NULL,
    gerado_pelo_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    exige_custo BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tipo_movimentacao_direcao
        CHECK (direcao IN ('ENTRADA', 'SAIDA')),

    CONSTRAINT chk_tipo_movimentacao_gerado_pelo_sistema
        CHECK ((codigo IN ('VENDA','DOACAO','ESTORNO_VENDA')) = gerado_pelo_sistema)
);