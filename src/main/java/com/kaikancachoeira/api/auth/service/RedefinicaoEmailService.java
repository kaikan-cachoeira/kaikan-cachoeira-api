package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.RedefinicaoEmailRequest;
import com.kaikancachoeira.api.auth.entity.TokenEmail;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import com.kaikancachoeira.api.auth.exception.EmailJaCadastradoException;
import com.kaikancachoeira.api.auth.exception.TokenSenhaInvalidoOuExpiradoException;
import com.kaikancachoeira.api.auth.repository.TokenEmailRepository;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.auth.security.TokenOpacoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class RedefinicaoEmailService {

    private final UsuarioRepository usuarioRepository;
    private final TokenEmailRepository tokenEmailRepository;
    private final TokenOpacoService tokenOpacoService;
    private final Clock clock;

    public RedefinicaoEmailService(UsuarioRepository usuarioRepository,
                                   TokenEmailRepository tokenEmailRepository,
                                   TokenOpacoService tokenOpacoService,
                                   Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tokenEmailRepository = tokenEmailRepository;
        this.tokenOpacoService = tokenOpacoService;
        this.clock = clock;
    }

    @Transactional
    public void redefinirEmail(RedefinicaoEmailRequest request) {
        TokenEmail tokenEmail = tokenEmailRepository.findByToken(tokenOpacoService.gerarHash(request.token()))
                .orElseThrow(TokenSenhaInvalidoOuExpiradoException::new);

        Usuario usuario = tokenEmail.getUsuario();

        if (tokenEmail.getUsado()
                || clock.instant().isAfter(tokenEmail.getExpiraEm())
                || usuario.getStatus() != Status.ATIVO) {
            throw new TokenSenhaInvalidoOuExpiradoException();
        }

        if (usuarioRepository.findByEmail(tokenEmail.getNovoEmail()).isPresent()) {
            throw new EmailJaCadastradoException();
        }

        usuario.setEmail(tokenEmail.getNovoEmail());
        usuarioRepository.save(usuario);

        tokenEmail.setUsado(true);
        tokenEmailRepository.save(tokenEmail);
    }
}