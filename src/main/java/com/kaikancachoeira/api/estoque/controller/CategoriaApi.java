
package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.estoque.dto.CategoriaRequest;
import com.kaikancachoeira.api.estoque.dto.CategoriaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(
        name = "Categorias",
        description = "Consulta e gerenciamento das categorias de produtos."
)
public interface CategoriaApi {

    @Operation(
            summary = "Lista categorias",
            description = "Retorna as categorias disponíveis para classificação dos produtos."
    )
    @ApiResponse(responseCode = "200", description = "Lista de categorias retornada")
    ResponseEntity<List<CategoriaResponse>> listar();

    @Operation(
            summary = "Cadastra categoria",
            description = "Cria uma categoria a partir dos dados informados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            )
                    )
            )
    })
    ResponseEntity<CategoriaResponse> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados para cadastro da categoria",
                    required = true
            )
            @RequestBody @Valid CategoriaRequest request
    );

    @Operation(
            summary = "Atualiza categoria",
            description = "Atualiza os dados de uma categoria existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            )
                    )
            )
    })
    ResponseEntity<CategoriaResponse> atualizar(
            @Parameter(description = "ID da categoria", required = true)
            @PathVariable Integer id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados da categoria",
                    required = true
            )
            @RequestBody @Valid CategoriaRequest request
    );

    @Operation(
            summary = "Exclui categoria",
            description = "Solicita a exclusão da categoria indicada."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Categoria excluída"
    )
    ResponseEntity<Void> excluir(
            @Parameter(description = "ID da categoria", required = true)
            @PathVariable Integer id
    );
}
