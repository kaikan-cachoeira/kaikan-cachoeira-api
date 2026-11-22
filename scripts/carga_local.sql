BEGIN;

TRUNCATE evento, produto, associado, configuracao_anuidade RESTART IDENTITY CASCADE;

DO $$
DECLARE
    evento_festa BIGINT;
    evento_reuniao BIGINT;

    produto_agua BIGINT;
    produto_pastel BIGINT;
    produto_suco BIGINT;

    produto_evento_agua BIGINT;
    produto_evento_pastel BIGINT;

    configuracao_2025 INTEGER;
    configuracao_2026 INTEGER;

    maria BIGINT;
    pedro BIGINT;
    joao BIGINT;
    ana BIGINT;
    carla BIGINT;
    luiza BIGINT;
    marcos BIGINT;

    anu_maria_2025 BIGINT;
    anu_joao_2025 BIGINT;
    anu_carla_2025 BIGINT;
    anu_maria_2026 BIGINT;
    anu_pedro_2026 BIGINT;
    anu_joao_2026 BIGINT;
    anu_ana_2026 BIGINT;
    anu_carla_2026 BIGINT;
    anu_luiza_2026 BIGINT;
    anu_marcos_2026 BIGINT;
BEGIN
    INSERT INTO evento (status_id, nome, tipo_evento, observacoes, inicio_previsto_em, encerramento_previsto_em, iniciado_em)
    VALUES (
        2,
        'Festa de Verão 2026',
        'EVENTO',
        'Evento principal para testes de caixa, produtos e relatório.',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        CURRENT_TIMESTAMP + INTERVAL '4 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour'
    )
    RETURNING id INTO evento_festa;

    INSERT INTO evento (status_id, nome, tipo_evento, observacoes, inicio_previsto_em, encerramento_previsto_em)
    VALUES (
        1,
        'Reunião Mensal',
        'REUNIAO',
        'Reunião administrativa para testes da tela de eventos.',
        CURRENT_TIMESTAMP + INTERVAL '1 day',
        CURRENT_TIMESTAMP + INTERVAL '1 day 2 hours'
    )
    RETURNING id INTO evento_reuniao;

    INSERT INTO produto (categoria_id, nome, descricao, quantidade_atual, estoque_minimo, ativo)
    VALUES (1, 'Água 500ml', 'Garrafa de água 500ml', 30, 10, TRUE)
    RETURNING id INTO produto_agua;

    INSERT INTO produto (categoria_id, nome, descricao, quantidade_atual, estoque_minimo, ativo)
    VALUES (1, 'Suco de Laranja', 'Suco natural de laranja', 20, 5, TRUE)
    RETURNING id INTO produto_suco;

    INSERT INTO produto (categoria_id, nome, descricao, quantidade_atual, estoque_minimo, ativo)
    VALUES (2, 'Pastel de Carne', 'Pastel frito de carne', 0, 0, TRUE)
    RETURNING id INTO produto_pastel;

    INSERT INTO produto_evento (
        produto_id, evento_id, origem, preco_venda, quantidade_prevista, nome, descricao, categoria
    ) VALUES (
        produto_agua,
        evento_festa,
        'ESTOQUE',
        5.00,
        40,
        'Água 500ml',
        'Venda de água para o evento',
        'BEBIDA'
    )
    RETURNING id INTO produto_evento_agua;

    INSERT INTO produto_evento (
        evento_id, origem, preco_venda, custo_total_ingredientes, nome, descricao, categoria
    ) VALUES (
        evento_festa,
        'PRODUZIDO',
        8.00,
        3.20,
        'Pastel de Carne',
        'Pastel produzido para venda no evento',
        'COMIDA'
    )
    RETURNING id INTO produto_evento_pastel;

    INSERT INTO custo_evento (evento_id, nome, descricao, valor)
    VALUES (
        evento_festa,
        'Aluguel do espaço',
        'Custo básico para o evento principal',
        250.00
    );

    INSERT INTO configuracao_anuidade (ano, valor)
    VALUES (2025, 150.00)
    RETURNING id INTO configuracao_2025;

    INSERT INTO configuracao_anuidade (ano, valor)
    VALUES (2026, 180.00)
    RETURNING id INTO configuracao_2026;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('Maria Oliveira', '1982-04-10', 'TITULAR', NULL, '11911110001')
    RETURNING id INTO maria;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('Pedro Oliveira', '2010-09-20', 'DEPENDENTE', maria, '11911110002')
    RETURNING id INTO pedro;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('João Pereira', '1978-11-03', 'TITULAR', NULL, '11911110003')
    RETURNING id INTO joao;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('Ana Pereira', '2012-06-15', 'DEPENDENTE', joao, '11911110004')
    RETURNING id INTO ana;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('Carla Souza', '1990-01-25', 'TITULAR', NULL, '11911110005')
    RETURNING id INTO carla;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('Luiza Souza', '2014-02-07', 'DEPENDENTE', carla, '11911110006')
    RETURNING id INTO luiza;

    INSERT INTO associado (nome, data_nascimento, tipo, titular_id, numero_contato)
    VALUES ('Marcos Lima', NULL, 'TITULAR', NULL, '11911110007')
    RETURNING id INTO marcos;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (maria, configuracao_2025, 150.00, 'PAGO')
    RETURNING id INTO anu_maria_2025;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (joao, configuracao_2025, 150.00, 'PENDENTE')
    RETURNING id INTO anu_joao_2025;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (carla, configuracao_2025, 150.00, 'ISENTO')
    RETURNING id INTO anu_carla_2025;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (maria, configuracao_2026, 180.00, 'PAGO')
    RETURNING id INTO anu_maria_2026;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (pedro, configuracao_2026, 180.00, 'PENDENTE')
    RETURNING id INTO anu_pedro_2026;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (joao, configuracao_2026, 180.00, 'PENDENTE')
    RETURNING id INTO anu_joao_2026;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (ana, configuracao_2026, 180.00, 'ISENTO')
    RETURNING id INTO anu_ana_2026;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (carla, configuracao_2026, 180.00, 'PENDENTE')
    RETURNING id INTO anu_carla_2026;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (luiza, configuracao_2026, 180.00, 'PENDENTE')
    RETURNING id INTO anu_luiza_2026;

    INSERT INTO anuidade (associado_id, configuracao_anuidade_id, valor, status)
    VALUES (marcos, configuracao_2026, 180.00, 'PENDENTE')
    RETURNING id INTO anu_marcos_2026;

    INSERT INTO pagamento (anuidade_id, valor, criado_em, atualizado_em)
    VALUES (anu_maria_2025, 150.00, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day');

    INSERT INTO pagamento (anuidade_id, valor, criado_em, atualizado_em)
    VALUES (anu_maria_2026, 100.00, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours');

    INSERT INTO pagamento (anuidade_id, valor, criado_em, atualizado_em)
    VALUES (anu_maria_2026, 80.00, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour');

    INSERT INTO pagamento (anuidade_id, valor, criado_em, atualizado_em)
    VALUES (anu_joao_2026, 60.00, CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes');
END $$;

COMMIT;
