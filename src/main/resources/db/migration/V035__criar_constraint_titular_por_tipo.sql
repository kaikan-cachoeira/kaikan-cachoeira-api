ALTER TABLE associado
    ADD CONSTRAINT chk_titular
        CHECK (
            (tipo = 'TITULAR' AND titular_id IS NULL)
            OR
            (tipo = 'DEPENDENTE' AND titular_id IS NOT NULL)
        );