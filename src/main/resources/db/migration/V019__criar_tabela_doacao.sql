CREATE TABLE doacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id BIGINT NOT NULL,
    produto_id BIGINT,
    tipo VARCHAR(20) NOT NULL,
    nome VARCHAR(150),
    quantidade INTEGER,
    valor DECIMAL(10, 2),
    forma_pagamento VARCHAR(50),
    doador VARCHAR(150) NOT NULL,
    valor_ficha DECIMAL(10, 2),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_doacao_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento(id),

    CONSTRAINT fk_doacao_produto
        FOREIGN KEY (produto_id)
        REFERENCES produto(id),

    CONSTRAINT chk_doacao_tipo
        CHECK (tipo IN ('DINHEIRO', 'PRODUTO', 'REABASTECIMENTO')),

    CONSTRAINT chk_doacao_quantidade
        CHECK (quantidade > 0),

    CONSTRAINT chk_doacao_valor
        CHECK (valor > 0),

    CONSTRAINT chk_doacao_forma_pagamento
            CHECK (forma_pagamento IN ('PIX', 'DEBITO', 'CREDITO', 'DINHEIRO')),

    CONSTRAINT chk_doacao_valor_ficha
        CHECK (valor_ficha >= 0),

    CONSTRAINT chk_doacao_dinheiro CHECK (
        tipo != 'DINHEIRO' OR (
            valor IS NOT NULL AND forma_pagamento IS NOT NULL
            AND produto_id IS NULL AND nome IS NULL AND quantidade IS NULL)),

    CONSTRAINT chk_doacao_produto CHECK (
        tipo != 'PRODUTO' OR (
            nome IS NOT NULL AND quantidade IS NOT NULL
            AND produto_id IS NULL AND valor IS NULL AND forma_pagamento IS NULL)),

    CONSTRAINT chk_doacao_reabastecimento CHECK (
        tipo != 'REABASTECIMENTO' OR (
            produto_id IS NOT NULL AND quantidade IS NOT NULL
            AND nome IS NULL AND valor IS NULL AND forma_pagamento IS NULL))

);