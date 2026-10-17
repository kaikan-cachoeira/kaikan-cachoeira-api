package com.kaikancachoeira.api.auth.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class TokenOpacoService {

    private static final int TOKEN_BYTES = 32;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final SecureRandom secureRandom = new SecureRandom();

    public String gerarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    public String gerarHash(String token) {
        Objects.requireNonNull(token, "O token não pode ser nulo");

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("O algoritmo SHA-256 não está disponível", ex);
        }
    }

}
