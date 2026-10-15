INSERT INTO tipo_movimentacao (codigo, direcao, exige_custo, gerado_pelo_sistema, descricao) VALUES
    ('REABASTECIMENTO', 'ENTRADA', TRUE, FALSE, 'Compra de produto. Cria lote com custo'),
    ('DOACAO', 'ENTRADA', FALSE, TRUE, 'Doação de reabastecimento. Cria lote sem custo'),
    ('VENDA', 'SAIDA', FALSE, TRUE, 'Uma movimentação por lote consumido (FIFO). Custo = custo do lote'),
    ('ESTORNO_VENDA', 'ENTRADA', FALSE, TRUE, 'Devolve ao lote de origem, mesmo zerado'),
    ('CONSUMO_INTERNO', 'SAIDA', FALSE, FALSE, 'Consumo em reunião. Custo = custo do lote consumido'),
    ('PERDA', 'SAIDA', FALSE, FALSE, 'Estragou, quebrou, venceu. Motivo em observações da movimentação'),
    ('AJUSTE_ENTRADA', 'ENTRADA', FALSE, FALSE, 'Correção de inventário. Cria lote sem custo'),
    ('AJUSTE_SAIDA', 'SAIDA', FALSE, FALSE, 'Correção de inventário (contagem menor que o sistema). Consome lotes em FIFO');