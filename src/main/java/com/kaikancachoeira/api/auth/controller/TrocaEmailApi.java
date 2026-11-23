package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.TrocaEmailRequest;
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
public interface TrocaEmailApi {

    @Operation(
            summary = "Solicita a troca de e-mail",
            description = """
            Envia para o **novo** e-mail um link `{url-do-front}/redefinicao-email?token={token}`, válido por 1 hora.
            Requer usuário autenticado.

            **Regras:**
            - Tokens anteriores ainda não usados do mesmo usuário são invalidados.
            - O e-mail só é alterado quando o link é confirmado.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Requisição aceita"),
            @ApiResponse(responseCode = "400", description = "E-mail nulo, em branco ou inválido"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    ResponseEntity<Void> trocarEmail(@Parameter(hidden = true) Authentication authentication,
                                     @RequestBody(description = "Novo e-mail", required = true) @Valid TrocaEmailRequest request);
}