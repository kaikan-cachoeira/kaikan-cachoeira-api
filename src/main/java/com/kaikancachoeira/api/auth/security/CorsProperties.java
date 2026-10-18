package com.kaikancachoeira.api.auth.security;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security.cors")
public class CorsProperties {

    @NotEmpty(message = "É obrigatório informar ao menos uma origem permitida no CORS")
    private List<String> allowedOrigins;

}