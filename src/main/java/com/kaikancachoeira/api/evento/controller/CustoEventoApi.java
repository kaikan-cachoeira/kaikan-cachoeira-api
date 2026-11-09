package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.evento.dto.CustoEventoRequest;
import com.kaikancachoeira.api.evento.dto.CustoEventoResponse;
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

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Custos do evento",
        description = "Consulta e gerenciamento dos custos de um evento."
)
public interface CustoEventoApi {

    @Operation(
            operationId = "listarCustosEvento",
            summary = "Lista custos do evento",
            description = "Retorna os custos cadastrados para o evento informado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Custos retornados"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(
                                    name = "Evento não encontrado",
                                    value = """
                                            {
                                              "codigo": "RECURSO_NAO_ENCONTRADO",
                                              "mensagem": "Evento não encontrado: 123",
                                              "campos": []
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<PaginaResponse<CustoEventoResponse>> listar(@Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "Índice da página (começando em 0)") @Min(0) int page,
            @Parameter(description = "Tamanho da página") @Min(1) @Max(100) int size
    );

    @Operation(
            operationId = "adicionarCustoEvento",
            summary = "Adiciona custo ao evento",
            description = "Cadastra um custo adicional em um evento em andamento ou encerrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Custo criado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(
                                    name = "Dados inválidos",
                                    value = """
                                            {
                                              "codigo": "VALIDACAO",
                                              "mensagem": "Dados inválidos",
                                              "campos": [
                                                {
                                                  "campo": "valor",
                                                  "mensagem": "O valor deve ser maior que zero"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(
                                    name = "Evento não encontrado",
                                    value = """
                                            {
                                              "codigo": "RECURSO_NAO_ENCONTRADO",
                                              "mensagem": "Evento não encontrado: 123",
                                              "campos": []
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Status do evento não permite adicionar custos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(
                                    name = "Status inválido",
                                    value = """
                                            {
                                              "codigo": "CUSTO_EVENTO_STATUS_INVALIDO",
                                              "mensagem": "Só é possível adicionar ou atualizar custos de eventos em andamento ou encerrados.",
                                              "campos": []
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<CustoEventoResponse> adicionar(@Parameter(description = "ID do evento", required = true) Long eventoId,

            @RequestBody(
                    description = "Dados do custo adicional",
                    required = true
            )
            @Valid CustoEventoRequest request
    );

    @Operation(
            operationId = "atualizarCustoEvento",
            summary = "Atualiza custo do evento",
            description = "Atualiza um custo adicional de um evento em andamento ou encerrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Custo atualizado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(
                                    name = "Dados inválidos",
                                    value = """
                                            {
                                              "codigo": "VALIDACAO",
                                              "mensagem": "Dados inválidos",
                                              "campos": [
                                                {
                                                  "campo": "valor",
                                                  "mensagem": "O valor deve ser maior que zero"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento ou custo não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Evento não encontrado",
                                            value = """
                                                    {
                                                      "codigo": "RECURSO_NAO_ENCONTRADO",
                                                      "mensagem": "Evento não encontrado: 123",
                                                      "campos": []
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Custo não encontrado",
                                            value = """
                                                    {
                                                      "codigo": "RECURSO_NAO_ENCONTRADO",
                                                      "mensagem": "Custo do evento não encontrado: 456",
                                                      "campos": []
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Status do evento não permite atualizar custos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(
                                    name = "Status inválido",
                                    value = """
                                            {
                                              "codigo": "CUSTO_EVENTO_STATUS_INVALIDO",
                                              "mensagem": "Só é possível adicionar ou atualizar custos de eventos em andamento ou encerrados.",
                                              "campos": []
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<CustoEventoResponse> atualizar(@Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do custo", required = true) Long custoId,

            @RequestBody(
                    description = "Novos dados do custo",
                    required = true
            )
            @Valid CustoEventoRequest request
    );

    @Operation(
            operationId = "removerCustoEvento",
            summary = "Remove custo do evento",
            description = "Remove um custo adicional do evento informado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Custo removido"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento ou custo não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Evento não encontrado",
                                            value = """
                                                    {
                                                      "codigo": "RECURSO_NAO_ENCONTRADO",
                                                      "mensagem": "Evento não encontrado: 123",
                                                      "campos": []
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Custo não encontrado",
                                            value = """
                                                    {
                                                      "codigo": "RECURSO_NAO_ENCONTRADO",
                                                      "mensagem": "Custo do evento não encontrado: 456",
                                                      "campos": []
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<Void> remover(@Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do custo", required = true) Long custoId
    );
}
