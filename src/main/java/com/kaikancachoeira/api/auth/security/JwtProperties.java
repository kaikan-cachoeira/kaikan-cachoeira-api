package com.kaikancachoeira.api.auth.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    @NotBlank(message = "O segredo JWT é obrigatório")
    private String secret;

    @NotBlank(message = "O issuer JWT é obrigatório")
    private String issuer;

    @NotNull(message = "O TTL do access token é obrigatório")
    private Duration accessTokenTtl;

    @NotNull(message = "O TTL do refresh token é obrigatório")
    private Duration refreshTokenTtl;

    @AssertTrue(message = "O segredo JWT deve possuir pelo menos 32 bytes")
    public boolean isTamanhoSegredoValido() {
        return secret != null
                && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    @AssertTrue(message = "O TTL do access token deve ser positivo")
    public boolean isPrazoTokenAcessoValido() {
        return accessTokenTtl != null
                && !accessTokenTtl.isZero()
                && !accessTokenTtl.isNegative();
    }

    @AssertTrue(message = "O TTL do refresh token deve ser positivo")
    public boolean isPrazoTokenAtualizacaoValido() {
        return refreshTokenTtl != null
                && !refreshTokenTtl.isZero()
                && !refreshTokenTtl.isNegative();
    }

}
