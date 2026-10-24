package com.kaikancachoeira.api.caixa.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaixaControllerIntegrationTest extends ControllerIntegrationTest {

    private ResultActions registrar(Long eventoId, String corpo, String auth) throws Exception {
        return enviar(post("/eventos/{id}/caixa", eventoId), auth, corpo);
    }

    private ResultActions registrar(Long eventoId, String corpo) throws Exception {
        return registrar(eventoId, corpo, auth("CAIXA_MENU"));
    }

    private static String lancamento(String tipo, String valor, String forma) {
        return "{\"tipo\": \"%s\", \"valor\": %s, \"formaPagamento\": \"%s\"}".formatted(tipo, valor, forma);
    }

    private static String fiado(String valor, String nome, String observacao) {
        String obs = observacao == null ? "" : ", \"observacao\": \"%s\"".formatted(observacao);
        return "{\"tipo\": \"VENDA_FICHA\", \"valor\": %s, \"formaPagamento\": \"FIADO\", \"nomeDevedor\": \"%s\"%s}"
                .formatted(valor, nome, obs);
    }

    private ResultActions listar(Long eventoId, String query) throws Exception {
        return enviar(get("/eventos/{id}/caixa" + query, eventoId), auth("CAIXA_MENU"), null);
    }

    private ResultActions excluir(Long eventoId, Long lancamentoId) throws Exception {
        return enviar(delete("/eventos/{id}/caixa/{lancamentoId}", eventoId, lancamentoId), auth("CAIXA_MENU"), null);
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long eventoId = eventoEmAndamento();

        mockMvc.perform(post("/eventos/{id}/caixa", eventoId)
                        .contentType("application/json")
                        .content(lancamento("VENDA_FICHA", "10", "PIX")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, lancamento("VENDA_FICHA", "10", "PIX"), auth("VENDAS_MENU"))
                .andExpect(status().isForbidden());
        enviar(get("/eventos/{id}/caixa", eventoId), auth("VENDAS_MENU"), null).andExpect(status().isForbidden());
        enviar(delete("/eventos/{id}/caixa/{l}", eventoId, 1L), auth("VENDAS_MENU"), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRegistrarVendaDeFichasERetornarLancamentoEResumo() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, lancamento("VENDA_FICHA", "50.00", "PIX"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lancamento.id").isNumber())
                .andExpect(jsonPath("$.lancamento.tipo").value("VENDA_FICHA"))
                .andExpect(jsonPath("$.lancamento.valor").value(50.0))
                .andExpect(jsonPath("$.lancamento.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.lancamento.criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.resumo.fichasVendidas").value(50.0))
                .andExpect(jsonPath("$.resumo.fichasDevolvidas").value(0.0))
                .andExpect(jsonPath("$.resumo.saldoFichas").value(50.0))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.PIX").value(50.0))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.DINHEIRO").value(0.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(50.0))
                .andExpect(jsonPath("$.resumo.fiadosEmAberto").value(0.0))
                .andExpect(jsonPath("$.resumo.quantidadeFiadosEmAberto").value(0));

        assertThat(count("select count(*) from pagamento_caixa where evento_id = ?", eventoId)).isEqualTo(1);
    }

    @Test
    void devolucaoDeveReduzirOTotalRecebidoNaFormaDevolvida() throws Exception {
        Long eventoId = eventoEmAndamento();
        registrar(eventoId, lancamento("VENDA_FICHA", "30.00", "DINHEIRO")).andExpect(status().isCreated());

        registrar(eventoId, lancamento("DEVOLUCAO_FICHA", "10.00", "DINHEIRO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumo.fichasVendidas").value(30.0))
                .andExpect(jsonPath("$.resumo.fichasDevolvidas").value(10.0))
                .andExpect(jsonPath("$.resumo.saldoFichas").value(20.0))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.DINHEIRO").value(20.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(20.0));
    }

    @Test
    void vendaFiadaDeveCriarFiadoENaoContarComoRecebido() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, fiado("40.00", "Maria", "paga amanhã"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lancamento.formaPagamento").value("FIADO"))
                .andExpect(jsonPath("$.lancamento.nomeDevedor").value("Maria"))
                .andExpect(jsonPath("$.resumo.fichasVendidas").value(40.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(0.0))
                .andExpect(jsonPath("$.resumo.fiadosEmAberto").value(40.0))
                .andExpect(jsonPath("$.resumo.quantidadeFiadosEmAberto").value(1));

        assertThat(count("""
                select count(*) from fiado
                where nome_devedor = 'Maria' and valor = 40.00 and observacoes = 'paga amanhã' and quitado = false
                """)).isEqualTo(1);
    }

    @Test
    void observacaoEmBrancoDeveSerGravadaComoNula() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, fiado("40.00", "Maria", "   ")).andExpect(status().isCreated());

        assertThat(count("select count(*) from fiado where observacoes is null")).isEqualTo(1);
    }

    @Test
    void nomeEObservacaoSaoIgnoradosForaDoFiado() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, """
                {"tipo": "VENDA_FICHA", "valor": 10, "formaPagamento": "PIX", "nomeDevedor": "Maria"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lancamento.nomeDevedor").doesNotExist());

        assertThat(count("select count(*) from fiado")).isZero();
    }

    @Test
    void fiadoSemNomeDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, lancamento("VENDA_FICHA", "40", "FIADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nomeDevedor"));
        registrar(eventoId, fiado("40", "   ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nomeDevedor"));
    }

    @Test
    void nomeComMaisDe150CaracteresDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, fiado("40", "a".repeat(151), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nomeDevedor"));
    }

    @Test
    void observacaoComMaisDe255CaracteresDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, fiado("40", "Maria", "a".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("observacao"));
    }

    @Test
    void fiadoEmDevolucaoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, """
                {"tipo": "DEVOLUCAO_FICHA", "valor": 10, "formaPagamento": "FIADO", "nomeDevedor": "Maria"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("formaPagamento"));
    }

    @Test
    void tipoQuitacaoNaoEhAceitoNoRegistro() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, lancamento("QUITACAO_FIADO", "10", "PIX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("tipo"));
    }

    @Test
    void valorInvalidoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, lancamento("VENDA_FICHA", "0", "PIX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("valor"));
        registrar(eventoId, lancamento("VENDA_FICHA", "-5", "PIX")).andExpect(status().isBadRequest());
        registrar(eventoId, "{\"tipo\": \"VENDA_FICHA\", \"formaPagamento\": \"PIX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("valor"));
    }

    @Test
    void tipoEFormaDePagamentoObrigatorios() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, "{\"valor\": 10, \"formaPagamento\": \"PIX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("tipo"));
        registrar(eventoId, "{\"tipo\": \"VENDA_FICHA\", \"valor\": 10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("formaPagamento"));
    }

    @Test
    void formaDePagamentoDesconhecidaDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        registrar(eventoId, lancamento("VENDA_FICHA", "10", "CHEQUE")).andExpect(status().isBadRequest());
    }

    @Test
    void eventoNaoEmAndamentoDeveRetornar409() throws Exception {
        Long eventoId = insertEvento("Futuro", "EVENTO", "NAO_INICIADO");

        registrar(eventoId, lancamento("VENDA_FICHA", "10", "PIX"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NAO_EM_ANDAMENTO"));
    }

    @Test
    void eventoInexistenteDeveRetornar404() throws Exception {
        registrar(999_999L, lancamento("VENDA_FICHA", "10", "PIX")).andExpect(status().isNotFound());
        listar(999_999L, "").andExpect(status().isNotFound());
    }

    @Test
    void historicoDeveListarDoMaisRecenteParaOMaisAntigoComResumo() throws Exception {
        Long eventoId = eventoEmAndamento();
        insertPagamento(eventoId, "VENDA_FICHA", bd("10.00"), "PIX", null);
        insertPagamento(eventoId, "VENDA_FICHA", bd("20.00"), "DINHEIRO", null);
        insertPagamento(eventoId, "DEVOLUCAO_FICHA", bd("5.00"), "DINHEIRO", null);

        listar(eventoId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(3)))
                .andExpect(jsonPath("$.lancamentos.itens[0].tipo").value("DEVOLUCAO_FICHA"))
                .andExpect(jsonPath("$.lancamentos.itens[2].valor").value(10.0))
                .andExpect(jsonPath("$.lancamentos.page").value(0))
                .andExpect(jsonPath("$.lancamentos.size").value(20))
                .andExpect(jsonPath("$.lancamentos.totalItens").value(3))
                .andExpect(jsonPath("$.lancamentos.totalPaginas").value(1))
                .andExpect(jsonPath("$.resumo.fichasVendidas").value(30.0))
                .andExpect(jsonPath("$.resumo.fichasDevolvidas").value(5.0))
                .andExpect(jsonPath("$.resumo.saldoFichas").value(25.0))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.PIX").value(10.0))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.DINHEIRO").value(15.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(25.0));
    }

    @Test
    void historicoNaoMisturaLancamentosDeOutroEvento() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        insertPagamento(outro, "VENDA_FICHA", bd("99.00"), "PIX", null);

        listar(eventoId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(0)))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(0.0));
    }

    @Test
    void historicoDeveFiltrarPorTipoEFormaDePagamento() throws Exception {
        Long eventoId = eventoEmAndamento();
        insertPagamento(eventoId, "VENDA_FICHA", bd("10.00"), "PIX", null);
        insertPagamento(eventoId, "VENDA_FICHA", bd("20.00"), "DINHEIRO", null);
        insertPagamento(eventoId, "DEVOLUCAO_FICHA", bd("5.00"), "DINHEIRO", null);

        listar(eventoId, "?tipo=DEVOLUCAO_FICHA")
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(1)))
                .andExpect(jsonPath("$.lancamentos.totalItens").value(1));
        listar(eventoId, "?formaPagamento=DINHEIRO")
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(2)));
        listar(eventoId, "?tipo=VENDA_FICHA&formaPagamento=PIX")
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(1)))
                .andExpect(jsonPath("$.lancamentos.itens[0].valor").value(10.0));
        listar(eventoId, "?tipo=VENDA_FICHA").andExpect(jsonPath("$.resumo.fichasDevolvidas").value(5.0));
    }

    @Test
    void historicoDeveSerPaginado() throws Exception {
        Long eventoId = eventoEmAndamento();
        for (int i = 1; i <= 3; i++) {
            insertPagamento(eventoId, "VENDA_FICHA", bd(i + ".00"), "PIX", null);
        }

        listar(eventoId, "?page=0&size=2")
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(2)))
                .andExpect(jsonPath("$.lancamentos.size").value(2))
                .andExpect(jsonPath("$.lancamentos.totalItens").value(3))
                .andExpect(jsonPath("$.lancamentos.totalPaginas").value(2));
        listar(eventoId, "?page=1&size=2")
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(1)))
                .andExpect(jsonPath("$.lancamentos.page").value(1))
                .andExpect(jsonPath("$.lancamentos.itens[0].valor").value(1.0));
    }

    @Test
    void tamanhoDaPaginaDeveSerLimitadoA100() throws Exception {
        Long eventoId = eventoEmAndamento();

        listar(eventoId, "?size=500").andExpect(jsonPath("$.lancamentos.size").value(100));
    }

    @Test
    void historicoDeveTrazerNomeDoDevedorNasVendasFiadasENasQuitacoes() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long origem = insertPagamento(eventoId, "VENDA_FICHA", bd("40.00"), "FIADO", null);
        Long fiadoId = insertFiadoPendente(origem, bd("40.00"));
        enviar(patch("/eventos/{id}/fiados/{f}/quitacao", eventoId, fiadoId), auth("CAIXA_MENU"),
                "{\"formaPagamento\": \"PIX\"}").andExpect(status().isOk());
        sincronizar();

        listar(eventoId, "")
                .andExpect(jsonPath("$.lancamentos.itens", hasSize(2)))
                .andExpect(jsonPath("$.lancamentos.itens[0].tipo").value("QUITACAO_FIADO"))
                .andExpect(jsonPath("$.lancamentos.itens[0].formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.lancamentos.itens[0].nomeDevedor").value("Fulano"))
                .andExpect(jsonPath("$.lancamentos.itens[1].tipo").value("VENDA_FICHA"))
                .andExpect(jsonPath("$.lancamentos.itens[1].nomeDevedor").value("Fulano"))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.PIX").value(40.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(40.0))
                .andExpect(jsonPath("$.resumo.fiadosEmAberto").value(0.0));
    }

    @Test
    void historicoFuncionaComEventoEncerrado() throws Exception {
        Long eventoId = eventoEmAndamento();
        insertPagamento(eventoId, "VENDA_FICHA", bd("10.00"), "PIX", null);
        encerrar(eventoId);

        listar(eventoId, "").andExpect(status().isOk()).andExpect(jsonPath("$.lancamentos.itens", hasSize(1)));
    }

    @Test
    void deveExcluirLancamentoSimples() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long id = insertPagamento(eventoId, "VENDA_FICHA", bd("10.00"), "PIX", null);
        insertPagamento(eventoId, "VENDA_FICHA", bd("5.00"), "DINHEIRO", null);

        excluir(eventoId, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.fichasVendidas").value(5.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(5.0));

        sincronizar();
        assertThat(count("select count(*) from pagamento_caixa where id = ?", id)).isZero();
    }

    @Test
    void excluirVendaFiadaPendenteDeveExcluirOFiado() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long origem = insertPagamento(eventoId, "VENDA_FICHA", bd("40.00"), "FIADO", null);
        insertFiadoPendente(origem, bd("40.00"));

        excluir(eventoId, origem)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.quantidadeFiadosEmAberto").value(0))
                .andExpect(jsonPath("$.resumo.fiadosEmAberto").value(0.0));

        sincronizar();
        assertThat(count("select count(*) from pagamento_caixa where id = ?", origem)).isZero();
        assertThat(count("select count(*) from fiado")).isZero();
    }

    @Test
    void excluirVendaFiadaJaQuitadaDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long origem = insertPagamento(eventoId, "VENDA_FICHA", bd("40.00"), "FIADO", null);
        Long fiadoId = insertFiadoPendente(origem, bd("40.00"));
        enviar(patch("/eventos/{id}/fiados/{f}/quitacao", eventoId, fiadoId), auth("CAIXA_MENU"),
                "{\"formaPagamento\": \"DINHEIRO\"}").andExpect(status().isOk());
        sincronizar();

        excluir(eventoId, origem)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FIADO_JA_QUITADO"));

        sincronizar();
        assertThat(count("select count(*) from fiado where id = ?", fiadoId)).isEqualTo(1);
    }

    @Test
    void excluirQuitacaoDeveReabrirOFiado() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long origem = insertPagamento(eventoId, "VENDA_FICHA", bd("40.00"), "FIADO", null);
        Long fiadoId = insertFiadoPendente(origem, bd("40.00"));
        Long quitacao = idDe(enviar(patch("/eventos/{id}/fiados/{f}/quitacao", eventoId, fiadoId), auth("CAIXA_MENU"),
                "{\"formaPagamento\": \"DINHEIRO\"}").andExpect(status().isOk()), "$.lancamento.id");
        sincronizar();

        excluir(eventoId, quitacao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.fiadosEmAberto").value(40.0))
                .andExpect(jsonPath("$.resumo.quantidadeFiadosEmAberto").value(1))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(0.0));

        sincronizar();
        assertThat(count("""
                select count(*) from fiado
                where id = ? and quitado = false and quitado_em is null and forma_pagamento is null
                """, fiadoId)).isEqualTo(1);
        assertThat(count("select count(*) from pagamento_caixa where id = ?", quitacao)).isZero();
    }

    @Test
    void excluirComEventoEncerradoDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long id = insertPagamento(eventoId, "VENDA_FICHA", bd("10.00"), "PIX", null);
        encerrar(eventoId);

        excluir(eventoId, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NAO_EM_ANDAMENTO"));

        assertThat(count("select count(*) from pagamento_caixa where id = ?", id)).isEqualTo(1);
    }

    @Test
    void excluirLancamentoDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        Long id = insertPagamento(outro, "VENDA_FICHA", bd("10.00"), "PIX", null);

        excluir(eventoId, id).andExpect(status().isNotFound());
    }

    @Test
    void excluirLancamentoInexistenteDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();

        excluir(eventoId, 999_999L).andExpect(status().isNotFound());
    }

    @Test
    void resumoComValoresDecimaisDeveSomarCorretamente() throws Exception {
        Long eventoId = eventoEmAndamento();
        registrar(eventoId, lancamento("VENDA_FICHA", "10.10", "PIX")).andExpect(status().isCreated());

        registrar(eventoId, lancamento("VENDA_FICHA", "0.20", "PIX"))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.PIX").value(10.3));

        assertThat(count("select count(*) from pagamento_caixa where valor = ?", new BigDecimal("0.20"))).isEqualTo(1);
    }
}
