package com.kaikancachoeira.api.auth.integration;

import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogoutIntegrationTest extends AuthIntegrationTest {

    @Test
    void deveRevogarORefreshTokenEImpedirNovoRefresh() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String token = refreshTokenDoLogin(usuario);

        postJson("/auth/logout", """
                {"refreshToken": "%s"}""".formatted(token))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario)).isEmpty();

        refresh(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("REFRESH_TOKEN_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void deveSerIdempotenteParaTokenDesconhecido() throws Exception {
        postJson("/auth/logout", """
                {"refreshToken": "%s"}""".formatted("f".repeat(64)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveSerIdempotenteParaTokenJaRevogado() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String token = refreshTokenDoLogin(usuario);
        String corpo = """
                {"refreshToken": "%s"}""".formatted(token);

        postJson("/auth/logout", corpo).andExpect(status().isNoContent());
        postJson("/auth/logout", corpo).andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar400QuandoTokenEmBranco() throws Exception {
        postJson("/auth/logout", """
                {"refreshToken": " "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("refreshToken")));
    }
}