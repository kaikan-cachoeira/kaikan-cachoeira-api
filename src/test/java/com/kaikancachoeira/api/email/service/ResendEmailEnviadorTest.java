package com.kaikancachoeira.api.email.service;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.config.ResendProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResendEmailEnviadorTest {

    private static final EmailMensagem MENSAGEM =
            new EmailMensagem("dest@teste.com", "Recuperação de senha", "<p>Olá</p>");

    private HttpServer server;
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> caminho = new AtomicReference<>();
    private final AtomicReference<String> autorizacao = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> corpo = new AtomicReference<>();
    private final AtomicInteger chamadas = new AtomicInteger();

    @BeforeEach
    void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void pararServidor() {
        server.stop(0);
    }

    private void responder(int status, long atrasoMs) {
        server.createContext("/", ex -> {
            chamadas.incrementAndGet();
            metodo.set(ex.getRequestMethod());
            caminho.set(ex.getRequestURI().getPath());
            autorizacao.set(ex.getRequestHeaders().getFirst("Authorization"));
            contentType.set(ex.getRequestHeaders().getFirst("Content-Type"));
            corpo.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                if (atrasoMs > 0) Thread.sleep(atrasoMs);
                ex.sendResponseHeaders(status, -1);
            } catch (Exception ignored) {
                // cliente desistiu (timeout)
            } finally {
                ex.close();
            }
        });
    }

    private ResendEmailEnviador enviadorApontandoPara(String baseUrl, Duration readTimeout) {
        ResendProperties resend = new ResendProperties();
        resend.setApiKey("re_chave_teste");
        resend.setBaseUrl(baseUrl);
        resend.setConnectTimeout(Duration.ofSeconds(2));
        resend.setReadTimeout(readTimeout);

        EmailProperties email = new EmailProperties();
        email.setRemetente("Kaikan <no-reply@kaikan.test>");
        return new ResendEmailEnviador(resend, email);
    }

    private ResendEmailEnviador enviador() {
        return enviadorApontandoPara("http://localhost:" + server.getAddress().getPort(),
                Duration.ofSeconds(5));
    }

    @Test
    void deveEnviarPostParaEmailsComAutenticacaoEPayloadCorreto() {
        responder(200, 0);

        enviador().enviar(MENSAGEM);

        assertThat(chamadas).hasValue(1);
        assertThat(metodo.get()).isEqualTo("POST");
        assertThat(caminho.get()).isEqualTo("/emails");
        assertThat(autorizacao.get()).isEqualTo("Bearer re_chave_teste");
        assertThat(contentType.get()).startsWith("application/json");

        assertThat((String) JsonPath.read(corpo.get(), "$.from"))
                .isEqualTo("Kaikan <no-reply@kaikan.test>");
        assertThat((String) JsonPath.read(corpo.get(), "$.to[0]")).isEqualTo("dest@teste.com");
        assertThat((String) JsonPath.read(corpo.get(), "$.subject"))
                .isEqualTo("Recuperação de senha");
        assertThat((String) JsonPath.read(corpo.get(), "$.html")).isEqualTo("<p>Olá</p>");
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 422})
    void erroDeConfiguracaoOuRequisicaoDeveSerDefinitivo(int status) {
        responder(status, 0);

        assertThatThrownBy(() -> enviador().enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class,
                        e -> assertThat(e.isTransitorio()).isFalse());
    }

    /**
     * Documenta o comportamento ATUAL: só ResourceAccessException (rede/timeout) é
     * tratada como transitória. 429 e 5xx do Resend caem em "definitiva", embora o
     * javadoc de EmailException cite indisponibilidade e limite de taxa como transitórios.
     */
    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void statusDeLimiteOuIndisponibilidadeHojeContaComoDefinitivo(int status) {
        responder(status, 0);

        assertThatThrownBy(() -> enviador().enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class,
                        e -> assertThat(e.isTransitorio()).isFalse());
    }

    @Test
    void servidorInacessivelDeveSerTransitorio() throws IOException {
        int portaFechada;
        try (ServerSocket socket = new ServerSocket(0)) {
            portaFechada = socket.getLocalPort();
        }

        ResendEmailEnviador enviador = enviadorApontandoPara(
                "http://localhost:" + portaFechada, Duration.ofSeconds(2));

        assertThatThrownBy(() -> enviador.enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class, e -> {
                    assertThat(e.isTransitorio()).isTrue();
                    assertThat(e.getCause()).isNotNull();
                });
    }

    @Test
    void timeoutDeLeituraDeveSerTransitorio() {
        responder(200, 1500);
        ResendEmailEnviador enviador = enviadorApontandoPara(
                "http://localhost:" + server.getAddress().getPort(), Duration.ofMillis(200));

        assertThatThrownBy(() -> enviador.enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class,
                        e -> assertThat(e.isTransitorio()).isTrue());
    }
}