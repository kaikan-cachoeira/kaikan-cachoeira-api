package com.kaikancachoeira.api.caixa.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FiadoControllerIntegrationTest extends ControllerIntegrationTest {

    private Long fiadoPendente(Long eventoId, String valor) {
        Long origem = insertPagamento(eventoId, "VENDA_FICHA", bd(valor), "FIADO", null);
        return insertFiadoPendente(origem, bd(valor));
    }

    private ResultActions quitar(Long eventoId, Long fiadoId, String corpo, String auth) throws Exception {
        return enviar(patch("/eventos/{id}/fiados/{f}/quitacao", eventoId, fiadoId), auth, corpo);
    }

    private ResultActions quitar(Long eventoId, Long fiadoId, String forma) throws Exception {
        return quitar(eventoId, fiadoId, "{\"formaPagamento\": \"%s\"}".formatted(forma), auth("CAIXA_MENU"));
    }

    private ResultActions listar(Long eventoId, String query, String auth) throws Exception {
        return enviar(get("/eventos/{id}/fiados" + query, eventoId), auth, null);
    }

    private ResultActions listar(Long eventoId, String query) throws Exception {
        return listar(eventoId, query, auth("CAIXA_MENU"));
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long eventoId = eventoEmAndamento();

        mockMvc.perform(get("/eventos/{id}/fiados", eventoId)).andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");

        listar(eventoId, "", auth("VENDAS_MENU")).andExpect(status().isForbidden());
        quitar(eventoId, fiadoId, "{\"formaPagamento\": \"PIX\"}", auth("VENDAS_MENU"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveListarTodosOsFiadosDoEvento() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        fiadoPendente(eventoId, "40.00");
        Long quitado = fiadoPendente(eventoId, "15.00");
        fiadoPendente(outro, "99.00");
        quitar(eventoId, quitado, "DINHEIRO").andExpect(status().isOk());
        sincronizar();

        listar(eventoId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].nomeDevedor").value("Fulano"))
                .andExpect(jsonPath("$[0].criadoEm").isNotEmpty());
    }

    @Test
    void deveFiltrarPorSituacao() throws Exception {
        Long eventoId = eventoEmAndamento();
        fiadoPendente(eventoId, "40.00");
        Long quitado = fiadoPendente(eventoId, "15.00");
        quitar(eventoId, quitado, "PIX").andExpect(status().isOk());
        sincronizar();

        listar(eventoId, "?situacao=pendente")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quitado").value(false))
                .andExpect(jsonPath("$[0].valor").value(40.0))
                .andExpect(jsonPath("$[0].formaPagamento").doesNotExist());
        listar(eventoId, "?situacao=quitado")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quitado").value(true))
                .andExpect(jsonPath("$[0].quitadoEm").isNotEmpty())
                .andExpect(jsonPath("$[0].formaPagamento").value("PIX"));
    }

    @Test
    void situacaoInvalidaDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();

        listar(eventoId, "?situacao=todos")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("situacao"));
    }

    @Test
    void listarEmEventoInexistenteDeveRetornar404() throws Exception {
        listar(999_999L, "").andExpect(status().isNotFound());
    }

    @Test
    void deveQuitarFiadoGerandoLancamentoEResumo() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");

        quitar(eventoId, fiadoId, "PIX")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiado.id").value(fiadoId))
                .andExpect(jsonPath("$.fiado.quitado").value(true))
                .andExpect(jsonPath("$.fiado.quitadoEm").isNotEmpty())
                .andExpect(jsonPath("$.fiado.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.lancamento.tipo").value("QUITACAO_FIADO"))
                .andExpect(jsonPath("$.lancamento.valor").value(40.0))
                .andExpect(jsonPath("$.lancamento.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.lancamento.nomeDevedor").value("Fulano"))
                .andExpect(jsonPath("$.resumo.fiadosEmAberto").value(0.0))
                .andExpect(jsonPath("$.resumo.quantidadeFiadosEmAberto").value(0))
                .andExpect(jsonPath("$.resumo.porFormaPagamento.PIX").value(40.0))
                .andExpect(jsonPath("$.resumo.totalRecebido").value(40.0));

        sincronizar();
        assertThat(count("""
                select count(*) from pagamento_caixa
                where fiado_id = ? and tipo = 'QUITACAO_FIADO' and forma_pagamento = 'PIX' and valor = 40.00
                """, fiadoId)).isEqualTo(1);
        assertThat(count("""
                select count(*) from fiado
                where id = ? and quitado = true and quitado_em is not null and forma_pagamento = 'PIX'
                """, fiadoId)).isEqualTo(1);
    }

    @Test
    void podeQuitarComEventoEncerrado() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");
        encerrar(eventoId);

        quitar(eventoId, fiadoId, "DINHEIRO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lancamento.formaPagamento").value("DINHEIRO"));
    }

    @Test
    void quitarFiadoJaQuitadoDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");
        quitar(eventoId, fiadoId, "PIX").andExpect(status().isOk());
        sincronizar();

        quitar(eventoId, fiadoId, "DINHEIRO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FIADO_JA_QUITADO"));

        sincronizar();
        assertThat(count("select count(*) from pagamento_caixa where fiado_id = ?", fiadoId)).isEqualTo(1);
    }

    @Test
    void formaFiadoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");

        quitar(eventoId, fiadoId, "FIADO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("formaPagamento"));
    }

    @Test
    void formaAusenteDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");

        quitar(eventoId, fiadoId, "{}", auth("CAIXA_MENU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("formaPagamento"));
    }

    @Test
    void formaDesconhecidaDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long fiadoId = fiadoPendente(eventoId, "40.00");

        quitar(eventoId, fiadoId, "CHEQUE").andExpect(status().isBadRequest());
    }

    @Test
    void fiadoDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        Long fiadoOutro = fiadoPendente(outro, "40.00");

        quitar(eventoId, fiadoOutro, "PIX").andExpect(status().isNotFound());

        sincronizar();
        assertThat(count("select count(*) from fiado where id = ? and quitado = false", fiadoOutro)).isEqualTo(1);
    }

    @Test
    void fiadoInexistenteDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();

        quitar(eventoId, 999_999L, "PIX").andExpect(status().isNotFound());
    }

    @Test
    void quitarEmEventoInexistenteDeveRetornar404() throws Exception {
        quitar(999_999L, 1L, "PIX").andExpect(status().isNotFound());
    }
}
