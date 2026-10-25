package com.kaikancachoeira.api.caixa.controller;

import com.kaikancachoeira.api.caixa.dto.FiadoResponse;
import com.kaikancachoeira.api.caixa.dto.QuitacaoRequest;
import com.kaikancachoeira.api.caixa.dto.QuitacaoResponse;
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

import java.util.List;

@Tag(name = "Fiados", description = "Consulta e quitação de fichas vendidas a prazo.")
public interface FiadoApi {

    @Operation(summary = "Lista fiados do evento", description = "Retorna os fiados, do mais recente para o mais antigo. situacao: pendente ou quitado; ausente retorna todos. Exige CAIXA_MENU.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiados retornados"),
            @ApiResponse(responseCode = "400", description = "Situação inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Situação inválida", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "situacao","mensagem": "Situação deve ser pendente ou quitado"}]}
                        """))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """)))
    })
    ResponseEntity<List<FiadoResponse>> listar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "pendente ou quitado") String situacao);

    @Operation(summary = "Quita fiado", description = """
        Marca o fiado como quitado e gera um lançamento QUITACAO_FIADO na forma de pagamento informada
        (PIX, DEBITO, CREDITO ou DINHEIRO; FIADO não é aceito). Permitido mesmo com o evento encerrado.
        Exige CAIXA_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiado quitado"),
            @ApiResponse(responseCode = "400", description = "Forma de pagamento inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Forma de pagamento inválida", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "formaPagamento","mensagem": "Forma de pagamento da quitação não pode ser FIADO"}]}
                        """))),
            @ApiResponse(responseCode = "404", description = "Fiado não encontrado neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Fiado não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Fiado já quitado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "FIADO_JA_QUITADO","mensagem": "O fiado já está quitado.","campos": []}
                        """)))
    })
    ResponseEntity<QuitacaoResponse> quitar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do fiado", required = true) Long fiadoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Forma de pagamento da quitação", required = true)
            QuitacaoRequest request);
}