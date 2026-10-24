package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VendaControllerIntegrationTest extends ControllerIntegrationTest {

    private ResultActions vender(Long eventoId, String corpo, String auth) throws Exception {
        return enviar(post("/eventos/{id}/vendas", eventoId), auth, corpo);
    }

    private ResultActions vender(Long eventoId, String corpo) throws Exception {
        return vender(eventoId, corpo, auth("VENDAS_MENU"));
    }

    private static String item(Long peId, int quantidade) {
        return "{\"produtoEventoId\": %d, \"quantidade\": %d}".formatted(peId, quantidade);
    }

    private static String venda(String... itens) {
        return "{\"itens\": [%s]}".formatted(String.join(",", itens));
    }

    private static String vendaComValorFinal(String valorFinal, String... itens) {
        return "{\"valorFinal\": %s, \"itens\": [%s]}".formatted(valorFinal, String.join(",", itens));
    }

    private int quantidadeLote(Long produtoId) {
        return count("select coalesce(sum(quantidade_atual), 0) from estoque where produto_id = ?", produtoId);
    }

    private int quantidadeProduto(Long produtoId) {
        return count("select quantidade_atual from produto where id = ?", produtoId);
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long eventoId = eventoEmAndamento();

        mockMvc.perform(post("/eventos/{id}/vendas", eventoId)
                        .contentType("application/json")
                        .content(venda(item(1L, 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long eventoId = eventoEmAndamento();

        vender(eventoId, venda(item(1L, 1)), auth("CAIXA_MENU"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void deveRegistrarVendaCalcularTotalEBaixarEstoque() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = produtoComLote("Yakisoba", 10, "4.00");
        Long peId = insertPeEstoque(eventoId, produtoId, "Yakisoba", bd("10.00"), 10);

        vender(eventoId, venda(item(peId, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.desconto").value(0.0))
                .andExpect(jsonPath("$.precoTotal").value(30.0));

        sincronizar();
        assertThat(quantidadeLote(produtoId)).isEqualTo(7);
        assertThat(quantidadeProduto(produtoId)).isEqualTo(7);
        assertThat(count("select count(*) from item_venda")).isEqualTo(1);
        assertThat(count("""
                select count(*) from movimentacao_estoque m
                join tipo_movimentacao t on t.id = m.tipo_movimentacao_id
                where t.codigo = 'VENDA' and m.quantidade = -3 and m.evento_id = ?
                  and m.item_venda_id is not null and m.custo_unitario = 4.00 and m.custo_total = 12.00
                """, eventoId)).isEqualTo(1);
    }

    @Test
    void valorFinalDeveGerarDescontoDaVenda() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = produtoComLote("Yakisoba", 10, "4.00");
        Long peId = insertPeEstoque(eventoId, produtoId, "Yakisoba", bd("10.00"), 10);

        vender(eventoId, vendaComValorFinal("25.00", item(peId, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.desconto").value(5.0))
                .andExpect(jsonPath("$.precoTotal").value(25.0));
    }

    @Test
    void valorFinalZeroDeveSerAceito() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = produtoComLote("Yakisoba", 10, "4.00");
        Long peId = insertPeEstoque(eventoId, produtoId, "Yakisoba", bd("10.00"), 10);

        vender(eventoId, vendaComValorFinal("0", item(peId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.desconto").value(10.0))
                .andExpect(jsonPath("$.precoTotal").value(0.0));
    }

    @Test
    void deveUsarODescontoUnitarioVigenteComoSnapshot() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = produtoComLote("Yakisoba", 10, "4.00");
        Long peId = insertPeEstoque(eventoId, produtoId, "Yakisoba", bd("10.00"), 10);
        jdbc.update("update produto_evento set desconto = 2.00, desconto_aplicado_em = now() where id = ?", peId);

        vender(eventoId, venda(item(peId, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoTotal").value(24.0));

        sincronizar();
        assertThat(count("""
                select count(*) from item_venda
                where produto_evento_id = ? and preco_unitario = 10.00 and desconto_unitario = 2.00
                """, peId)).isEqualTo(1);
    }

    @Test
    void deveConsumirLotesEmFifoGerandoUmaMovimentacaoPorLote() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = insertProduto("Refrigerante");
        Long loteAntigo = insertLote(produtoId, 2, 2, bd("1.00"), bd("2.00"));
        Long loteNovo = insertLote(produtoId, 5, 5, bd("2.00"), bd("10.00"));
        jdbc.update("update produto set quantidade_atual = 7 where id = ?", produtoId);
        Long peId = insertPeEstoque(eventoId, produtoId, "Refrigerante", bd("5.00"), 7);

        vender(eventoId, venda(item(peId, 4))).andExpect(status().isCreated());

        sincronizar();
        assertThat(count("select quantidade_atual from estoque where id = ?", loteAntigo)).isZero();
        assertThat(count("select quantidade_atual from estoque where id = ?", loteNovo)).isEqualTo(3);
        assertThat(count("select count(*) from movimentacao_estoque where item_venda_id is not null")).isEqualTo(2);
        assertThat(count("select count(*) from movimentacao_estoque where estoque_id = ? and quantidade = -2", loteAntigo))
                .isEqualTo(1);
        assertThat(count("select count(*) from movimentacao_estoque where estoque_id = ? and quantidade = -2", loteNovo))
                .isEqualTo(1);
        assertThat(quantidadeProduto(produtoId)).isEqualTo(3);
    }

    @Test
    void saldoInsuficienteDeveRetornar409SemAlterarEstoque() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = produtoComLote("Yakisoba", 2, "4.00");
        Long peId = insertPeEstoque(eventoId, produtoId, "Yakisoba", bd("10.00"), 10);

        vender(eventoId, venda(item(peId, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SALDO_INSUFICIENTE"));

        sincronizar();
        assertThat(quantidadeLote(produtoId)).isEqualTo(2);
        assertThat(quantidadeProduto(produtoId)).isEqualTo(2);
    }

    @Test
    void quantidadeAcimaDaPrevistaNaoBloqueiaSeHouverSaldo() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = produtoComLote("Yakisoba", 10, "4.00");
        Long peId = insertPeEstoque(eventoId, produtoId, "Yakisoba", bd("10.00"), 1);

        vender(eventoId, venda(item(peId, 5))).andExpect(status().isCreated());
    }

    @Test
    void itemProduzidoNaoValidaNemBaixaEstoque() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, venda(item(peId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoTotal").value(16.0));

        sincronizar();
        assertThat(count("select count(*) from movimentacao_estoque where item_venda_id is not null")).isZero();
    }

    @Test
    void eventoNaoEmAndamentoDeveRetornar409() throws Exception {
        Long eventoId = insertEvento("Futuro", "EVENTO", "NAO_INICIADO");
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, venda(item(peId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NAO_EM_ANDAMENTO"));
    }

    @Test
    void eventoDoTipoReuniaoDeveRetornar409() throws Exception {
        Long eventoId = insertEvento("Reunião", "REUNIAO", "EM_ANDAMENTO");
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, venda(item(peId, 1))).andExpect(status().isConflict());
    }

    @Test
    void eventoInexistenteDeveRetornar409() throws Exception {
        vender(999_999L, venda(item(1L, 1))).andExpect(status().isConflict());
    }

    @Test
    void itensVaziosDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        vender(eventoId, "{\"itens\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("itens"));
    }

    @Test
    void quantidadeZeroDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, venda(item(peId, 0))).andExpect(status().isBadRequest());
    }

    @Test
    void valorFinalNegativoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, vendaComValorFinal("-1", item(peId, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("valorFinal"));
    }

    @Test
    void valorFinalMaiorQueOTotalDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, vendaComValorFinal("8.01", item(peId, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("valorFinal"));
    }

    @Test
    void produtoRepetidoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, venda(item(peId, 1), item(peId, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("itens"));
    }

    @Test
    void produtoDeOutroEventoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        Long peOutro = insertPeProduzido(outro, "Pastel", bd("8.00"), bd("20.00"));

        vender(eventoId, venda(item(peOutro, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("itens"));
    }

    private Long registrarVenda(Long eventoId, Long peId, int quantidade) throws Exception {
        return idDe(vender(eventoId, venda(item(peId, quantidade))).andExpect(status().isCreated()), "$.id");
    }

    private ResultActions cancelar(Long eventoId, Long vendaId, String auth) throws Exception {
        return enviar(post("/eventos/{id}/vendas/{vendaId}/cancelamento", eventoId, vendaId), auth, null);
    }

    @Test
    void cancelamentoDeveEstornarEstoqueNoMesmoLoteEComMesmoCusto() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long produtoId = insertProduto("Refrigerante");
        Long loteAntigo = insertLote(produtoId, 2, 2, bd("1.00"), bd("2.00"));
        Long loteNovo = insertLote(produtoId, 5, 5, bd("2.00"), bd("10.00"));
        jdbc.update("update produto set quantidade_atual = 7 where id = ?", produtoId);
        Long peId = insertPeEstoque(eventoId, produtoId, "Refrigerante", bd("5.00"), 7);
        Long vendaId = registrarVenda(eventoId, peId, 4);
        sincronizar();

        cancelar(eventoId, vendaId, auth("VENDAS_MENU")).andExpect(status().isNoContent());

        sincronizar();
        assertThat(count("select count(*) from venda where id = ? and cancelada_em is not null", vendaId)).isEqualTo(1);
        assertThat(count("select quantidade_atual from estoque where id = ?", loteAntigo)).isEqualTo(2);
        assertThat(count("select quantidade_atual from estoque where id = ?", loteNovo)).isEqualTo(5);
        assertThat(quantidadeProduto(produtoId)).isEqualTo(7);
        assertThat(count("""
                select count(*) from movimentacao_estoque m
                join tipo_movimentacao t on t.id = m.tipo_movimentacao_id
                where t.codigo = 'ESTORNO_VENDA' and m.estoque_id = ? and m.quantidade = 2
                  and m.custo_unitario = 1.00 and m.custo_total = 2.00 and m.item_venda_id is not null
                """, loteAntigo)).isEqualTo(1);
        assertThat(count("""
                select count(*) from movimentacao_estoque m
                join tipo_movimentacao t on t.id = m.tipo_movimentacao_id
                where t.codigo = 'ESTORNO_VENDA' and m.estoque_id = ? and m.quantidade = 2
                  and m.custo_unitario = 2.00 and m.custo_total = 4.00
                """, loteNovo)).isEqualTo(1);
    }

    @Test
    void cancelamentoDeItemProduzidoNaoGeraEstorno() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));
        Long vendaId = registrarVenda(eventoId, peId, 2);
        sincronizar();

        cancelar(eventoId, vendaId, auth("VENDAS_MENU")).andExpect(status().isNoContent());

        sincronizar();
        assertThat(count("select count(*) from movimentacao_estoque where item_venda_id is not null")).isZero();
    }

    @Test
    void cancelamentoPodeSerFeitoComPermissaoDeEventos() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));
        Long vendaId = registrarVenda(eventoId, peId, 1);
        sincronizar();

        cancelar(eventoId, vendaId, auth("EVENTOS_MENU")).andExpect(status().isNoContent());
    }

    @Test
    void cancelamentoSemPermissaoDeveRetornar403() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long vendaId = insertVenda(eventoId, BigDecimal.ZERO, bd("10.00"));

        cancelar(eventoId, vendaId, auth("CAIXA_MENU")).andExpect(status().isForbidden());
    }

    @Test
    void cancelarDuasVezesDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));
        Long vendaId = registrarVenda(eventoId, peId, 1);
        sincronizar();

        cancelar(eventoId, vendaId, auth("VENDAS_MENU")).andExpect(status().isNoContent());
        sincronizar();

        cancelar(eventoId, vendaId, auth("VENDAS_MENU"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VENDA_JA_CANCELADA"));
    }

    @Test
    void cancelarVendaDeEventoEncerradoDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = insertPeProduzido(eventoId, "Pastel", bd("8.00"), bd("20.00"));
        Long vendaId = registrarVenda(eventoId, peId, 1);
        sincronizar();
        encerrar(eventoId);

        cancelar(eventoId, vendaId, auth("VENDAS_MENU"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_ENCERRADO"));

        sincronizar();
        assertThat(count("select count(*) from venda where id = ? and cancelada_em is null", vendaId)).isEqualTo(1);
    }

    @Test
    void cancelarVendaDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        Long vendaId = insertVenda(outro, BigDecimal.ZERO, bd("10.00"));

        cancelar(eventoId, vendaId, auth("VENDAS_MENU")).andExpect(status().isNotFound());
    }

    @Test
    void cancelarVendaInexistenteDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();

        cancelar(eventoId, 999_999L, auth("VENDAS_MENU")).andExpect(status().isNotFound());
    }

    @Test
    void cancelarEmEventoInexistenteDeveRetornar404() throws Exception {
        cancelar(999_999L, 1L, auth("VENDAS_MENU")).andExpect(status().isNotFound());
    }
}
