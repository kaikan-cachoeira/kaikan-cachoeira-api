package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.LoginRequest;
import com.kaikancachoeira.api.auth.dto.LoginResponse;
import com.kaikancachoeira.api.auth.entity.Permissao;
import com.kaikancachoeira.api.auth.entity.RefreshToken;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.repository.RefreshTokenRepository;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.auth.util.TokenUtil;
import com.kaikancachoeira.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.refresh-expiracao-dias}")
    private long refreshDias;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);

        if (usuario == null
                || !"ativo".equals(usuario.getStatus())
                || usuario.getSenha() == null
                || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "CREDENCIAIS_INVALIDAS", "Usuário ou senha inválidos");
        }

        String refreshBruto = TokenUtil.gerarAleatorio();

        long segundos = refreshDias * 24 * 60 * 60;

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash(TokenUtil.sha256(refreshBruto));
        refreshToken.setRevogado(false);
        refreshToken.setExpiraEm(Instant.now().plusSeconds(segundos));
        refreshTokenRepository.save(refreshToken);

        List<String> permissoes = new ArrayList<>();
        for (Permissao permissao : usuario.getCargo().getPermissoes()) {
            permissoes.add(permissao.getNome());
        }

        String accessToken = null;

        return new LoginResponse(
                accessToken,
                refreshBruto,
                new LoginResponse.UsuarioResponse(
                        usuario.getNome(),
                        usuario.getCargo().getNome(),
                        permissoes));
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(TokenUtil.sha256(refreshToken))
                .orElse(null);

        if (token != null) {
            token.setRevogado(true);
        }
    }
}