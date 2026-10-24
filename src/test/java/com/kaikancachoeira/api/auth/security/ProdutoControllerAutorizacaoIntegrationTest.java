
package com.kaikancachoeira.api.auth.security;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

class ProdutoControllerAutorizacaoIntegrationTest extends SecurityIntegrationTest {

    @Test
    void devePermitirListagemComPermissaoDeLeitura() throws Exception {
        mockMvc.perform(get("/produtos")
                        .header(AUTHORIZATION, bearer(tokenCom("PRODUTOS_LEITURA"))))
                .andExpect(status().isOk());
    }

    @Test
    void devePermitirBuscaPorIdComPermissaoDeLeitura() throws Exception {
        mockMvc.perform(get("/produtos/999999999")
                        .header(AUTHORIZATION, bearer(tokenCom("PRODUTOS_LEITURA"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveNegarListagemSemPermissaoDeLeitura() throws Exception {
        esperarAcessoNegado(mockMvc.perform(get("/produtos")
                .header(AUTHORIZATION, bearer(tokenCom("VENDAS_MENU")))));
    }

    @Test
    void deveNegarCriacaoComPermissaoSomenteDeLeitura() throws Exception {
        esperarAcessoNegado(mockMvc.perform(post("/produtos")
                .header(AUTHORIZATION, bearer(tokenCom("PRODUTOS_LEITURA")))
                .contentType("application/json")
                .content("""
                        {
                          "nome": "Produto de teste",
                          "descricao": "Teste de autorização",
                          "estoqueMinimo": 0
                        }
                        """)));
    }

    @Test
    void deveNegarAtualizacaoComPermissaoSomenteDeLeitura() throws Exception {
        esperarAcessoNegado(mockMvc.perform(put("/produtos/999999999")
                .header(AUTHORIZATION, bearer(tokenCom("PRODUTOS_LEITURA")))
                .contentType("application/json")
                .content("""
                        {
                          "nome": "Produto de teste",
                          "descricao": "Teste de autorização",
                          "estoqueMinimo": 0
                        }
                        """)));
    }

    @Test
    void deveNegarExclusaoComPermissaoSomenteDeLeitura() throws Exception {
        esperarAcessoNegado(mockMvc.perform(delete("/produtos/999999999")
                .header(AUTHORIZATION, bearer(tokenCom("PRODUTOS_LEITURA")))));
    }

    @Test
    void deveNegarEnvioDeFotoComPermissaoSomenteDeLeitura() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "produto.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        esperarAcessoNegado(mockMvc.perform(
                multipart("/produtos/999999999/foto")
                        .file(arquivo)
                        .header(AUTHORIZATION, bearer(tokenCom("PRODUTOS_LEITURA")))
        ));
    }

}
