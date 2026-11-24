package com.kaikancachoeira.api.auth.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TentativaLoginService {

    private static final int LIMITE_ENTRADAS_PARA_LIMPEZA = 10_000;

    private final Clock clock;
    private final LoginRateLimitProperties properties;
    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public TentativaLoginService(Clock clock, LoginRateLimitProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public Optional<Duration> bloqueioRestante(String email, String ip) {
        Instant agora = clock.instant();
        Duration maiorBloqueio = null;

        for (String chave : chaves(email, ip)) {
            Estado estado = estados.get(chave);

            if (estado != null && estado.bloqueadoAte() != null && estado.bloqueadoAte().isAfter(agora)) {
                Duration restante = Duration.between(agora, estado.bloqueadoAte());

                if (maiorBloqueio == null || restante.compareTo(maiorBloqueio) > 0) {
                    maiorBloqueio = restante;
                }
            }
        }

        return Optional.ofNullable(maiorBloqueio);
    }

    public void registrarFalha(String email, String ip) {
        String chaveEmail = chaveEmail(email);

        if (chaveEmail != null) {
            registrar(chaveEmail, properties.getMaxTentativasEmail());
        }

        registrar(chaveIp(ip), properties.getMaxTentativasIp());

        if (estados.size() > LIMITE_ENTRADAS_PARA_LIMPEZA) {
            limparExpirados();
        }
    }

    public void registrarSucesso(String email) {
        String chaveEmail = chaveEmail(email);

        if (chaveEmail != null) {
            estados.remove(chaveEmail);
        }
    }

    private record Estado(int falhas, Instant inicioJanela, Instant bloqueadoAte) {}

    private void registrar(String chave, int limite) {
        Instant agora = clock.instant();

        estados.compute(chave, (chaveDaVez, atual) -> {
            if (atual != null && atual.bloqueadoAte() != null && atual.bloqueadoAte().isAfter(agora)) {
                return atual;
            }

            boolean recomecar = atual == null
                    || atual.bloqueadoAte() != null
                    || atual.inicioJanela().plus(properties.getJanela()).isBefore(agora);

            int falhas = recomecar ? 1 : atual.falhas() + 1;
            Instant inicioJanela = recomecar ? agora : atual.inicioJanela();
            Instant bloqueadoAte = falhas >= limite ? agora.plus(properties.getBloqueio()) : null;

            return new Estado(falhas, inicioJanela, bloqueadoAte);
        });
    }

    private void limparExpirados() {
        Instant agora = clock.instant();

        estados.entrySet().removeIf(entrada -> {
            Estado estado = entrada.getValue();

            if (estado.bloqueadoAte() != null) {
                return !estado.bloqueadoAte().isAfter(agora);
            }

            return estado.inicioJanela().plus(properties.getJanela()).isBefore(agora);
        });
    }

    private String[] chaves(String email, String ip) {
        String chaveEmail = chaveEmail(email);

        if (chaveEmail == null) {
            return new String[]{chaveIp(ip)};
        }

        return new String[]{chaveEmail, chaveIp(ip)};
    }

    private String chaveEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return "email:" + email.trim().toLowerCase();
    }

    private String chaveIp(String ip) {
        return "ip:" + ip;
    }
}