
package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventoVendasHistoricoIntegrationTest extends DbIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken(
                "1", "TESTE", List.of(permissoes));
    }

    private String menu() {
        return auth("EVENTOS_MENU");
    }

    private ResultActions historico(Long eventoId, String token, String... params)
            throws Exception {
        MockHttpServletRequestBuilder req =
                get("/eventos/{eventoId}/vendas", eventoId);

        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, token);
        }

        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }

        return mockMvc.perform(req)
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
    }

    private ResultActions detalhe(Long eventoId, Long vendaId, String token)
            throws Exception {
        MockHttpServletRequestBuilder req =
                get("/eventos/{eventoId}/vendas/{vendaId}", eventoId, vendaId);

        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, token);
        }

        return mockMvc.perform(req);
    }

    private Long criarVenda(Long eventoId, String nomeProduto,
                            int quantidade, String criadoEm, boolean cancelada)
            throws Exception {
        Long produtoEvento = insertPeProduzido(
                eventoId, nomeProduto, bd("10.00"), bd("3.00"));

        Long venda = insertVenda(eventoId, bd("2.00"), bd("8.00"));

        insertItemVenda(
                venda, produtoEvento, quantidade, bd("10.00"), bd("2.00"));

        jdbc.update(
                "update venda set criado_em = ?::timestamptz, cancelada_em = " +
                        "case when ? then ?::timestamptz else null end where id = ?",
                criadoEm,
                cancelada,
                criadoEm,
                venda
        );

        return venda;
    }

    // ---------------------------------------------------- autorização

    @Test
    void listarSemTokenDeveRetornar401() throws Exception {
        Long evento = insertEvento(
                "Evento histórico", "EVENTO", "NAO_INICIADO");

        historico(evento, null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarExigeEventosMenu() throws Exception {
        Long evento = insertEvento(
                "Evento histórico", "EVENTO", "NAO_INICIADO");

        historico(evento, auth("VENDAS_MENU"))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalharSemTokenDeveRetornar401() throws Exception {
        Long evento = insertEvento(
                "Evento histórico", "EVENTO", "NAO_INICIADO");
        Long venda = insertVenda(evento, bd("0.00"), bd("10.00"));

        detalhe(evento, venda, null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detalharExigeEventosMenu() throws Exception {
        Long evento = insertEvento(
                "Evento histórico", "EVENTO", "NAO_INICIADO");
        Long venda = insertVenda(evento, bd("0.00"), bd("10.00"));

        detalhe(evento, venda, auth("VENDAS_MENU"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------- listagem

    @Test
    void listarEventoSemVendasDeveRetornarPaginaVazia() throws Exception {
        Long evento = insertEvento(
                "Evento vazio", "EVENTO", "NAO_INICIADO");

        historico(evento, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void listarDeveRetornarResumoDaVenda() throws Exception {
        Long evento = insertEvento(
                "Evento resumo", "EVENTO", "NAO_INICIADO");

        criarVenda(
                evento, "Tempurá", 3,
                "2026-10-20T10:00:00Z", false);

        historico(evento, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").isNumber())
                .andExpect(jsonPath("$.itens[0].criadoEm").exists())
                .andExpect(jsonPath("$.itens[0].desconto").value(2.00))
                .andExpect(jsonPath("$.itens[0].precoTotal").value(8.00))
                .andExpect(jsonPath("$.itens[0].quantidadeItens").value(3))
                .andExpect(jsonPath("$.itens[0].cancelada").value(false));
    }

    @Test
    void listarDeveOrdenarDaMaisRecenteParaAMaisAntiga() throws Exception {
        Long evento = insertEvento(
                "Evento ordenação", "EVENTO", "NAO_INICIADO");

        Long antiga = criarVenda(
                evento, "Produto antigo", 1,
                "2026-10-20T10:00:00Z", false);

        Long recente = criarVenda(
                evento, "Produto recente", 1,
                "2026-10-22T10:00:00Z", false);

        historico(evento, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value(recente))
                .andExpect(jsonPath("$.itens[1].id").value(antiga));
    }

    @Test
    void listarDeveAplicarPaginacao() throws Exception {
        Long evento = insertEvento(
                "Evento paginação", "EVENTO", "NAO_INICIADO");

        criarVenda(evento, "Produto A", 1,
                "2026-10-20T10:00:00Z", false);
        criarVenda(evento, "Produto B", 1,
                "2026-10-21T10:00:00Z", false);
        criarVenda(evento, "Produto C", 1,
                "2026-10-22T10:00:00Z", false);

        historico(evento, menu(), "page", "0", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        historico(evento, menu(), "page", "1", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void listarDeveFiltrarPorNomeDoProdutoSemDiferenciarMaiusculas()
            throws Exception {
        Long evento = insertEvento(
                "Evento filtro produto", "EVENTO", "NAO_INICIADO");

        criarVenda(evento, "Tempurá", 1,
                "2026-10-20T10:00:00Z", false);
        criarVenda(evento, "Refrigerante", 1,
                "2026-10-21T10:00:00Z", false);

        historico(evento, menu(), "q", "TEMPURÁ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].quantidadeItens").value(1));

        historico(evento, menu(), "q", "produto inexistente")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    void listarDeveFiltrarPeloIntervaloDeDataHora() throws Exception {
        Long evento = insertEvento(
                "Evento filtro data", "EVENTO", "NAO_INICIADO");

        criarVenda(evento, "Produto A", 1,
                "2026-10-20T10:00:00Z", false);
        criarVenda(evento, "Produto B", 1,
                "2026-10-21T10:00:00Z", false);
        criarVenda(evento, "Produto C", 1,
                "2026-10-22T10:00:00Z", false);

        historico(
                evento, menu(),
                "dataHoraInicio", "2026-10-21T10:00:00Z",
                "dataHoraFim", "2026-10-21T10:00:00Z"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.totalItens").value(1));
    }

    @Test
    void listarSemFiltroDeCancelamentoDeveRetornarVendasAtivasECanceladas()
            throws Exception {
        Long evento = insertEvento(
                "Evento cancelamentos", "EVENTO", "NAO_INICIADO");

        criarVenda(evento, "Produto ativo", 1,
                "2026-10-20T10:00:00Z", false);
        criarVenda(evento, "Produto cancelado", 1,
                "2026-10-21T10:00:00Z", true);

        historico(evento, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2));
    }

    @Test
    void listarCanceladaTrueDeveRetornarSomenteCanceladas() throws Exception {
        Long evento = insertEvento(
                "Evento canceladas", "EVENTO", "NAO_INICIADO");

        criarVenda(evento, "Produto ativo", 1,
                "2026-10-20T10:00:00Z", false);
        criarVenda(evento, "Produto cancelado", 1,
                "2026-10-21T10:00:00Z", true);

        historico(evento, menu(), "cancelada", "true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].cancelada").value(true));
    }

    @Test
    void listarCanceladaFalseDeveRetornarSomenteAtivas() throws Exception {
        Long evento = insertEvento(
                "Evento ativas", "EVENTO", "NAO_INICIADO");

        criarVenda(evento, "Produto ativo", 1,
                "2026-10-20T10:00:00Z", false);
        criarVenda(evento, "Produto cancelado", 1,
                "2026-10-21T10:00:00Z", true);

        historico(evento, menu(), "cancelada", "false")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].cancelada").value(false));
    }

    @Test
    void listarEventoInexistenteDeveRetornar404() throws Exception {
        historico(999_999L, menu())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    // ---------------------------------------------------- validação paginação

    @Test
    void listarComPaginaNegativaDeveRetornar400() throws Exception {
        Long evento = insertEvento(
                "Evento page inválida", "EVENTO", "NAO_INICIADO");

        historico(evento, menu(), "page", "-1")
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarComSizeZeroDeveRetornar400() throws Exception {
        Long evento = insertEvento(
                "Evento size zero", "EVENTO", "NAO_INICIADO");

        historico(evento, menu(), "size", "0")
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarComSizeMaiorQueCemDeveRetornar400() throws Exception {
        Long evento = insertEvento(
                "Evento size alto", "EVENTO", "NAO_INICIADO");

        historico(evento, menu(), "size", "101")
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------- detalhamento

    @Test
    void detalharDeveRetornarVendaComItensEValores() throws Exception {
        Long evento = insertEvento(
                "Evento detalhe", "EVENTO", "NAO_INICIADO");

        Long produtoA = insertPeProduzido(
                evento, "Tempurá", bd("10.00"), bd("3.00"));
        Long produtoB = insertPeProduzido(
                evento, "Refrigerante", bd("5.00"), bd("1.00"));

        Long venda = insertVenda(evento, bd("3.00"), bd("12.00"));

        insertItemVenda(venda, produtoA, 1, bd("10.00"), bd("2.00"));
        insertItemVenda(venda, produtoB, 1, bd("5.00"), bd("1.00"));

        String corpo = detalhe(evento, venda, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(venda))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].produtoEventoId").isNumber())
                .andExpect(jsonPath("$.itens[0].nome").exists())
                .andExpect(jsonPath("$.itens[0].quantidade").value(1))
                .andExpect(jsonPath("$.itens[0].precoUnitario").isNumber())
                .andExpect(jsonPath("$.itens[0].descontoUnitario").isNumber())
                .andExpect(jsonPath("$.desconto").value(3.00))
                .andExpect(jsonPath("$.precoTotal").value(12.00))
                .andExpect(jsonPath("$.cancelada").value(false))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("\"cancelada\":true");
    }

    @Test
    void detalharVendaCanceladaDeveRetornarDataDoCancelamento()
            throws Exception {
        Long evento = insertEvento(
                "Evento venda cancelada", "EVENTO", "NAO_INICIADO");

        Long venda = insertVenda(evento, bd("0.00"), bd("10.00"));

        jdbc.update(
                "update venda set cancelada_em = now() where id = ?",
                venda);

        detalhe(evento, venda, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(venda))
                .andExpect(jsonPath("$.cancelada").value(true))
                .andExpect(jsonPath("$.canceladaEm").isNotEmpty());
    }

    @Test
    void detalharVendaDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoA = insertEvento(
                "Evento A", "EVENTO", "NAO_INICIADO");
        Long eventoB = insertEvento(
                "Evento B", "EVENTO", "NAO_INICIADO");

        Long venda = insertVenda(eventoB, bd("0.00"), bd("10.00"));

        detalhe(eventoA, venda, menu())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void detalharVendaInexistenteDeveRetornar404() throws Exception {
        Long evento = insertEvento(
                "Evento sem essa venda", "EVENTO", "NAO_INICIADO");

        detalhe(evento, 999_999L, menu())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }
}
