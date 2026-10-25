package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.estoque.dto.FotoResponse;
import com.kaikancachoeira.api.estoque.dto.ProdutoRequest;
import com.kaikancachoeira.api.estoque.dto.ProdutoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(
        name = "Produtos",
        description = "Consulta e gerenciamento de produtos do estoque, incluindo fotos."
)
public interface ProdutoApi {

    @Operation(
            summary = "Lista produtos",
            description = """
            Retorna os produtos cadastrados, permitindo aplicar filtros opcionais
            por texto, categorias e situação de estoque abaixo do mínimo.
            """
    )
    @ApiResponse(responseCode = "200", description = "Lista de produtos retornada")
    ResponseEntity<List<ProdutoResponse>> listar(
            @Parameter(description = "Texto para pesquisa de produtos")
            @RequestParam(required = false) String q,

            @Parameter(description = "IDs das categorias para filtrar os produtos")
            @RequestParam(required = false) List<Integer> categoriaId,

            @Parameter(description = "Filtra produtos abaixo do estoque mínimo")
            @RequestParam(required = false) Boolean abaixoDoMinimo
    );

    @Operation(
            summary = "Busca produto por ID",
            description = "Retorna os dados de um produto específico."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """))
            )
    })
    ResponseEntity<ProdutoResponse> buscar(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id
    );

    @Operation(
            summary = "Cadastra produto",
            description = "Cria um produto a partir dos dados informados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Dados inválidos", value = """
                                    {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "nome","mensagem": "Nome é obrigatório"}]}
                                    """)
                    )
            )
    })
    ResponseEntity<ProdutoResponse> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados para cadastro do produto",
                    required = true
            )
            @RequestBody @Valid ProdutoRequest request
    );

    @Operation(
            summary = "Envia ou atualiza foto do produto",
            description = """
            Envia uma imagem para o produto indicado.
            A requisição deve usar multipart/form-data e o campo arquivo.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto processada com sucesso"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """)
                    )
            )
    })
    ResponseEntity<FotoResponse> enviarFoto(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id,

            @Parameter(description = "Arquivo da foto do produto", required = true)
            @RequestParam("arquivo") MultipartFile arquivo
    );

    @Operation(
            summary = "Atualiza produto",
            description = "Atualiza os dados de um produto existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto atualizado"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Dados inválidos", value = """
                                    {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "nome","mensagem": "Nome é obrigatório"}]}
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """)))
    })
    ResponseEntity<Void> atualizar(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados do produto",
                    required = true
            )
            @RequestBody @Valid ProdutoRequest request
    );

    @Operation(
            summary = "Exclui produto",
            description = "Exclui o produto indicado, caso exista."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Produto excluído"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """))
            )
    })
    ResponseEntity<Void> excluir(
            @Parameter(description = "ID do produto", required = true)
            @PathVariable Long id
    );
}