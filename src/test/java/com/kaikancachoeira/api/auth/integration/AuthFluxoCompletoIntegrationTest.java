package com.kaikancachoeira.api.auth.integration;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFluxoCompletoIntegrationTest extends AuthIntegrationTest {

    @Test
    void contaPendenteAteLogout() throws Exception {
        Usuario usuario = criarUsuario(Status.PENDENTE);

        // conta pendente não loga
        login(usuario.getEmail(), SENHA).andExpect(status().isUnauthorized());

        // primeira configuração de senha via link de recuperação
        recuperarSenha(usuario.getEmail()).andExpect(status().isAccepted());
        String token = tokenDoEmailEnviado();
        redefinirSenha(token, "SenhaDefinitiva@1").andExpect(status().isNoContent());
        assertThat(recarregar(usuario).getStatus()).isEqualTo(Status.ATIVO);

        // login com a nova senha
        String corpo = login(usuario.getEmail(), "SenhaDefinitiva@1")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refresh1 = JsonPath.read(corpo, "$.refreshToken");

        // refresh rotaciona
        String corpoRefresh = refresh(refresh1)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refresh2 = JsonPath.read(corpoRefresh, "$.refreshToken");
        refresh(refresh1).andExpect(status().isUnauthorized());

        // logout encerra a sessão
        postJson("/auth/logout", """
                {"refreshToken": "%s"}""".formatted(refresh2))
                .andExpect(status().isNoContent());
        refresh(refresh2).andExpect(status().isUnauthorized());
    }
}