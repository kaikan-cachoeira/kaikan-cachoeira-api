package com.kaikancachoeira.api.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "seguranca.login")
public class LoginRateLimitProperties {

    private int maxTentativasEmail = 5;
    private int maxTentativasIp = 20;
    private Duration janela = Duration.ofMinutes(15);
    private Duration bloqueio = Duration.ofMinutes(15);

    public int getMaxTentativasEmail() {
        return maxTentativasEmail;
    }

    public void setMaxTentativasEmail(int maxTentativasEmail) {
        this.maxTentativasEmail = maxTentativasEmail;
    }

    public int getMaxTentativasIp() {
        return maxTentativasIp;
    }

    public void setMaxTentativasIp(int maxTentativasIp) {
        this.maxTentativasIp = maxTentativasIp;
    }

    public Duration getJanela() {
        return janela;
    }

    public void setJanela(Duration janela) {
        this.janela = janela;
    }

    public Duration getBloqueio() {
        return bloqueio;
    }

    public void setBloqueio(Duration bloqueio) {
        this.bloqueio = bloqueio;
    }
}