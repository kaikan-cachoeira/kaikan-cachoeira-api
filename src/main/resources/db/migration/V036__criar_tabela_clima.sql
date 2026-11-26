CREATE TABLE clima (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id BIGINT NOT NULL,
    data_hora_prevista TIMESTAMPTZ NOT NULL,
    temperatura DECIMAL(5, 2) NOT NULL,
    sensacao_termica DECIMAL(5, 2) NOT NULL,
    chuva_mm DECIMAL(7, 2) NOT NULL DEFAULT 0,
    probabilidade_chuva INTEGER NOT NULL DEFAULT 0,
    vento_velocidade DECIMAL(7, 2) NOT NULL DEFAULT 0,
    codigo_clima INTEGER NOT NULL,
    descricao_clima VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_clima_evento
        FOREIGN KEY (evento_id)
        REFERENCES evento (id),

    CONSTRAINT uq_evento_data_hora_prevista
        UNIQUE (evento_id, data_hora_prevista),

    CONSTRAINT chk_probabilidade_chuva
        CHECK (probabilidade_chuva BETWEEN 0 AND 100),

    CONSTRAINT chk_chuva_mm
        CHECK (chuva_mm >= 0),

    CONSTRAINT chk_vento_velocidade
        CHECK (vento_velocidade >= 0)

);
