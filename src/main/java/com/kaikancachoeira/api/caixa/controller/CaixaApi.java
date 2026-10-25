package com.kaikancachoeira.api.caixa.controller;

import com.kaikancachoeira.api.caixa.dto.CaixaRequest;
import com.kaikancachoeira.api.caixa.dto.CaixaResponse;
import com.kaikancachoeira.api.caixa.dto.ExclusaoCaixaResponse;
import com.kaikancachoeira.api.caixa.dto.RegistroCaixaResponse;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
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

@Tag(name = "Caixa", description = "Lançamentos de venda e devolução de fichas, formas de pagamento e resumo do caixa do evento.")
public interface CaixaApi {

    @Operation(summary = "Registra lançamento no caixa", description = """
        Registra VENDA_FICHA ou DEVOLUCAO_FICHA no evento em andamento. FIADO só é aceito em venda de fichas
        e exige nomeDevedor: cria o fiado vinculado ao lançamento. O tipo QUITACAO_FIADO não é aceito aqui
        (use a quitação de fiados). Retorna o lançamento e o resumo atualizado. Exige CAIXA_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lançamento registrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "tipo","mensagem": "Tipo deve ser VENDA_FICHA ou DEVOLUCAO_FICHA"}]}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento não está em andamento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_NAO_EM_ANDAMENTO","mensagem": "O evento não está em andamento.","campos": []}
                        """)))
    })
    ResponseEntity<RegistroCaixaResponse> registrar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do lançamento", required = true)
            CaixaRequest request);

    @Operation(summary = "Lista lançamentos do caixa", description = """
        Retorna o resumo do caixa e o histórico paginado de lançamentos, do mais recente para o mais antigo.
        O resumo considera todos os lançamentos do evento, independentemente dos filtros. Exige CAIXA_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixa retornado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """)))
    })
    ResponseEntity<CaixaResponse> listar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "Tipo do lançamento") TipoLancamento tipo,
            @Parameter(description = "Forma de pagamento") FormaPagamento formaPagamento,
            @Parameter(description = "Índice da página, começando em zero") int page,
            @Parameter(description = "Registros por página (máximo 100)") int size);

    @Operation(summary = "Exclui lançamento", description = """
        Corrige registros acidentais (exclusão física), apenas com o evento em andamento.
        Excluir venda de fichas fiada remove o fiado (se pendente); excluir uma quitação reabre o fiado.
        Exige CAIXA_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento excluído; retorna o resumo atualizado"),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Lançamento não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento não está em andamento ou fiado já quitado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "FIADO_JA_QUITADO","mensagem": "O fiado já está quitado.","campos": []}
                        """)))
    })
    ResponseEntity<ExclusaoCaixaResponse> excluir(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do lançamento", required = true) Long lancamentoId);
}