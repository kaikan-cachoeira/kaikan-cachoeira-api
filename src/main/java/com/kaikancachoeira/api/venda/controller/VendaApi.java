package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.venda.dto.VendaDetalheResponse;
import com.kaikancachoeira.api.venda.dto.VendaHistoricoResponse;
import com.kaikancachoeira.api.venda.dto.VendaRequest;
import com.kaikancachoeira.api.venda.dto.VendaResponse;
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

@Tag(name = "Vendas", description = "Registro, cancelamento e histórico de vendas do evento em andamento.")
public interface VendaApi {

    @Operation(summary = "Registra venda", description = """
        Registra uma venda no evento em andamento (somente tipo EVENTO).
        O desconto por produto vem do cardápio; o desconto da venda inteira é calculado
        a partir de valorFinal (total dos itens menos valorFinal).
        Itens de origem ESTOQUE baixam o estoque em FIFO, uma movimentação por lote consumido.
        Itens PRODUZIDO não movimentam estoque. Exige VENDAS_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venda registrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (itens repetidos, produto de outro evento, valorFinal acima do total)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "itens","mensagem": "Produto repetido na venda"}]}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento não está em andamento ou saldo de estoque insuficiente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "SALDO_INSUFICIENTE","mensagem": "Saldo insuficiente. Disponível: 2.","campos": []}
                        """)))
    })
    ResponseEntity<VendaResponse> registrar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Itens e valor final da venda", required = true)
            VendaRequest request);

    @Operation(summary = "Cancela venda", description = """
        Cancelamento definitivo. Gera movimentações ESTORNO_VENDA devolvendo a quantidade ao lote de origem.
        A venda cancelada deixa de contar em relatórios. Não permitido com o evento ENCERRADO.
        Exige VENDAS_MENU ou EVENTOS_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Venda cancelada"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Venda não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Venda já cancelada ou evento encerrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_ENCERRADO","mensagem": "O evento está encerrado.","campos": []}
                        """)))
    })
    ResponseEntity<Void> cancelar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID da venda", required = true) Long vendaId);

    @Operation(summary = "Lista histórico de vendas", description = """
        Histórico paginado, da venda mais recente para a mais antiga. q pesquisa pelo nome do produto vendido.
        Sem o filtro cancelada retorna todas; true só canceladas; false só ativas. Exige EVENTOS_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado"),
            @ApiResponse(responseCode = "400", description = "Paginação inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Parâmetro inválido", value = """
                        {"codigo": "REQUISICAO_INVALIDA","mensagem": "Valor inválido para o parâmetro","campos": [{"campo": "page","mensagem": "Valor inválido"}]}
                        """))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """)))
    })
    ResponseEntity<PaginaResponse<VendaHistoricoResponse>> listarHistorico(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "Nome do produto") String q,
            @Parameter(description = "Início do intervalo (ISO-8601)") Instant dataHoraInicio,
            @Parameter(description = "Fim do intervalo (ISO-8601)") Instant dataHoraFim,
            @Parameter(description = "Filtra por situação: true canceladas, false ativas, ausente todas") Boolean cancelada,
            @Parameter(description = "Índice da página, começando em zero") int page,
            @Parameter(description = "Registros por página (1 a 100)") int size);

    @Operation(summary = "Detalha venda", description = "Retorna os itens, descontos e situação de uma venda. Exige EVENTOS_MENU.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda encontrada"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Venda não encontrado: 123","campos": []}
                        """)))
    })
    ResponseEntity<VendaDetalheResponse> detalhar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID da venda", required = true) Long vendaId);
}