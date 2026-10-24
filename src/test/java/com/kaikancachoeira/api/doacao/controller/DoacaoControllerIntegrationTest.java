package com.kaikancachoeira.api.doacao.controller;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoacaoControllerIntegrationTest extends DbIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private EntityManager entityManager;

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    private String menu() {
        return auth("DOACOES_MENU");
    }

    private ResultActions chamar(MockHttpServletRequestBuilder requisicao) throws Exception {
        sincronizar();
        return mockMvc.perform(requisicao);
    }

    private ResultActions enviarPost(String corpo) throws Exception {
        return enviarPost(corpo, menu());
    }

    private ResultActions enviarPost(String corpo, String token) throws Exception {
        return chamar(MockMvcRequestBuilders.post("/doacoes")
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions enviarPut(Long id, String corpo) throws Exception {
        return enviarPut(id, corpo, menu());
    }

    private ResultActions enviarPut(Long id, String corpo, String token) throws Exception {
        return chamar(MockMvcRequestBuilders.put("/doacoes/{id}", id)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions listar(String query) throws Exception {
        return listar(query, menu());
    }

    private ResultActions listar(String query, String token) throws Exception {
        return chamar(MockMvcRequestBuilders.get("/doacoes" + query).header(AUTHORIZATION, token));
    }

    private ResultActions pesquisar(String texto) throws Exception {
        return chamar(MockMvcRequestBuilders.get("/doacoes")
                .param("q", texto)
                .header(AUTHORIZATION, menu()));
    }

    private static String corpo(Object... pares) {
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (int i = 0; i < pares.length; i += 2) {
            Object valor = pares[i + 1];
            if (valor == null) {
                continue;
            }
            if (!primeiro) {
                sb.append(",");
            }
            primeiro = false;
            sb.append("\"").append(pares[i]).append("\":");
            if (valor instanceof Number) {
                sb.append(valor);
            } else {
                sb.append("\"").append(valor).append("\"");
            }
        }
        return sb.append("}").toString();
    }

    private static String dinheiro(Long evento, String doador, Object valor, String forma, Object ficha) {
        return corpo("eventoId", evento, "tipo", "dinheiro", "valor", valor,
                "formaPagamento", forma, "doador", doador, "valorFicha", ficha);
    }

    private static String produto(Long evento, String doador, String nome, Object ficha) {
        return corpo("eventoId", evento, "tipo", "produto", "nome", nome,
                "doador", doador, "valorFicha", ficha);
    }

    private static String reabastecimento(Long evento, String doador, Long produtoId, Object quantidade, Object ficha) {
        return corpo("eventoId", evento, "tipo", "reabastecimento", "produtoId", produtoId,
                "quantidade", quantidade, "doador", doador, "valorFicha", ficha);
    }

    private Long evento(String nome, String status) {
        return insertEvento(nome, "EVENTO", status);
    }

    private Long idDe(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private Map<String, Object> linhaDoacao(Long id) {
        sincronizar();
        return jdbc.queryForMap("select * from doacao where id = ?", id);
    }

    private Map<String, Object> lote(Long doacaoId) {
        sincronizar();
        return jdbc.queryForMap("""
                select e.* from estoque e
                join movimentacao_estoque m on m.estoque_id = e.id
                where m.doacao_id = ?""", doacaoId);
    }

    private Map<String, Object> movimentacao(Long doacaoId) {
        sincronizar();
        return jdbc.queryForMap("select * from movimentacao_estoque where doacao_id = ?", doacaoId);
    }

    private int contar(String tabela) {
        sincronizar();
        return count("select count(*) from " + tabela);
    }

    private Integer saldoProduto(Long produtoId) {
        sincronizar();
        return jdbc.queryForObject("select quantidade_atual from produto where id = ?", Integer.class, produtoId);
    }

    private void consumirLote(Long loteId, int quantidadeAtual, Long produtoId) {
        sincronizar();
        jdbc.update("update estoque set quantidade_atual = ? where id = ?", quantidadeAtual, loteId);
        jdbc.update("update produto set quantidade_atual = ? where id = ?", quantidadeAtual, produtoId);
    }

    private static Long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    private static BigDecimal decimal(Object valor) {
        return (BigDecimal) valor;
    }

    @Test
    void getSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/doacoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/doacoes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSemPermissaoDeveRetornar403() throws Exception {
        listar("", auth("EVENTOS_MENU"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void postSemPermissaoDeveRetornar403ENaoGravar() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "Ana", bd("50.00"), "PIX", null), auth("ESTOQUE_MENU"))
                .andExpect(status().isForbidden());

        assertThat(contar("doacao")).isZero();
    }

    @Test
    void putSemPermissaoDeveRetornar403() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);

        enviarPut(id, dinheiro(ev, "Bia", bd("20.00"), "PIX", null), auth("EVENTOS_MENU"))
                .andExpect(status().isForbidden());

        assertThat(linhaDoacao(id).get("doador")).isEqualTo("Ana");
    }

    @Test
    void registrarDoacaoEmDinheiroDeveRetornar201ComADoacao() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        ResultActions resposta = enviarPost(dinheiro(ev, "Ana", bd("50.00"), "PIX", bd("5.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.eventoId").value(ev.intValue()))
                .andExpect(jsonPath("$.tipo").value("DINHEIRO"))
                .andExpect(jsonPath("$.valor").value(50.0))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.doador").value("Ana"))
                .andExpect(jsonPath("$.valorFicha").value(5.0))
                .andExpect(jsonPath("$.criadoEm").value(notNullValue()))
                .andExpect(jsonPath("$.atualizadoEm").value(notNullValue()));

        Map<String, Object> linha = linhaDoacao(idDe(resposta));
        assertThat(linha.get("tipo")).isEqualTo("DINHEIRO");
        assertThat(decimal(linha.get("valor"))).isEqualByComparingTo("50.00");
        assertThat(linha.get("forma_pagamento")).isEqualTo("PIX");
        assertThat(linha.get("produto_id")).isNull();
        assertThat(linha.get("nome")).isNull();
        assertThat(linha.get("quantidade")).isNull();
        assertThat(contar("movimentacao_estoque")).isZero();
        assertThat(contar("estoque")).isZero();
    }

    @Test
    void tipoEFormaDePagamentoDevemSerAceitosSemDiferenciarMaiusculas() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(corpo("eventoId", ev, "tipo", "Dinheiro", "valor", bd("10.00"),
                "formaPagamento", "pix", "doador", "Ana"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DINHEIRO"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"));

        enviarPost(corpo("eventoId", ev, "tipo", "DINHEIRO", "valor", bd("10.00"),
                "formaPagamento", "DEBITO", "doador", "Ana"))
                .andExpect(status().isCreated());
    }

    @Test
    void valorFichaOpcionalEZeroEPermitido() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        Long semFicha = idDe(enviarPost(dinheiro(ev, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isCreated()));
        Long fichaZero = idDe(enviarPost(dinheiro(ev, "Ana", bd("10.00"), "PIX", bd("0")))
                .andExpect(status().isCreated()));

        assertThat(linhaDoacao(semFicha).get("valor_ficha")).isNull();
        assertThat(decimal(linhaDoacao(fichaZero).get("valor_ficha"))).isEqualByComparingTo("0");
    }

    @Test
    void doadorDeveSerAparadoNaGravacao() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        Long id = idDe(enviarPost(dinheiro(ev, "  Ana Maria  ", bd("10.00"), "PIX", null))
                .andExpect(status().isCreated()));

        assertThat(linhaDoacao(id).get("doador")).isEqualTo("Ana Maria");
    }

    @Test
    void valorMaiorQueZeroEObrigatorioParaDinheiro() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "Ana", bd("0"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")));

        enviarPost(dinheiro(ev, "Ana", bd("-5.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")));

        enviarPost(dinheiro(ev, "Ana", null, "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")));

        assertThat(contar("doacao")).isZero();
    }

    @Test
    void valorComMaisDeDuasCasasDeveSerRejeitado() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "Ana", bd("1.234"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")));
    }

    @Test
    void formaDePagamentoEObrigatoriaEPrecisaSerValida() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "Ana", bd("10.00"), null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("formaPagamento")));

        enviarPost(dinheiro(ev, "Ana", bd("10.00"), "FIADO", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("formaPagamento")));

        enviarPost(dinheiro(ev, "Ana", bd("10.00"), "CHEQUE", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("formaPagamento")));
    }

    @Test
    void erroDeMaisDeUmCampoDeveListarTodos() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "Ana", null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("formaPagamento")));
    }

    @Test
    void doadorEmBrancoOuAusenteDeveSerRejeitado() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "   ", bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("doador")));

        enviarPost(dinheiro(ev, null, bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("doador")));
    }

    @Test
    void doadorComMaisDe150CaracteresDeveSerRejeitado() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "x".repeat(151), bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("doador")));

        enviarPost(dinheiro(ev, "x".repeat(150), bd("10.00"), "PIX", null))
                .andExpect(status().isCreated());
    }

    @Test
    void valorFichaNegativoDeveSerRejeitado() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(dinheiro(ev, "Ana", bd("10.00"), "PIX", bd("-1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valorFicha")));
    }

    @Test
    void eventoAusenteOuInexistenteDeveRetornar400NoCampoEventoId() throws Exception {
        enviarPost(dinheiro(null, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("eventoId")));

        enviarPost(dinheiro(999999L, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("eventoId")));

        assertThat(contar("doacao")).isZero();
    }

    @Test
    void eventoEncerradoDeveRetornar409() throws Exception {
        Long ev = evento("Festa", "ENCERRADO");

        enviarPost(dinheiro(ev, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_ENCERRADO"));

        assertThat(contar("doacao")).isZero();
    }

    @Test
    void eventoEmAndamentoDeveAceitarDoacao() throws Exception {
        Long ev = evento("Festa", "EM_ANDAMENTO");

        enviarPost(dinheiro(ev, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isCreated());
    }

    @Test
    void tipoInvalidoOuAusenteDeveRetornar400() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(corpo("eventoId", ev, "tipo", "emprestimo", "doador", "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("tipo")));

        enviarPost(corpo("eventoId", ev, "doador", "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("tipo")));

        assertThat(contar("doacao")).isZero();
    }

    @Test
    void corpoMalformadoDeveRetornar400() throws Exception {
        enviarPost("{nao e json")
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarDoacaoDeProdutoNaoGeraEstoqueEIgnoraCamposDeOutrosTipos() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        ResultActions resposta = enviarPost(corpo("eventoId", ev, "tipo", "produto", "nome", "Arroz",
                "doador", "Bia", "valorFicha", bd("3.50"),
                "valor", bd("99.00"), "formaPagamento", "PIX", "quantidade", 7))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PRODUTO"))
                .andExpect(jsonPath("$.nome").value("Arroz"));

        Map<String, Object> linha = linhaDoacao(idDe(resposta));
        assertThat(linha.get("nome")).isEqualTo("Arroz");
        assertThat(linha.get("valor")).isNull();
        assertThat(linha.get("forma_pagamento")).isNull();
        assertThat(linha.get("quantidade")).isNull();
        assertThat(linha.get("produto_id")).isNull();
        assertThat(decimal(linha.get("valor_ficha"))).isEqualByComparingTo("3.50");
        assertThat(contar("movimentacao_estoque")).isZero();
        assertThat(contar("estoque")).isZero();
    }

    @Test
    void descricaoDoItemEObrigatoriaEAteCentoECinquentaCaracteres() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPost(produto(ev, "Bia", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")));

        enviarPost(produto(ev, "Bia", "   ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")));

        enviarPost(produto(ev, "Bia", "x".repeat(151), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")));

        enviarPost(produto(ev, "Bia", "x".repeat(150), null))
                .andExpect(status().isCreated());
    }

    @Test
    void registrarReabastecimentoDeveCriarLoteSemCustoMovimentacaoEAtualizarSaldo() throws Exception {
        Long ev = evento("Festa", "EM_ANDAMENTO");
        Long prod = insertProduto("Coxinha");

        ResultActions resposta = enviarPost(reabastecimento(ev, "Cid", prod, 12, bd("2.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("REABASTECIMENTO"))
                .andExpect(jsonPath("$.produtoId").value(prod.intValue()))
                .andExpect(jsonPath("$.quantidade").value(12))
                .andExpect(jsonPath("$.nome").value("Coxinha"));

        Long id = idDe(resposta);

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(linha.get("nome")).isEqualTo("Coxinha");
        assertThat(numero(linha.get("produto_id"))).isEqualTo(prod);
        assertThat(linha.get("quantidade")).isEqualTo(12);
        assertThat(linha.get("valor")).isNull();
        assertThat(linha.get("forma_pagamento")).isNull();

        Map<String, Object> lote = lote(id);
        assertThat(lote.get("quantidade_inicial")).isEqualTo(12);
        assertThat(lote.get("quantidade_atual")).isEqualTo(12);
        assertThat(decimal(lote.get("custo_unitario"))).isEqualByComparingTo("0");
        assertThat(decimal(lote.get("custo_total"))).isEqualByComparingTo("0");
        assertThat(numero(lote.get("produto_id"))).isEqualTo(prod);

        Map<String, Object> mov = movimentacao(id);
        assertThat(mov.get("quantidade")).isEqualTo(12);
        assertThat(numero(mov.get("estoque_id"))).isEqualTo(numero(lote.get("id")));
        assertThat(numero(mov.get("evento_id"))).isEqualTo(ev);
        assertThat(numero(mov.get("doacao_id"))).isEqualTo(id);
        assertThat(mov.get("item_venda_id")).isNull();
        assertThat(decimal(mov.get("custo_unitario"))).isEqualByComparingTo("0");
        assertThat(decimal(mov.get("custo_total"))).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("""
                select t.codigo from tipo_movimentacao t where t.id = ?""",
                String.class, mov.get("tipo_movimentacao_id"))).isEqualTo("DOACAO");

        assertThat(saldoProduto(prod)).isEqualTo(12);
        assertThat(contar("movimentacao_estoque")).isEqualTo(1);
        assertThat(contar("estoque")).isEqualTo(1);
    }

    @Test
    void reabastecimentoDeveSomarAoSaldoExistenteEGerarNovoLote() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        insertLote(prod, 5, 5, bd("1.00"), bd("5.00"));
        jdbc.update("update produto set quantidade_atual = 5 where id = ?", prod);

        enviarPost(reabastecimento(ev, "Cid", prod, 8, null))
                .andExpect(status().isCreated());

        assertThat(saldoProduto(prod)).isEqualTo(13);
        assertThat(contar("estoque")).isEqualTo(2);
    }

    @Test
    void reabastecimentoExigeProdutoExistenteEAtivo() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long inativo = insertProduto("Antigo");
        jdbc.update("update produto set ativo = false where id = ?", inativo);

        enviarPost(reabastecimento(ev, "Cid", null, 5, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("produtoId")));

        enviarPost(reabastecimento(ev, "Cid", 999999L, 5, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("produtoId")));

        enviarPost(reabastecimento(ev, "Cid", inativo, 5, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("produtoId")));

        assertThat(contar("doacao")).isZero();
        assertThat(contar("estoque")).isZero();
    }

    @Test
    void reabastecimentoExigeQuantidadeMaiorQueZero() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");

        enviarPost(reabastecimento(ev, "Cid", prod, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("quantidade")));

        enviarPost(reabastecimento(ev, "Cid", prod, 0, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("quantidade")));

        enviarPost(reabastecimento(ev, "Cid", prod, -3, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("quantidade")));

        assertThat(contar("doacao")).isZero();
        assertThat(saldoProduto(prod)).isZero();
    }

    @Test
    void reabastecimentoEmEventoEncerradoNaoDeveAlterarEstoque() throws Exception {
        Long ev = evento("Festa", "ENCERRADO");
        Long prod = insertProduto("Coxinha");

        enviarPost(reabastecimento(ev, "Cid", prod, 5, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_ENCERRADO"));

        assertThat(contar("estoque")).isZero();
        assertThat(contar("movimentacao_estoque")).isZero();
        assertThat(saldoProduto(prod)).isZero();
    }

    @Test
    void editarDoacaoEmDinheiroDeveAtualizarOsCamposPermitidos() throws Exception {
        Long ev1 = evento("Festa", "NAO_INICIADO");
        Long ev2 = evento("Bazar", "NAO_INICIADO");
        Long id = insertDoacao(ev1, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);

        enviarPut(id, dinheiro(ev2, "Ana Maria", bd("75.50"), "CREDITO", bd("4.00")))
                .andExpect(status().isNoContent());

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(numero(linha.get("evento_id"))).isEqualTo(ev2);
        assertThat(linha.get("doador")).isEqualTo("Ana Maria");
        assertThat(decimal(linha.get("valor"))).isEqualByComparingTo("75.50");
        assertThat(linha.get("forma_pagamento")).isEqualTo("CREDITO");
        assertThat(decimal(linha.get("valor_ficha"))).isEqualByComparingTo("4.00");
        assertThat(linha.get("tipo")).isEqualTo("DINHEIRO");
    }

    @Test
    void editarNaoDeveAlterarCriadoEm() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);
        jdbc.update("update doacao set criado_em = ? where id = ?", T0.minusDays(10), id);
        Object antes = linhaDoacao(id).get("criado_em");

        enviarPut(id, dinheiro(ev, "Ana", bd("20.00"), "PIX", null))
                .andExpect(status().isNoContent());

        assertThat(linhaDoacao(id).get("criado_em")).isEqualTo(antes);
    }

    @Test
    void editarDoacaoInexistenteDeveRetornar404() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");

        enviarPut(999999L, dinheiro(ev, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void editarNaoPermiteAlterarOTipo() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);

        enviarPut(id, produto(ev, "Ana", "Arroz", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("tipo")));

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(linha.get("tipo")).isEqualTo("DINHEIRO");
        assertThat(linha.get("nome")).isNull();
    }

    @Test
    void editarDeveValidarOsCamposDoTipo() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);

        enviarPut(id, dinheiro(ev, "Ana", bd("0"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")));

        enviarPut(id, dinheiro(ev, "Ana", bd("10.00"), "XYZ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("formaPagamento")));

        enviarPut(id, dinheiro(ev, " ", bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("doador")));

        assertThat(decimal(linhaDoacao(id).get("valor"))).isEqualByComparingTo("10.00");
    }

    @Test
    void editarComEventoInexistenteDeveRetornar400() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);

        enviarPut(id, dinheiro(999999L, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("eventoId")));

        assertThat(numero(linhaDoacao(id).get("evento_id"))).isEqualTo(ev);
    }

    @Test
    void editarEPermitidoEmEventoEncerrado() throws Exception {
        Long encerrado = evento("Festa velha", "ENCERRADO");
        Long outroEncerrado = evento("Bazar velho", "ENCERRADO");
        Long id = insertDoacao(encerrado, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);

        enviarPut(id, dinheiro(outroEncerrado, "Ana", bd("30.00"), "DEBITO", bd("1.00")))
                .andExpect(status().isNoContent());

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(numero(linha.get("evento_id"))).isEqualTo(outroEncerrado);
        assertThat(decimal(linha.get("valor"))).isEqualByComparingTo("30.00");
    }

    @Test
    void editarDoacaoDeProdutoDeveAlterarDescricaoDoadorEFicha() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "PRODUTO", null, "Arroz", null, null, null, "Bia", null);

        enviarPut(id, corpo("eventoId", ev, "tipo", "produto", "nome", "Arroz integral",
                "doador", "Beatriz", "valorFicha", bd("2.50"), "quantidade", 4, "valor", bd("9.00")))
                .andExpect(status().isNoContent());

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(linha.get("nome")).isEqualTo("Arroz integral");
        assertThat(linha.get("doador")).isEqualTo("Beatriz");
        assertThat(decimal(linha.get("valor_ficha"))).isEqualByComparingTo("2.50");
        assertThat(linha.get("quantidade")).isNull();
        assertThat(linha.get("valor")).isNull();
        assertThat(contar("movimentacao_estoque")).isZero();
    }

    @Test
    void editarDoacaoDeProdutoExigeDescricao() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "PRODUTO", null, "Arroz", null, null, null, "Bia", null);

        enviarPut(id, produto(ev, "Bia", " ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")));

        assertThat(linhaDoacao(id).get("nome")).isEqualTo("Arroz");
    }

    @Test
    void editarValorFichaAusenteDeveLimparOValor() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", bd("5.00"));

        enviarPut(id, dinheiro(ev, "Ana", bd("10.00"), "PIX", null))
                .andExpect(status().isNoContent());

        assertThat(linhaDoacao(id).get("valor_ficha")).isNull();
    }

    @Test
    void aumentarQuantidadeDoReabastecimentoDeveAjustarLoteMovimentacaoESaldo() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));

        enviarPut(id, reabastecimento(ev, "Cid", prod, 15, bd("1.00")))
                .andExpect(status().isNoContent());

        assertThat(linhaDoacao(id).get("quantidade")).isEqualTo(15);
        Map<String, Object> lote = lote(id);
        assertThat(lote.get("quantidade_inicial")).isEqualTo(15);
        assertThat(lote.get("quantidade_atual")).isEqualTo(15);
        assertThat(movimentacao(id).get("quantidade")).isEqualTo(15);
        assertThat(saldoProduto(prod)).isEqualTo(15);
        assertThat(contar("movimentacao_estoque")).isEqualTo(1);
        assertThat(contar("estoque")).isEqualTo(1);
    }

    @Test
    void reduzirQuantidadeAteOConsumidoDeveAjustarPelaDiferenca() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));
        Long loteId = numero(lote(id).get("id"));
        consumirLote(loteId, 4, prod);

        enviarPut(id, reabastecimento(ev, "Cid", prod, 7, null))
                .andExpect(status().isNoContent());

        Map<String, Object> lote = lote(id);
        assertThat(lote.get("quantidade_inicial")).isEqualTo(7);
        assertThat(lote.get("quantidade_atual")).isEqualTo(1);
        assertThat(movimentacao(id).get("quantidade")).isEqualTo(7);
        assertThat(saldoProduto(prod)).isEqualTo(1);

        enviarPut(id, reabastecimento(ev, "Cid", prod, 6, null))
                .andExpect(status().isNoContent());

        lote = lote(id);
        assertThat(lote.get("quantidade_inicial")).isEqualTo(6);
        assertThat(lote.get("quantidade_atual")).isEqualTo(0);
        assertThat(saldoProduto(prod)).isEqualTo(0);
    }

    @Test
    void reduzirQuantidadeAbaixoDoConsumidoDeveRetornar409ENaoAlterarNada() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));
        Long loteId = numero(lote(id).get("id"));
        consumirLote(loteId, 4, prod);

        enviarPut(id, reabastecimento(ev, "Outro", prod, 5, bd("9.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("QUANTIDADE_ABAIXO_DO_CONSUMIDO"));

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(linha.get("quantidade")).isEqualTo(10);
        assertThat(linha.get("doador")).isEqualTo("Cid");
        assertThat(linha.get("valor_ficha")).isNull();
        Map<String, Object> lote = lote(id);
        assertThat(lote.get("quantidade_inicial")).isEqualTo(10);
        assertThat(lote.get("quantidade_atual")).isEqualTo(4);
        assertThat(movimentacao(id).get("quantidade")).isEqualTo(10);
        assertThat(saldoProduto(prod)).isEqualTo(4);
    }

    @Test
    void editarReabastecimentoSemMudarQuantidadeNaoDeveMexerNoEstoque() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));
        Long loteId = numero(lote(id).get("id"));
        consumirLote(loteId, 3, prod);

        enviarPut(id, reabastecimento(ev, "Cid Souza", prod, 10, bd("2.00")))
                .andExpect(status().isNoContent());

        Map<String, Object> linha = linhaDoacao(id);
        assertThat(linha.get("doador")).isEqualTo("Cid Souza");
        assertThat(decimal(linha.get("valor_ficha"))).isEqualByComparingTo("2.00");
        Map<String, Object> lote = lote(id);
        assertThat(lote.get("quantidade_inicial")).isEqualTo(10);
        assertThat(lote.get("quantidade_atual")).isEqualTo(3);
        assertThat(saldoProduto(prod)).isEqualTo(3);
        assertThat(contar("movimentacao_estoque")).isEqualTo(1);
    }

    @Test
    void editarReabastecimentoNaoPermiteTrocarOProduto() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long outro = insertProduto("Pastel");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));

        enviarPut(id, reabastecimento(ev, "Cid", outro, 10, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("produtoId")));

        assertThat(numero(linhaDoacao(id).get("produto_id"))).isEqualTo(prod);
    }

    @Test
    void editarReabastecimentoSemInformarProdutoMantemOProduto() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));

        enviarPut(id, reabastecimento(ev, "Cid", null, 12, null))
                .andExpect(status().isNoContent());

        assertThat(numero(linhaDoacao(id).get("produto_id"))).isEqualTo(prod);
        assertThat(saldoProduto(prod)).isEqualTo(12);
    }

    @Test
    void editarReabastecimentoDeveExigirQuantidadeMaiorQueZero() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev, "Cid", prod, 10, null)).andExpect(status().isCreated()));

        enviarPut(id, reabastecimento(ev, "Cid", prod, 0, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("quantidade")));

        assertThat(linhaDoacao(id).get("quantidade")).isEqualTo(10);
        assertThat(saldoProduto(prod)).isEqualTo(10);
    }

    @Test
    void trocarOEventoDoReabastecimentoDeveAtualizarOEventoDaMovimentacao() throws Exception {
        Long ev1 = evento("Festa", "NAO_INICIADO");
        Long ev2 = evento("Bazar", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        Long id = idDe(enviarPost(reabastecimento(ev1, "Cid", prod, 10, null)).andExpect(status().isCreated()));

        enviarPut(id, reabastecimento(ev2, "Cid", prod, 10, null))
                .andExpect(status().isNoContent());

        assertThat(numero(linhaDoacao(id).get("evento_id"))).isEqualTo(ev2);
        assertThat(numero(movimentacao(id).get("evento_id"))).isEqualTo(ev2);
        assertThat(contar("movimentacao_estoque")).isEqualTo(1);
    }

    @Test
    void listarSemDoacoesDeveRetornarPaginaVazia() throws Exception {
        listar("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void listarDeveOrdenarDaMaisRecenteParaAMaisAntiga() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long a = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);
        Long b = insertDoacao(ev, "PRODUTO", null, "Arroz", null, null, null, "Bia", null);
        Long c = insertDoacao(ev, "DINHEIRO", null, null, null, bd("30.00"), "PIX", "Cid", null);
        jdbc.update("update doacao set criado_em = ? where id = ?", T0, a);
        jdbc.update("update doacao set criado_em = ? where id = ?", T0.plusHours(1), b);
        jdbc.update("update doacao set criado_em = ? where id = ?", T0.plusHours(2), c);

        listar("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.itens[0].id").value(c.intValue()))
                .andExpect(jsonPath("$.itens[1].id").value(b.intValue()))
                .andExpect(jsonPath("$.itens[2].id").value(a.intValue()))
                .andExpect(jsonPath("$.itens[0].doador").value("Cid"))
                .andExpect(jsonPath("$.itens[0].eventoId").value(ev.intValue()))
                .andExpect(jsonPath("$.itens[0].tipo").value("DINHEIRO"))
                .andExpect(jsonPath("$.itens[0].criadoEm").value(notNullValue()))
                .andExpect(jsonPath("$.itens[0].atualizadoEm").value(notNullValue()));
    }

    @Test
    void listarDevePaginarERetornarOEnvelope() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        for (int i = 1; i <= 3; i++) {
            insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Doador " + i, null);
        }

        listar("?page=1&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void listarDeveValidarPaginacao() throws Exception {
        listar("?size=101").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        listar("?size=0").andExpect(status().isBadRequest());
        listar("?page=-1").andExpect(status().isBadRequest());
        listar("?size=100").andExpect(status().isOk());
    }

    @Test
    void listarDeveFiltrarPorTextoNoDoadorOuNoItemSemDiferenciarMaiusculas() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Refrigerante");
        Long porDoador = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Maria Silva", null);
        Long porItem = insertDoacao(ev, "PRODUTO", null, "Bolo de MARIA", null, null, null, "Joao", null);
        Long porProduto = insertDoacao(ev, "REABASTECIMENTO", prod, "Refrigerante", 5, null, null, "Pedro", null);

        pesquisar("maria")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[*].id", hasItem(porDoador.intValue())))
                .andExpect(jsonPath("$.itens[*].id", hasItem(porItem.intValue())));

        pesquisar("refri")
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(porProduto.intValue()));

        pesquisar("nao existe")
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test
    void listarDeveTratarCaracteresEspeciaisDoLikeComoTextoLiteral() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long comPercentual = insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "100% Ana", null);
        insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Bia", null);

        pesquisar("%")
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(comPercentual.intValue()));
    }

    @Test
    void listarDeveAceitarVariosTipos() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long prod = insertProduto("Coxinha");
        insertDoacao(ev, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);
        insertDoacao(ev, "PRODUTO", null, "Arroz", null, null, null, "Bia", null);
        insertDoacao(ev, "REABASTECIMENTO", prod, "Coxinha", 5, null, null, "Cid", null);

        listar("?tipo=dinheiro&tipo=produto")
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[*].tipo", hasItem("DINHEIRO")))
                .andExpect(jsonPath("$.itens[*].tipo", hasItem("PRODUTO")));

        listar("?tipo=reabastecimento")
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].tipo").value("REABASTECIMENTO"));
    }

    @Test
    void listarDeveFiltrarPorTipoInvalidoRetornando400() throws Exception {
        listar("?tipo=emprestimo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("tipo")));
    }

    @Test
    void listarDeveAceitarVariosEventos() throws Exception {
        Long e1 = evento("Festa", "NAO_INICIADO");
        Long e2 = evento("Bazar", "NAO_INICIADO");
        Long e3 = evento("Rifa", "NAO_INICIADO");
        insertDoacao(e1, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana", null);
        insertDoacao(e2, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Bia", null);
        insertDoacao(e3, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Cid", null);

        listar("?eventoId=" + e1 + "&eventoId=" + e3)
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[*].eventoId", hasItem(e1.intValue())))
                .andExpect(jsonPath("$.itens[*].eventoId", hasItem(e3.intValue())));

        listar("?eventoId=" + e2)
                .andExpect(jsonPath("$.totalItens").value(1));
    }

    @Test
    void listarDeveCombinarTextoTipoEEvento() throws Exception {
        Long e1 = evento("Festa", "NAO_INICIADO");
        Long e2 = evento("Bazar", "NAO_INICIADO");
        Long alvo = insertDoacao(e1, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana Paula", null);
        insertDoacao(e2, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Ana Clara", null);
        insertDoacao(e1, "PRODUTO", null, "Arroz", null, null, null, "Ana Lima", null);
        insertDoacao(e1, "DINHEIRO", null, null, null, bd("10.00"), "PIX", "Bia", null);

        listar("?q=ana&tipo=dinheiro&eventoId=" + e1)
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(alvo.intValue()));
    }

    @Test
    void registroCriadoPelaApiDeveAparecerNaListagem() throws Exception {
        Long ev = evento("Festa", "NAO_INICIADO");
        Long id = idDe(enviarPost(dinheiro(ev, "Ana", bd("25.00"), "DINHEIRO", bd("2.00")))
                .andExpect(status().isCreated()));

        pesquisar("ana")
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id.intValue()))
                .andExpect(jsonPath("$.itens[0].valor").value(25.0))
                .andExpect(jsonPath("$.itens[0].formaPagamento").value("DINHEIRO"))
                .andExpect(jsonPath("$.itens[0].valorFicha").value(2.0));
    }
}
