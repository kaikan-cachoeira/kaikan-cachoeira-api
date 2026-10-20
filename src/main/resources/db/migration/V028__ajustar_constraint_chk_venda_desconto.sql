ALTER TABLE venda DROP CONSTRAINT chk_venda_desconto;

ALTER TABLE venda
    ADD CONSTRAINT chk_venda_desconto CHECK (desconto >= 0);