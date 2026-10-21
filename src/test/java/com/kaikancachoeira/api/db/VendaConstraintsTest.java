package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendaConstraintsTest extends DbIntegrationTest {

    private Long evento;
    private Long pe;
    private Long pe2;

    @BeforeEach
    void setUp() {
        evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");
        Long produto = insertProduto("Coca");
        pe = insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);
        pe2 = insertPeProduzido(evento, "Tempurá", bd("8.00"), bd("10.00"));
    }

    // ----------------------------------------------------------------- venda

    @Test
    void vendaDeveNascerSemDescontoComTotalZeroEAtiva() {
        Long venda = jdbc.queryForObject(
                "insert into venda (evento_id) values (?) returning id", Long.class, evento);

        assertThat(jdbc.queryForObject(
                "select desconto || '|' || preco_total || '|' || (cancelada_em is null) from venda where id = ?",
                String.class, venda)).isEqualTo("0.00|0.00|true");
    }

    @Test
    void vendaComDescontoMenorOuIgualAoTotalPagoEhValida() {
        assertThatCode(() -> {
            insertVenda(evento, bd("10.00"), bd("90.00"));
            insertVenda(evento, bd("50.00"), bd("50.00"));
        }).doesNotThrowAnyException();
    }

    @Test
    void descontoNegativoDeveSerRejeitado() {
        assertViolacao(() -> insertVenda(evento, bd("-1.00"), bd("10.00")), "chk_venda_desconto");
    }

    /** Regressão da V028: desconto pode superar o total pago (venda de 100 com desconto 60 → total 40). */
    @Test
    void descontoMaiorQueOTotalPagoEhAceito() {
        assertThatCode(() -> insertVenda(evento, bd("60.00"), bd("40.00"))).doesNotThrowAnyException();
    }

    @Test
    void totalNegativoDeveSerRejeitado() {
        assertViolacao(() -> insertVenda(evento, bd("0.00"), bd("-1.00")), "chk_venda_preco_total");
    }

    @Test
    void eventoInexistenteDeveSerRejeitado() {
        assertViolacao(() -> insertVenda(999_999L, bd("0.00"), bd("1.00")), "fk_venda_evento");
    }

    // ------------------------------------------------------------ item_venda

    @Test
    void itemValidoComEOutroSemDesconto() {
        Long venda = insertVenda(evento, bd("0.00"), bd("13.00"));

        assertThatCode(() -> {
            insertItemVenda(venda, pe, 1, bd("5.00"), bd("0.00"));
            insertItemVenda(venda, pe2, 1, bd("8.00"), bd("0.00"));
        }).doesNotThrowAnyException();
    }

    @Test
    void descontoUnitarioDeveNascerZero() {
        Long venda = insertVenda(evento, bd("0.00"), bd("5.00"));
        jdbc.update("insert into item_venda (venda_id, produto_evento_id, quantidade, preco_unitario) values (?, ?, 1, 5)",
                venda, pe);

        assertThat(jdbc.queryForObject("select desconto_unitario from item_venda", java.math.BigDecimal.class))
                .isEqualByComparingTo("0");
    }

    @Test
    void quantidadeZeroOuNegativaDeveSerRejeitada() {
        Long venda = insertVenda(evento, bd("0.00"), bd("0.00"));

        assertViolacao(() -> insertItemVenda(venda, pe, 0, bd("5.00"), bd("0.00")), "chk_item_venda_quantidade");
    }

    @Test
    void descontoUnitarioIgualAoPrecoEhPermitidoEMaiorNao() {
        Long venda = insertVenda(evento, bd("0.00"), bd("0.00"));
        assertThatCode(() -> insertItemVenda(venda, pe, 1, bd("5.00"), bd("5.00"))).doesNotThrowAnyException();

        assertViolacao(() -> insertItemVenda(venda, pe2, 1, bd("5.00"), bd("5.01")),
                "chk_item_venda_desconto_unitario");
    }

    @Test
    void descontoUnitarioNegativoDeveSerRejeitado() {
        Long venda = insertVenda(evento, bd("0.00"), bd("0.00"));

        assertViolacao(() -> insertItemVenda(venda, pe, 1, bd("5.00"), bd("-1.00")),
                "chk_item_venda_desconto_unitario");
    }

    @Test
    void mesmoProdutoDuasVezesNaMesmaVendaDeveSerRejeitado() {
        Long venda = insertVenda(evento, bd("0.00"), bd("10.00"));
        insertItemVenda(venda, pe, 1, bd("5.00"), bd("0.00"));

        assertViolacao(() -> insertItemVenda(venda, pe, 1, bd("5.00"), bd("0.00")), "uq_item_venda_produto");
    }

    @Test
    void mesmoProdutoEmVendasDiferentesEhPermitido() {
        Long v1 = insertVenda(evento, bd("0.00"), bd("5.00"));
        Long v2 = insertVenda(evento, bd("0.00"), bd("5.00"));
        insertItemVenda(v1, pe, 1, bd("5.00"), bd("0.00"));

        assertThatCode(() -> insertItemVenda(v2, pe, 1, bd("5.00"), bd("0.00"))).doesNotThrowAnyException();
    }

    @Test
    void naoDeveExcluirVendaComItens() {
        Long venda = insertVenda(evento, bd("0.00"), bd("5.00"));
        insertItemVenda(venda, pe, 1, bd("5.00"), bd("0.00"));

        assertViolacao(() -> jdbc.update("delete from venda where id = ?", venda), "fk_item_venda_venda");
    }

    @Test
    void naoDeveExcluirItemDoCardapioJaVendido() {
        Long venda = insertVenda(evento, bd("0.00"), bd("5.00"));
        insertItemVenda(venda, pe, 1, bd("5.00"), bd("0.00"));

        assertViolacao(() -> jdbc.update("delete from produto_evento where id = ?", pe),
                "fk_item_venda_produto_evento");
    }
}