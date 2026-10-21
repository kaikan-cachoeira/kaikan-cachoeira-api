package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.*;
import com.kaikancachoeira.api.auth.entity.TokenSenha;
import com.kaikancachoeira.api.auth.enums.Status;
import com.kaikancachoeira.api.auth.entity.Permissao;
import com.kaikancachoeira.api.auth.entity.RefreshToken;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.exception.TokenSenhaInvalidoOuExpiradoException;
import com.kaikancachoeira.api.auth.repository.RefreshTokenRepository;
import com.kaikancachoeira.api.auth.repository.TokenSenhaRepository;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.auth.security.JwtProperties;
import com.kaikancachoeira.api.auth.security.TokenOpacoService;
import com.kaikancachoeira.api.auth.security.TokenSenhaProperties;
import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.auth.exception.CredenciaisInvalidasException;
import com.kaikancachoeira.api.auth.exception.RefreshTokenInvalidoOuExpiradoException;
import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.service.EmailService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenOpacoService tokenOpacoService;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenSenhaProperties tokenSenhaProperties;
    private final TokenSenhaRepository tokenSenhaRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public AuthService(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, TokenOpacoService tokenOpacoService, JwtProperties jwtProperties, TokenService tokenService, Clock clock, ApplicationEventPublisher eventPublisher, TokenSenhaProperties tokenSenhaProperties, TokenSenhaRepository tokenSenhaRepository, EmailService emailService, EmailProperties emailProperties) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenOpacoService = tokenOpacoService;
        this.jwtProperties = jwtProperties;
        this.tokenService = tokenService;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
        this.tokenSenhaProperties = tokenSenhaProperties;
        this.tokenSenhaRepository = tokenSenhaRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailAndStatus(request.email(), Status.ATIVO)
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        TokensEmitidos tokens = emitirTokens(usuario);

        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                new LoginResponse.UsuarioResponse(
                        usuario.getNome(),
                        usuario.getCargo().getNome(),
                        tokens.permissoes()
                )
        );
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenOpacoService.gerarHash(request.refreshToken()))
                .orElseThrow(RefreshTokenInvalidoOuExpiradoException::new);

        Usuario usuario = refreshToken.getUsuario();

        if (refreshToken.getRevogado()
                || refreshToken.getExpiraEm().isBefore(clock.instant())
                || usuario.getStatus() != Status.ATIVO
        ) {
            throw new RefreshTokenInvalidoOuExpiradoException();
        }

        refreshToken.setRevogado(true);
        refreshTokenRepository.save(refreshToken);

        TokensEmitidos tokens = emitirTokens(usuario);

        return new RefreshTokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(tokenOpacoService.gerarHash(request.refreshToken()))
                .orElse(null);

        if (token != null) {
            token.setRevogado(true);
            refreshTokenRepository.save(token);
        }
    }

    @Transactional
    public void recuperarSenha(RecuperacaoSenhaRequest request) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(request.email());

        if (usuarioOptional.isEmpty()) {
            return;
        }

        if (usuarioOptional.get().getStatus().equals(Status.DESATIVADO)) {
            return;
        }

        Usuario usuario = usuarioOptional.get();

        invalidarTodosTokenSenha(usuario);

        String token = tokenOpacoService.gerarToken();

        TokenSenha tokenSenha = new TokenSenha();
        tokenSenha.setUsuario(usuario);
        tokenSenha.setToken(tokenOpacoService.gerarHash(token));
        tokenSenha.setUsado(false);
        tokenSenha.setExpiraEm(clock.instant().plus(tokenSenhaProperties.getTokenSenhaTtl()));

        tokenSenhaRepository.save(tokenSenha);

        emailService.enviar(new EmailMensagem(usuario.getEmail(), "Recuperação de senha", montarCorpo(token)));
    }

    @Transactional
    public void redefinirSenha(RedefinicaoSenhaRequest request) {
        TokenSenha tokenSenha = tokenSenhaRepository.findByToken(tokenOpacoService.gerarHash(request.token()))
                .orElseThrow(TokenSenhaInvalidoOuExpiradoException::new);

        if (tokenSenha.getUsado() || clock.instant().isAfter(tokenSenha.getExpiraEm())) {
            throw new TokenSenhaInvalidoOuExpiradoException();
        }

        Usuario usuario = tokenSenha.getUsuario();
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));

        if (usuario.getStatus().equals(Status.PENDENTE)) {
            usuario.setStatus(Status.ATIVO);
        }

        usuarioRepository.save(usuario);

        tokenSenha.setUsado(true);
        tokenSenhaRepository.save(tokenSenha);

        revogarTodosRefreshTokens(usuario);
    }

    private record TokensEmitidos(
            String accessToken,
            String refreshToken,
            List<String> permissoes
    ) {}

    private TokensEmitidos emitirTokens(Usuario usuario) {
        String refreshToken = tokenOpacoService.gerarToken();
        String refreshTokenHash = tokenOpacoService.gerarHash(refreshToken);

        List<String> permissoes = usuario.getCargo().getPermissoes().stream()
                .map(Permissao::getNome)
                .toList();

        String accessToken = tokenService.gerarAccessToken(
                usuario.getId().toString(),
                usuario.getCargo().getNome(),
                permissoes
        );

        persistirRefreshToken(usuario, refreshTokenHash);

        return new TokensEmitidos(accessToken, refreshToken, permissoes);
    }

    private void persistirRefreshToken(Usuario usuario, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setRevogado(false);
        refreshToken.setExpiraEm(clock.instant().plus(jwtProperties.getRefreshTokenTtl()));
        refreshTokenRepository.save(refreshToken);
    }

    private void invalidarTodosTokenSenha(Usuario usuario) {
        List<TokenSenha> tokensParaInvalidar = tokenSenhaRepository.findByUsuarioAndUsadoFalse(usuario);
        tokensParaInvalidar.forEach(tokenSenhaDaVez ->
                tokenSenhaDaVez.setUsado(true)
        );

        tokenSenhaRepository.saveAll(tokensParaInvalidar);
    }

    private void revogarTodosRefreshTokens(Usuario usuario) {
        List<RefreshToken> refreshTokensParaRevogar = refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario);

        refreshTokensParaRevogar.forEach(
                refreshToken -> refreshToken.setRevogado(true)
        );

        refreshTokenRepository.saveAll(refreshTokensParaRevogar);
    }

    private String montarCorpo(String token) {
        String linkRecuperacao = emailProperties.getFrontUrl()
                + "/redefinicao-senha?token="
                + token;

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Recuperação de senha</title>
            </head>
            <body>
                <h2>Recuperação de senha</h2>

                <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>

                <p>
                    Para criar uma nova senha, clique no botão abaixo:
                </p>

                <p>
                    <a href="%s"
                       style="display: inline-block;
                              padding: 12px 20px;
                              background-color: #2563eb;
                              color: #ffffff;
                              text-decoration: none;
                              border-radius: 6px;">
                        Redefinir senha
                    </a>
                </p>

                <p>Se você não solicitou a redefinição, ignore este e-mail.</p>

                <p>Por segurança, o link possui prazo de validade.</p>
            </body>
            </html>
            """.formatted(linkRecuperacao);
    }

}