package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DescontoControllerIntegrationTest extends ControllerIntegrationTest {

    private ResultActions aplicar(Long eventoId, Long peId, String novoPreco, String auth) throws Exception {
        return enviar(patch("/eventos/{id}/produtos/{peId}/desconto", eventoId, peId), auth,
                "{\"novoPreco\": %s}".formatted(novoPreco));
    }

    private ResultActions aplicar(Long eventoId, Long peId, String novoPreco) throws Exception {
        return aplicar(eventoId, peId, novoPreco, auth("VENDAS_MENU"));
    }

    private ResultActions remover(Long eventoId, Long peId) throws Exception {
        return enviar(delete("/eventos/{id}/produtos/{peId}/desconto", eventoId, peId), auth("VENDAS_MENU"), null);
    }

    private Long produto(Long eventoId) {
        return insertPeProduzido(eventoId, "Pastel", bd("10.00"), bd("20.00"));
    }

    @Test
    void deveAplicarDescontoPorUnidade() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "7.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(peId))
                .andExpect(jsonPath("$.precoVenda").value(10.0))
                .andExpect(jsonPath("$.desconto").value(3.0))
                .andExpect(jsonPath("$.precoComDesconto").value(7.0))
                .andExpect(jsonPath("$.descontoAplicadoEm").isNotEmpty());

        sincronizar();
        assertThat(count("select count(*) from produto_evento where id = ? and desconto = 3.00 and desconto_aplicado_em is not null",
                peId)).isEqualTo(1);
    }

    @Test
    void novoPrecoZeroDeveSerAceito() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desconto").value(10.0))
                .andExpect(jsonPath("$.precoComDesconto").value(0.0));
    }

    @Test
    void novoPrecoIgualAoPrecoDeVendaNaoGravaNada() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "10.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desconto").value(0.0))
                .andExpect(jsonPath("$.descontoAplicadoEm").doesNotExist());

        sincronizar();
        assertThat(count("select count(*) from produto_evento where id = ? and desconto = 0 and desconto_aplicado_em is null",
                peId)).isEqualTo(1);
    }

    @Test
    void novoPrecoMaiorQueOPrecoDeVendaDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "10.01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("novoPreco"));
    }

    @Test
    void novoPrecoNegativoDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void novoPrecoAusenteDeveRetornar400() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        enviar(patch("/eventos/{id}/produtos/{peId}/desconto", eventoId, peId), auth("VENDAS_MENU"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("novoPreco"));
    }

    @Test
    void produtoJaComDescontoDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);
        aplicar(eventoId, peId, "7.00").andExpect(status().isOk());

        aplicar(eventoId, peId, "6.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DESCONTO_JA_APLICADO"));
    }

    @Test
    void eventoNaoEmAndamentoDeveRetornar409() throws Exception {
        Long eventoId = insertEvento("Futuro", "EVENTO", "NAO_INICIADO");
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "7.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NAO_EM_ANDAMENTO"));
    }

    @Test
    void produtoDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        Long peOutro = produto(outro);

        aplicar(eventoId, peOutro, "7.00").andExpect(status().isNotFound());
    }

    @Test
    void eventoInexistenteDeveRetornar404() throws Exception {
        aplicar(999_999L, 1L, "7.00").andExpect(status().isNotFound());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        aplicar(eventoId, peId, "7.00", auth("CAIXA_MENU")).andExpect(status().isForbidden());
    }

    @Test
    void deveRemoverDesconto() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);
        aplicar(eventoId, peId, "7.00").andExpect(status().isOk());

        remover(eventoId, peId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desconto").value(0.0))
                .andExpect(jsonPath("$.precoComDesconto").value(10.0))
                .andExpect(jsonPath("$.descontoAplicadoEm").doesNotExist());

        sincronizar();
        assertThat(count("select count(*) from produto_evento where id = ? and desconto = 0 and desconto_aplicado_em is null",
                peId)).isEqualTo(1);
    }

    @Test
    void removerSemDescontoNaoAlteraNada() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);

        remover(eventoId, peId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desconto").value(0.0))
                .andExpect(jsonPath("$.precoComDesconto").value(10.0));
    }

    @Test
    void depoisDeRemoverPodeAplicarNovoDesconto() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);
        aplicar(eventoId, peId, "7.00").andExpect(status().isOk());
        remover(eventoId, peId).andExpect(status().isOk());

        aplicar(eventoId, peId, "5.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desconto").value(5.0));
    }

    @Test
    void removerComEventoEncerradoDeveRetornar409() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long peId = produto(eventoId);
        aplicar(eventoId, peId, "7.00").andExpect(status().isOk());
        sincronizar();
        encerrar(eventoId);

        remover(eventoId, peId).andExpect(status().isConflict());
    }

    @Test
    void removerProdutoDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoId = eventoEmAndamento();
        Long outro = insertEvento("Outro", "EVENTO", "NAO_INICIADO");
        Long peOutro = produto(outro);

        remover(eventoId, peOutro).andExpect(status().isNotFound());
    }
}
