package com.kaikancachoeira.api.auth.integration;

import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.kaikancachoeira.api.support.TestData.tokenSenha;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RedefinicaoSenhaIntegrationTest extends AuthIntegrationTest {

    private static final String NOVA_SENHA = "NovaSenha@987";

    private String criarTokenValido(Usuario usuario) {
        String puro = tokenOpacoService.gerarToken();
        tokenSenhaRepository.save(tokenSenha(
                usuario, tokenOpacoService.gerarHash(puro), false, Instant.now().plusSeconds(600)));
        return puro;
    }

    @Test
    void deveTrocarASenhaEPermitirLoginComANova() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String token = criarTokenValido(usuario);

        redefinirSenha(token, NOVA_SENHA).andExpect(status().isNoContent());

        login(usuario.getEmail(), NOVA_SENHA).andExpect(status().isOk());
        login(usuario.getEmail(), SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void devePromoverUsuarioPendenteParaAtivo() throws Exception {
        Usuario usuario = criarUsuario(Status.PENDENTE);
        String token = criarTokenValido(usuario);

        redefinirSenha(token, NOVA_SENHA).andExpect(status().isNoContent());

        assertThat(recarregar(usuario).getStatus()).isEqualTo(Status.ATIVO);
        login(usuario.getEmail(), NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    void naoDeveReativarUsuarioDesativadoMasAindaTrocaASenha() throws Exception {
        // Documenta o comportamento atual: o service só promove PENDENTE.
        Usuario usuario = criarUsuario(Status.DESATIVADO);
        String token = criarTokenValido(usuario);

        redefinirSenha(token, NOVA_SENHA).andExpect(status().isNoContent());

        assertThat(recarregar(usuario).getStatus()).isEqualTo(Status.DESATIVADO);
        login(usuario.getEmail(), NOVA_SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void deveRevogarTodosOsRefreshTokensDoUsuario() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String refresh1 = refreshTokenDoLogin(usuario);
        String refresh2 = refreshTokenDoLogin(usuario);
        String token = criarTokenValido(usuario);

        redefinirSenha(token, NOVA_SENHA).andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario)).isEmpty();
        refresh(refresh1).andExpect(status().isUnauthorized());
        refresh(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeveSerDeUsoUnico() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String token = criarTokenValido(usuario);

        redefinirSenha(token, NOVA_SENHA).andExpect(status().isNoContent());

        redefinirSenha(token, "OutraSenha@111")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void deveRejeitarTokenExpirado() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String puro = tokenOpacoService.gerarToken();
        tokenSenhaRepository.save(tokenSenha(
                usuario, tokenOpacoService.gerarHash(puro), false, Instant.now().minusSeconds(1)));

        redefinirSenha(puro, NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));

        login(usuario.getEmail(), SENHA).andExpect(status().isOk());
    }

    @Test
    void deveRejeitarTokenDesconhecido() throws Exception {
        redefinirSenha("a".repeat(64), NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void naoDeveAceitarOHashArmazenadoComoSeFosseOToken() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);
        String puro = criarTokenValido(usuario);
        String hash = tokenOpacoService.gerarHash(puro);

        redefinirSenha(hash, NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void deveRetornar400QuandoSenhaForaDoTamanho() throws Exception {
        redefinirSenha("a".repeat(64), "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("novaSenha")));
    }

    @Test
    void deveRetornar400QuandoTokenEmBranco() throws Exception {
        redefinirSenha("", NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("token")));
    }
}