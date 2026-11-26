package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.RedefinicaoEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação")
@SecurityRequirements
public interface RedefinicaoEmailApi {

    @Operation(
            summary = "Confirma a troca de e-mail a partir do token",
            description = """
            Valida o token recebido por e-mail e substitui o e-mail atual do usuário pelo novo e-mail guardado no token.
            Rota pública: não exige JWT.

            **Efeitos:**
            - Marca o token como usado.
            - O token é inválido se não existir, já foi usado, expirou ou a conta não está `ATIVO`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "E-mail alterado"),
            @ApiResponse(responseCode = "400", description = "Token nulo, em branco, inválido, já usado ou expirado"),
            @ApiResponse(responseCode = "409", description = "O novo e-mail foi cadastrado por outro usuário")
    })
    ResponseEntity<Void> redefinirEmail(@RequestBody(description = "Token recebido por e-mail", required = true) @Valid RedefinicaoEmailRequest request);
}