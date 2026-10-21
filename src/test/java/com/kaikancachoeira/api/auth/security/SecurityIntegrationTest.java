package com.kaikancachoeira.api.auth.security;

import com.kaikancachoeira.api.support.IntegrationTestBase;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class SecurityIntegrationTest extends IntegrationTestBase {

    protected static final String ROTA_AUTENTICADA = "/test-seguranca/autenticado";
    protected static final String ROTA_VENDAS = "/test-seguranca/vendas";
    protected static final String ROTA_ESTOQUE = "/test-seguranca/estoque";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected TokenService tokenService;
    @Autowired protected JwtProperties jwtProperties;
    @Autowired protected JwtEncoder jwtEncoder;

    /** Token real, emitido pelo TokenService de produção. */
    protected String tokenCom(String... permissoes) {
        return tokenService.gerarAccessToken("1", "OPERADOR", List.of(permissoes));
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** Token com controle total dos claims. {@code permissoes == null} omite o claim. */
    protected String tokenCustom(JwtEncoder encoder, String issuer, Instant emitidoEm,
                                 Instant expiraEm, List<String> permissoes) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("1")
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claim(JwtClaims.CARGO, "ADMINISTRADOR");
        if (permissoes != null) {
            claims.claim(JwtClaims.PERMISSOES, permissoes);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    protected JwtEncoder encoderComSegredo(String segredo) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(segredo.getBytes(StandardCharsets.UTF_8)));
    }

    protected void esperarNaoAutenticado(ResultActions resultado) throws Exception {
        resultado
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().string("Content-Type", startsWith("application/json")))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária"));
    }

    protected void esperarAcessoNegado(ResultActions resultado) throws Exception {
        resultado
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Você não tem permissão para realizar esta operação."));
    }
}