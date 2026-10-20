package com.kaikancachoeira.api.email.service;

import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.config.ResendProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailEnviador implements EmailEnviador {

    private final RestClient client;
    private final String remetente;

    public ResendEmailEnviador(ResendProperties resend, EmailProperties email) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(resend.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(resend.getReadTimeout());

        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(resend.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + resend.getApiKey())
                .build();

        this.remetente = email.getRemetente();
    }

    @Override
    public void enviar(EmailMensagem mensagem) {
        try {
            client.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", remetente,
                            "to", List.of(mensagem.destinatario()),
                            "subject", mensagem.assunto(),
                            "html", mensagem.corpoHtml()
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new EmailException("Falha de comunicação com o Resend", e, true);
        } catch (RestClientException e) {
            throw new EmailException("Falha ao enviar email via Resend", e, false);
        }
    }

}
