INSERT INTO permissao (nome, descricao) VALUES
    ('ASSOCIADOS_LEITURA', 'Listar e consultar associados'),
    ('ASSOCIADOS_MENU', 'Cadastro, edição, listagem e exclusão de associados'),
    ('ANUIDADES_LEITURA', 'Listar e consultar configurações de anuidade'),
    ('ANUIDADES_MENU', 'Cadastro, edição e exclusão de anuidades e configurações')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO cargo_permissao (cargo_id, permissao_id)
    SELECT c.id, p.id
    FROM cargo c
    JOIN permissao p
        ON p.nome IN (
            'ASSOCIADOS_LEITURA',
            'ASSOCIADOS_MENU',
            'ANUIDADES_LEITURA',
            'ANUIDADES_MENU'
        )
    WHERE c.nome = 'ADMINISTRADOR'
ON CONFLICT DO NOTHING;
