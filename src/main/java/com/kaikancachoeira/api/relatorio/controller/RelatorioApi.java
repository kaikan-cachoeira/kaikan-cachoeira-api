package com.kaikancachoeira.api.relatorio.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
        name = "Relatórios",
        description = "Relatório individual por evento com vendas, custos, doações e lucro."
)
public interface RelatorioApi {

    @Operation(
            summary = "Consulta o relatório do evento",
            description = """
            Retorna o relatório do evento. Evento encerrado gera o relatório final;
            evento em andamento gera o relatório parcial (parcial = true);
            evento não iniciado retorna 409.
            Para eventos do tipo EVENTO retorna resumo financeiro, produtos, custos adicionais,
            caixa, doações, movimentações de consumo e perda, ranking de produtos e vendas por hora.
            Para reuniões retorna apenas os custos (compras, consumo interno e custos adicionais).
            Vendas canceladas não entram em nenhum valor.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório retornado"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Evento não iniciado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_NAO_INICIADO","mensagem": "Evento não iniciado.","campos": []}
                        """)
                    )
            )
    })
    ResponseEntity<RelatorioResponse> gerar(
            @Parameter(description = "ID do evento", required = true)
            @PathVariable Long id
    );
}