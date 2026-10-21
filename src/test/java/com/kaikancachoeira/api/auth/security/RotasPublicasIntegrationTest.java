package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RotasPublicasIntegrationTest extends SecurityIntegrationTest {

    /** Rota liberada = o filtro de segurança não barrou (nem 401 nem 403). */
    private void esperarLiberada(String caminho) throws Exception {
        MvcResult resultado = mockMvc.perform(get(caminho)).andReturn();
        assertThat(resultado.getResponse().getStatus())
                .as("status de %s sem token", caminho)
                .isNotIn(401, 403);
    }

    @Test
    void openApiDocsDeveSerPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void swaggerUiDeveSerPublico() throws Exception {
        esperarLiberada("/swagger-ui.html");
        esperarLiberada("/swagger-ui/index.html");
    }

    @Test
    void healthDeveSerPublico() throws Exception {
        // Não assertamos 200: o health agrega o indicador de e-mail, que tenta
        // conectar no SMTP e pode dar 503 se o Mailpit não estiver rodando.
        esperarLiberada("/actuator/health");
    }

    @Test
    void demaisEndpointsDoActuatorDevemExigirAutenticacao() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/actuator/env")));
        esperarNaoAutenticado(mockMvc.perform(get("/actuator/beans")));
    }
}