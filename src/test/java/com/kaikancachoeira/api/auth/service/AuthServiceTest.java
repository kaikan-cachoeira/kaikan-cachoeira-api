package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.LoginRequest;
import com.kaikancachoeira.api.auth.dto.LoginResponse;
import com.kaikancachoeira.api.auth.dto.LogoutRequest;
import com.kaikancachoeira.api.auth.dto.RecuperacaoSenhaRequest;
import com.kaikancachoeira.api.auth.dto.RedefinicaoSenhaRequest;
import com.kaikancachoeira.api.auth.dto.RefreshTokenRequest;
import com.kaikancachoeira.api.auth.dto.RefreshTokenResponse;
import com.kaikancachoeira.api.auth.entity.RefreshToken;
import com.kaikancachoeira.api.auth.entity.TokenSenha;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import com.kaikancachoeira.api.auth.exception.CredenciaisInvalidasException;
import com.kaikancachoeira.api.auth.exception.RefreshTokenInvalidoOuExpiradoException;
import com.kaikancachoeira.api.auth.exception.TokenSenhaInvalidoOuExpiradoException;
import com.kaikancachoeira.api.auth.repository.RefreshTokenRepository;
import com.kaikancachoeira.api.auth.repository.TokenSenhaRepository;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.auth.security.JwtProperties;
import com.kaikancachoeira.api.auth.security.TokenOpacoService;
import com.kaikancachoeira.api.auth.security.TokenSenhaProperties;
import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.kaikancachoeira.api.support.TestData.cargo;
import static com.kaikancachoeira.api.support.TestData.refreshToken;
import static com.kaikancachoeira.api.support.TestData.tokenSenha;
import static com.kaikancachoeira.api.support.TestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-18T12:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration SENHA_TTL = Duration.ofHours(1);
    private static final String FRONT_URL = "https://front.test";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenOpacoService tokenOpacoService;
    @Mock private TokenService tokenService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TokenSenhaRepository tokenSenhaRepository;
    @Mock private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenTtl(REFRESH_TTL);

        TokenSenhaProperties tokenSenhaProperties = new TokenSenhaProperties();
        tokenSenhaProperties.setTokenSenhaTtl(SENHA_TTL);

        EmailProperties emailProperties = new EmailProperties();
        emailProperties.setFrontUrl(FRONT_URL);

        Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

        authService = new AuthService(
                usuarioRepository, refreshTokenRepository, passwordEncoder,
                tokenOpacoService, jwtProperties, tokenService, clock,
                eventPublisher, tokenSenhaProperties, tokenSenhaRepository,
                emailService, emailProperties
        );
    }

    private Usuario usuarioComStatus(Status status) {
        return usuario(1, "admin@email.com", status,
                cargo("ADMINISTRADOR", "VENDAS_MENU", "CAIXA_MENU"));
    }

    // ---------------------------------------------------------------- login

    @Nested
    class Login {

        @Test
        void deveAutenticarEEmitirTokensQuandoCredenciaisValidas() {
            Usuario usuario = usuarioComStatus(Status.ATIVO);
            when(usuarioRepository.findByEmailAndStatus("admin@email.com", Status.ATIVO))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Senha123", "hash-bcrypt")).thenReturn(true);
            when(tokenOpacoService.gerarToken()).thenReturn("refresh-puro");
            when(tokenOpacoService.gerarHash("refresh-puro")).thenReturn("refresh-hash");
            when(tokenService.gerarAccessToken(eq("1"), eq("ADMINISTRADOR"), anyList()))
                    .thenReturn("access-token");

            LoginResponse response = authService.login(new LoginRequest("admin@email.com", "Senha123"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-puro");
            assertThat(response.usuario().nome()).isEqualTo("Fulano");
            assertThat(response.usuario().cargo()).isEqualTo("ADMINISTRADOR");
            assertThat(response.usuario().permissoes())
                    .containsExactlyInAnyOrder("VENDAS_MENU", "CAIXA_MENU");
        }

        @Test
        void devePersistirApenasOHashDoRefreshTokenComExpiracaoConfigurada() {
            Usuario usuario = usuarioComStatus(Status.ATIVO);
            when(usuarioRepository.findByEmailAndStatus("admin@email.com", Status.ATIVO))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Senha123", "hash-bcrypt")).thenReturn(true);
            when(tokenOpacoService.gerarToken()).thenReturn("refresh-puro");
            when(tokenOpacoService.gerarHash("refresh-puro")).thenReturn("refresh-hash");
            when(tokenService.gerarAccessToken(any(), any(), anyList())).thenReturn("access-token");

            authService.login(new LoginRequest("admin@email.com", "Senha123"));

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken salvo = captor.getValue();

            assertThat(salvo.getTokenHash()).isEqualTo("refresh-hash");
            assertThat(salvo.getTokenHash()).isNotEqualTo("refresh-puro");
            assertThat(salvo.getRevogado()).isFalse();
            assertThat(salvo.getUsuario()).isSameAs(usuario);
            assertThat(salvo.getExpiraEm()).isEqualTo(AGORA.plus(REFRESH_TTL));
        }

        @Test
        void deveRejeitarQuandoNaoExisteUsuarioAtivoComOEmail() {
            when(usuarioRepository.findByEmailAndStatus("x@email.com", Status.ATIVO))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("x@email.com", "Senha123")))
                    .isInstanceOf(CredenciaisInvalidasException.class);

            verifyNoInteractions(passwordEncoder, tokenService);
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void deveRejeitarQuandoSenhaIncorreta() {
            Usuario usuario = usuarioComStatus(Status.ATIVO);
            when(usuarioRepository.findByEmailAndStatus("admin@email.com", Status.ATIVO))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("errada", "hash-bcrypt")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("admin@email.com", "errada")))
                    .isInstanceOf(CredenciaisInvalidasException.class);

            verifyNoInteractions(tokenService);
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------- refresh

    @Nested
    class Refresh {

        private void stubBusca(RefreshToken token) {
            when(tokenOpacoService.gerarHash("refresh-antigo")).thenReturn("hash-antigo");
            when(refreshTokenRepository.findByTokenHash("hash-antigo")).thenReturn(Optional.of(token));
        }

        @Test
        void deveRotacionarRevogandoOAntigoEEmitindoUmNovoPar() {
            Usuario usuario = usuarioComStatus(Status.ATIVO);
            RefreshToken antigo = refreshToken(usuario, "hash-antigo", false, AGORA.plusSeconds(60));
            stubBusca(antigo);
            when(tokenOpacoService.gerarToken()).thenReturn("refresh-novo");
            when(tokenOpacoService.gerarHash("refresh-novo")).thenReturn("hash-novo");
            when(tokenService.gerarAccessToken(eq("1"), eq("ADMINISTRADOR"), anyList()))
                    .thenReturn("access-novo");

            RefreshTokenResponse response =
                    authService.refreshToken(new RefreshTokenRequest("refresh-antigo"));

            assertThat(response.accessToken()).isEqualTo("access-novo");
            assertThat(response.refreshToken()).isEqualTo("refresh-novo");
            assertThat(antigo.getRevogado()).isTrue();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
            RefreshToken novo = captor.getAllValues().get(1);
            assertThat(novo.getTokenHash()).isEqualTo("hash-novo");
            assertThat(novo.getRevogado()).isFalse();
            assertThat(novo.getExpiraEm()).isEqualTo(AGORA.plus(REFRESH_TTL));
        }

        @Test
        void deveRejeitarQuandoTokenNaoExiste() {
            when(tokenOpacoService.gerarHash("refresh-antigo")).thenReturn("hash-antigo");
            when(refreshTokenRepository.findByTokenHash("hash-antigo")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("refresh-antigo")))
                    .isInstanceOf(RefreshTokenInvalidoOuExpiradoException.class);

            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void deveRejeitarQuandoTokenJaRevogado() {
            RefreshToken token = refreshToken(usuarioComStatus(Status.ATIVO), "hash-antigo", true, AGORA.plusSeconds(60));
            stubBusca(token);

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("refresh-antigo")))
                    .isInstanceOf(RefreshTokenInvalidoOuExpiradoException.class);

            verify(refreshTokenRepository, never()).save(any());
            verifyNoInteractions(tokenService);
        }

        @Test
        void deveRejeitarQuandoTokenExpirado() {
            RefreshToken token = refreshToken(usuarioComStatus(Status.ATIVO), "hash-antigo", false, AGORA.minusSeconds(1));
            stubBusca(token);

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("refresh-antigo")))
                    .isInstanceOf(RefreshTokenInvalidoOuExpiradoException.class);

            verify(refreshTokenRepository, never()).save(any());
            verifyNoInteractions(tokenService);
        }

        @ParameterizedTest
        @EnumSource(value = Status.class, names = {"PENDENTE", "DESATIVADO"})
        void deveRejeitarQuandoUsuarioNaoEstaAtivo(Status status) {
            RefreshToken token = refreshToken(usuarioComStatus(status), "hash-antigo", false, AGORA.plusSeconds(60));
            stubBusca(token);

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("refresh-antigo")))
                    .isInstanceOf(RefreshTokenInvalidoOuExpiradoException.class);

            assertThat(token.getRevogado()).isFalse();
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // --------------------------------------------------------------- logout

    @Nested
    class Logout {

        @Test
        void deveRevogarOTokenExistente() {
            RefreshToken token = refreshToken(usuarioComStatus(Status.ATIVO), "hash", false, AGORA.plusSeconds(60));
            when(tokenOpacoService.gerarHash("refresh-puro")).thenReturn("hash");
            when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            authService.logout(new LogoutRequest("refresh-puro"));

            assertThat(token.getRevogado()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        void deveSerIdempotenteQuandoTokenNaoExiste() {
            when(tokenOpacoService.gerarHash("desconhecido")).thenReturn("hash");
            when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

            authService.logout(new LogoutRequest("desconhecido"));

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // --------------------------------------------------- recuperar senha

    @Nested
    class RecuperarSenha {

        @Test
        void naoDeveFazerNadaQuandoEmailNaoCadastrado() {
            when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

            authService.recuperarSenha(new RecuperacaoSenhaRequest("x@email.com"));

            verifyNoInteractions(tokenSenhaRepository, tokenOpacoService, emailService);
        }

        @Test
        void naoDeveFazerNadaQuandoUsuarioDesativado() {
            when(usuarioRepository.findByEmail("admin@email.com"))
                    .thenReturn(Optional.of(usuarioComStatus(Status.DESATIVADO)));

            authService.recuperarSenha(new RecuperacaoSenhaRequest("admin@email.com"));

            verifyNoInteractions(tokenSenhaRepository, tokenOpacoService, emailService);
        }

        @ParameterizedTest
        @EnumSource(value = Status.class, names = {"ATIVO", "PENDENTE"})
        void deveGerarTokenEEnviarEmailParaUsuarioAtivoOuPendente(Status status) {
            Usuario usuario = usuarioComStatus(status);
            when(usuarioRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(usuario));
            when(tokenSenhaRepository.findByUsuarioAndUsadoFalse(usuario)).thenReturn(List.of());
            when(tokenOpacoService.gerarToken()).thenReturn("token-puro");
            when(tokenOpacoService.gerarHash("token-puro")).thenReturn("token-hash");

            authService.recuperarSenha(new RecuperacaoSenhaRequest("admin@email.com"));

            ArgumentCaptor<TokenSenha> tokenCaptor = ArgumentCaptor.forClass(TokenSenha.class);
            verify(tokenSenhaRepository).save(tokenCaptor.capture());
            TokenSenha salvo = tokenCaptor.getValue();
            assertThat(salvo.getToken()).isEqualTo("token-hash");
            assertThat(salvo.getUsado()).isFalse();
            assertThat(salvo.getUsuario()).isSameAs(usuario);
            assertThat(salvo.getExpiraEm()).isEqualTo(AGORA.plus(SENHA_TTL));

            ArgumentCaptor<EmailMensagem> emailCaptor = ArgumentCaptor.forClass(EmailMensagem.class);
            verify(emailService).enviar(emailCaptor.capture());
            EmailMensagem email = emailCaptor.getValue();
            assertThat(email.destinatario()).isEqualTo("admin@email.com");
            assertThat(email.assunto()).isEqualTo("Recuperação de senha");
            assertThat(email.corpoHtml())
                    .contains(FRONT_URL + "/redefinicao-senha?token=token-puro")
                    .doesNotContain("token-hash");
        }

        @Test
        void deveInvalidarTokensAnterioresAntesDeCriarONovo() {
            Usuario usuario = usuarioComStatus(Status.ATIVO);
            TokenSenha antigo1 = tokenSenha(usuario, "a", false, AGORA.plusSeconds(100));
            TokenSenha antigo2 = tokenSenha(usuario, "b", false, AGORA.plusSeconds(200));
            List<TokenSenha> antigos = List.of(antigo1, antigo2);
            when(usuarioRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(usuario));
            when(tokenSenhaRepository.findByUsuarioAndUsadoFalse(usuario)).thenReturn(antigos);
            when(tokenOpacoService.gerarToken()).thenReturn("token-puro");
            when(tokenOpacoService.gerarHash("token-puro")).thenReturn("token-hash");

            authService.recuperarSenha(new RecuperacaoSenhaRequest("admin@email.com"));

            assertThat(antigo1.getUsado()).isTrue();
            assertThat(antigo2.getUsado()).isTrue();

            InOrder ordem = inOrder(tokenSenhaRepository, emailService);
            ordem.verify(tokenSenhaRepository).saveAll(antigos);
            ordem.verify(tokenSenhaRepository).save(any(TokenSenha.class));
            ordem.verify(emailService).enviar(any(EmailMensagem.class));
        }
    }

    // ------------------------------------------------- redefinir senha

    @Nested
    class RedefinirSenha {

        private void stubBusca(TokenSenha token) {
            when(tokenOpacoService.gerarHash("token-puro")).thenReturn("hash");
            when(tokenSenhaRepository.findByToken("hash")).thenReturn(Optional.of(token));
        }

        @Test
        void deveAtivarUsuarioPendenteGravarHashDaSenhaEConsumirToken() {
            Usuario usuario = usuarioComStatus(Status.PENDENTE);
            TokenSenha token = tokenSenha(usuario, "hash", false, AGORA.plusSeconds(60));
            RefreshToken refresh1 = refreshToken(usuario, "r1", false, AGORA.plusSeconds(60));
            RefreshToken refresh2 = refreshToken(usuario, "r2", false, AGORA.plusSeconds(60));
            stubBusca(token);
            when(passwordEncoder.encode("NovaSenha123")).thenReturn("novo-hash-bcrypt");
            when(refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario))
                    .thenReturn(List.of(refresh1, refresh2));

            authService.redefinirSenha(new RedefinicaoSenhaRequest("token-puro", "NovaSenha123"));

            assertThat(usuario.getSenha()).isEqualTo("novo-hash-bcrypt");
            assertThat(usuario.getStatus()).isEqualTo(Status.ATIVO);
            assertThat(token.getUsado()).isTrue();
            assertThat(refresh1.getRevogado()).isTrue();
            assertThat(refresh2.getRevogado()).isTrue();
            verify(usuarioRepository).save(usuario);
            verify(tokenSenhaRepository).save(token);
            verify(refreshTokenRepository).saveAll(List.of(refresh1, refresh2));
        }

        @Test
        void deveManterUsuarioAtivoQuandoJaEstavaAtivo() {
            Usuario usuario = usuarioComStatus(Status.ATIVO);
            TokenSenha token = tokenSenha(usuario, "hash", false, AGORA.plusSeconds(60));
            stubBusca(token);
            when(passwordEncoder.encode("NovaSenha123")).thenReturn("novo-hash-bcrypt");
            when(refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario)).thenReturn(List.of());

            authService.redefinirSenha(new RedefinicaoSenhaRequest("token-puro", "NovaSenha123"));

            assertThat(usuario.getStatus()).isEqualTo(Status.ATIVO);
            assertThat(usuario.getSenha()).isEqualTo("novo-hash-bcrypt");
        }

        @Test
        void deveRejeitarQuandoTokenNaoExiste() {
            when(tokenOpacoService.gerarHash("token-puro")).thenReturn("hash");
            when(tokenSenhaRepository.findByToken("hash")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authService.redefinirSenha(new RedefinicaoSenhaRequest("token-puro", "NovaSenha123")))
                    .isInstanceOf(TokenSenhaInvalidoOuExpiradoException.class);

            verifyNoInteractions(passwordEncoder, refreshTokenRepository);
        }

        @Test
        void deveRejeitarQuandoTokenJaUsado() {
            TokenSenha token = tokenSenha(usuarioComStatus(Status.PENDENTE), "hash", true, AGORA.plusSeconds(60));
            stubBusca(token);

            assertThatThrownBy(() ->
                    authService.redefinirSenha(new RedefinicaoSenhaRequest("token-puro", "NovaSenha123")))
                    .isInstanceOf(TokenSenhaInvalidoOuExpiradoException.class);

            verifyNoInteractions(passwordEncoder, refreshTokenRepository);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        void deveRejeitarQuandoTokenExpirado() {
            TokenSenha token = tokenSenha(usuarioComStatus(Status.PENDENTE), "hash", false, AGORA.minusSeconds(1));
            stubBusca(token);

            assertThatThrownBy(() ->
                    authService.redefinirSenha(new RedefinicaoSenhaRequest("token-puro", "NovaSenha123")))
                    .isInstanceOf(TokenSenhaInvalidoOuExpiradoException.class);

            verifyNoInteractions(passwordEncoder, refreshTokenRepository);
            verify(usuarioRepository, never()).save(any());
        }
    }
}