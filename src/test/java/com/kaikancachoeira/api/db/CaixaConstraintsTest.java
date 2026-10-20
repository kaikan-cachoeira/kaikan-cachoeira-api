package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CaixaConstraintsTest extends DbIntegrationTest {

    private Long evento;

    @BeforeEach
    void setUp() {
        evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");
    }

    private int fiado(Long pagamentoId, boolean quitado, OffsetDateTime quitadoEm, String forma) {
        return jdbc.update("""
                insert into fiado (pagamento_caixa_id, nome_devedor, valor, quitado, quitado_em, forma_pagamento)
                values (?, 'Fulano', 20, ?, cast(? as timestamptz), cast(? as varchar))""",
                pagamentoId, quitado, quitadoEm, forma);
    }

    // -------------------------------------------------------- pagamento_caixa

    @Test
    void tipoPadraoEhVendaDeFicha() {
        jdbc.update("insert into pagamento_caixa (evento_id, valor, forma_pagamento) values (?, 10, 'PIX')", evento);

        assertThat(jdbc.queryForObject("select tipo from pagamento_caixa", String.class)).isEqualTo("VENDA_FICHA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIX", "DEBITO", "CREDITO", "DINHEIRO", "FIADO"})
    void vendaDeFichaAceitaTodasAsFormasDePagamento(String forma) {
        assertThatCode(() -> insertPagamento(evento, "VENDA_FICHA", bd("10.00"), forma, null))
                .doesNotThrowAnyException();
    }

    @Test
    void devolucaoEmDinheiroEhValida() {
        assertThatCode(() -> insertPagamento(evento, "DEVOLUCAO_FICHA", bd("10.00"), "DINHEIRO", null))
                .doesNotThrowAnyException();
    }

    @Test
    void devolucaoEmFiadoDeveSerRejeitada() {
        assertViolacao(() -> insertPagamento(evento, "DEVOLUCAO_FICHA", bd("10.00"), "FIADO", null),
                "chk_pagamento_caixa_fiado_so_em_venda");
    }

    @Test
    void valorZeroOuNegativoDeveSerRejeitado() {
        assertViolacao(() -> insertPagamento(evento, "VENDA_FICHA", bd("0.00"), "PIX", null),
                "chk_pagamento_caixa_valor");
    }

    @Test
    void tipoForaDaListaDeveSerRejeitado() {
        assertViolacao(() -> insertPagamento(evento, "ESTORNO", bd("10.00"), "PIX", null),
                "chk_pagamento_caixa_tipo");
    }

    @Test
    void formaDePagamentoForaDaListaDeveSerRejeitada() {
        assertViolacao(() -> insertPagamento(evento, "VENDA_FICHA", bd("10.00"), "BOLETO", null),
                "chk_pagamento_caixa_forma_pagamento");
    }

    @Test
    void quitacaoSemFiadoDeveSerRejeitada() {
        assertViolacao(() -> insertPagamento(evento, "QUITACAO_FIADO", bd("20.00"), "PIX", null),
                "chk_pagamento_caixa_fiado_quitacao");
    }

    @Test
    void vendaDeFichaComFiadoIdDeveSerRejeitada() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        Long fiadoId = insertFiadoPendente(pag, bd("20.00"));

        assertViolacao(() -> insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "PIX", fiadoId),
                "chk_pagamento_caixa_fiado_quitacao");
    }

    @Test
    void fluxoCompletoDeFiadoEQuitacao() {
        Long venda = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        Long fiadoId = insertFiadoPendente(venda, bd("20.00"));

        assertThatCode(() -> {
            insertPagamento(evento, "QUITACAO_FIADO", bd("20.00"), "PIX", fiadoId);
            jdbc.update("update fiado set quitado = true, quitado_em = now(), forma_pagamento = 'PIX' where id = ?",
                    fiadoId);
        }).doesNotThrowAnyException();
    }

    @Test
    void quitacaoComFiadoInexistenteDeveSerRejeitada() {
        assertViolacao(() -> insertPagamento(evento, "QUITACAO_FIADO", bd("20.00"), "PIX", 999_999L),
                "fk_pagamento_caixa_fiado");
    }

    @Test
    void naoDeveExcluirLancamentoReferenciadoPorFiado() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        insertFiadoPendente(pag, bd("20.00"));

        assertViolacao(() -> jdbc.update("delete from pagamento_caixa where id = ?", pag),
                "fk_fiado_pagamento_caixa");
    }

    // ------------------------------------------------------------------ fiado

    @Test
    void fiadoPendenteSemQuitacaoEhValidoEDeveNascerNaoQuitado() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        Long id = insertFiadoPendente(pag, bd("20.00"));

        assertThat(jdbc.queryForObject("select quitado from fiado where id = ?", Boolean.class, id)).isFalse();
    }

    @Test
    void fiadoQuitadoCompletoEhValido() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);

        assertThatCode(() -> fiado(pag, true, OffsetDateTime.now(), "DINHEIRO")).doesNotThrowAnyException();
    }

    @Test
    void quitadoSemDataOuSemFormaDeveSerRejeitado() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);

        assertViolacao(() -> fiado(pag, true, null, "PIX"), "chk_fiado_quitacao");
    }

    @Test
    void quitadoSemFormaDeveSerRejeitado() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);

        assertViolacao(() -> fiado(pag, true, OffsetDateTime.now(), null), "chk_fiado_quitacao");
    }

    @Test
    void pendenteComFormaOuDataDeveSerRejeitado() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);

        assertViolacao(() -> fiado(pag, false, null, "PIX"), "chk_fiado_quitacao");
    }

    @Test
    void formaDeQuitacaoNaoPodeSerFiado() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);

        assertViolacao(() -> fiado(pag, true, OffsetDateTime.now(), "FIADO"), "chk_fiado_forma_pagamento");
    }

    @Test
    void valorDoFiadoDeveSerPositivo() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);

        assertViolacao(() -> insertFiadoPendente(pag, bd("0.00")), "chk_fiado_valor");
    }

    /** Documenta: o banco não garante que o lançamento de origem seja FIADO; isso fica com o service. */
    @Test
    void fiadoApontandoParaLancamentoQueNaoEhFiadoNaoEhBarradoPeloBanco() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "PIX", null);

        assertThatCode(() -> insertFiadoPendente(pag, bd("20.00"))).doesNotThrowAnyException();
    }

    /** Documenta: nada impede dois fiados para o mesmo lançamento (sem UNIQUE em pagamento_caixa_id). */
    @Test
    void doisFiadosParaOMesmoLancamentoNaoSaoBarradosPeloBanco() {
        Long pag = insertPagamento(evento, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        insertFiadoPendente(pag, bd("20.00"));

        assertThatCode(() -> insertFiadoPendente(pag, bd("20.00"))).doesNotThrowAnyException();
    }
}