package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtAutorizacaoIntegrationTest extends SecurityIntegrationTest {

    // ------------------------------------------------ 401: não autenticado

    @Test
    void deveRetornar401SemToken() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get(ROTA_AUTENTICADA)));
    }

    @Test
    void deveRetornar401ComTokenMalformado() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get(ROTA_AUTENTICADA)
                .header(AUTHORIZATION, bearer("isto-nao-e-um-jwt"))));
    }

    @Test
    void deveRetornar401ComEsquemaDiferenteDeBearer() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get(ROTA_AUTENTICADA)
                .header(AUTHORIZATION, "Basic dXNlcjpwYXNz")));
    }

    @Test
    void deveRetornar401ComTokenExpirado() throws Exception {
        // 10 min no passado: fora da tolerância de relógio (60 s) do validador padrão
        String expirado = tokenCustom(jwtEncoder, jwtProperties.getIssuer(),
                Instant.now().minusSeconds(1200), Instant.now().minusSeconds(600),
                List.of("VENDAS_MENU"));

        esperarNaoAutenticado(mockMvc.perform(get(ROTA_VENDAS)
                .header(AUTHORIZATION, bearer(expirado))));
    }

    @Test
    void deveRetornar401ComAssinaturaDeOutroSegredo() throws Exception {
        String forjado = tokenCustom(
                encoderComSegredo("outro-segredo-com-pelo-menos-32-bytes!!"),
                jwtProperties.getIssuer(),
                Instant.now(), Instant.now().plusSeconds(600),
                List.of("VENDAS_MENU", "ESTOQUE_MENU"));

        esperarNaoAutenticado(mockMvc.perform(get(ROTA_VENDAS)
                .header(AUTHORIZATION, bearer(forjado))));
    }

    @Test
    void deveRetornar401ComIssuerDiferente() throws Exception {
        String outroIssuer = tokenCustom(jwtEncoder, "outro-issuer",
                Instant.now(), Instant.now().plusSeconds(600), List.of("VENDAS_MENU"));

        esperarNaoAutenticado(mockMvc.perform(get(ROTA_VENDAS)
                .header(AUTHORIZATION, bearer(outroIssuer))));
    }

    // ------------------------------------------------ 200: autenticado

    @Test
    void deveAceitarTokenValidoEmRotaQueSoExigeAutenticacao() throws Exception {
        mockMvc.perform(get(ROTA_AUTENTICADA)
                        .header(AUTHORIZATION, bearer(tokenCom())))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    // ------------------------------------------------ 403: sem permissão

    @Test
    void deveRetornar403QuandoTokenNaoTemAPermissaoDaRota() throws Exception {
        esperarAcessoNegado(mockMvc.perform(get(ROTA_ESTOQUE)
                .header(AUTHORIZATION, bearer(tokenCom("VENDAS_MENU")))));
    }

    @Test
    void deveRetornar200QuandoTokenTemAPermissaoDaRota() throws Exception {
        mockMvc.perform(get(ROTA_VENDAS)
                        .header(AUTHORIZATION, bearer(tokenCom("VENDAS_MENU", "CAIXA_MENU"))))
                .andExpect(status().isOk());
    }

    @Test
    void permissaoDeUmaRotaNaoLiberaOutra() throws Exception {
        String token = tokenCom("ESTOQUE_MENU");

        mockMvc.perform(get(ROTA_ESTOQUE).header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        esperarAcessoNegado(mockMvc.perform(get(ROTA_VENDAS)
                .header(AUTHORIZATION, bearer(token))));
    }

    @Test
    void deveRetornar403QuandoTokenNaoTemOClaimDePermissoes() throws Exception {
        // O token tem cargo = ADMINISTRADOR, mas o cargo sozinho não concede nada.
        String semPermissoes = tokenCustom(jwtEncoder, jwtProperties.getIssuer(),
                Instant.now(), Instant.now().plusSeconds(600), null);

        mockMvc.perform(get(ROTA_AUTENTICADA).header(AUTHORIZATION, bearer(semPermissoes)))
                .andExpect(status().isOk());
        esperarAcessoNegado(mockMvc.perform(get(ROTA_VENDAS)
                .header(AUTHORIZATION, bearer(semPermissoes))));
    }

    // ------------------------------------------------ rotas inexistentes

    @Test
    void rotaInexistenteSemTokenDeveRetornar401() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/rota-que-nao-existe")));
    }

    @Test
    void rotaInexistenteComTokenDeveRetornar404ComCorpoPadrao() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe")
                        .header(AUTHORIZATION, bearer(tokenCom())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    // ------------------------------------------------ /auth/** ignora Bearer

    @Test
    void rotasDeAuthDevemIgnorarBearerExpirado() throws Exception {
        String expirado = tokenCustom(jwtEncoder, jwtProperties.getIssuer(),
                Instant.now().minusSeconds(1200), Instant.now().minusSeconds(600),
                List.of("VENDAS_MENU"));

        mockMvc.perform(post("/auth/recuperacao-senha")
                        .header(AUTHORIZATION, bearer(expirado))
                        .contentType("application/json")
                        .content("""
                                {"email": "naoexiste@teste.com"}"""))
                .andExpect(status().isAccepted());
    }
}