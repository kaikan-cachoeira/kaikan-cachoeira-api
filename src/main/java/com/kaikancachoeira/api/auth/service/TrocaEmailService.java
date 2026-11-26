package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.TrocaEmailRequest;
import com.kaikancachoeira.api.auth.entity.TokenEmail;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.exception.EmailJaCadastradoException;
import com.kaikancachoeira.api.auth.repository.TokenEmailRepository;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.auth.security.TokenOpacoService;
import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
public class TrocaEmailService {

    private static final Duration TOKEN_EMAIL_TTL = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final TokenEmailRepository tokenEmailRepository;
    private final TokenOpacoService tokenOpacoService;
    private final Clock clock;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public TrocaEmailService(UsuarioRepository usuarioRepository,
                             TokenEmailRepository tokenEmailRepository,
                             TokenOpacoService tokenOpacoService,
                             Clock clock,
                             EmailService emailService,
                             EmailProperties emailProperties) {
        this.usuarioRepository = usuarioRepository;
        this.tokenEmailRepository = tokenEmailRepository;
        this.tokenOpacoService = tokenOpacoService;
        this.clock = clock;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @Transactional
    public void trocarEmail(Integer idAutenticado, TrocaEmailRequest request) {
        Usuario usuario = usuarioRepository.findById(idAutenticado)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String novoEmail = request.email();

        if (usuarioRepository.findByEmail(novoEmail).isPresent()) {
            throw new EmailJaCadastradoException();
        }

        invalidarTokensAnteriores(usuario);

        String token = tokenOpacoService.gerarToken();

        TokenEmail tokenEmail = new TokenEmail();
        tokenEmail.setUsuario(usuario);
        tokenEmail.setToken(tokenOpacoService.gerarHash(token));
        tokenEmail.setUsado(false);
        tokenEmail.setExpiraEm(clock.instant().plus(TOKEN_EMAIL_TTL));
        tokenEmail.setNovoEmail(novoEmail);

        tokenEmailRepository.save(tokenEmail);

        emailService.enviar(new EmailMensagem(novoEmail, "Confirmação de troca de e-mail", montarCorpo(token)));
    }

    private void invalidarTokensAnteriores(Usuario usuario) {
        List<TokenEmail> tokens = tokenEmailRepository.findByUsuarioAndUsadoFalse(usuario);
        tokens.forEach(t -> t.setUsado(true));
        tokenEmailRepository.saveAll(tokens);
    }

    private String montarCorpo(String token) {
        String link = emailProperties.getFrontUrl()
                + "/redefinicao-email?token="
                + token;

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Confirmação de troca de e-mail</title>
            </head>
            <body>
                <h2>Confirmação de troca de e-mail</h2>

                <p>Recebemos uma solicitação para usar este endereço como e-mail da sua conta.</p>

                <p>Para confirmar a troca, clique no botão abaixo:</p>

                <p>
                    <a href="%s"
                       style="display: inline-block;
                              padding: 12px 20px;
                              background-color: #2563eb;
                              color: #ffffff;
                              text-decoration: none;
                              border-radius: 6px;">
                        Confirmar novo e-mail
                    </a>
                </p>

                <p>Se você não solicitou a troca, ignore este e-mail.</p>

                <p>Por segurança, o link possui prazo de validade.</p>
            </body>
            </html>
            """.formatted(link);
    }
}
