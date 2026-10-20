package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstoqueConstraintsTest extends DbIntegrationTest {

    private Long produto;

    @BeforeEach
    void setUp() {
        produto = insertProduto("Coca-Cola");
    }

    // --------------------------------------------------------------- produto

    @Test
    void produtoDeveNascerAtivoComEstoqueZero() {
        assertThat(jdbc.queryForObject("select ativo || '|' || quantidade_atual from produto where id = ?",
                String.class, produto)).isEqualTo("true|0");
    }

    @Test
    void nomeDeProdutoDeveSerUnico() {
        assertViolacao(() -> insertProduto("Coca-Cola"), "produto_nome_key");
    }

    /** Documenta: a unicidade do nome do produto também diferencia maiúsculas (a da categoria, pela H3, não). */
    @Test
    void nomeDeProdutoComCaixaDiferenteHojeEhAceito() {
        assertThatCode(() -> insertProduto("COCA-COLA")).doesNotThrowAnyException();
    }

    @Test
    void estoqueMinimoZeroEhPermitidoENegativoNao() {
        assertThatCode(() -> jdbc.update("update produto set estoque_minimo = 0 where id = ?", produto))
                .doesNotThrowAnyException();
        assertViolacao(() -> jdbc.update("update produto set estoque_minimo = -1 where id = ?", produto),
                "chk_produto_estoque_minimo");
    }

    @Test
    void categoriaInexistenteDeveSerRejeitada() {
        assertViolacao(() -> jdbc.update("update produto set categoria_id = 999999 where id = ?", produto),
                "fk_produto_categoria");
    }

    // ---------------------------------------------------------- categoria

    /**
     * [achado] A história H3 pede unicidade do nome sem diferenciar maiúsculas/minúsculas e sem
     * espaços externos, "garantida também no banco". A migration só tem UNIQUE simples.
     */
    @Test
    void categoriaComCaixaDiferenteHojeEhAceitaApesarDaHistoriaH3() {
        assertThatCode(() -> jdbc.update("insert into categoria (nome) values ('bebida')"))
                .doesNotThrowAnyException();
    }

    @Test
    void categoriaComNomeExatoDuplicadoDeveSerRejeitada() {
        assertViolacao(() -> jdbc.update("insert into categoria (nome) values ('BEBIDA')"), "categoria_nome_key");
    }

    /** [achado] H3 permite nome de até 255 caracteres; a coluna é VARCHAR(100). */
    @Test
    void categoriaComNomeCom101CaracteresEhRejeitadaApesarDaHistoriaH3() {
        assertThatThrownBy(() -> jdbc.update("insert into categoria (nome) values (?)", "x".repeat(101)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------- estoque

    @Test
    void loteValido() {
        assertThatCode(() -> insertLote(produto, 10, 10, bd("2.50"), bd("25.00"))).doesNotThrowAnyException();
    }

    /**
     * [achado] O dicionário diz que custo_unitario/custo_total são NULOS em lote de doação e ajuste.
     * A migration tem NOT NULL DEFAULT 0, então "sem custo" vira 0 e é indistinguível de custo zero.
     */
    @Test
    void loteSemCustoNasceComZeroEmVezDeNulo() {
        jdbc.update("insert into estoque (produto_id, quantidade_inicial, quantidade_atual) values (?, 5, 5)", produto);

        assertThat(jdbc.queryForObject("select custo_unitario || '|' || custo_total from estoque", String.class))
                .isEqualTo("0.00|0.00");
    }

    @Test
    void custoNuloEhRejeitadoHojeDivergindoDoDicionario() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into estoque (produto_id, quantidade_inicial, quantidade_atual, custo_unitario, custo_total)
                values (?, 5, 5, null, null)""", produto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void quantidadeInicialZeroDeveSerRejeitada() {
        assertViolacao(() -> insertLote(produto, 0, 0, bd("1.00"), bd("0.00")), "chk_estoque_quantidade_inicial");
    }

    @Test
    void quantidadeAtualZeroEhPermitidaENegativaNao() {
        assertThatCode(() -> insertLote(produto, 10, 0, bd("1.00"), bd("10.00"))).doesNotThrowAnyException();
        assertViolacao(() -> insertLote(produto, 10, -1, bd("1.00"), bd("10.00")), "chk_estoque_quantidade_atual");
    }

    @Test
    void custosNegativosDevemSerRejeitados() {
        assertViolacao(() -> insertLote(produto, 10, 10, bd("-1.00"), bd("10.00")), "chk_estoque_custo_unitario");
    }

    @Test
    void custoTotalNegativoDeveSerRejeitado() {
        assertViolacao(() -> insertLote(produto, 10, 10, bd("1.00"), bd("-10.00")), "chk_estoque_custo_total");
    }

    /** Documenta: nada impede quantidade_atual > quantidade_inicial; a regra fica só na aplicação. */
    @Test
    void quantidadeAtualMaiorQueInicialNaoEhBarradaPeloBanco() {
        assertThatCode(() -> insertLote(produto, 5, 8, bd("1.00"), bd("5.00"))).doesNotThrowAnyException();
    }

    @Test
    void produtoInexistenteDeveSerRejeitado() {
        assertViolacao(() -> insertLote(999_999L, 5, 5, bd("1.00"), bd("5.00")), "fk_estoque_produto");
    }
}