package com.kaikancachoeira.api.email.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
@ConfigurationProperties(prefix = "app.email.resend")
public class ResendProperties {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String baseUrl = "https://api.resend.com";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);

}
