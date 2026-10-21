package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdutoEventoConstraintsTest extends DbIntegrationTest {

    private Long evento;
    private Long produto;

    @BeforeEach
    void setUp() {
        evento = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        produto = insertProduto("Coca-Cola");
    }

    private int inserirEstoque(Long prod, int prevista, BigDecimal custoIngredientes, BigDecimal preco, String nome) {
        return jdbc.update("""
                insert into produto_evento (produto_id, evento_id, origem, preco_venda, quantidade_prevista,
                                            custo_total_ingredientes, nome)
                values (?, ?, 'ESTOQUE', ?, ?, cast(? as numeric), ?)""",
                prod, evento, preco, prevista, custoIngredientes, nome);
    }

    private int comDesconto(BigDecimal preco, BigDecimal desconto, OffsetDateTime aplicadoEm) {
        return jdbc.update("""
                insert into produto_evento (produto_id, evento_id, origem, preco_venda, quantidade_prevista,
                                            desconto, desconto_aplicado_em, nome)
                values (?, ?, 'ESTOQUE', ?, 5, ?, cast(? as timestamptz), 'Item')""",
                produto, evento, preco, desconto, aplicadoEm);
    }

    // ---------------------------------------------------------------- origem

    @Test
    void itemDeEstoqueValido() {
        assertThatCode(() -> insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10)).doesNotThrowAnyException();
    }

    @Test
    void itemProduzidoValido() {
        assertThatCode(() -> insertPeProduzido(evento, "Tempurá", bd("8.00"), bd("40.00"))).doesNotThrowAnyException();
    }

    @Test
    void estoqueSemProdutoDeveSerRejeitado() {
        assertViolacao(() -> jdbc.update("""
                insert into produto_evento (evento_id, origem, preco_venda, quantidade_prevista, nome)
                values (?, 'ESTOQUE', 5, 10, 'X')""", evento), "chk_produto_evento_origem");
    }

    @Test
    void estoqueSemQuantidadePrevistaDeveSerRejeitado() {
        assertViolacao(() -> jdbc.update("""
                insert into produto_evento (produto_id, evento_id, origem, preco_venda, nome)
                values (?, ?, 'ESTOQUE', 5, 'X')""", produto, evento), "chk_produto_evento_origem");
    }

    @Test
    void estoqueComCustoDeIngredientesDeveSerRejeitado() {
        assertViolacao(() -> inserirEstoque(produto, 10, bd("20.00"), bd("5.00"), "X"),
                "chk_produto_evento_origem");
    }

    @Test
    void produzidoComProdutoDoEstoqueDeveSerRejeitado() {
        assertViolacao(() -> jdbc.update("""
                insert into produto_evento (produto_id, evento_id, origem, preco_venda, custo_total_ingredientes, nome)
                values (?, ?, 'PRODUZIDO', 5, 10, 'X')""", produto, evento), "chk_produto_evento_origem");
    }

    @Test
    void produzidoComQuantidadePrevistaDeveSerRejeitado() {
        assertViolacao(() -> jdbc.update("""
                insert into produto_evento (evento_id, origem, preco_venda, quantidade_prevista,
                                            custo_total_ingredientes, nome)
                values (?, 'PRODUZIDO', 5, 10, 10, 'X')""", evento), "chk_produto_evento_origem");
    }

    @Test
    void produzidoSemCustoDeIngredientesDeveSerRejeitado() {
        assertViolacao(() -> jdbc.update("""
                insert into produto_evento (evento_id, origem, preco_venda, nome)
                values (?, 'PRODUZIDO', 5, 'X')""", evento), "chk_produto_evento_origem");
    }

    @Test
    void origemDesconhecidaDeveSerRejeitada() {
        assertViolacao(() -> jdbc.update("""
                insert into produto_evento (produto_id, evento_id, origem, preco_venda, quantidade_prevista, nome)
                values (?, ?, 'OUTRA', 5, 10, 'X')""", produto, evento), "chk_produto_evento_origem");
    }

    // ----------------------------------------------------------- valores

    @Test
    void precoZeroEhPermitidoENegativoNao() {
        assertThatCode(() -> insertPeEstoque(evento, produto, "Brinde", bd("0.00"), 1)).doesNotThrowAnyException();
        Long outro = insertProduto("Outro");

        assertThatThrownBy(() -> insertPeEstoque(evento, outro, "Neg", bd("-1.00"), 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void quantidadePrevistaZeroDeveSerRejeitada() {
        assertViolacao(() -> insertPeEstoque(evento, produto, "X", bd("5.00"), 0),
                "chk_produto_evento_quantidade_prevista");
    }

    @Test
    void custoDeIngredientesZeroEhPermitidoENegativoNao() {
        assertThatCode(() -> insertPeProduzido(evento, "Grátis", bd("5.00"), bd("0.00"))).doesNotThrowAnyException();
        assertViolacao(() -> insertPeProduzido(evento, "Neg", bd("5.00"), bd("-1.00")),
                "chk_produto_evento_custo_total_ingredientes");
    }

    // ---------------------------------------------------------- desconto

    @Test
    void semDescontoDeveNascerComZeroESemDataDeAplicacao() {
        insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "select desconto || '|' || coalesce(desconto_aplicado_em::text, 'null') from produto_evento",
                String.class)).isEqualTo("0.00|null");
    }

    @Test
    void descontoValidoExigeDataDeAplicacao() {
        assertThatCode(() -> comDesconto(bd("10.00"), bd("3.00"), OffsetDateTime.now())).doesNotThrowAnyException();
    }

    @Test
    void descontoIgualAoPrecoEhPermitido() {
        assertThatCode(() -> comDesconto(bd("10.00"), bd("10.00"), OffsetDateTime.now())).doesNotThrowAnyException();
    }

    @Test
    void descontoMaiorQueOPrecoDeveSerRejeitado() {
        assertViolacao(() -> comDesconto(bd("10.00"), bd("11.00"), OffsetDateTime.now()),
                "chk_produto_evento_desconto");
    }

    @Test
    void descontoNegativoDeveSerRejeitado() {
        assertViolacao(() -> comDesconto(bd("10.00"), bd("-1.00"), null), "chk_produto_evento_desconto");
    }

    @Test
    void descontoSemDataDeAplicacaoDeveSerRejeitado() {
        assertViolacao(() -> comDesconto(bd("10.00"), bd("3.00"), null), "chk_produto_evento_desconto_aplicado");
    }

    @Test
    void dataDeAplicacaoSemDescontoDeveSerRejeitada() {
        assertViolacao(() -> comDesconto(bd("10.00"), bd("0.00"), OffsetDateTime.now()),
                "chk_produto_evento_desconto_aplicado");
    }

    // -------------------------------------------------------- unicidade / FK

    @Test
    void mesmoProdutoDoEstoqueDuasVezesNoEventoDeveSerRejeitado() {
        insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);

        assertViolacao(() -> insertPeEstoque(evento, produto, "Coca 2", bd("5.00"), 10), "uq_produto_evento");
    }

    @Test
    void mesmoProdutoEmEventosDiferentesEhPermitido() {
        insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);
        Long outroEvento = insertEvento("Outro", "EVENTO", "NAO_INICIADO");

        assertThatCode(() -> insertPeEstoque(outroEvento, produto, "Coca", bd("5.00"), 10))
                .doesNotThrowAnyException();
    }

    @Test
    void nomeRepetidoNoMesmoEventoDeveSerRejeitadoInclusiveEntreOrigens() {
        insertPeEstoque(evento, produto, "Tempurá", bd("5.00"), 10);

        assertViolacao(() -> insertPeProduzido(evento, "Tempurá", bd("8.00"), bd("10.00")),
                "uq_produto_evento_nome");
    }

    @Test
    void doisProduzidosComNomesDiferentesNaoConflitamPorProdutoNulo() {
        insertPeProduzido(evento, "Tempurá", bd("8.00"), bd("10.00"));

        assertThatCode(() -> insertPeProduzido(evento, "Lámen", bd("12.00"), bd("30.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void naoDeveExcluirEventoComCardapio() {
        insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);

        assertViolacao(() -> jdbc.update("delete from evento where id = ?", evento), "fk_produto_evento_evento");
    }
}