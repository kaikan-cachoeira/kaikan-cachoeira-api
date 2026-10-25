package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.venda.dto.DescontoProdutoResponse;
import com.kaikancachoeira.api.venda.dto.DescontoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Descontos", description = "Desconto por unidade aplicado a produtos do cardápio durante o evento.")
public interface DescontoApi {

    @Operation(summary = "Aplica desconto a um produto", description = """
        Define o novo preço do produto (menor ou igual ao preço de venda). O desconto por unidade
        (precoVenda - novoPreco) vale apenas para vendas feitas depois; vendas anteriores não mudam.
        Se novoPreco for igual ao preço de venda, nada é gravado. Exige VENDAS_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desconto aplicado"),
            @ApiResponse(responseCode = "400", description = "Novo preço inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Preço inválido", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "novoPreco","mensagem": "Novo preço não pode ser maior que o preço de venda"}]}
                        """))),
            @ApiResponse(responseCode = "404", description = "Produto não pertence ao evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento não está em andamento ou produto já tem desconto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_NAO_EM_ANDAMENTO","mensagem": "O evento não está em andamento.","campos": []}
                        """)))
    })
    ResponseEntity<DescontoProdutoResponse> aplicar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do produto no cardápio do evento", required = true) Long produtoEventoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novo preço do produto", required = true)
            DescontoRequest request);

    @Operation(summary = "Remove desconto de um produto", description = "Zera o desconto. Se não houver desconto aplicado, não altera nada. Exige VENDAS_MENU.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desconto removido"),
            @ApiResponse(responseCode = "404", description = "Produto não pertence ao evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento não está em andamento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_NAO_EM_ANDAMENTO","mensagem": "O evento não está em andamento.","campos": []}
                        """)))
    })
    ResponseEntity<DescontoProdutoResponse> remover(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do produto no cardápio do evento", required = true) Long produtoEventoId);
}