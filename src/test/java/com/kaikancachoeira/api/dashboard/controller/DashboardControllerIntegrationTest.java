package com.kaikancachoeira.api.dashboard.controller;

import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerIntegrationTest extends DbIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    private ResultActions get(String uri, String token, String... params) throws Exception {
        var req = MockMvcRequestBuilders.get(uri).header(AUTHORIZATION, token);
        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(req);
    }

    private void marcarInicio(Long eventoId, OffsetDateTime iniciadoEm) {
        jdbc.update("update evento set iniciado_em = ? where id = ?", iniciadoEm, eventoId);
    }

    private void marcarInicioEStatus(Long eventoId, OffsetDateTime iniciadoEm, String status) {
        jdbc.update("update evento set iniciado_em = ?, status_id = (select id from status_evento where codigo = ?) where id = ?",
                iniciadoEm, status, eventoId);
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

    private void inserirCusto(Long eventoId, String nome, BigDecimal valor) {
        jdbc.update("insert into custo_evento (evento_id, nome, valor) values (?, ?, ?)", eventoId, nome, valor);
    }

    private Long criarCenarioDashboard() {
        Long eventoPeriodo = insertEvento("Festa principal", "EVENTO", "ENCERRADO", T0, T0.plusHours(8));
        marcarInicioEStatus(eventoPeriodo, T0, "ENCERRADO");

        Long eventoForaDoPeriodo = insertEvento("Festa antiga", "EVENTO", "ENCERRADO", T0, T0.plusHours(8));
        marcarInicio(eventoForaDoPeriodo, T0.minusMonths(7));

        Long reuniao = insertEvento("Reunião", "REUNIAO", "ENCERRADO", T0, T0.plusHours(2));
        marcarInicio(reuniao, T0);

        Long coxinha = insertProduto("Coxinha");
        Long loteCoxinha = insertLote(coxinha, 4, 4, bd("2.00"), bd("8.00"));
        jdbc.update("update produto set quantidade_atual = ?, estoque_minimo = ? where id = ?", 4, 10, coxinha);

        Long feijao = insertProduto("Feijão");
        insertLote(feijao, 3, 3, bd("1.00"), bd("3.00"));
        jdbc.update("update produto set quantidade_atual = ?, estoque_minimo = null where id = ?", -2, feijao);

        Long bolo = insertPeProduzido(eventoPeriodo, "Bolo", bd("20.00"), bd("7.00"));
        Long peCoxinha = insertPeEstoque(eventoPeriodo, coxinha, "Coxinha", bd("10.00"), 20);

        Long venda1 = insertVenda(eventoPeriodo, bd("0.00"), bd("40.00"));
        Long item1 = insertItemVenda(venda1, peCoxinha, 2, bd("10.00"), bd("0.00"));
        insertItemVenda(venda1, bolo, 1, bd("20.00"), bd("0.00"));

        Long venda2 = insertVenda(eventoPeriodo, bd("0.00"), bd("10.00"));
        Long item2 = insertItemVenda(venda2, peCoxinha, 1, bd("10.00"), bd("0.00"));

        Long vendaCancelada = insertVenda(eventoPeriodo, bd("0.00"), bd("100.00"));
        insertItemVenda(vendaCancelada, peCoxinha, 5, bd("20.00"), bd("0.00"));
        jdbc.update("update venda set cancelada_em = ? where id = ?", T0.plusHours(4), vendaCancelada);

        Long doacao = insertDoacao(eventoPeriodo, "DINHEIRO", null, null, null, bd("15.00"), "PIX", "Ana", bd("2.00"));

        insertMov(loteCoxinha, "REABASTECIMENTO", eventoPeriodo, null, null,
                4, bd("2.00"), bd("8.00"));
        insertMov(loteCoxinha, "VENDA", null, null, item1,
                -2, bd("2.00"), bd("4.00"));
        insertMov(loteCoxinha, "VENDA", null, null, item2,
                -1, bd("2.00"), bd("2.00"));

        inserirCusto(eventoPeriodo, "Aluguel", bd("3.00"));

        // Garante que a doação existe, ainda que o resumo não a use diretamente aqui.
        jdbc.queryForObject("select id from doacao where id = ?", Long.class, doacao);

        return eventoPeriodo;
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard/resumo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        get("/dashboard/resumo", auth("EVENTOS_MENU"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void resumoDeveCalcularIndicadoresDoPeriodo() throws Exception {
        criarCenarioDashboard();

        get("/dashboard/resumo", auth("DASHBOARD_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(1))
                .andExpect(jsonPath("$.receitaTotal").value(65.0))
                .andExpect(jsonPath("$.custoTotal").value(20.0))
                .andExpect(jsonPath("$.lucroTotal").value(45.0));
    }

    @Test
    void eventosDeveRetornarSerieCronologicaComValores() throws Exception {
        Long eventoPeriodo = criarCenarioDashboard();

        get("/dashboard/eventos", auth("DASHBOARD_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventoId").value(eventoPeriodo.intValue()))
                .andExpect(jsonPath("$[0].nome").value("Festa principal"))
                .andExpect(jsonPath("$[0].receita").value(65.0))
                .andExpect(jsonPath("$[0].custo").value(20.0))
                .andExpect(jsonPath("$[0].lucro").value(45.0));
    }

    @Test
    void produtosMaisVendidosDeveOrdenarPorReceitaECalcularParticipacao() throws Exception {
        criarCenarioDashboard();

        get("/dashboard/produtos-mais-vendidos", auth("DASHBOARD_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Coxinha"))
                .andExpect(jsonPath("$[0].quantidade").value(3))
                .andExpect(jsonPath("$[0].receita").value(30.0))
                .andExpect(jsonPath("$[0].participacao").value(60.0))
                .andExpect(jsonPath("$[0].participacaoAcumulada").value(60.0))
                .andExpect(jsonPath("$[1].nome").value("Bolo"))
                .andExpect(jsonPath("$[1].quantidade").value(1))
                .andExpect(jsonPath("$[1].receita").value(20.0))
                .andExpect(jsonPath("$[1].participacao").value(40.0))
                .andExpect(jsonPath("$[1].participacaoAcumulada").value(100.0));
    }

    @Test
    void estoqueDeveRetornarValorEListasDeAlerta() throws Exception {
        criarCenarioDashboard();

        get("/dashboard/estoque", auth("DASHBOARD_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorEstoque").value(11.0))
                .andExpect(jsonPath("$.produtosAbaixoDoMinimo", hasSize(1)))
                .andExpect(jsonPath("$.produtosAbaixoDoMinimo[0].nome").value("Coxinha"))
                .andExpect(jsonPath("$.produtosNegativos", hasSize(1)))
                .andExpect(jsonPath("$.produtosNegativos[0].nome").value("Feijão"));
    }

    @Test
    void periodoInvalidoDeveRetornar400() throws Exception {
        get("/dashboard/resumo", auth("DASHBOARD_MENU"),
                "dataInicio", "2026-11-10T00:00:00Z",
                "dataFim", "2026-11-01T00:00:00Z")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("dataFim"));
    }
}
