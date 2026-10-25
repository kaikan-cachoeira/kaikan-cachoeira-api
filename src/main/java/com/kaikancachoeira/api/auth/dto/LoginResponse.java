package com.kaikancachoeira.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record LoginResponse(
        @Schema(description = "JWT de curta duração, enviado em Authorization: Bearer") String accessToken,
        @Schema(description = "Token opaco para renovar a sessão. Só o hash é persistido") String refreshToken,
        UsuarioResponse usuario
) {
    @Schema(description = "Dados do usuário autenticado")
    public record UsuarioResponse(
            @Schema(example = "admin") String nome,
            @Schema(example = "ADMINISTRADOR") String cargo,
            @Schema(description = "Permissões concedidas pelo cargo", example = "[\"EVENTOS_MENU\", \"ESTOQUE_MENU\"]")
            List<String> permissoes
    ) {}
}