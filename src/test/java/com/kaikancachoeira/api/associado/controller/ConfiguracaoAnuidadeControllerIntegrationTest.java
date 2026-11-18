package com.kaikancachoeira.api.associado.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfiguracaoAnuidadeControllerIntegrationTest extends ControllerIntegrationTest {

    private String permissao() {
        return auth("ANUIDADES_MENU", "ANUIDADES_LEITURA");
    }

    private String semPermissao() {
        return auth("VENDAS_MENU");
    }

    @Test
    void listarSemPermissaoDeveRetornar403() throws Exception {
        enviar(get("/config-anuidades"), semPermissao(), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void criarSemPermissaoDeveRetornar403() throws Exception {
        enviar(post("/config-anuidades"), semPermissao(), """
                {
                  "ano": 2027,
                  "valor": 180.00
                }
                """)
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizarSemPermissaoDeveRetornar403() throws Exception {
        Long id = insertConfiguracaoAnuidade(2027, bd("180.00"));

        enviar(put("/config-anuidades/{id}", id), semPermissao(), """
                {
                  "ano": 2028,
                  "valor": 200.00
                }
                """)
                .andExpect(status().isForbidden());
    }

    @Test
    void excluirSemPermissaoDeveRetornar403() throws Exception {
        Long id = insertConfiguracaoAnuidade(2027, bd("180.00"));

        enviar(delete("/config-anuidades/{id}", id), semPermissao(), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void listarDeveRetornarTodasAsConfiguracoes() throws Exception {
        insertConfiguracaoAnuidade(2025, bd("100.00"));
        insertConfiguracaoAnuidade(2026, bd("120.00"));

        enviar(get("/config-anuidades"), permissao(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].ano").value(containsInAnyOrder(2025, 2026)));
    }

    @Test
    void criarConfiguracaoDeveRetornar201() throws Exception {
        enviar(post("/config-anuidades"), permissao(), """
                {
                  "ano": 2027,
                  "valor": 180.00
                }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ano").value(2027))
                .andExpect(jsonPath("$.valor").value(180.00));
    }

    @Test
    void naoDevePermitirAnoDuplicado() throws Exception {
        insertConfiguracaoAnuidade(2027, bd("180.00"));

        enviar(post("/config-anuidades"), permissao(), """
                {
                  "ano": 2027,
                  "valor": 200.00
                }
                """)
                .andExpect(status().isConflict());
    }

    @Test
    void atualizarConfiguracaoDevePreservarUnicidadeDoAno() throws Exception {
        Long outra = insertConfiguracaoAnuidade(2028, bd("150.00"));
        Long alvo = insertConfiguracaoAnuidade(2029, bd("160.00"));

        enviar(put("/config-anuidades/{id}", alvo), permissao(), """
                {
                  "ano": 2030,
                  "valor": 190.00
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alvo.intValue()))
                .andExpect(jsonPath("$.ano").value(2030))
                .andExpect(jsonPath("$.valor").value(190.00));

        enviar(put("/config-anuidades/{id}", outra), permissao(), """
                {
                  "ano": 2030,
                  "valor": 200.00
                }
                """)
                .andExpect(status().isConflict());
    }

    @Test
    void excluirConfiguracaoDeveRemoverAnuidadesEPagamentosVinculados() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long associado = insertAssociado("Titular", LocalDate.of(1980, 2, 2), "TITULAR", null, "11977778888");
        Long anuidade = insertAnuidade(associado, config, bd("180.00"), "PAGO");
        Long pagamento = insertPagamentoAnuidade(anuidade, bd("180.00"));

        enviar(delete("/config-anuidades/{id}", config), permissao(), null)
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(count("select count(*) from configuracao_anuidade where id = ?", config)).isZero();
        org.assertj.core.api.Assertions.assertThat(count("select count(*) from anuidade where id = ?", anuidade)).isZero();
        org.assertj.core.api.Assertions.assertThat(count("select count(*) from pagamento where id = ?", pagamento)).isZero();
    }
}
