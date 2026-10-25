package com.kaikancachoeira.api.doacao.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.doacao.dto.DoacaoRequest;
import com.kaikancachoeira.api.doacao.dto.DoacaoResponse;
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

import java.util.List;

@Tag(
        name = "Doações",
        description = "Registro, edição e consulta das doações recebidas pela associação."
)
public interface DoacaoApi {

    @Operation(
            summary = "Lista doações",
            description = """
            Retorna as doações paginadas, da mais recente para a mais antiga.
            Os filtros tipo e eventoId aceitam mais de um valor (parâmetro repetido) e se combinam.
            O parâmetro q pesquisa pelo nome do doador ou pela descrição do item.
            A paginação usa índice iniciado em zero.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doações retornadas"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Parâmetro inválido", value = """
                        {"codigo": "REQUISICAO_INVALIDA","mensagem": "Valor inválido para o parâmetro","campos": [{"campo": "page","mensagem": "Valor inválido"}]}
                        """)
                    )
            )
    })
    ResponseEntity<PaginaResponse<DoacaoResponse>> listar(
            @Parameter(description = "Texto para pesquisar por doador ou descrição do item")
            @RequestParam(required = false) String q,

            @Parameter(description = "Tipos da doação: dinheiro, produto ou reabastecimento (aceita repetição)")
            @RequestParam(required = false) List<String> tipo,

            @Parameter(description = "IDs dos eventos (aceita repetição)")
            @RequestParam(required = false) List<Long> eventoId,

            @Parameter(description = "Índice da página, começando em zero")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Quantidade de registros por página (de 1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(
            summary = "Registra doação",
            description = """
            Registra uma doação em dinheiro, de produto ou de reabastecimento.
            Somente o reabastecimento altera o estoque: cria um lote sem custo e a movimentação DOACAO.
            O evento deve existir e não pode estar encerrado.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Doação registrada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "tipo","mensagem": "Tipo de doação inválido"}]}
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Evento encerrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_ENCERRADO","mensagem": "O evento está encerrado.","campos": []}
                        """)
                    )
            )
    })
    ResponseEntity<DoacaoResponse> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da doação",
                    required = true
            )
            @RequestBody @Valid DoacaoRequest request
    );

    @Operation(
            summary = "Atualiza doação",
            description = """
            Atualiza os dados de uma doação em qualquer status do evento.
            O tipo e o produto (no reabastecimento) não podem ser alterados.
            Quando a quantidade de um reabastecimento muda, o lote e a movimentação DOACAO são ajustados.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Doação atualizada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "tipo","mensagem": "Tipo de doação inválido"}]}
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doação não encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Doação não encontrado: 123","campos": []}
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Quantidade menor que a já consumida do lote",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "CONFLITO_DADOS","mensagem": "A operação viola uma regra de integridade dos dados.","campos": []}
                        """)
                    )
            )
    })
    ResponseEntity<Void> atualizar(
            @Parameter(description = "ID da doação", required = true)
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da doação",
                    required = true
            )
            @RequestBody @Valid DoacaoRequest request
    );
}