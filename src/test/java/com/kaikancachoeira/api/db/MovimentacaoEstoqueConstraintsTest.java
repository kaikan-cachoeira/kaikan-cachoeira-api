package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MovimentacaoEstoqueConstraintsTest extends DbIntegrationTest {

    private Long evento;
    private Long produto;
    private Long lote;
    private Long lote2;
    private Long doacao;
    private Long itemVenda;

    @BeforeEach
    void setUp() {
        evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");
        produto = insertProduto("Coca-Cola");
        lote = insertLote(produto, 10, 10, bd("2.00"), bd("20.00"));
        lote2 = insertLote(produto, 10, 10, bd("3.00"), bd("30.00"));
        doacao = insertDoacao(evento, "REABASTECIMENTO", produto, "Coca", 10, null, null, "Ana", null);
        Long pe = insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);
        Long venda = insertVenda(evento, bd("0.00"), bd("5.00"));
        itemVenda = insertItemVenda(venda, pe, 1, bd("5.00"), bd("0.00"));
    }

    private int mov(Long estoqueId, String tipo, int quantidade, Long doacaoId, Long itemVendaId) {
        return jdbc.update("""
                insert into movimentacao_estoque (estoque_id, tipo_movimentacao_id, quantidade, doacao_id, item_venda_id)
                values (?, ?, ?, cast(? as bigint), cast(? as bigint))""",
                estoqueId, tipoMov(tipo), quantidade, doacaoId, itemVendaId);
    }

    // ------------------------------------------------------------ quantidade

    @Test
    void entradaESaidaManuaisValidas() {
        assertThatCode(() -> {
            mov(lote, "REABASTECIMENTO", 10, null, null);
            mov(lote, "PERDA", -2, null, null);
        }).doesNotThrowAnyException();
    }

    @Test
    void quantidadeZeroDeveSerRejeitada() {
        assertViolacao(() -> mov(lote, "PERDA", 0, null, null), "chk_mov_quantidade");
    }

    /** Documenta: o banco não confere o sinal com a direção do tipo (SAIDA com quantidade positiva passa). */
    @Test
    void sinalDaQuantidadeNaoEhConferidoContraADirecaoDoTipo() {
        assertThatCode(() -> mov(lote, "PERDA", 5, null, null)).doesNotThrowAnyException();
    }

    // ----------------------------------------------------------------- custo

    /** [achado] O dicionário pede custo_unitario nulo quando não há custo, e não prevê custo_total. */
    @Test
    void custosNascemComZeroEColunaCustoTotalExisteForaDoDicionario() {
        mov(lote, "DOACAO", 5, doacao, null);

        assertThat(jdbc.queryForObject("select custo_unitario || '|' || custo_total from movimentacao_estoque",
                String.class)).isEqualTo("0.00|0.00");
    }

    @Test
    void custosNegativosDevemSerRejeitados() {
        assertViolacao(() -> jdbc.update("""
                insert into movimentacao_estoque (estoque_id, tipo_movimentacao_id, quantidade, custo_unitario)
                values (?, ?, 1, -1)""", lote, tipoMov("REABASTECIMENTO")), "chk_mov_custo_unitario");
    }

    /** [achado] O dicionário diz "CHECK > 0 quando preenchido"; a migration aceita 0. */
    @Test
    void custoUnitarioZeroEhAceitoHojeDivergindoDoDicionario() {
        assertThatCode(() -> jdbc.update("""
                insert into movimentacao_estoque (estoque_id, tipo_movimentacao_id, quantidade, custo_unitario)
                values (?, ?, 1, 0)""", lote, tipoMov("REABASTECIMENTO"))).doesNotThrowAnyException();
    }

    // --------------------------------------------------------------- origem

    @Test
    void movimentacaoComDoacaoValida() {
        assertThatCode(() -> mov(lote, "DOACAO", 10, doacao, null)).doesNotThrowAnyException();
    }

    @Test
    void movimentacaoComItemDeVendaValida() {
        assertThatCode(() -> mov(lote, "VENDA", -1, null, itemVenda)).doesNotThrowAnyException();
    }

    @Test
    void doacaoEItemDeVendaJuntosDevemSerRejeitados() {
        assertViolacao(() -> mov(lote, "DOACAO", 1, doacao, itemVenda), "chk_mov_origem_unica");
    }

    @Test
    void mesmoItemTipoELoteDuasVezesDeveSerRejeitado() {
        mov(lote, "VENDA", -1, null, itemVenda);

        assertViolacao(() -> mov(lote, "VENDA", -1, null, itemVenda), "uq_mov_item_venda_tipo_lote");
    }

    @Test
    void mesmoItemEmLotesDiferentesEhPermitidoPorCausaDoFifo() {
        mov(lote, "VENDA", -1, null, itemVenda);

        assertThatCode(() -> mov(lote2, "VENDA", -1, null, itemVenda)).doesNotThrowAnyException();
    }

    @Test
    void estornoDoMesmoItemNoMesmoLoteNaoConflitaComAVenda() {
        mov(lote, "VENDA", -1, null, itemVenda);

        assertThatCode(() -> mov(lote, "ESTORNO_VENDA", 1, null, itemVenda)).doesNotThrowAnyException();
    }

    @Test
    void mesmaDoacaoETipoDuasVezesDeveSerRejeitado() {
        mov(lote, "DOACAO", 10, doacao, null);

        assertViolacao(() -> mov(lote2, "DOACAO", 10, doacao, null), "uq_mov_doacao_tipo");
    }

    @Test
    void movimentacoesManuaisSemOrigemNaoConflitamEntreSi() {
        assertThatCode(() -> {
            mov(lote, "PERDA", -1, null, null);
            mov(lote, "PERDA", -1, null, null);
        }).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------- FK

    @Test
    void loteInexistenteDeveSerRejeitado() {
        assertViolacao(() -> mov(999_999L, "PERDA", -1, null, null), "fk_movimentacao_estoque_estoque");
    }

    @Test
    void tipoInexistenteDeveSerRejeitado() {
        assertViolacao(() -> jdbc.update("""
                insert into movimentacao_estoque (estoque_id, tipo_movimentacao_id, quantidade)
                values (?, 9999, 1)""", lote), "fk_movimentacao_estoque_tipo_movimentacao");
    }

    @Test
    void naoDeveExcluirLoteComMovimentacoes() {
        mov(lote, "REABASTECIMENTO", 10, null, null);

        assertViolacao(() -> jdbc.update("delete from estoque where id = ?", lote),
                "fk_movimentacao_estoque_estoque");
    }
}