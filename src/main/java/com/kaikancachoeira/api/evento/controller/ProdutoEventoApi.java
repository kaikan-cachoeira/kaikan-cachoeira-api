package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.evento.dto.FotoEventoResponse;
import com.kaikancachoeira.api.evento.dto.ProdutoEventoRequest;
import com.kaikancachoeira.api.evento.dto.ProdutoEventoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Cardápio do evento", description = "Produtos do cardápio de um evento: itens do estoque e itens produzidos no evento.")
public interface ProdutoEventoApi {

    @Operation(summary = "Lista cardápio do evento", description = """
        Retorna os produtos do cardápio com quantidade vendida (vendas não canceladas) e restante previsto.
        q pesquisa pelo nome; categoria filtra pelo nome guardado no snapshot e aceita repetição.
        Exige EVENTOS_MENU ou CARDAPIO_EVENTO_LEITURA.
        """)
    @ApiResponse(responseCode = "200", description = "Cardápio retornado")
    ResponseEntity<List<ProdutoEventoResponse>> listar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "Texto para pesquisa pelo nome") String q,
            @Parameter(description = "Nomes de categoria (aceita repetição)") List<String> categoria);

    @Operation(summary = "Lista categorias do cardápio", description = "Retorna os nomes distintos das categorias do cardápio. Exige EVENTOS_MENU ou CARDAPIO_EVENTO_LEITURA.")
    @ApiResponse(responseCode = "200", description = "Categorias retornadas")
    ResponseEntity<List<String>> listarCategorias(
            @Parameter(description = "ID do evento", required = true) Long eventoId);

    @Operation(summary = "Adiciona produto ao cardápio", description = """
        Só com o evento NAO_INICIADO e que não seja REUNIAO. origem ESTOQUE exige produtoId, precoVenda e
        quantidadePrevista (o snapshot vem do produto). origem PRODUZIDO exige nome, precoVenda e
        custoTotalIngredientes, e não gera movimentação de estoque. Exige EVENTOS_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto adicionado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para a origem informada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "origem","mensagem": "Origem deve ser ESTOQUE ou PRODUZIDO"}]}
                        """))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento já iniciado, reunião ou produto/nome duplicado no evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_JA_INICIADO","mensagem": "Só é possível alterar com o evento no status NAO_INICIADO.","campos": []}
                        """)))
    })
    ResponseEntity<ProdutoEventoResponse> adicionar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do item do cardápio", required = true)
            ProdutoEventoRequest request);

    @Operation(summary = "Envia foto do item do cardápio", description = "multipart/form-data com o campo arquivo (JPG, PNG ou WEBP). Exige EVENTOS_MENU.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto processada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """)))
    })
    ResponseEntity<FotoEventoResponse> atualizarFoto(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do item do cardápio", required = true) Long produtoEventoId,
            @Parameter(description = "Arquivo da foto", required = true) MultipartFile arquivo);

    @Operation(summary = "Atualiza item do cardápio", description = """
        Só com o evento NAO_INICIADO. Itens ESTOQUE editam precoVenda e quantidadePrevista; itens PRODUZIDO
        editam nome, descrição, categoria, precoVenda e custoTotalIngredientes. Exige EVENTOS_MENU.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Dados inválidos", value = """
                        {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "origem","mensagem": "Origem deve ser ESTOQUE ou PRODUZIDO"}]}
                        """))),
            @ApiResponse(responseCode = "404", description = "Item não encontrado neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento já iniciado ou nome duplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_JA_INICIADO","mensagem": "Só é possível alterar com o evento no status NAO_INICIADO.","campos": []}
                        """)))
    })
    ResponseEntity<ProdutoEventoResponse> atualizar(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do item do cardápio", required = true) Long produtoEventoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do item", required = true)
            ProdutoEventoRequest request);

    @Operation(summary = "Remove item do cardápio", description = "Só com o evento NAO_INICIADO e se o item nunca foi vendido. Exige EVENTOS_MENU.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado neste evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Recurso não encontrado", value = """
                        {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Evento não encontrado: 123","campos": []}
                        """))),
            @ApiResponse(responseCode = "409", description = "Evento já iniciado ou item já vendido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Conflito de negócio", value = """
                        {"codigo": "EVENTO_JA_INICIADO","mensagem": "Só é possível alterar com o evento no status NAO_INICIADO.","campos": []}
                        """)))
    })
    ResponseEntity<Void> remover(
            @Parameter(description = "ID do evento", required = true) Long eventoId,
            @Parameter(description = "ID do item do cardápio", required = true) Long produtoEventoId);
}