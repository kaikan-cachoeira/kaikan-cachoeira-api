package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProdutoControllerIntegrationTest extends ControllerIntegrationTest {

    private static final String ROTA = "/produtos";

    private org.springframework.test.web.servlet.ResultActions listar(String token, String... params) throws Exception {
        var req = get(ROTA).header(AUTHORIZATION, token);
        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(req);
    }

    private String leitura() {
        return auth("PRODUTOS_LEITURA");
    }

    private Integer novaCategoria(String nome) {
        jdbc.update("insert into categoria (nome) values (?)", nome);
        return jdbc.queryForObject("select id from categoria where nome = ?", Integer.class, nome);
    }

    private Long produtoComCategoriaESaldo(String nome, Integer categoriaId, int quantidadeAtual, Integer estoqueMinimo) {
        Long produtoId = produtoComLote(nome, quantidadeAtual, "2.50");
        jdbc.update("update produto set categoria_id = ?, estoque_minimo = ? where id = ?", categoriaId, estoqueMinimo, produtoId);
        return produtoId;
    }

    private Long produtoSemCategoriaESaldo(String nome, int quantidadeAtual, Integer estoqueMinimo) {
        Long produtoId = insertProduto(nome);
        jdbc.update("update produto set quantidade_atual = ?, estoque_minimo = ? where id = ?", quantidadeAtual, estoqueMinimo, produtoId);
        return produtoId;
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get(ROTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        listar(auth("VENDAS_MENU"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void semProdutosDeveRetornarPaginaVaziaComEnvelope() throws Exception {
        listar(leitura())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void deveListarProdutosComContratoDaResposta() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");
        Long agua = produtoComCategoriaESaldo("Água 500ml", bebidas, 18, 10);

        listar(leitura())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].id").value(agua.intValue()))
                .andExpect(jsonPath("$.itens[0].nome").value("Água 500ml"))
                .andExpect(jsonPath("$.itens[0].descricao").doesNotExist())
                .andExpect(jsonPath("$.itens[0].categoria.id").value(bebidas))
                .andExpect(jsonPath("$.itens[0].categoria.nome").value("Bebidas"))
                .andExpect(jsonPath("$.itens[0].quantidadeAtual").value(18))
                .andExpect(jsonPath("$.itens[0].estoqueMinimo").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void deveFiltrarPorTextoSemDiferenciarMaiusculas() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");
        produtoComCategoriaESaldo("Água 500ml", bebidas, 18, 10);
        produtoComCategoriaESaldo("Refrigerante cola", bebidas, 5, 10);

        listar(leitura(), "q", "  refri  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Refrigerante cola"));
    }

    @Test
    void deveFiltrarPorCategoriaId() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");
        Integer salgados = novaCategoria("Salgados");

        produtoComCategoriaESaldo("Água", bebidas, 18, 10);
        produtoComCategoriaESaldo("Coxinha", salgados, 5, 10);

        listar(leitura(), "categoriaId", salgados.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Coxinha"))
                .andExpect(jsonPath("$.itens[0].categoria.id").value(salgados));
    }

    @Test
    void deveFiltrarPorSituacaoOkOutLow() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");

        produtoComCategoriaESaldo("Produto ok", bebidas, 18, 10);
        produtoComCategoriaESaldo("Produto low", bebidas, 8, 10);
        produtoSemCategoriaESaldo("Produto out", 0, null);

        listar(leitura(), "situacao", "ok")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Produto ok"));

        listar(leitura(), "situacao", "low")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Produto low"));

        listar(leitura(), "situacao", "out")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Produto out"));
    }

    @Test
    void deveCombinarTextoCategoriaESituacao() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");
        Integer salgados = novaCategoria("Salgados");

        produtoComCategoriaESaldo("Água 500ml", bebidas, 18, 10);
        produtoComCategoriaESaldo("Refrigerante cola", bebidas, 5, 10);
        produtoComCategoriaESaldo("Coxinha", salgados, 7, 10);

        listar(leitura(), "q", "cola", "categoriaId", bebidas.toString(), "situacao", "low")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Refrigerante cola"));
    }

    @Test
    void devePaginarNoBanco() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");
        produtoComCategoriaESaldo("Produto 1", bebidas, 18, 10);
        produtoComCategoriaESaldo("Produto 2", bebidas, 18, 10);
        produtoComCategoriaESaldo("Produto 3", bebidas, 18, 10);

        listar(leitura(), "page", "0", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        listar(leitura(), "page", "1", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Produto 3"));
    }

    @Test
    void deveRetornarListaOrdenadaDeFormaEstavel() throws Exception {
        Integer bebidas = novaCategoria("Bebidas");
        produtoComCategoriaESaldo("B", bebidas, 18, 10);
        produtoComCategoriaESaldo("A", bebidas, 18, 10);
        produtoComCategoriaESaldo("C", bebidas, 18, 10);

        listar(leitura())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].nome").value("A"))
                .andExpect(jsonPath("$.itens[1].nome").value("B"))
                .andExpect(jsonPath("$.itens[2].nome").value("C"));
    }

    @Test
    void situacaoInvalidaDeveRetornar400() throws Exception {
        listar(leitura(), "situacao", "tudo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("situacao")));
    }
}
