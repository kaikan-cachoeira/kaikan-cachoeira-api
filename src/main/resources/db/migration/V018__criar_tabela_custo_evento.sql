CREATE TABLE custo_evento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT,
    valor DECIMAL(10, 2) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_custo_evento_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento(id),

    CONSTRAINT chk_custo_evento_valor
        CHECK (valor > 0)
);