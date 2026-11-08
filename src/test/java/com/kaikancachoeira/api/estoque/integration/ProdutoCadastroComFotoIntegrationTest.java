package com.kaikancachoeira.api.estoque.integration;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProdutoCadastroComFotoIntegrationTest extends ControllerIntegrationTest {

    private static final byte[] IMAGEM = {1, 2, 3, 4};

    private MockMultipartFile dados(String nome) {
        String json = "{\"nome\": \"%s\", \"estoqueMinimo\": 5}".formatted(nome);
        return new MockMultipartFile("produto", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile foto(String nomeArquivo, String contentType) {
        return new MockMultipartFile("arquivo", nomeArquivo, contentType, IMAGEM);
    }

    private ResultActions cadastrar(MockMultipartFile... partes) throws Exception {
        MockMultipartHttpServletRequestBuilder requisicao = multipart("/produtos");
        for (MockMultipartFile parte : partes) {
            requisicao.file(parte);
        }
        return mockMvc.perform(requisicao.header(AUTHORIZATION, auth("ESTOQUE_MENU")));
    }

    private int produtosComNome(String nome) {
        sincronizar();
        return count("select count(*) from produto where nome = ?", nome);
    }

    @Test
    void imagemInvalidaNaoDeveCadastrarProduto() throws Exception {
        cadastrar(dados("Refrigerante"), foto("foto.avif", "image/avif"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ARQUIVO_INVALIDO"));

        assertThat(produtosComNome("Refrigerante")).isZero();
    }

    @Test
    void imagemValidaDeveCadastrarProdutoComFoto() throws Exception {
        cadastrar(dados("Suco"), foto("foto.png", "image/png"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Suco"))
                .andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.startsWith("/arquivos/")));

        sincronizar();
        assertThat(count("select count(*) from produto where nome = ? and foto_url is not null", "Suco"))
                .isEqualTo(1);
    }

    @Test
    void semImagemDeveCadastrarProdutoSemFoto() throws Exception {
        cadastrar(dados("Agua"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotoUrl").doesNotExist());

        assertThat(produtosComNome("Agua")).isEqualTo(1);
    }

    @Test
    void dadosInvalidosNaoDevemCadastrarProduto() throws Exception {
        cadastrar(dados(""), foto("foto.png", "image/png"))
                .andExpect(status().isBadRequest());

        assertThat(produtosComNome("")).isZero();
    }

    @Test
    void nomeDuplicadoNaoDeveCadastrarProduto() throws Exception {
        insertProduto("Cerveja");

        cadastrar(dados("Cerveja"), foto("foto.png", "image/png"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRODUTO_DUPLICADO"));

        assertThat(produtosComNome("Cerveja")).isEqualTo(1);
    }
}
