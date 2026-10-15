CREATE TABLE pagamento_caixa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id BIGINT NOT NULL,
    fiado_id BIGINT,
    tipo VARCHAR(20) NOT NULL DEFAULT 'VENDA_FICHA',
    valor DECIMAL(10, 2) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pagamento_caixa_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento(id),

    CONSTRAINT chk_pagamento_caixa_tipo
        CHECK (tipo IN ('VENDA_FICHA', 'DEVOLUCAO_FICHA', 'QUITACAO_FIADO')),

    CONSTRAINT chk_pagamento_caixa_fiado_quitacao
        CHECK ((tipo = 'QUITACAO_FIADO') = (fiado_id IS NOT NULL)),

    CONSTRAINT chk_pagamento_caixa_valor
        CHECK (valor > 0),

    CONSTRAINT chk_pagamento_caixa_forma_pagamento
        CHECK (forma_pagamento IN ('PIX', 'DEBITO', 'CREDITO', 'DINHEIRO', 'FIADO')),

    CONSTRAINT chk_pagamento_caixa_fiado_so_em_venda
        CHECK (forma_pagamento != 'FIADO' OR tipo = 'VENDA_FICHA')
);