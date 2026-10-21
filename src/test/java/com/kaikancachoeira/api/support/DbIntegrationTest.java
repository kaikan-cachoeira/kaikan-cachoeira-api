package com.kaikancachoeira.api.support;

import org.assertj.core.api.ThrowableAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Transactional
public abstract class DbIntegrationTest extends IntegrationTestBase {

    protected static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-01T09:00:00Z");

    @Autowired protected JdbcTemplate jdbc;

    protected Long insertEvento(String nome, String tipo, String statusCodigo,
                                OffsetDateTime inicio, OffsetDateTime fim) {
        return jdbc.queryForObject("""
                insert into evento (status_id, nome, tipo_evento, inicio_previsto_em, encerramento_previsto_em)
                values ((select id from status_evento where codigo = ?), ?, ?, ?, ?)
                returning id""",
                Long.class, statusCodigo, nome, tipo, inicio, fim);
    }

    protected Long insertEvento(String nome, String tipo, String statusCodigo) {
        return insertEvento(nome, tipo, statusCodigo, T0, T0.plusHours(8));
    }

    protected int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    protected Integer cargoId(String nome) {
        return jdbc.queryForObject("select id from cargo where nome = ?", Integer.class, nome);
    }

    protected void assertViolacao(ThrowableAssert.ThrowingCallable acao, String constraint) {
        assertThatThrownBy(acao)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }

    protected static BigDecimal bd(String v) { return new BigDecimal(v); }

    protected Integer tipoMov(String codigo) {
        return jdbc.queryForObject("select id from tipo_movimentacao where codigo = ?", Integer.class, codigo);
    }

    protected Long insertProduto(String nome) {
        return jdbc.queryForObject("insert into produto (nome) values (?) returning id", Long.class, nome);
    }

    protected Long insertLote(Long produtoId, int inicial, int atual, BigDecimal custoUnit, BigDecimal custoTotal) {
        return jdbc.queryForObject("""
            insert into estoque (produto_id, quantidade_inicial, quantidade_atual, custo_unitario, custo_total)
            values (?, ?, ?, ?, ?) returning id""", Long.class, produtoId, inicial, atual, custoUnit, custoTotal);
    }

    protected Long insertPeEstoque(Long eventoId, Long produtoId, String nome, BigDecimal preco, int prevista) {
        return jdbc.queryForObject("""
            insert into produto_evento (produto_id, evento_id, origem, preco_venda, quantidade_prevista, nome)
            values (?, ?, 'ESTOQUE', ?, ?, ?) returning id""",
                Long.class, produtoId, eventoId, preco, prevista, nome);
    }

    protected Long insertPeProduzido(Long eventoId, String nome, BigDecimal preco, BigDecimal custo) {
        return jdbc.queryForObject("""
            insert into produto_evento (evento_id, origem, preco_venda, custo_total_ingredientes, nome)
            values (?, 'PRODUZIDO', ?, ?, ?) returning id""", Long.class, eventoId, preco, custo, nome);
    }

    protected Long insertVenda(Long eventoId, BigDecimal desconto, BigDecimal total) {
        return jdbc.queryForObject("""
            insert into venda (evento_id, desconto, preco_total) values (?, ?, ?) returning id""",
                Long.class, eventoId, desconto, total);
    }

    protected Long insertItemVenda(Long vendaId, Long peId, int qtd, BigDecimal preco, BigDecimal desconto) {
        return jdbc.queryForObject("""
            insert into item_venda (venda_id, produto_evento_id, quantidade, preco_unitario, desconto_unitario)
            values (?, ?, ?, ?, ?) returning id""", Long.class, vendaId, peId, qtd, preco, desconto);
    }

    protected Long insertDoacao(Long eventoId, String tipo, Long produtoId, String nome, Integer quantidade,
                                BigDecimal valor, String forma, String doador, BigDecimal valorFicha) {
        return jdbc.queryForObject("""
            insert into doacao (evento_id, tipo, produto_id, nome, quantidade, valor, forma_pagamento, doador, valor_ficha)
            values (?, ?, cast(? as bigint), cast(? as varchar), cast(? as integer), cast(? as numeric),
                    cast(? as varchar), ?, cast(? as numeric)) returning id""",
                Long.class, eventoId, tipo, produtoId, nome, quantidade, valor, forma, doador, valorFicha);
    }

    protected Long insertPagamento(Long eventoId, String tipo, BigDecimal valor, String forma, Long fiadoId) {
        return jdbc.queryForObject("""
            insert into pagamento_caixa (evento_id, tipo, valor, forma_pagamento, fiado_id)
            values (?, ?, ?, ?, cast(? as bigint)) returning id""",
                Long.class, eventoId, tipo, valor, forma, fiadoId);
    }

    protected Long insertFiadoPendente(Long pagamentoId, BigDecimal valor) {
        return jdbc.queryForObject("""
            insert into fiado (pagamento_caixa_id, nome_devedor, valor) values (?, 'Fulano', ?) returning id""",
                Long.class, pagamentoId, valor);
    }
}