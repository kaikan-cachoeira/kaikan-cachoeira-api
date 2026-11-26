package com.kaikancachoeira.api.clima.controller;

import com.kaikancachoeira.api.clima.dto.ClimaResponse;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "Clima",
        description = "Consulta das previsões climáticas do evento."
)
public interface ClimaApi {

    @Operation(
            summary = "Consulta o clima do evento",
            description = "Retorna as leituras climáticas persistidas para o evento e um resumo calculado em leitura."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clima retornado"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                            {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                            """)
                    )
            )
    })
    ResponseEntity<ClimaResponse> consultar(
            @Parameter(description = "ID do evento", required = true) Long id
    );

    @Operation(
            summary = "Sincroniza o clima do evento",
            description = "Dispara a sincronização das previsões climáticas do evento no Open Meteo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sincronização executada"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class)
                    )
            )
    })
    ResponseEntity<Void> sincronizar(
            @Parameter(description = "ID do evento", required = true) Long id
    );
}
