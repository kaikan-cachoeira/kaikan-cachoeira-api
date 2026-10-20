package com.kaikancachoeira.api.auth.integration;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.kaikancachoeira.api.support.TestData.refreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshIntegrationTest extends AuthIntegrationTest {

    @Test
    void deveRotacionarOTokenEmitindoNovoParERevogandoOAntigo() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String antigo = refreshTokenDoLogin(usuario);

        String corpo = refresh(antigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken", matchesPattern("[0-9a-f]{64}")))
                .andReturn().getResponse().getContentAsString();

        String novo = JsonPath.read(corpo, "$.refreshToken");
        assertThat(novo).isNotEqualTo(antigo);

        // só o novo continua ativo
        assertThat(refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario))
                .singleElement()
                .satisfies(t -> assertThat(t.getTokenHash()).isEqualTo(tokenOpacoService.gerarHash(novo)));
    }

    @Test
    void deveRejeitarReutilizacaoDoTokenAntigo() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String antigo = refreshTokenDoLogin(usuario);

        refresh(antigo).andExpect(status().isOk());

        refresh(antigo)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_TOKEN_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void deveRejeitarTokenDesconhecido() throws Exception {
        refresh("0".repeat(64))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_TOKEN_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void deveRejeitarTokenExpirado() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String puro = tokenOpacoService.gerarToken();
        refreshTokenRepository.save(refreshToken(
                usuario, tokenOpacoService.gerarHash(puro), false, Instant.now().minusSeconds(60)));

        refresh(puro)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_TOKEN_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void deveRejeitarQuandoUsuarioFoiDesativadoDepoisDoLogin() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String token = refreshTokenDoLogin(usuario);

        Usuario atual = recarregar(usuario);
        atual.setStatus(Status.DESATIVADO);
        usuarioRepository.save(atual);

        refresh(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_TOKEN_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void deveRetornar400QuandoTokenEmBranco() throws Exception {
        postJson("/auth/refresh", """
                {"refreshToken": ""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("refreshToken")));
    }
}