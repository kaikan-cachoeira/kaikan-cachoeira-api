package com.kaikancachoeira.api.auth.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public String gerarAccessToken(
            String usuarioId,
            String cargo,
            List<String> permissoes
    ) {
        Objects.requireNonNull(usuarioId, "O ID do usuário é obrigatório");
        Objects.requireNonNull(cargo, "O cargo é obrigatório");
        Objects.requireNonNull(permissoes, "A lista de permissões é obrigatória");

        Instant agora = clock.instant();
        Instant expiracao = agora.plus(jwtProperties.getAccessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(usuarioId)
                .issuedAt(agora)
                .expiresAt(expiracao)
                .claim(JwtClaims.CARGO, cargo)
                .claim(JwtClaims.PERMISSOES, permissoes)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

}