package com.kaikancachoeira.api.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UsuarioResponse usuario
) {

    public record UsuarioResponse(
            String nome,
            String cargo,
            List<String> permissoes
    ) {
    }
}