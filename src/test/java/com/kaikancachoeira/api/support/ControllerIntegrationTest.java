package com.kaikancachoeira.api.support;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.auth.security.TokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public abstract class ControllerIntegrationTest extends DbIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected TokenService tokenService;
    @PersistenceContext protected EntityManager em;

    protected String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    protected void sincronizar() {
        em.flush();
        em.clear();
    }

    protected Long eventoEmAndamento() {
        return insertEvento("Festa", "EVENTO", "EM_ANDAMENTO");
    }

    protected void encerrar(Long eventoId) {
        jdbc.update("update evento set status_id = (select id from status_evento where codigo = 'ENCERRADO') where id = ?",
                eventoId);
        sincronizar();
    }

    protected ResultActions enviar(MockHttpServletRequestBuilder requisicao, String auth, String corpo) throws Exception {
        MockHttpServletRequestBuilder builder = requisicao.header(AUTHORIZATION, auth);
        if (corpo != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(builder);
    }

    protected Long idDe(ResultActions resultado, String caminho) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, caminho)).longValue();
    }

    protected Long produtoComLote(String nome, int quantidade, String custoUnitario) {
        Long produtoId = insertProduto(nome);
        insertLote(produtoId, quantidade, quantidade, bd(custoUnitario),
                bd(custoUnitario).multiply(java.math.BigDecimal.valueOf(quantidade)));
        jdbc.update("update produto set quantidade_atual = ? where id = ?", quantidade, produtoId);
        return produtoId;
    }
}
