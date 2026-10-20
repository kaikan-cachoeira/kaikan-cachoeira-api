package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class DoacaoConstraintsTest extends DbIntegrationTest {

    private Long evento;
    private Long produto;

    @BeforeEach
    void setUp() {
        evento = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        produto = insertProduto("Arroz");
    }

    // -------------------------------------------------------------- DINHEIRO

    @Test
    void doacaoEmDinheiroValida() {
        assertThatCode(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("50.00"), "PIX", "Ana", bd("20.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void dinheiroSemValorDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, null, null, null, "PIX", "Ana", null),
                "chk_doacao_dinheiro");
    }

    @Test
    void dinheiroSemFormaDePagamentoDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("50.00"), null, "Ana", null),
                "chk_doacao_dinheiro");
    }

    @Test
    void dinheiroComNomeDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, "bolo", null, bd("50.00"), "PIX", "Ana", null),
                "chk_doacao_dinheiro");
    }

    @Test
    void valorZeroOuNegativoDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("0.00"), "PIX", "Ana", null),
                "chk_doacao_valor");
    }

    @Test
    void formaDePagamentoForaDaListaDeveSerRejeitada() {
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("10.00"), "FIADO", "Ana", null),
                "chk_doacao_forma_pagamento");
    }

    // --------------------------------------------------------------- PRODUTO

    @Test
    void doacaoDeProdutoSemQuantidadeEhAceitaHoje() {
        assertThatCode(() -> insertDoacao(evento, "PRODUTO", null, "bolo de cenoura", null, null, null, "Ana", null))
                .doesNotThrowAnyException();
    }

    /**
     * [achado] O dicionário e a história H4 exigem quantidade (e unidade_medida) em doação de
     * produto. A migration exige o contrário: quantidade NULA. Além disso, a coluna unidade_medida
     * não existe.
     */
    @Test
    void doacaoDeProdutoComQuantidadeEhRejeitadaHojeDivergindoDoDicionario() {
        assertViolacao(() -> insertDoacao(evento, "PRODUTO", null, "arroz", 5, null, null, "Ana", null),
                "chk_doacao_produto");
    }

    @Test
    void colunaUnidadeMedidaNaoExisteHoje() {
        org.assertj.core.api.Assertions.assertThat(count("""
                select count(*) from information_schema.columns
                where table_name = 'doacao' and column_name = 'unidade_medida'""")).isZero();
    }

    @Test
    void doacaoDeProdutoSemNomeDeveSerRejeitada() {
        assertViolacao(() -> insertDoacao(evento, "PRODUTO", null, null, null, null, null, "Ana", null),
                "chk_doacao_produto");
    }

    @Test
    void doacaoDeProdutoComProdutoDoCadastroDeveSerRejeitada() {
        assertViolacao(() -> insertDoacao(evento, "PRODUTO", produto, "arroz", null, null, null, "Ana", null),
                "chk_doacao_produto");
    }

    // -------------------------------------------------------- REABASTECIMENTO

    /** [achado] Pelo dicionário, nome deve ser NULO em reabastecimento; a migration o exige NÃO nulo. */
    @Test
    void reabastecimentoExigeNomeHojeDivergindoDoDicionario() {
        assertThatCode(() -> insertDoacao(evento, "REABASTECIMENTO", produto, "Arroz", 5, null, null, "Ana", null))
                .doesNotThrowAnyException();
        Long outro = insertProduto("Feijão");
        assertViolacao(() -> insertDoacao(evento, "REABASTECIMENTO", outro, null, 5, null, null, "Ana", null),
                "chk_doacao_reabastecimento");
    }

    @Test
    void reabastecimentoSemProdutoOuSemQuantidadeDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "REABASTECIMENTO", null, "x", 5, null, null, "Ana", null),
                "chk_doacao_reabastecimento");
    }

    @Test
    void reabastecimentoSemQuantidadeDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "REABASTECIMENTO", produto, "x", null, null, null, "Ana", null),
                "chk_doacao_reabastecimento");
    }

    @Test
    void reabastecimentoComValorEmDinheiroDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "REABASTECIMENTO", produto, "x", 5, bd("10.00"), null, "Ana", null),
                "chk_doacao_reabastecimento");
    }

    @Test
    void quantidadeZeroDeveSerRejeitada() {
        assertViolacao(() -> insertDoacao(evento, "REABASTECIMENTO", produto, "x", 0, null, null, "Ana", null),
                "chk_doacao_quantidade");
    }

    // ----------------------------------------------------------------- geral

    @Test
    void tipoEmMinusculoOuDesconhecidoDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(evento, "dinheiro", null, null, null, bd("10.00"), "PIX", "Ana", null),
                "chk_doacao_tipo");
    }

    @Test
    void valorDaFichaZeroEhPermitidoENegativoNao() {
        assertThatCode(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", bd("0.00")))
                .doesNotThrowAnyException();
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", bd("-1.00")),
                "chk_doacao_valor_ficha");
    }

    @Test
    void doadorEhObrigatorio() {
        assertViolacao(() -> insertDoacao(evento, "DINHEIRO", null, null, null, bd("10.00"), "PIX", null, null),
                "doador");
    }

    @Test
    void eventoInexistenteDeveSerRejeitado() {
        assertViolacao(() -> insertDoacao(999_999L, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null),
                "fk_doacao_evento");
    }
}