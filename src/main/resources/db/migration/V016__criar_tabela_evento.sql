CREATE TABLE evento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status_id INT NOT NULL DEFAULT 1,
    nome VARCHAR(150) NOT NULL,
    tipo_evento VARCHAR(20) NOT NULL DEFAULT 'EVENTO',
    observacoes TEXT,
    inicio_previsto_em TIMESTAMPTZ NOT NULL,
    encerramento_previsto_em TIMESTAMPTZ NOT NULL,
    iniciado_em TIMESTAMPTZ,
    encerrado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_evento_status
        FOREIGN KEY (status_id)
        REFERENCES status_evento(id),

    CONSTRAINT uq_evento_nome_periodo
        UNIQUE (nome, inicio_previsto_em, encerramento_previsto_em),

    CONSTRAINT chk_evento_tipo_evento
        CHECK (tipo_evento IN ('EVENTO', 'REUNIAO')),

    CONSTRAINT chk_evento_periodo
        CHECK (encerramento_previsto_em > inicio_previsto_em)
);

CREATE UNIQUE INDEX uq_evento_em_andamento
    ON evento (tipo_evento)
    WHERE tipo_evento = 'EVENTO'
    AND status_id = 2;