package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.AtualizarNomeRequest;
import com.kaikancachoeira.api.auth.dto.PerfilResponse;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Autenticação")
public interface PerfilApi {

    @Operation(summary = "Consulta o perfil do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil do usuário",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PerfilResponse.class)))
    @ApiResponse(responseCode = "401", description = "Autenticação necessária",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErroResponse.class)))
    ResponseEntity<PerfilResponse> consultarPerfil(@Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "Atualiza o nome do usuário autenticado")
    @ApiResponse(responseCode = "202", description = "Nome atualizado")
    @ApiResponse(responseCode = "400", description = "Nome nulo, em branco ou com mais de 150 caracteres",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "401", description = "Autenticação necessária",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErroResponse.class)))
    ResponseEntity<Void> atualizarNome(@Parameter(hidden = true) Authentication authentication,
                                        AtualizarNomeRequest request);
}
