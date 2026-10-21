package com.kaikancachoeira.api.email.config;

import com.kaikancachoeira.api.email.service.ResendEmailEnviador;
import com.kaikancachoeira.api.email.service.SmtpEmailEnviador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmailProviderSelecaoTest {

    @Configuration
    @EnableConfigurationProperties
    @Import({EmailProperties.class, ResendProperties.class,
            ResendEmailEnviador.class, SmtpEmailEnviador.class})
    static class Cfg {}

    private ApplicationContextRunner runner(String provider) {
        return new ApplicationContextRunner()
                .withUserConfiguration(Cfg.class)
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withPropertyValues(
                        "app.email.provider=" + provider,
                        "app.email.front-url=http://localhost:5173",
                        "app.email.remetente=no-reply@kaikan.test",
                        "app.email.resend.api-key=re_chave");
    }

    @Test
    void providerResendDeveRegistrarApenasOEnviadorResend() {
        runner("resend").run(ctx -> {
            assertThat(ctx).hasSingleBean(ResendEmailEnviador.class);
            assertThat(ctx).doesNotHaveBean(SmtpEmailEnviador.class);
        });
    }

    @Test
    void providerSmtpDeveRegistrarApenasOEnviadorSmtp() {
        runner("smtp").run(ctx -> {
            assertThat(ctx).hasSingleBean(SmtpEmailEnviador.class);
            assertThat(ctx).doesNotHaveBean(ResendEmailEnviador.class);
            assertThat(ctx).doesNotHaveBean(ResendProperties.class);
        });
    }

    @Test
    void providerInvalidoDeveImpedirOContextoDeSubir() {
        runner("gmail").run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void providerResendSemApiKeyDeveImpedirOContextoDeSubir() {
        new ApplicationContextRunner()
                .withUserConfiguration(Cfg.class)
                .withPropertyValues(
                        "app.email.provider=resend",
                        "app.email.front-url=http://localhost:5173",
                        "app.email.remetente=no-reply@kaikan.test")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}