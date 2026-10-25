
package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.evento.dto.EventoDetalheResponse;
import com.kaikancachoeira.api.evento.dto.EventoRequest;
import com.kaikancachoeira.api.evento.dto.EventoResponse;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(
        name = "Eventos",
        description = "Consulta, cadastro e atualização de eventos."
)
public interface EventoApi {

    @Operation(
            summary = "Lista eventos",
            description = """
                    Retorna os eventos cadastrados, permitindo aplicar filtros
                    opcionais por texto, status, tipo de evento e período.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de eventos retornada"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros de consulta inválidos"
            )
    })
    ResponseEntity<List<EventoResponse>> listar(
            @Parameter(description = "Texto para pesquisa de eventos")
            @RequestParam(required = false) String q,

            @Parameter(description = "ID do status para filtrar os eventos")
            @RequestParam(required = false) Integer statusId,

            @Parameter(description = "Tipo de evento para filtrar os resultados")
            @RequestParam(required = false) TipoEvento tipoEvento,

            @Parameter(
                    description = "Data e hora inicial do período de consulta, em formato ISO 8601",
                    example = "2026-10-01T00:00:00Z"
            )
            @RequestParam(required = false) Instant dataInicio,

            @Parameter(
                    description = "Data e hora final do período de consulta, em formato ISO 8601",
                    example = "2026-10-31T23:59:59Z"
            )
            @RequestParam(required = false) Instant dataFim
    );

    @Operation(
            summary = "Busca evento por ID",
            description = """
                    Retorna os detalhes de um evento específico, incluindo seu status,
                    datas previstas e realizadas e resumo de produtos, vendas,
                    valores vendidos e custos adicionais.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado"
            )
    })
    ResponseEntity<EventoDetalheResponse> buscar(
            @Parameter(description = "ID do evento", required = true)
            @PathVariable Long id
    );

    @Operation(
            summary = "Cadastra evento",
            description = """
                    Cria um evento a partir dos dados informados.
                    A data e hora de encerramento previstas devem ser posteriores
                    à data e hora de início previstas.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Evento criado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito com as regras de integridade dos eventos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class)
                    )
            )
    })
    ResponseEntity<EventoResponse> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados para cadastro do evento",
                    required = true
            )
            @RequestBody @Valid EventoRequest request
    );

    @Operation(
            summary = "Atualiza evento",
            description = """
                    Atualiza os dados de um evento existente.
                    A data e hora de encerramento previstas devem ser posteriores
                    à data e hora de início previstas.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento atualizado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito com as regras de integridade dos eventos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class)
                    )
            )
    })
    ResponseEntity<EventoResponse> atualizar(
            @Parameter(description = "ID do evento", required = true)
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados para atualização do evento",
                    required = true
            )
            @RequestBody @Valid EventoRequest request
    );

    @Operation(
            summary = "Exclui evento",
            description = """
                    Exclui um evento que ainda não foi iniciado e que não possui
                    vendas, doações ou movimentações de estoque.
                    Os produtos do evento e os custos adicionais são removidos
                    explicitamente na mesma transação.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Evento excluído"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Evento não pode ser excluído devido ao status ou a registros associados",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class)
                    )
            )
    })
    ResponseEntity<Void> excluir(
            @Parameter(description = "ID do evento", required = true)
            @PathVariable Long id
    );

}
