package com.kaikancachoeira.api.email.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    @NotBlank
    @Pattern(regexp = "smtp|resend", message = "O provider deve ser smtp ou resend")
    private String provider;

    @NotBlank
    private String frontUrl;

    @NotBlank
    private String remetente;

}
