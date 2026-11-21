package com.kaikancachoeira.api.associado.controller;

import com.kaikancachoeira.api.associado.dto.*;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.ErroResponse;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@Tag(name = "Associados", description = "Cadastro, consulta e controle de anuidades dos associados.")
public interface AssociadoApi {

    @Operation(summary = "Lista associados", description = "Retorna os associados com filtros por nome, idade, tipo e status da anuidade do ano informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de associados retornada"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Parâmetro inválido", value = """
                            {"codigo":"REQUISICAO_INVALIDA","mensagem":"Valor inválido para o parâmetro","campos":[{"campo":"ano","mensagem":"Campo obrigatório"}]}
                            """)))
    })
    ResponseEntity<PaginaResponse<AssociadoListaResponse>> listar(
            @Parameter(description = "Busca por nome do associado ou do titular") String q,
            @Parameter(description = "Filtra data de nascimento a partir de") LocalDate dataInicio,
            @Parameter(description = "Filtra data de nascimento até") LocalDate dataFim,
            @Parameter(description = "Tipo do associado") com.kaikancachoeira.api.associado.enums.TipoAssociado tipo,
            @Parameter(description = "Ano da anuidade", required = true) Integer ano,
            @Parameter(description = "Status da anuidade") com.kaikancachoeira.api.associado.enums.StatusAnuidade status,
            @Parameter(description = "Índice da página") @Min(0) int page,
            @Parameter(description = "Tamanho da página") @Min(1) @Max(100) int size
    );

    @Operation(summary = "Cadastra associado", description = "Cria um associado titular ou dependente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Associado criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                            {"codigo":"VALIDACAO","mensagem":"Dados inválidos","campos":[{"campo":"titularId","mensagem":"Dependente deve informar o titular responsável."}]}
                            """))),
            @ApiResponse(responseCode = "409", description = "Nome duplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito", value = """
                            {"codigo":"ASSOCIADO_DUPLICADO","mensagem":"Já existe um associado com este nome.","campos":[]}
                            """)))
    })
    ResponseEntity<AssociadoResponse> criar(@RequestBody(description = "Dados do associado", required = true) @Valid AssociadoRequest request);

    @Operation(summary = "Atualiza associado", description = "Atualiza os dados de um associado existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Associado atualizado"),
            @ApiResponse(responseCode = "404", description = "Associado não encontrado"),
            @ApiResponse(responseCode = "409", description = "Nome duplicado")
    })
    ResponseEntity<AssociadoResponse> atualizar(@Parameter(description = "ID do associado", required = true) Long id,
                                                @RequestBody(description = "Dados do associado", required = true) @Valid AssociadoRequest request);

    @Operation(summary = "Exclui associado", description = "Remove um associado existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Associado excluído"),
            @ApiResponse(responseCode = "404", description = "Associado não encontrado")
    })
    ResponseEntity<Void> excluir(@Parameter(description = "ID do associado", required = true) Long id);

    @Operation(summary = "Registra pagamento de anuidade", description = "Registra um pagamento para a anuidade informada do associado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento registrado"),
            @ApiResponse(responseCode = "404", description = "Associado ou anuidade não encontrados"),
            @ApiResponse(responseCode = "409", description = "Anuidade isenta")
    })
    ResponseEntity<PagamentoResponse> registrarPagamento(@Parameter(description = "ID do associado", required = true) Long id,
                                                         @RequestBody(description = "Dados do pagamento", required = true) @Valid PagamentoRequest request);

    @Operation(summary = "Exclui pagamento de anuidade", description = "Remove um pagamento de anuidade vinculado ao associado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pagamento excluído"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    ResponseEntity<Void> excluirPagamento(@Parameter(description = "ID do associado", required = true) Long idAssociado,
                                          @Parameter(description = "ID do pagamento", required = true) Long idPagamento);
}
