package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.estoque.dto.EstoqueResponse;
import com.kaikancachoeira.api.estoque.dto.MovimentacaoResponse;
import com.kaikancachoeira.api.estoque.dto.MovimentacaoResultadoResponse;
import com.kaikancachoeira.api.estoque.dto.MovimentarEstoqueRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@Tag(
        name = "Movimentações de estoque",
        description = "Registro de movimentações e consulta de lotes e históricos do estoque."
)
public interface MovimentacaoApi {

    @Operation(
            summary = "Registra movimentação de estoque",
            description = "Registra uma movimentação para o produto indicado e retorna o resultado da operação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimentação registrada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados da movimentação inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Requisição inválida", value = """
                                    {"codigo": "REQUISICAO_INVALIDA","mensagem": "Corpo da requisição inválido ou malformado","campos": []}
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto ou evento relacionado não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """))),
            @ApiResponse(responseCode = "409", description = "Saldo insuficiente para a movimentação",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Saldo insuficiente", value = """
                                    {"codigo": "SALDO_INSUFICIENTE","mensagem": "Saldo insuficiente. Disponível: 0.","campos": []}
                                    """)))
    })
    ResponseEntity<MovimentacaoResultadoResponse> movimentar(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da movimentação de estoque",
                    required = true
            )
            @RequestBody @Valid MovimentarEstoqueRequest request
    );

    @Operation(
            summary = "Consulta histórico de movimentações do produto",
            description = """
            Retorna o histórico paginado de movimentações de um produto.
            Permite filtrar por tipo de movimentação, evento e intervalo de datas.
            A paginação usa índice iniciado em zero.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta ou paginação inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Parâmetro inválido", value = """
                                    {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "page","mensagem": "deve ser maior ou igual a 0"}]}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """)))
    })
    ResponseEntity<PaginaResponse<MovimentacaoResponse>> historicoDoProduto(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id,

            @Parameter(description = "ID do tipo de movimentação")
            @RequestParam(required = false) Integer tipoMovimentacaoId,

            @Parameter(description = "ID do evento relacionado")
            @RequestParam(required = false) Long eventoId,

            @Parameter(description = "Início do intervalo de consulta (ISO-8601)")
            @RequestParam(required = false) Instant dataInicio,

            @Parameter(description = "Fim do intervalo de consulta (ISO-8601)")
            @RequestParam(required = false) Instant dataFim,

            @Parameter(description = "Índice da página, começando em zero")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Quantidade de registros por página (de 1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(
            summary = "Lista lotes de estoque de um produto",
            description = "Retorna os lotes de estoque associados ao produto indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lotes retornados"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """)))
    })
    ResponseEntity<List<EstoqueResponse>> listarLotes(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id
    );

    @Operation(
            summary = "Consulta histórico de movimentações de um lote",
            description = """
            Retorna o histórico paginado de movimentações do lote indicado.
            A paginação usa índice iniciado em zero.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico do lote retornado"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta ou paginação inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Parâmetro inválido", value = """
                                    {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "page","mensagem": "deve ser maior ou igual a 0"}]}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Lote não encontrado: 123","campos": []}
                                    """)))
    })
    ResponseEntity<PaginaResponse<MovimentacaoResponse>> historicoDoLote(
            @Parameter(description = "ID do lote de estoque", required = true)
            @PathVariable Long id,

            @Parameter(description = "Índice da página, começando em zero")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Quantidade de registros por página (de 1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );
}