package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.common.exception.ErroResponse;
import com.kaikancachoeira.api.estoque.dto.FotoResponse;
import com.kaikancachoeira.api.estoque.dto.ProdutoRequest;
import com.kaikancachoeira.api.estoque.dto.ProdutoResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestPart;
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
    ResponseEntity<PaginaResponse<ProdutoResponse>> listar(
            @Parameter(description = "Texto para pesquisa de produtos") String q,

            @Parameter(description = "IDs das categorias para filtrar os produtos") List<Integer> categoriaId,

            @Parameter(description = "Filtra produtos abaixo do estoque mínimo") Boolean abaixoDoMinimo,
            @Parameter(description = "Índice da página (começando em 0)") @Min(0) int page,
            @Parameter(description = "Tamanho da página") @Min(1) @Max(100) int size
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
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """))
            )
    })
    ResponseEntity<ProdutoResponse> buscar(@Parameter(description = "ID do produto", required = true) Long id);

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
                                    implementation = ErroResponse.class
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
            @Valid ProdutoRequest request
    );

    @Operation(
            summary = "Cadastra produto com foto",
            description = """
            Cria um produto e, opcionalmente, sua foto em uma única requisição multipart/form-data.
            A parte produto contém os dados em JSON (Content-Type application/json) e a parte arquivo
            contém a imagem (JPG, PNG ou WEBP). Se a imagem for inválida, nada é cadastrado.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados ou arquivo inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Arquivo inválido", value = """
                                    {"codigo": "ARQUIVO_INVALIDO","mensagem": "Formato aceito: JPG, PNG ou WEBP.","campos": []}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe um produto com este nome",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = com.kaikancachoeira.api.common.exception.ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Produto duplicado", value = """
                                    {"codigo": "PRODUTO_DUPLICADO","mensagem": "Já existe um produto com este nome.","campos": []}
                                    """)
                    )
            )
    })
    ResponseEntity<ProdutoResponse> criarComFoto(
            @Parameter(description = "Dados para cadastro do produto (JSON)", required = true)
            @RequestPart("produto") @Valid ProdutoRequest request,

            @Parameter(description = "Foto do produto (opcional)")
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo
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
                                    implementation = ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """)
                    )
            )
    })
    ResponseEntity<FotoResponse> enviarFoto(
            @Parameter(description = "ID do produto", required = true) Long id,

            @Parameter(description = "Arquivo da foto do produto", required = true) MultipartFile arquivo
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
                                    implementation = ErroResponse.class
                            ),
                            examples = @ExampleObject(name = "Dados inválidos", value = """
                                    {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "nome","mensagem": "Nome é obrigatório"}]}
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """)))
    })
    ResponseEntity<Void> atualizar(@Parameter(description = "ID do produto", required = true) Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados do produto",
                    required = true
            )
            @Valid ProdutoRequest request
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
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Recurso não encontrado", value = """
                                    {"codigo": "RECURSO_NAO_ENCONTRADO","mensagem": "Produto não encontrado: 123","campos": []}
                                    """))
            )
    })
    ResponseEntity<Void> excluir(@Parameter(description = "ID do produto", required = true) Long id);
}
