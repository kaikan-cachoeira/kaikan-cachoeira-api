package com.kaikancachoeira.api.clima.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaikancachoeira.api.clima.config.OpenMeteoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenMeteoClient implements ClimaClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient client;
    private final OpenMeteoProperties properties;

    public OpenMeteoClient(OpenMeteoProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.getReadTimeout());

        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Override
    public List<ClimaPrevisaoHoraria> consultar(Instant inicio, Instant fim) {
        try {
            LocalDate dataInicio = inicio.atZone(properties.getTimeZone()).toLocalDate();
            LocalDate dataFim = fim.atZone(properties.getTimeZone()).toLocalDate();

            Resposta resposta = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", properties.getLatitude())
                            .queryParam("longitude", properties.getLongitude())
                            .queryParam("timezone", properties.getTimeZone().getId())
                            .queryParam("start_date", DATE.format(dataInicio))
                            .queryParam("end_date", DATE.format(dataFim))
                            .queryParam("hourly", String.join(",",
                                    "temperature_2m",
                                    "apparent_temperature",
                                    "precipitation",
                                    "precipitation_probability",
                                    "wind_speed_10m",
                                    "weather_code"))
                            .build())
                    .retrieve()
                    .body(Resposta.class);

            if (resposta == null || resposta.hourly == null) {
                return List.of();
            }

            return resposta.hourly.toDominios(properties.getTimeZone());
        } catch (RestClientException ex) {
            log.warn("Falha ao consultar Open Meteo", ex);
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(@JsonProperty("hourly") Horario hourly) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Horario(
            @JsonProperty("time") List<String> time,
            @JsonProperty("temperature_2m") List<BigDecimal> temperatura,
            @JsonProperty("apparent_temperature") List<BigDecimal> sensacaoTermica,
            @JsonProperty("precipitation") List<BigDecimal> chuvaMm,
            @JsonProperty("precipitation_probability") List<Integer> probabilidadeChuva,
            @JsonProperty("wind_speed_10m") List<BigDecimal> ventoVelocidade,
            @JsonProperty("weather_code") List<Integer> codigoClima
    ) {
        List<ClimaPrevisaoHoraria> toDominios(java.time.ZoneId zoneId) {
            List<ClimaPrevisaoHoraria> saida = new ArrayList<>();
            int tamanho = (time == null) ? 0 : time.size();

            for (int i = 0; i < tamanho; i++) {
                Instant instante = LocalDateTime.parse(time.get(i)).atZone(zoneId).toInstant();
                Integer codigo = valor(codigoClima, i, 0);
                saida.add(new ClimaPrevisaoHoraria(
                        instante,
                        valor(temperatura, i, BigDecimal.ZERO),
                        valor(sensacaoTermica, i, BigDecimal.ZERO),
                        valor(chuvaMm, i, BigDecimal.ZERO),
                        valor(probabilidadeChuva, i, 0),
                        valor(ventoVelocidade, i, BigDecimal.ZERO),
                        codigo,
                        descricao(codigo)
                ));
            }

            return saida;
        }

        private static <T> T valor(List<T> valores, int indice, T padrao) {
            if (valores == null || indice >= valores.size() || valores.get(indice) == null) {
                return padrao;
            }
            return valores.get(indice);
        }

        private static String descricao(Integer codigo) {
            return switch (codigo == null ? -1 : codigo) {
                case 0 -> "Céu limpo";
                case 1 -> "Principalmente limpo";
                case 2 -> "Parcialmente nublado";
                case 3 -> "Nublado";
                case 45 -> "Nevoeiro";
                case 48 -> "Nevoeiro com geada";
                case 51 -> "Chuvisco leve";
                case 53 -> "Chuvisco moderado";
                case 55 -> "Chuvisco intenso";
                case 56 -> "Chuvisco congelante leve";
                case 57 -> "Chuvisco congelante intenso";
                case 61 -> "Chuva fraca";
                case 63 -> "Chuva moderada";
                case 65 -> "Chuva forte";
                case 66 -> "Chuva congelante leve";
                case 67 -> "Chuva congelante forte";
                case 71 -> "Neve fraca";
                case 73 -> "Neve moderada";
                case 75 -> "Neve forte";
                case 77 -> "Granizo";
                case 80 -> "Pancadas fracas";
                case 81 -> "Pancadas moderadas";
                case 82 -> "Pancadas fortes";
                case 85 -> "Pancadas de neve fracas";
                case 86 -> "Pancadas de neve fortes";
                case 95 -> "Trovoadas";
                case 96, 99 -> "Trovoadas com granizo";
                default -> "Código " + codigo;
            };
        }
    }
}
