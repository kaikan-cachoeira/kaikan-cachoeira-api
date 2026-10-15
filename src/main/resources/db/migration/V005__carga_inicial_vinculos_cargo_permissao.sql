INSERT INTO cargo_permissao (cargo_id, permissao_id)
    SELECT c.id, p.id
    FROM cargo c
    CROSS JOIN permissao p
    WHERE c.nome = 'ADMINISTRADOR';

INSERT INTO cargo_permissao (cargo_id, permissao_id)
    SELECT c.id, p.id
    FROM cargo c
    JOIN permissao p
        ON p.nome IN ('VENDAS_MENU', 'CAIXA_MENU', 'EVENTOS_LEITURA', 'PRODUTOS_LEITURA', 'CARDAPIO_EVENTO_LEITURA')
    WHERE c.nome = 'OPERADOR';