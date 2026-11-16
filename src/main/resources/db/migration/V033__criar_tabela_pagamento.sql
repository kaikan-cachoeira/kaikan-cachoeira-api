CREATE TABLE pagamento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    anuidade_id BIGINT NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_anuidade_pagamento
        FOREIGN KEY (anuidade_id)
        REFERENCES anuidade (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_valor
        CHECK (valor > 0)
);
