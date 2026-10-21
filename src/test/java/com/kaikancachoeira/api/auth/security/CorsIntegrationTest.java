package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsIntegrationTest extends SecurityIntegrationTest {

    // application-test.properties: app.security.cors.allowed-origins
    private static final String ORIGEM_PERMITIDA = "http://localhost:5173";
    private static final String ORIGEM_NAO_PERMITIDA = "http://site-malicioso.com";

    @Test
    void preflightDeOrigemPermitidaDeveSerAceitoSemAutenticacao() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(ORIGIN, ORIGEM_PERMITIDA)
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(header().string(ACCESS_CONTROL_MAX_AGE, "3600"))
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preflightDeRotaProtegidaNaoDeveExigirToken() throws Exception {
        mockMvc.perform(options(ROTA_AUTENTICADA)
                        .header(ORIGIN, ORIGEM_PERMITIDA)
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
    }

    @Test
    void deveListarTodosOsMetodosUsadosPelaApi() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(ORIGIN, ORIGEM_PERMITIDA)
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")));
    }

    @Test
    void preflightDeOrigemNaoPermitidaDeveSerRejeitado() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(ORIGIN, ORIGEM_NAO_PERMITIDA)
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflightComCabecalhoNaoPermitidoDeveSerRejeitado() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(ORIGIN, ORIGEM_PERMITIDA)
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-Cabecalho-Custom"))
                .andExpect(status().isForbidden());
    }

    @Test
    void requisicaoRealDeOrigemPermitidaDeveReceberCabecalhosCors() throws Exception {
        mockMvc.perform(get(ROTA_AUTENTICADA)
                        .header(ORIGIN, ORIGEM_PERMITIDA)
                        .header("Authorization", bearer(tokenCom())))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(header().string(ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString("WWW-Authenticate")));
    }

    @Test
    void respostaDe401DeOrigemPermitidaTambemDeveTerCabecalhosCors() throws Exception {
        // Sem isso o navegador esconde o 401 do front e ele vê só "erro de CORS".
        mockMvc.perform(get(ROTA_AUTENTICADA).header(ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA));
    }

    @Test
    void requisicaoDeOrigemNaoPermitidaNaoDeveReceberAllowOrigin() throws Exception {
        mockMvc.perform(get(ROTA_AUTENTICADA)
                        .header(ORIGIN, ORIGEM_NAO_PERMITIDA)
                        .header("Authorization", bearer(tokenCom())))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}