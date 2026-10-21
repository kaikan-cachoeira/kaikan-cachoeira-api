package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.FactorGrantedAuthority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    private final JwtAuthenticationConverter converter =
            new JwtConfig().jwtAuthenticationConverter();

    private Jwt.Builder jwtBase() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }

    private List<String> permissoes(Jwt jwt) {
        return converter.convert(jwt).getAuthorities().stream()
                .filter(a -> !(a instanceof FactorGrantedAuthority))
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    void deveMapearPermissoesParaAuthoritiesSemPrefixo() {
        Jwt jwt = jwtBase()
                .claim(JwtClaims.CARGO, "OPERADOR")
                .claim(JwtClaims.PERMISSOES, List.of("VENDAS_MENU", "CAIXA_MENU"))
                .build();

        assertThat(permissoes(jwt)).containsExactlyInAnyOrder("VENDAS_MENU", "CAIXA_MENU");
    }

    @Test
    void naoDeveGerarAuthorityAPartirDoCargo() {
        Jwt jwt = jwtBase()
                .claim(JwtClaims.CARGO, "ADMINISTRADOR")
                .claim(JwtClaims.PERMISSOES, List.of("VENDAS_MENU"))
                .build();

        assertThat(permissoes(jwt)).doesNotContain("ADMINISTRADOR", "ROLE_ADMINISTRADOR");
    }

    @Test
    void deveRetornarSemAuthoritiesDePermissaoQuandoNaoHaClaimDePermissoes() {
        Jwt jwt = jwtBase().claim(JwtClaims.CARGO, "ADMINISTRADOR").build();

        assertThat(permissoes(jwt)).isEmpty();
    }

    @Test
    void deveUsarOSubjectComoNomeDoPrincipal() {
        Jwt jwt = jwtBase().claim(JwtClaims.PERMISSOES, List.of()).build();

        assertThat(converter.convert(jwt).getName()).isEqualTo("1");
    }
}