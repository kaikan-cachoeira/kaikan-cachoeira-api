INSERT INTO cargo (nome, descricao) VALUES
    ('ADMINISTRADOR', 'Administrador da associação'),
    ('OPERADOR', 'Operador da associação. Atendente responsável pelo gerenciamento do evento em andamento (PDV)');

INSERT INTO permissao (nome, descricao) VALUES
    ('EVENTOS_MENU', 'Eventos, detalhes, cardápio e histórico de vendas'),
    ('ESTOQUE_MENU', 'Estoque, categorias, lotes e histórico de movimentações'),
    ('DOACOES_MENU', 'Doações'),
    ('VENDAS_MENU', 'Registro de venda'),
    ('CAIXA_MENU', 'Caixa e fiados'),
    ('RELATORIOS_MENU', 'Relatórios por evento'),
    ('DASHBOARD_MENU', 'Dashboards'),
    ('EVENTOS_LEITURA', 'Listar e consultar eventos'),
    ('PRODUTOS_LEITURA', 'Consultar produtos'),
    ('CARDAPIO_EVENTO_LEITURA', 'Consultar cardápio de eventos');