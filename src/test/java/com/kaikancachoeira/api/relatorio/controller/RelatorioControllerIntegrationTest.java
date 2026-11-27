package com.kaikancachoeira.api.relatorio.controller;

import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelatorioControllerIntegrationTest extends DbIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    private ResultActions relatorio(Long eventoId) throws Exception {
        return relatorio(eventoId, auth("RELATORIOS_MENU"));
    }

    private ResultActions relatorio(Long eventoId, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/eventos/{id}/relatorio", eventoId)
                .header(AUTHORIZATION, token));
    }

    private ResultActions relatorioGeral(String dataInicio, String dataFim, String tipos) throws Exception {
        return relatorioGeral(auth("RELATORIOS_MENU"), dataInicio, dataFim, tipos);
    }

    private ResultActions relatorioGeral(
            String token,
            String dataInicio,
            String dataFim,
            String tipos) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/relatorios/geral")
                .header(AUTHORIZATION, token);
        if (dataInicio != null) {
            request.param("dataInicio", dataInicio);
        }
        if (dataFim != null) {
            request.param("dataFim", dataFim);
        }
        if (tipos != null) {
            request.param("tipos", tipos);
        }
        return mockMvc.perform(request);
    }

    private ResultActions produtosMaisVendidos(
            String token,
            String dataInicio,
            String dataFim,
            String tipos,
            String limite) throws Exception {
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.get("/relatorios/produtos-mais-vendidos")
                        .header(AUTHORIZATION, token);
        if (dataInicio != null) {
            request.param("dataInicio", dataInicio);
        }
        if (dataFim != null) {
            request.param("dataFim", dataFim);
        }
        if (tipos != null) {
            request.param("tipos", tipos);
        }
        if (limite != null) {
            request.param("limite", limite);
        }
        return mockMvc.perform(request);
    }

    private Long insertMov(Long loteId, String tipo, Long eventoId, Long doacaoId, Long itemVendaId,
                           int quantidade, BigDecimal custoUnitario, BigDecimal custoTotal) {
        return jdbc.queryForObject("""
                insert into movimentacao_estoque
                    (estoque_id, tipo_movimentacao_id, evento_id, doacao_id, item_venda_id,
                     quantidade, custo_unitario, custo_total)
                values (?, ?, cast(? as bigint), cast(? as bigint), cast(? as bigint), ?, ?, ?)
                returning id""",
                Long.class, loteId, tipoMov(tipo), eventoId, doacaoId, itemVendaId,
                quantidade, custoUnitario, custoTotal);
    }

    private void insertCusto(Long eventoId, String nome, String descricao, BigDecimal valor) {
        jdbc.update("insert into custo_evento (evento_id, nome, descricao, valor) values (?, ?, ?, ?)",
                eventoId, nome, descricao, valor);
    }

    private void definirDataDaVenda(Long vendaId, String instante) {
        jdbc.update("update venda set criado_em = ? where id = ?", OffsetDateTime.parse(instante), vendaId);
    }

    private Long cenarioCompleto(String statusDoEvento) {
        Long ev = insertEvento("Festa", "EVENTO", statusDoEvento);
        Long outro = insertEvento("Outra festa", "EVENTO", "ENCERRADO");

        Long coxinha = insertProduto("Coxinha");
        Long refri = insertProduto("Refrigerante");
        Long loteCoxinha = insertLote(coxinha, 10, 2, bd("1.00"), bd("10.00"));
        Long loteRefri = insertLote(refri, 4, 4, bd("2.50"), bd("10.00"));
        Long loteDoado = insertLote(coxinha, 6, 6, bd("0"), bd("0"));

        Long pe1 = insertPeEstoque(ev, coxinha, "Coxinha", bd("5.00"), 20);
        Long pe2 = insertPeProduzido(ev, "Bolo", bd("8.00"), bd("30.00"));
        Long pe3 = insertPeEstoque(ev, refri, "Refrigerante", bd("6.00"), 10);
        jdbc.update("update produto_evento set categoria = 'COMIDA' where id = ?", pe1);
        jdbc.update("update produto_evento set categoria = 'BEBIDA' where id = ?", pe3);

        Long v1 = insertVenda(ev, bd("1.50"), bd("20.00"));
        Long i11 = insertItemVenda(v1, pe1, 3, bd("5.00"), bd("0.50"));
        insertItemVenda(v1, pe2, 1, bd("8.00"), bd("0"));
        Long v2 = insertVenda(ev, bd("0"), bd("9.00"));
        Long i21 = insertItemVenda(v2, pe1, 2, bd("5.00"), bd("0.50"));
        Long v3 = insertVenda(ev, bd("0"), bd("25.00"));
        Long i31 = insertItemVenda(v3, pe1, 5, bd("5.00"), bd("0"));
        jdbc.update("update venda set cancelada_em = ? where id = ?", T0.plusHours(1), v3);
        definirDataDaVenda(v1, "2026-11-01T13:10:00Z");
        definirDataDaVenda(v2, "2026-11-01T17:00:00Z");
        definirDataDaVenda(v3, "2026-11-01T15:00:00Z");

        insertMov(loteCoxinha, "REABASTECIMENTO", ev, null, null, 10, bd("1.00"), bd("10.00"));
        insertMov(loteRefri, "REABASTECIMENTO", ev, null, null, 4, bd("2.50"), bd("10.00"));
        insertMov(loteCoxinha, "VENDA", null, null, i11, -3, bd("1.00"), bd("3.00"));
        insertMov(loteCoxinha, "VENDA", null, null, i21, -2, bd("1.00"), bd("2.00"));
        insertMov(loteCoxinha, "VENDA", null, null, i31, -5, bd("1.00"), bd("5.00"));
        insertMov(loteCoxinha, "ESTORNO_VENDA", null, null, i31, 5, bd("1.00"), bd("5.00"));
        insertMov(loteCoxinha, "PERDA", ev, null, null, -2, bd("1.00"), bd("2.00"));
        insertMov(loteCoxinha, "REABASTECIMENTO", outro, null, null, 50, bd("1.00"), bd("50.00"));

        insertDoacao(ev, "DINHEIRO", null, null, null, bd("50.00"), "PIX", "Ana", bd("2.00"));
        insertDoacao(ev, "PRODUTO", null, "Arroz", null, null, null, "Bia", bd("1.50"));
        Long doacaoReab = insertDoacao(ev, "REABASTECIMENTO", coxinha, "Coxinha", 6, null, null, "Cid", null);
        insertMov(loteDoado, "DOACAO", ev, doacaoReab, null, 6, bd("0"), bd("0"));
        insertDoacao(outro, "DINHEIRO", null, null, null, bd("99.00"), "PIX", "Zed", bd("9.00"));

        insertCusto(ev, "Aluguel", "mesas", bd("40.00"));
        insertCusto(ev, "Gás", null, bd("10.00"));
        insertCusto(outro, "Outro custo", null, bd("77.00"));

        insertPagamento(ev, "VENDA_FICHA", bd("30.00"), "PIX", null);
        Long pagamentoFiado = insertPagamento(ev, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        insertFiadoPendente(pagamentoFiado, bd("20.00"));
        insertPagamento(ev, "DEVOLUCAO_FICHA", bd("5.00"), "PIX", null);

        return ev;
    }

    private Long cenarioDeReuniao(String statusDoEvento) {
        Long reuniao = insertEvento("Reunião mensal", "REUNIAO", statusDoEvento);
        Long coxinha = insertProduto("Coxinha");
        Long lote = insertLote(coxinha, 10, 6, bd("1.50"), bd("15.00"));
        Long loteDoado = insertLote(coxinha, 3, 3, bd("0"), bd("0"));

        insertMov(lote, "REABASTECIMENTO", reuniao, null, null, 10, bd("1.50"), bd("15.00"));
        insertMov(lote, "CONSUMO_INTERNO", reuniao, null, null, -4, bd("1.50"), bd("6.00"));
        Long doacao = insertDoacao(reuniao, "REABASTECIMENTO", coxinha, "Coxinha", 3, null, null, "Ana", null);
        insertMov(loteDoado, "DOACAO", reuniao, doacao, null, 3, bd("0"), bd("0"));
        insertCusto(reuniao, "Água", "garrafões", bd("12.00"));
        return reuniao;
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "ENCERRADO");

        mockMvc.perform(MockMvcRequestBuilders.get("/eventos/{id}/relatorio", ev))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "ENCERRADO");

        relatorio(ev, auth("EVENTOS_MENU"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void eventoInexistenteDeveRetornar404() throws Exception {
        relatorio(999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void eventoNaoIniciadoDeveRetornar409() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "NAO_INICIADO");

        relatorio(ev)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NAO_INICIADO"));
    }

    @Test
    void reuniaoNaoIniciadaDeveRetornar409() throws Exception {
        Long reuniao = insertEvento("Reunião", "REUNIAO", "NAO_INICIADO");

        relatorio(reuniao)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NAO_INICIADO"));
    }

    @Test
    void eventoEncerradoDeveGerarRelatorioFinal() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value(ev.intValue()))
                .andExpect(jsonPath("$.nome").value("Festa"))
                .andExpect(jsonPath("$.tipoEvento").value("EVENTO"))
                .andExpect(jsonPath("$.status.codigo").value("ENCERRADO"))
                .andExpect(jsonPath("$.parcial").value(false));
    }

    @Test
    void eventoEmAndamentoDeveGerarRelatorioParcial() throws Exception {
        Long ev = cenarioCompleto("EM_ANDAMENTO");

        relatorio(ev)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.codigo").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.parcial").value(true));
    }

    @Test
    void resumoDeveCalcularReceitasCustosELucro() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.resumo.receitaVendas").value(29.0))
                .andExpect(jsonPath("$.resumo.descontosVendas").value(1.5))
                .andExpect(jsonPath("$.resumo.custoCompras").value(20.0))
                .andExpect(jsonPath("$.resumo.custoFichasAgradecimento").value(3.5))
                .andExpect(jsonPath("$.resumo.custoIngredientes").value(30.0))
                .andExpect(jsonPath("$.resumo.totalCustosAdicionais").value(50.0))
                .andExpect(jsonPath("$.resumo.doacoesEmDinheiro").value(50.0))
                .andExpect(jsonPath("$.resumo.receitaTotal").value(79.0))
                .andExpect(jsonPath("$.resumo.custoTotal").value(103.5))
                .andExpect(jsonPath("$.resumo.lucro").value(-24.5))
                .andExpect(jsonPath("$.resumo.vendas").value(2));
    }

    @Test
    void vendaCanceladaNaoDeveContarEmNenhumValor() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.resumo.vendas").value(2))
                .andExpect(jsonPath("$.resumo.receitaVendas").value(29.0))
                .andExpect(jsonPath("$.produtos[?(@.nome == 'Coxinha')].quantidadeVendida").value(5))
                .andExpect(jsonPath("$.produtosMaisVendidos.quantidadeVendasTotais").value(6))
                .andExpect(jsonPath("$.vendasPorHora.quantidadeVendasTotais").value(2));
    }

    @Test
    void produtosDevemListarTodosOsItensDoCardapioInclusiveSemVenda() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.produtos", hasSize(3)))
                .andExpect(jsonPath("$.produtos[0].nome").value("Bolo"))
                .andExpect(jsonPath("$.produtos[0].origem").value("PRODUZIDO"))
                .andExpect(jsonPath("$.produtos[0].quantidadeVendida").value(1))
                .andExpect(jsonPath("$.produtos[0].totalDescontos").value(0.0))
                .andExpect(jsonPath("$.produtos[0].custoTotal").value(30.0))
                .andExpect(jsonPath("$.produtos[0].receitaVendas").value(8.0))
                .andExpect(jsonPath("$.produtos[0].lucroBruto").value(8.0))
                .andExpect(jsonPath("$.produtos[0].lucroLiquido").value(-22.0))
                .andExpect(jsonPath("$.produtos[0].precoVendido").value(8.0))
                .andExpect(jsonPath("$.produtos[1].nome").value("Coxinha"))
                .andExpect(jsonPath("$.produtos[1].origem").value("ESTOQUE"))
                .andExpect(jsonPath("$.produtos[1].quantidadePrevista").value(20))
                .andExpect(jsonPath("$.produtos[1].quantidadeVendida").value(5))
                .andExpect(jsonPath("$.produtos[1].totalDescontos").value(2.5))
                .andExpect(jsonPath("$.produtos[1].custoTotal").value(5.0))
                .andExpect(jsonPath("$.produtos[1].receitaVendas").value(22.5))
                .andExpect(jsonPath("$.produtos[1].lucroBruto").value(22.5))
                .andExpect(jsonPath("$.produtos[1].lucroLiquido").value(17.5))
                .andExpect(jsonPath("$.produtos[1].precoVendido").value(5.0))
                .andExpect(jsonPath("$.produtos[2].nome").value("Refrigerante"))
                .andExpect(jsonPath("$.produtos[2].quantidadePrevista").value(10))
                .andExpect(jsonPath("$.produtos[2].quantidadeVendida").value(0))
                .andExpect(jsonPath("$.produtos[2].custoTotal").value(0.0))
                .andExpect(jsonPath("$.produtos[2].lucroBruto").value(0.0))
                .andExpect(jsonPath("$.produtos[2].lucroLiquido").value(0.0))
                .andExpect(jsonPath("$.produtos[2].precoVendido").value(6.0));
    }

    @Test
    void itemProduzidoNaoDeveTerQuantidadePrevista() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.produtos[0].quantidadePrevista").doesNotExist());
    }

    @Test
    void somaDoLucroBrutoMenosDescontosDaVendaDeveSerIgualAReceitaDeVendas() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        String json = relatorio(ev).andReturn().getResponse().getContentAsString();
        List<Number> brutos = com.jayway.jsonpath.JsonPath.read(json, "$.produtos[*].lucroBruto");
        Number descontos = com.jayway.jsonpath.JsonPath.read(json, "$.resumo.descontosVendas");
        Number receita = com.jayway.jsonpath.JsonPath.read(json, "$.resumo.receitaVendas");

        BigDecimal soma = brutos.stream()
                .map(n -> new BigDecimal(n.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(new BigDecimal(descontos.toString()));

        org.assertj.core.api.Assertions.assertThat(soma)
                .isEqualByComparingTo(new BigDecimal(receita.toString()));
    }

    @Test
    void loteSemCustoDeveContarZeroNoCustoDoProduto() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "ENCERRADO");
        Long prod = insertProduto("Coxinha");
        Long lote = insertLote(prod, 5, 3, bd("0"), bd("0"));
        Long pe = insertPeEstoque(ev, prod, "Coxinha", bd("5.00"), 10);
        Long venda = insertVenda(ev, bd("0"), bd("10.00"));
        Long item = insertItemVenda(venda, pe, 2, bd("5.00"), bd("0"));
        insertMov(lote, "VENDA", null, null, item, -2, bd("0"), bd("0"));

        relatorio(ev)
                .andExpect(jsonPath("$.produtos[0].custoTotal").value(0.0))
                .andExpect(jsonPath("$.produtos[0].lucroLiquido").value(10.0));
    }

    @Test
    void produtoConsumidoDeVariosLotesDeveSomarOCustoDeCadaLote() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "ENCERRADO");
        Long prod = insertProduto("Coxinha");
        Long lote1 = insertLote(prod, 2, 0, bd("1.00"), bd("2.00"));
        Long lote2 = insertLote(prod, 5, 3, bd("2.00"), bd("10.00"));
        Long pe = insertPeEstoque(ev, prod, "Coxinha", bd("5.00"), 10);
        Long venda = insertVenda(ev, bd("0"), bd("20.00"));
        Long item = insertItemVenda(venda, pe, 4, bd("5.00"), bd("0"));
        insertMov(lote1, "VENDA", null, null, item, -2, bd("1.00"), bd("2.00"));
        insertMov(lote2, "VENDA", null, null, item, -2, bd("2.00"), bd("4.00"));

        relatorio(ev)
                .andExpect(jsonPath("$.produtos[0].custoTotal").value(6.0))
                .andExpect(jsonPath("$.produtos[0].lucroLiquido").value(14.0));
    }

    @Test
    void custosAdicionaisDevemListarNomeDescricaoEValor() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.custosAdicionais", hasSize(2)))
                .andExpect(jsonPath("$.custosAdicionais[0].nome").value("Aluguel"))
                .andExpect(jsonPath("$.custosAdicionais[0].descricao").value("mesas"))
                .andExpect(jsonPath("$.custosAdicionais[0].valor").value(40.0))
                .andExpect(jsonPath("$.custosAdicionais[1].nome").value("Gás"))
                .andExpect(jsonPath("$.custosAdicionais[1].valor").value(10.0));
    }

    @Test
    void caixaDeveTrazerFichasFormasDePagamentoEFiadosEmAberto() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.caixa.fichasVendidas").value(50.0))
                .andExpect(jsonPath("$.caixa.fichasDevolvidas").value(5.0))
                .andExpect(jsonPath("$.caixa.saldoFichas").value(45.0))
                .andExpect(jsonPath("$.caixa.porFormaPagamento.PIX").value(25.0))
                .andExpect(jsonPath("$.caixa.porFormaPagamento.DINHEIRO").value(0.0))
                .andExpect(jsonPath("$.caixa.fiadosEmAberto").value(20.0))
                .andExpect(jsonPath("$.caixa.quantidadeFiadosEmAberto").value(1))
                .andExpect(jsonPath("$.caixa.totalRecebido").value(25.0));
    }

    @Test
    void caixaDeveIncluirQuitacoesDeFiadoNoTotalPorForma() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "ENCERRADO");
        Long pagamentoFiado = insertPagamento(ev, "VENDA_FICHA", bd("20.00"), "FIADO", null);
        Long fiado = insertFiadoPendente(pagamentoFiado, bd("20.00"));
        jdbc.update("""
                update fiado set quitado = true, quitado_em = now(), forma_pagamento = 'DINHEIRO'
                where id = ?""", fiado);
        insertPagamento(ev, "QUITACAO_FIADO", bd("20.00"), "DINHEIRO", fiado);

        relatorio(ev)
                .andExpect(jsonPath("$.caixa.porFormaPagamento.DINHEIRO").value(20.0))
                .andExpect(jsonPath("$.caixa.fiadosEmAberto").value(0.0))
                .andExpect(jsonPath("$.caixa.quantidadeFiadosEmAberto").value(0))
                .andExpect(jsonPath("$.caixa.totalRecebido").value(20.0));
    }

    @Test
    void doacoesDevemListarItensEDinheiroDoEvento() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.doacoes.itens", hasSize(2)))
                .andExpect(jsonPath("$.doacoes.itens[0].tipo").value("PRODUTO"))
                .andExpect(jsonPath("$.doacoes.itens[0].nome").value("Arroz"))
                .andExpect(jsonPath("$.doacoes.itens[0].doador").value("Bia"))
                .andExpect(jsonPath("$.doacoes.itens[1].tipo").value("REABASTECIMENTO"))
                .andExpect(jsonPath("$.doacoes.itens[1].nome").value("Coxinha"))
                .andExpect(jsonPath("$.doacoes.itens[1].quantidade").value(6))
                .andExpect(jsonPath("$.doacoes.emDinheiro", hasSize(1)))
                .andExpect(jsonPath("$.doacoes.emDinheiro[0].doador").value("Ana"))
                .andExpect(jsonPath("$.doacoes.emDinheiro[0].valor").value(50.0))
                .andExpect(jsonPath("$.doacoes.emDinheiro[0].formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.doacoes.emDinheiro[0].valorFicha").value(2.0))
                .andExpect(jsonPath("$.doacoes.totalEmDinheiro").value(50.0));
    }

    @Test
    void movimentacoesDoEventoDevemTrazerConsumoEPerdasVinculados() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.movimentacoesEvento", hasSize(1)))
                .andExpect(jsonPath("$.movimentacoesEvento[0].tipo").value("PERDA"))
                .andExpect(jsonPath("$.movimentacoesEvento[0].produtoNome").value("Coxinha"))
                .andExpect(jsonPath("$.movimentacoesEvento[0].quantidade").value(2))
                .andExpect(jsonPath("$.movimentacoesEvento[0].custoTotal").value(2.0));
    }

    @Test
    void rankingDeProdutosDeveOrdenarPorQuantidadeVendidaIgnorandoSemVenda() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.produtosMaisVendidos.quantidadeVendasTotais").value(6))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking", hasSize(2)))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking[0].nome").value("Coxinha"))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking[0].categoria").value("COMIDA"))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking[0].quantidadeVendida").value(5))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking[1].nome").value("Bolo"))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking[1].quantidadeVendida").value(1));
    }

    @Test
    void vendasPorHoraDevemUsarOFusoDeSaoPauloEOrdenarPorHora() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");

        relatorio(ev)
                .andExpect(jsonPath("$.vendasPorHora.quantidadeVendasTotais").value(2))
                .andExpect(jsonPath("$.vendasPorHora.ranking", hasSize(2)))
                .andExpect(jsonPath("$.vendasPorHora.ranking[0].hora").value(10))
                .andExpect(jsonPath("$.vendasPorHora.ranking[0].quantidadeVendas").value(1))
                .andExpect(jsonPath("$.vendasPorHora.ranking[1].hora").value(14))
                .andExpect(jsonPath("$.vendasPorHora.ranking[1].quantidadeVendas").value(1));
    }

    @Test
    void vendasNaMesmaHoraDevemSerAgrupadas() throws Exception {
        Long ev = insertEvento("Festa", "EVENTO", "ENCERRADO");
        Long v1 = insertVenda(ev, bd("0"), bd("5.00"));
        Long v2 = insertVenda(ev, bd("0"), bd("5.00"));
        Long v3 = insertVenda(ev, bd("0"), bd("5.00"));
        definirDataDaVenda(v1, "2026-11-01T13:05:00Z");
        definirDataDaVenda(v2, "2026-11-01T13:55:00Z");
        definirDataDaVenda(v3, "2026-11-01T02:30:00Z");

        relatorio(ev)
                .andExpect(jsonPath("$.vendasPorHora.quantidadeVendasTotais").value(3))
                .andExpect(jsonPath("$.vendasPorHora.ranking[0].hora").value(10))
                .andExpect(jsonPath("$.vendasPorHora.ranking[0].quantidadeVendas").value(2))
                .andExpect(jsonPath("$.vendasPorHora.ranking[1].hora").value(23))
                .andExpect(jsonPath("$.vendasPorHora.ranking[1].quantidadeVendas").value(1));
    }

    @Test
    void eventoSemMovimentoDeveRetornarZerosEListasVazias() throws Exception {
        Long ev = insertEvento("Festa vazia", "EVENTO", "ENCERRADO");

        relatorio(ev)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.receitaVendas").value(0.0))
                .andExpect(jsonPath("$.resumo.receitaTotal").value(0.0))
                .andExpect(jsonPath("$.resumo.custoTotal").value(0.0))
                .andExpect(jsonPath("$.resumo.lucro").value(0.0))
                .andExpect(jsonPath("$.resumo.vendas").value(0))
                .andExpect(jsonPath("$.produtos", hasSize(0)))
                .andExpect(jsonPath("$.custosAdicionais", hasSize(0)))
                .andExpect(jsonPath("$.caixa.totalRecebido").value(0.0))
                .andExpect(jsonPath("$.doacoes.itens", hasSize(0)))
                .andExpect(jsonPath("$.doacoes.emDinheiro", hasSize(0)))
                .andExpect(jsonPath("$.doacoes.totalEmDinheiro").value(0.0))
                .andExpect(jsonPath("$.movimentacoesEvento", hasSize(0)))
                .andExpect(jsonPath("$.produtosMaisVendidos.quantidadeVendasTotais").value(0))
                .andExpect(jsonPath("$.produtosMaisVendidos.ranking", hasSize(0)))
                .andExpect(jsonPath("$.vendasPorHora.quantidadeVendasTotais").value(0))
                .andExpect(jsonPath("$.vendasPorHora.ranking", hasSize(0)));
    }

    @Test
    void dadosDeOutroEventoNaoDevemEntrarNoRelatorio() throws Exception {
        Long ev = cenarioCompleto("ENCERRADO");
        Long outro = jdbc.queryForObject("select id from evento where nome = 'Outra festa'", Long.class);

        relatorio(outro)
                .andExpect(jsonPath("$.resumo.custoCompras").value(50.0))
                .andExpect(jsonPath("$.resumo.doacoesEmDinheiro").value(99.0))
                .andExpect(jsonPath("$.resumo.custoFichasAgradecimento").value(9.0))
                .andExpect(jsonPath("$.resumo.totalCustosAdicionais").value(77.0))
                .andExpect(jsonPath("$.resumo.receitaVendas").value(0.0))
                .andExpect(jsonPath("$.produtos", hasSize(0)));

        relatorio(ev)
                .andExpect(jsonPath("$.resumo.custoCompras").value(20.0));
    }

    @Test
    void reuniaoDeveRetornarSomenteOsCustos() throws Exception {
        Long reuniao = cenarioDeReuniao("ENCERRADO");

        relatorio(reuniao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEvento").value("REUNIAO"))
                .andExpect(jsonPath("$.parcial").value(false))
                .andExpect(jsonPath("$.resumo.custoCompras").value(15.0))
                .andExpect(jsonPath("$.resumo.custoConsumoInterno").value(6.0))
                .andExpect(jsonPath("$.resumo.totalCustosAdicionais").value(12.0))
                .andExpect(jsonPath("$.resumo.custoTotal").value(33.0))
                .andExpect(jsonPath("$.resumo.receitaTotal").doesNotExist())
                .andExpect(jsonPath("$.resumo.lucro").doesNotExist())
                .andExpect(jsonPath("$.produtos").doesNotExist())
                .andExpect(jsonPath("$.caixa").doesNotExist());
    }

    @Test
    void reuniaoDeveListarItensECustosAdicionais() throws Exception {
        Long reuniao = cenarioDeReuniao("ENCERRADO");

        relatorio(reuniao)
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.itens[0].tipo").value("REABASTECIMENTO"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(10))
                .andExpect(jsonPath("$.itens[0].custoTotal").value(15.0))
                .andExpect(jsonPath("$.itens[1].tipo").value("CONSUMO_INTERNO"))
                .andExpect(jsonPath("$.itens[1].quantidade").value(4))
                .andExpect(jsonPath("$.itens[1].custoTotal").value(6.0))
                .andExpect(jsonPath("$.itens[2].tipo").value("DOACAO"))
                .andExpect(jsonPath("$.itens[2].custoTotal").value(0.0))
                .andExpect(jsonPath("$.custosAdicionais", hasSize(1)))
                .andExpect(jsonPath("$.custosAdicionais[0].nome").value("Água"))
                .andExpect(jsonPath("$.custosAdicionais[0].valor").value(12.0));
    }

    @Test
    void reuniaoEmAndamentoDeveGerarRelatorioParcial() throws Exception {
        Long reuniao = cenarioDeReuniao("EM_ANDAMENTO");

        relatorio(reuniao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcial").value(true))
                .andExpect(jsonPath("$.resumo.custoTotal").value(33.0));
    }

    @Test
    void relatorioGeralDeveExigirPermissaoDeRelatorios() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/relatorios/geral"))
                .andExpect(status().isUnauthorized());

        relatorioGeral(auth("EVENTOS_MENU"), null, null, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void relatorioGeralDeveConsolidarReceitasCustosELucros() throws Exception {
        cenarioCompleto("ENCERRADO");
        Long reuniao = insertEvento("Reunião geral", "REUNIAO", "ENCERRADO");
        insertCusto(reuniao, "Custos da reunião", null, bd("33.00"));
        jdbc.update("""
                update evento
                set inicio_previsto_em = ?, encerramento_previsto_em = ?
                where nome = 'Outra festa'
                """, T0.plusDays(3), T0.plusDays(3).plusHours(8));

        relatorioGeral(
                T0.minusHours(1).toInstant().toString(),
                T0.plusHours(1).toInstant().toString(),
                null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEventos").value(1))
                .andExpect(jsonPath("$.quantidadeReunioes").value(1))
                .andExpect(jsonPath("$.quantidadeVendas").value(2))
                .andExpect(jsonPath("$.receitaVendas").value(29.0))
                .andExpect(jsonPath("$.doacoesEmDinheiro").value(50.0))
                .andExpect(jsonPath("$.receitaTotal").value(79.0))
                .andExpect(jsonPath("$.custoFichasAgradecimento").value(3.5))
                .andExpect(jsonPath("$.custosTotais").value(136.5))
                .andExpect(jsonPath("$.lucroTotal").value(-57.5))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].receitaTotal").value(79.0))
                .andExpect(jsonPath("$.itens[0].custoTotal").value(103.5))
                .andExpect(jsonPath("$.itens[1].tipoEvento").value("REUNIAO"))
                .andExpect(jsonPath("$.itens[1].receitaTotal").value(0.0))
                .andExpect(jsonPath("$.itens[1].custoTotal").value(33.0))
                .andExpect(jsonPath("$.itens[1].lucro").value(-33.0));
    }

    @Test
    void relatorioGeralSemEventosDeveRetornarTotaisZeradosEListaVazia() throws Exception {
        relatorioGeral("2026-12-01T00:00:00Z", "2026-12-02T00:00:00Z", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEventos").value(0))
                .andExpect(jsonPath("$.quantidadeReunioes").value(0))
                .andExpect(jsonPath("$.quantidadeVendas").value(0))
                .andExpect(jsonPath("$.receitaTotal").value(0.0))
                .andExpect(jsonPath("$.custosTotais").value(0.0))
                .andExpect(jsonPath("$.lucroTotal").value(0.0))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    void relatorioGeralComSomenteReunioesDeveSomarApenasCustos() throws Exception {
        cenarioDeReuniao("ENCERRADO");

        relatorioGeral(null, null, "REUNIAO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEventos").value(0))
                .andExpect(jsonPath("$.quantidadeReunioes").value(1))
                .andExpect(jsonPath("$.quantidadeVendas").value(0))
                .andExpect(jsonPath("$.receitaVendas").value(0.0))
                .andExpect(jsonPath("$.doacoesEmDinheiro").value(0.0))
                .andExpect(jsonPath("$.receitaTotal").value(0.0))
                .andExpect(jsonPath("$.custosTotais").value(33.0))
                .andExpect(jsonPath("$.lucroTotal").value(-33.0))
                .andExpect(jsonPath("$.itens", hasSize(1)));
    }

    @Test
    void relatorioGeralDeveAplicarFiltrosDePeriodoETipo() throws Exception {
        insertEvento("Festa no período", "EVENTO", "ENCERRADO", T0, T0.plusHours(8));
        insertEvento("Reunião fora do período", "REUNIAO", "ENCERRADO",
                T0.plusDays(2), T0.plusDays(2).plusHours(8));

        relatorioGeral(
                T0.minusHours(1).toInstant().toString(),
                T0.plusHours(1).toInstant().toString(),
                "EVENTO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEventos").value(1))
                .andExpect(jsonPath("$.quantidadeReunioes").value(0))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Festa no período"))
                .andExpect(jsonPath("$.itens[0].tipoEvento").value("EVENTO"));
    }

    @Test
    void relatorioGeralDeveRejeitarTipoDesconhecidoEPeriodoInvertido() throws Exception {
        relatorioGeral(null, null, "OUTRO")
                .andExpect(status().isBadRequest());

        relatorioGeral("2026-11-02T00:00:00Z", "2026-11-01T00:00:00Z", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void produtosMaisVendidosDeveConsolidarOrdenarLimitarESegregarProdutosSemVinculo()
            throws Exception {
        Long evento1 = insertEvento("Festa 1", "EVENTO", "ENCERRADO");
        Long evento2 = insertEvento("Festa 2", "EVENTO", "ENCERRADO",
                T0.plusDays(1), T0.plusDays(1).plusHours(8));
        Long produtoEstoque = insertProduto("Produto compartilhado");
        Long produtoEvento1 = insertPeEstoque(
                evento1, produtoEstoque, "Nome no primeiro evento", bd("10.00"), 20);
        Long produtoEvento2 = insertPeEstoque(
                evento2, produtoEstoque, "Nome no segundo evento", bd("12.00"), 20);
        Long boloEvento1 = insertPeProduzido(evento1, "Bolo", bd("8.00"), bd("2.00"));
        Long boloEvento2 = insertPeProduzido(evento2, "Bolo", bd("9.00"), bd("2.00"));

        Long venda1 = insertVenda(evento1, bd("0"), bd("27.00"));
        insertItemVenda(venda1, produtoEvento1, 3, bd("10.00"), bd("1.00"));
        Long venda2 = insertVenda(evento2, bd("0"), bd("24.00"));
        insertItemVenda(venda2, produtoEvento2, 2, bd("12.00"), bd("0"));
        Long venda3 = insertVenda(evento1, bd("0"), bd("32.00"));
        insertItemVenda(venda3, boloEvento1, 4, bd("8.00"), bd("0"));
        Long venda4 = insertVenda(evento2, bd("0"), bd("9.00"));
        insertItemVenda(venda4, boloEvento2, 1, bd("9.00"), bd("0"));

        produtosMaisVendidos(auth("RELATORIOS_MENU"), null, null, null, "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Produto compartilhado"))
                .andExpect(jsonPath("$.items[0].sold").value(5))
                .andExpect(jsonPath("$.items[0].revenue").value(51.0))
                .andExpect(jsonPath("$.items[1].name").value("Bolo"))
                .andExpect(jsonPath("$.items[1].sold").value(4))
                .andExpect(jsonPath("$.items[1].revenue").value(32.0));

        produtosMaisVendidos(auth("RELATORIOS_MENU"), null, null, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[2].name").value("Bolo"))
                .andExpect(jsonPath("$.items[2].sold").value(1));
    }

    @Test
    void produtosMaisVendidosDeveFiltrarPeriodoExcluirCanceladasEReunioes()
            throws Exception {
        Long eventoDentro = insertEvento(
                "Festa no período", "EVENTO", "ENCERRADO", T0, T0.plusHours(8));
        Long eventoFora = insertEvento(
                "Festa fora", "EVENTO", "ENCERRADO",
                T0.plusSeconds(1), T0.plusHours(8));
        Long reuniao = insertEvento(
                "Reunião", "REUNIAO", "ENCERRADO", T0, T0.plusHours(8));
        Long produto = insertProduto("Refrigerante");
        Long peDentro = insertPeEstoque(eventoDentro, produto, "Refrigerante", bd("5.00"), 50);
        Long peFora = insertPeEstoque(eventoFora, produto, "Refrigerante", bd("5.00"), 50);
        Long peReuniao = insertPeEstoque(reuniao, produto, "Refrigerante", bd("5.00"), 50);

        Long vendaAtiva = insertVenda(eventoDentro, bd("0"), bd("10.00"));
        insertItemVenda(vendaAtiva, peDentro, 2, bd("5.00"), bd("0"));
        Long vendaCancelada = insertVenda(eventoDentro, bd("0"), bd("50.00"));
        insertItemVenda(vendaCancelada, peDentro, 10, bd("5.00"), bd("0"));
        jdbc.update("update venda set cancelada_em = ? where id = ?",
                T0.plusHours(1), vendaCancelada);
        Long vendaFora = insertVenda(eventoFora, bd("0"), bd("100.00"));
        insertItemVenda(vendaFora, peFora, 20, bd("5.00"), bd("0"));
        Long vendaReuniao = insertVenda(reuniao, bd("0"), bd("250.00"));
        insertItemVenda(vendaReuniao, peReuniao, 50, bd("5.00"), bd("0"));

        produtosMaisVendidos(
                auth("RELATORIOS_MENU"),
                T0.toInstant().toString(),
                T0.toInstant().toString(),
                "EVENTO,REUNIAO",
                null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].sold").value(2))
                .andExpect(jsonPath("$.items[0].revenue").value(10.0));

        produtosMaisVendidos(
                auth("RELATORIOS_MENU"),
                null,
                null,
                "REUNIAO",
                null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void produtosMaisVendidosDeveExigirPermissaoEValidarLimiteEPeriodo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/relatorios/produtos-mais-vendidos"))
                .andExpect(status().isUnauthorized());

        produtosMaisVendidos(auth("EVENTOS_MENU"), null, null, null, null)
                .andExpect(status().isForbidden());

        produtosMaisVendidos(auth("RELATORIOS_MENU"), null, null, null, "0")
                .andExpect(status().isBadRequest());

        produtosMaisVendidos(
                auth("RELATORIOS_MENU"),
                "2026-11-02T00:00:00Z",
                "2026-11-01T00:00:00Z",
                null,
                null)
                .andExpect(status().isBadRequest());
    }
}
