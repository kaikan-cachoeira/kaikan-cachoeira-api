package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TokenOpacoServiceTest {

    private static final Pattern TOKEN_HEXADECIMAL =
            Pattern.compile("^[0-9a-fA-F]{64}$");

    private final TokenOpacoService tokenOpacoService =
            new TokenOpacoService();

    @Test
    void deveGerarTokenCom64CaracteresHexadecimais() {
        String token = tokenOpacoService.gerarToken();

        assertNotNull(token);
        assertTrue(TOKEN_HEXADECIMAL.matcher(token).matches());
    }

    @Test
    void deveGerarTokensDiferentes() {
        String primeiro = tokenOpacoService.gerarToken();
        String segundo = tokenOpacoService.gerarToken();

        assertNotEquals(primeiro, segundo);
    }

    @Test
    void deveGerarHashCom64CaracteresHexadecimais() {
        String token = tokenOpacoService.gerarToken();
        String hash = tokenOpacoService.gerarHash(token);

        assertNotNull(hash);
        assertTrue(TOKEN_HEXADECIMAL.matcher(hash).matches());
    }

    @Test
    void deveGerarHashDeterministico() {
        String token = "token-de-teste";

        String primeiroHash = tokenOpacoService.gerarHash(token);
        String segundoHash = tokenOpacoService.gerarHash(token);

        assertEquals(primeiroHash, segundoHash);
    }

    @Test
    void tokensDiferentesDevemGerarHashesDiferentes() {
        String primeiroHash =
                tokenOpacoService.gerarHash("primeiro-token");

        String segundoHash =
                tokenOpacoService.gerarHash("segundo-token");

        assertNotEquals(primeiroHash, segundoHash);
    }

    @Test
    void hashNaoDeveSerIgualAoTokenOriginal() {
        String token = tokenOpacoService.gerarToken();
        String hash = tokenOpacoService.gerarHash(token);

        assertNotEquals(token, hash);
    }
}