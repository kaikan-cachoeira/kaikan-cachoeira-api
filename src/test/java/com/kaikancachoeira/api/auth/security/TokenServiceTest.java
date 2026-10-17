package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private Clock clock;

    private String gerarTokenValido() {
        return tokenService.gerarAccessToken(
                String.valueOf(123L),
                "ADMIN",
                List.of("CARDAPIO_LEITURA", "VENDA_CRIAR")
        );
    }

    @Test
    void deveGerarTokenJwtNaoVazio() {
        String token = gerarTokenValido();

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void deveGerarTokenComIssuerCorreto() {
        Jwt jwt = jwtDecoder.decode(gerarTokenValido());

        assertEquals(jwtProperties.getIssuer(), jwt.getIssuer().toString());
    }

    @Test
    void deveGerarTokenComSubjectCorrespondenteAoIdDoUsuario() {
        Jwt jwt = jwtDecoder.decode(gerarTokenValido());

        assertEquals("123", jwt.getSubject());
    }

    @Test
    void deveGerarTokenComCargo() {
        Jwt jwt = jwtDecoder.decode(gerarTokenValido());

        assertEquals("ADMIN", jwt.getClaimAsString(JwtClaims.CARGO));
    }

    @Test
    void deveGerarTokenComPermissoes() {
        Jwt jwt = jwtDecoder.decode(gerarTokenValido());

        assertEquals(
                List.of("CARDAPIO_LEITURA", "VENDA_CRIAR"),
                jwt.getClaimAsStringList(JwtClaims.PERMISSOES)
        );
    }

    @Test
    void deveGerarTokenComInstanteDeEmissao() {
        Jwt jwt = jwtDecoder.decode(gerarTokenValido());

        assertNotNull(jwt.getIssuedAt());
        assertFalse(jwt.getIssuedAt().isAfter(Instant.now(clock).plusSeconds(1)));
    }

    @Test
    void deveDefinirExpiracaoDeAcordoComTtlConfigurado() {
        Jwt jwt = jwtDecoder.decode(gerarTokenValido());

        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());

        long diferencaEmSegundos =
                jwt.getExpiresAt().getEpochSecond()
                        - jwt.getIssuedAt().getEpochSecond();

        assertEquals(
                jwtProperties.getAccessTokenTtl().toSeconds(),
                diferencaEmSegundos
        );
    }

    @Test
    void deveRejeitarTokenComAssinaturaAdulterada() {
        String token = gerarTokenValido();
        String[] partes = token.split("\\.");

        String assinaturaAdulterada =
                partes[2].startsWith("a") ? "b" : "a";

        String tokenAdulterado =
                partes[0] + "." + partes[1] + "." + assinaturaAdulterada;

        assertThrows(
                RuntimeException.class,
                () -> jwtDecoder.decode(tokenAdulterado)
        );
    }

    @Test
    void deveRejeitarTokenMalformado() {
        assertThrows(
                RuntimeException.class,
                () -> jwtDecoder.decode("isto-nao-e-um-jwt")
        );
    }
}