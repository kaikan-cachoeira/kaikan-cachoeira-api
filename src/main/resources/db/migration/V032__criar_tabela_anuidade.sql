CREATE TABLE anuidade (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    associado_id BIGINT NOT NULL,
    configuracao_anuidade_id INTEGER NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_associado_anuidade
        FOREIGN KEY (associado_id)
        REFERENCES associado (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_configuracao_anuidade_anuidade
        FOREIGN KEY (configuracao_anuidade_id)
        REFERENCES configuracao_anuidade (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_valor
        CHECK (valor >= 0),

    CONSTRAINT chk_status
        CHECK (status IN ('PAGO', 'PENDENTE', 'ISENTO'))

);
