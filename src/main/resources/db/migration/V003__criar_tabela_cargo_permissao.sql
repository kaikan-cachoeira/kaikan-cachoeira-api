CREATE TABLE cargo_permissao (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cargo_id INTEGER NOT NULL,
    permissao_id INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cargo_permissao
        UNIQUE (cargo_id, permissao_id),

    CONSTRAINT fk_cargo_permissao_cargo
        FOREIGN KEY (cargo_id)
        REFERENCES cargo(id),

    CONSTRAINT fk_cargo_permissao_permissao
        FOREIGN KEY (permissao_id)
        REFERENCES permissao(id)
);

