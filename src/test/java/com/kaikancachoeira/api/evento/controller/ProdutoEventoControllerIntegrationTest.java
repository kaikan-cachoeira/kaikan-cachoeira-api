package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProdutoEventoControllerIntegrationTest extends ControllerIntegrationTest {

    private static final String ROTA = "/eventos/{eventoId}/produtos";

    private org.springframework.test.web.servlet.ResultActions listar(Long eventoId, String token, String... params) throws Exception {
        var req = get(ROTA, eventoId).header(AUTHORIZATION, token);
        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(req);
    }

    private org.springframework.test.web.servlet.ResultActions listarCategorias(Long eventoId, String token) throws Exception {
        return mockMvc.perform(get(ROTA + "/categorias", eventoId).header(AUTHORIZATION, token));
    }

    private String leituraCardapio() {
        return auth("CARDAPIO_EVENTO_LEITURA");
    }

    private Long inserirItemEstoque(Long eventoId, Long produtoId, String nomeSnapshot, String categoria, String preco, int prevista) {
        Long pe = insertPeEstoque(eventoId, produtoId, nomeSnapshot, bd(preco), prevista);
        jdbc.update("update produto_evento set categoria = ? where id = ?", categoria, pe);
        return pe;
    }

    private Long inserirItemProduzido(Long eventoId, String nomeSnapshot, String categoria, String preco, String custo) {
        Long pe = insertPeProduzido(eventoId, nomeSnapshot, bd(preco), bd(custo));
        jdbc.update("update produto_evento set categoria = ? where id = ?", categoria, pe);
        return pe;
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");

        mockMvc.perform(get(ROTA, eventoId)).andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");

        listar(eventoId, auth("VENDAS_MENU"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void eventoInexistenteDeveRetornar404() throws Exception {
        listar(999_999L, leituraCardapio())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void semItensDeveRetornarPaginaVaziaComEnvelope() throws Exception {
        Long eventoId = insertEvento("Evento vazio", "EVENTO", "NAO_INICIADO");

        listar(eventoId, leituraCardapio())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void deveListarItensDoEventoComContratoDaResposta() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto = insertProduto("Coxinha de frango");

        Long peEstoque = inserirItemEstoque(eventoId, produto, "Coxinha de frango", "Salgados", "8.50", 100);
        Long venda = insertVenda(eventoId, bd("0.00"), bd("25.50"));
        insertItemVenda(venda, peEstoque, 3, bd("8.50"), bd("0.00"));
        inserirItemProduzido(eventoId, "Bolo de fubá", "Doces", "12.00", "4.00");

        listar(eventoId, leituraCardapio())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].id").isNumber())
                .andExpect(jsonPath("$.itens[0].nome").value("Coxinha de frango"))
                .andExpect(jsonPath("$.itens[0].origem").value("ESTOQUE"))
                .andExpect(jsonPath("$.itens[0].produtoId").value(produto.intValue()))
                .andExpect(jsonPath("$.itens[0].categoria").value("Salgados"))
                .andExpect(jsonPath("$.itens[0].quantidadeVendida").value(3))
                .andExpect(jsonPath("$.itens[0].restantePrevisto").value(97))
                .andExpect(jsonPath("$.itens[1].nome").value("Bolo de fubá"))
                .andExpect(jsonPath("$.itens[1].origem").value("PRODUZIDO"))
                .andExpect(jsonPath("$.itens[1].produtoId").doesNotExist())
                .andExpect(jsonPath("$.itens[1].categoria").value("Doces"))
                .andExpect(jsonPath("$.itens[1].quantidadePrevista").doesNotExist())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void deveFiltrarPorQSemDiferenciarMaiusculas() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto = insertProduto("Refrigerante");

        inserirItemEstoque(eventoId, produto, "Refrigerante cola", "Bebidas", "6.00", 20);
        inserirItemProduzido(eventoId, "Bolo de milho", "Doces", "10.00", "3.00");

        listar(eventoId, leituraCardapio(), "q", "  refri  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Refrigerante cola"));
    }

    @Test
    void deveFiltrarPorCategorias() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto = insertProduto("Água");

        inserirItemEstoque(eventoId, produto, "Água 500ml", "Bebidas", "4.50", 30);
        inserirItemProduzido(eventoId, "Coxinha", "Salgados", "8.00", "2.00");

        listar(eventoId, leituraCardapio(), "categoria", "Salgados")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Coxinha"))
                .andExpect(jsonPath("$.itens[0].categoria").value("Salgados"));
    }

    @Test
    void deveFiltrarPorOrigem() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto = insertProduto("Coca");

        inserirItemEstoque(eventoId, produto, "Coca-Cola", "Bebidas", "7.00", 15);
        inserirItemProduzido(eventoId, "Bolo de coco", "Doces", "11.00", "4.00");

        listar(eventoId, leituraCardapio(), "origem", "ESTOQUE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].origem").value("ESTOQUE"));

        listar(eventoId, leituraCardapio(), "origem", "PRODUZIDO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].origem").value("PRODUZIDO"));
    }

    @Test
    void deveCombinarQCategoriaEOrigem() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto1 = insertProduto("Misto 1");
        Long produto2 = insertProduto("Misto 2");

        inserirItemEstoque(eventoId, produto1, "Coxinha de carne", "Salgados", "8.50", 12);
        inserirItemEstoque(eventoId, produto2, "Coxinha de frango", "Salgados", "8.50", 12);
        inserirItemProduzido(eventoId, "Bolo de festa", "Doces", "14.00", "5.00");

        listar(eventoId, leituraCardapio(),
                "q", "coxinha",
                "categoria", "Salgados",
                "origem", "ESTOQUE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[*].nome", hasItem("Coxinha de carne")))
                .andExpect(jsonPath("$.itens[*].nome", hasItem("Coxinha de frango")))
                .andExpect(jsonPath("$.itens[*].nome", not(hasItem("Bolo de festa"))));
    }

    @Test
    void devePaginarNoBanco() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto1 = insertProduto("Base 1");
        Long produto2 = insertProduto("Base 2");
        Long produto3 = insertProduto("Base 3");

        inserirItemEstoque(eventoId, produto1, "Item 1", "Salgados", "1.00", 10);
        inserirItemEstoque(eventoId, produto2, "Item 2", "Salgados", "2.00", 10);
        inserirItemEstoque(eventoId, produto3, "Item 3", "Salgados", "3.00", 10);

        listar(eventoId, leituraCardapio(), "page", "0", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        listar(eventoId, leituraCardapio(), "page", "1", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Item 3"));
    }

    @Test
    void categoriasDevemContinuarCompletasESemPaginacao() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        Long produto1 = insertProduto("Base 1");
        Long produto2 = insertProduto("Base 2");

        inserirItemEstoque(eventoId, produto1, "Item 1", "Bebidas", "1.00", 10);
        inserirItemEstoque(eventoId, produto2, "Item 2", "Salgados", "2.00", 10);
        inserirItemProduzido(eventoId, "Item 3", "Doces", "3.00", "1.00");

        listarCategorias(eventoId, leituraCardapio())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(3)))
                .andExpect(jsonPath("$").value(hasItem("Bebidas")))
                .andExpect(jsonPath("$").value(hasItem("Salgados")))
                .andExpect(jsonPath("$").value(hasItem("Doces")));
    }

    @Test
    void origemInvalidaDeveRetornar400() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");

        listar(eventoId, leituraCardapio(), "origem", "OUTRA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }
}
