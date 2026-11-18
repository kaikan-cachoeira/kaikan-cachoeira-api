package com.kaikancachoeira.api.associado.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnuidadeControllerIntegrationTest extends ControllerIntegrationTest {

    private String permissao() {
        return auth("ANUIDADES_MENU", "ANUIDADES_LEITURA");
    }

    @Test
    void criarAnuidadesParaTodosOsTitularesDeveGerarSomenteParaTitulares() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long titular1 = insertAssociado("Titular 1", LocalDate.of(1980, 1, 1), "TITULAR", null, "11910000001");
        Long titular2 = insertAssociado("Titular 2", LocalDate.of(1975, 1, 1), "TITULAR", null, "11910000002");
        insertAssociado("Dependente", LocalDate.of(2010, 1, 1), "DEPENDENTE", titular1, "11910000003");

        enviar(post("/anuidades"), permissao(), """
                {
                  "idConfAnuidade": %d,
                  "valor": 180.00
                }
                """.formatted(config))
                .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(count("select count(*) from anuidade where configuracao_anuidade_id = ?", config)).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(count("select count(*) from anuidade where associado_id = ?", titular1)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(count("select count(*) from anuidade where associado_id in (select id from associado where tipo = 'DEPENDENTE')")).isZero();
    }

    @Test
    void criarAnuidadeParaAssociadoEspecificoDeveRespeitarOAssociadoInformado() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long titular = insertAssociado("Titular", LocalDate.of(1980, 1, 1), "TITULAR", null, "11910000001");

        enviar(post("/anuidades/associados/{idAssociado}", titular), permissao(), """
                {
                  "idConfAnuidade": %d,
                  "valor": 180.00
                }
                """.formatted(config))
                .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(count("select count(*) from anuidade where associado_id = ? and configuracao_anuidade_id = ?", titular, config))
                .isEqualTo(1);
    }

    @Test
    void atualizarAnuidadeDeveAlterarApenasValor() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long titular = insertAssociado("Titular", LocalDate.of(1980, 1, 1), "TITULAR", null, "11910000001");
        Long anuidade = insertAnuidade(titular, config, bd("180.00"), "PENDENTE");

        enviar(patch("/anuidades/{id}", anuidade), permissao(), """
                {
                  "valor": 220.00
                }
                """)
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select valor from anuidade where id = ?", java.math.BigDecimal.class, anuidade))
                .isEqualByComparingTo("220.00");
    }
}
