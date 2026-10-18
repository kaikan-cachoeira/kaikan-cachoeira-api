CREATE TABLE fiado (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pagamento_caixa_id BIGINT NOT NULL,
    nome_devedor VARCHAR(150) NOT NULL,
    observacoes TEXT,
    valor DECIMAL(10, 2) NOT NULL,
    quitado BOOLEAN NOT NULL DEFAULT FALSE,
    quitado_em TIMESTAMPTZ,
    forma_pagamento VARCHAR(50),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fiado_pagamento_caixa
        FOREIGN KEY (pagamento_caixa_id)
        REFERENCES pagamento_caixa(id),

    CONSTRAINT chk_fiado_valor
        CHECK (valor > 0),

    CONSTRAINT chk_fiado_forma_pagamento
        CHECK (forma_pagamento IN ('PIX', 'DEBITO', 'CREDITO', 'DINHEIRO')),

    CONSTRAINT chk_fiado_quitacao CHECK (
        (quitado AND quitado_em IS NOT NULL AND forma_pagamento IS NOT NULL)
        OR (NOT quitado AND quitado_em IS NULL AND forma_pagamento IS NULL)
    )
);