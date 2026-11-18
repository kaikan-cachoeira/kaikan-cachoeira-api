package com.kaikancachoeira.api.dashboard.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardEstoqueResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardEventoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardProdutoMaisVendidoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardResumoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

@Tag(
        name = "Dashboard",
        description = "Indicadores financeiros, de vendas e de estoque dos eventos."
)
public interface DashboardApi {

    @Operation(
            summary = "Consulta o resumo do dashboard",
            description = "Retorna o total de eventos, a receita total, o custo total e o lucro total no período."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Período inválido", value = """
                            {"codigo":"VALIDACAO","mensagem":"Dados inválidos","campos":[{"campo":"dataFim","mensagem":"A data fim deve ser posterior ou igual à data início"}]}
                            """)
                    )
            )
    })
    ResponseEntity<DashboardResumoResponse> consultarResumo(
            @Parameter(description = "Data e hora inicial do período, em ISO-8601", example = "2026-05-01T00:00:00Z") Instant dataInicio,
            @Parameter(description = "Data e hora final do período, em ISO-8601", example = "2026-10-31T23:59:59Z") Instant dataFim
    );

    @Operation(
            summary = "Lista eventos do dashboard",
            description = "Retorna a série financeira por evento, em ordem cronológica."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos retornados"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Período inválido", value = """
                            {"codigo":"VALIDACAO","mensagem":"Dados inválidos","campos":[{"campo":"dataFim","mensagem":"A data fim deve ser posterior ou igual à data início"}]}
                            """)
                    )
            )
    })
    ResponseEntity<List<DashboardEventoResponse>> listarEventos(
            @Parameter(description = "Data e hora inicial do período, em ISO-8601", example = "2026-05-01T00:00:00Z") Instant dataInicio,
            @Parameter(description = "Data e hora final do período, em ISO-8601", example = "2026-10-31T23:59:59Z") Instant dataFim
    );

    @Operation(
            summary = "Lista os produtos mais vendidos",
            description = "Retorna os produtos ordenados por receita, com participação e participação acumulada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produtos retornados"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Limite inválido", value = """
                            {"codigo":"VALIDACAO","mensagem":"Dados inválidos","campos":[{"campo":"limite","mensagem":"O limite deve ser maior que zero"}]}
                            """)
                    )
            )
    })
    ResponseEntity<List<DashboardProdutoMaisVendidoResponse>> listarProdutosMaisVendidos(
            @Parameter(description = "Data e hora inicial do período, em ISO-8601", example = "2026-05-01T00:00:00Z") Instant dataInicio,
            @Parameter(description = "Data e hora final do período, em ISO-8601", example = "2026-10-31T23:59:59Z") Instant dataFim,
            @Parameter(description = "Quantidade máxima de itens retornados", example = "10") Integer limite
    );

    @Operation(
            summary = "Consulta o estoque atual",
            description = "Retorna o valor total do estoque e os alertas de produtos abaixo do mínimo ou negativos."
    )
    @ApiResponse(responseCode = "200", description = "Estoque retornado")
    ResponseEntity<DashboardEstoqueResponse> consultarEstoque();
}
