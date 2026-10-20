package com.kaikancachoeira.api.auth.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security.senha")
public class TokenSenhaProperties {

    @NotNull(message = "O TTL do token senha é obrigatório")
    private Duration tokenSenhaTtl;

    @AssertTrue(message = "O TTL do token senha deve ser positivo")
    public boolean isPrazoTokenSenhaValido() {
        return tokenSenhaTtl != null
                && !tokenSenhaTtl.isZero()
                && !tokenSenhaTtl.isNegative();
    }

}
