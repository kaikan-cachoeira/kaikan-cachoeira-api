CREATE TABLE associado (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titular_id BIGINT,
    nome VARCHAR(150) NOT NULL UNIQUE,
    data_nascimento DATE,
    tipo VARCHAR(20) NOT NULL,
    numero_contato VARCHAR(20),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_titular_associado
        FOREIGN KEY (titular_id)
        REFERENCES associado (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_tipo
        CHECK (tipo IN ('TITULAR', 'DEPENDENTE'))
);
