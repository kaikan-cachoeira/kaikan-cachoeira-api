package com.kaikancachoeira.api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CommonConfigTest {

    @Test
    void clockDeveSerUtcEAvancar() throws InterruptedException {
        Clock clock = new ClockConfig().clock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(Duration.between(Instant.now(), clock.instant()).abs())
                .isLessThan(Duration.ofSeconds(2));

        Instant primeiro = clock.instant();
        Thread.sleep(5);
        assertThat(clock.instant()).isAfter(primeiro);
    }

    @Test
    void openApiDeveTerTituloEVersao() {
        OpenAPI openApi = new OpenApiConfig().openAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Kaikan Cachoeira API");
        assertThat(openApi.getInfo().getVersion()).isNotBlank();
        assertThat(openApi.getInfo().getDescription()).isNotBlank();
    }
}