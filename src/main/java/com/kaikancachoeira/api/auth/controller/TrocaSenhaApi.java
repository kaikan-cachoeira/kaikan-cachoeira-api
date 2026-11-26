package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.TrocaSenhaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Autenticação")
public interface TrocaSenhaApi {

    @Operation(
            summary = "Troca a senha do usuário",
            description = "Troca a senha. Requer usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Senha alterada"),
            @ApiResponse(responseCode = "400", description = "novaSenha nula, em branco ou fora do tamanho"),
            @ApiResponse(responseCode = "403", description = "idUsuario diferente do usuário autenticado")
    })
    ResponseEntity<Void> trocarSenha(@Parameter(hidden = true) Authentication authentication,
                                     @RequestBody(description = "Nova senha", required = true) @Valid TrocaSenhaRequest request);
}