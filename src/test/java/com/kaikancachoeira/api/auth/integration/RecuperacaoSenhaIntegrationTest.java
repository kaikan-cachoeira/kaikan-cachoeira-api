package com.kaikancachoeira.api.auth.integration;

import com.kaikancachoeira.api.auth.entity.TokenSenha;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecuperacaoSenhaIntegrationTest extends AuthIntegrationTest {

    private List<TokenSenha> tokensDe(Usuario usuario) {
        return tokenSenhaRepository.findAll().stream()
                .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
                .toList();
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"ATIVO", "PENDENTE"})
    void deveGerarTokenEEnviarEmail(Status status) throws Exception {
        Usuario usuario = criarUsuario(status);
        Instant antes = Instant.now();

        recuperarSenha(usuario.getEmail()).andExpect(status().isAccepted());

        String tokenPuro = tokenDoEmailEnviado();

        assertThat(tokensDe(usuario)).singleElement().satisfies(t -> {
            assertThat(t.getUsado()).isFalse();
            assertThat(t.getToken())
                    .isNotEqualTo(tokenPuro)
                    .isEqualTo(tokenOpacoService.gerarHash(tokenPuro));
            assertThat(t.getExpiraEm())
                    .isBetween(antes.plus(Duration.ofMinutes(59)), Instant.now().plus(Duration.ofMinutes(61)));
        });
    }

    @Test
    void deveEnviarEmailParaODestinatarioCorreto() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);

        recuperarSenha(usuario.getEmail()).andExpect(status().isAccepted());

        org.mockito.ArgumentCaptor<EmailMensagem> captor =
                org.mockito.ArgumentCaptor.forClass(EmailMensagem.class);
        verify(emailService).enviar(captor.capture());
        assertThat(captor.getValue().destinatario()).isEqualTo(usuario.getEmail());
        assertThat(captor.getValue().assunto()).isEqualTo("Recuperação de senha");
        assertThat(captor.getValue().corpoHtml()).contains("/redefinicao-senha?token=");
    }

    @Test
    void deveRetornar202SemEnviarEmailQuandoEmailNaoCadastrado() throws Exception {
        recuperarSenha("naoexiste@teste.com").andExpect(status().isAccepted());

        verify(emailService, never()).enviar(any());
        assertThat(tokenSenhaRepository.findAll()).isEmpty();
    }

    @Test
    void deveRetornar202SemEnviarEmailQuandoUsuarioDesativado() throws Exception {
        Usuario usuario = criarUsuario(Status.DESATIVADO);

        recuperarSenha(usuario.getEmail()).andExpect(status().isAccepted());

        verify(emailService, never()).enviar(any());
        assertThat(tokensDe(usuario)).isEmpty();
    }

    @Test
    void segundaSolicitacaoDeveInvalidarOTokenAnterior() throws Exception {
        Usuario usuario = criarUsuario(Status.ATIVO);

        recuperarSenha(usuario.getEmail()).andExpect(status().isAccepted());
        recuperarSenha(usuario.getEmail()).andExpect(status().isAccepted());

        verify(emailService, times(2)).enviar(any());
        List<TokenSenha> tokens = tokensDe(usuario);
        assertThat(tokens).hasSize(2);
        assertThat(tokens).filteredOn(t -> !t.getUsado()).hasSize(1);
        assertThat(tokens).filteredOn(TokenSenha::getUsado).hasSize(1);
    }

    @Test
    void deveRetornar400QuandoEmailInvalido() throws Exception {
        recuperarSenha("nao-e-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("email")));
    }
}