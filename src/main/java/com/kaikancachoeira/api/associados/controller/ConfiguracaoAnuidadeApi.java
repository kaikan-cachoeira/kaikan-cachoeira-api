package com.kaikancachoeira.api.associados.controller;

import com.kaikancachoeira.api.associados.dto.ConfiguracaoAnuidadeRequest;
import com.kaikancachoeira.api.associados.dto.ConfiguracaoAnuidadeResponse;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Configurações de anuidade", description = "Cadastro e manutenção das configurações de anuidade por ano.")
public interface ConfiguracaoAnuidadeApi {

    @Operation(summary = "Lista configurações de anuidade", description = "Retorna todas as configurações cadastradas em ordem crescente de ano.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    ResponseEntity<List<ConfiguracaoAnuidadeResponse>> listar();

    @Operation(summary = "Cadastra configuração de anuidade", description = "Cria a configuração de anuidade para um ano específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Configuração criada"),
            @ApiResponse(responseCode = "409", description = "Ano já cadastrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito", value = """
                            {"codigo":"ANUIDADE_DUPLICADA","mensagem":"Já existe uma configuração de anuidade para este ano.","campos":[]}
                            """)))
    })
    ResponseEntity<ConfiguracaoAnuidadeResponse> criar(@RequestBody(description = "Dados da configuração", required = true) @Valid ConfiguracaoAnuidadeRequest request);

    @Operation(summary = "Atualiza configuração de anuidade", description = "Atualiza ano e valor de uma configuração existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuração atualizada"),
            @ApiResponse(responseCode = "404", description = "Configuração não encontrada"),
            @ApiResponse(responseCode = "409", description = "Ano já cadastrado")
    })
    ResponseEntity<ConfiguracaoAnuidadeResponse> atualizar(@Parameter(description = "ID da configuração", required = true) Integer id,
                                                           @RequestBody(description = "Dados da configuração", required = true) @Valid ConfiguracaoAnuidadeRequest request);

    @Operation(summary = "Exclui configuração de anuidade", description = "Remove uma configuração e suas anuidades vinculadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Configuração excluída"),
            @ApiResponse(responseCode = "404", description = "Configuração não encontrada")
    })
    ResponseEntity<Void> excluir(@Parameter(description = "ID da configuração", required = true) Integer id);
}
