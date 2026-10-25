package com.kaikancachoeira.api.evento.controller;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

class EventoControllerIntegrationTest extends DbIntegrationTest {

    private static final Instant INICIO = T0.toInstant();
    private static final Instant FIM = INICIO.plusSeconds(8 * 3600);

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    private String leitura() {
        return auth("EVENTOS_LEITURA");
    }

    private String menu() {
        return auth("EVENTOS_MENU");
    }

    private ResultActions listar(String auth, String... params) throws Exception {
        MockHttpServletRequestBuilder req = get("/eventos").header(AUTHORIZATION, auth);
        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(req);
    }

    private ResultActions buscar(Long id, String auth) throws Exception {
        return mockMvc.perform(get("/eventos/{id}", id).header(AUTHORIZATION, auth));
    }

    private ResultActions criar(String corpo, String auth) throws Exception {
        return mockMvc.perform(post("/eventos").header(AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions atualizar(Long id, String corpo, String auth) throws Exception {
        return mockMvc.perform(put("/eventos/{id}", id).header(AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions excluir(Long id, String auth) throws Exception {
        return mockMvc.perform(delete("/eventos/{id}", id)
                .header(AUTHORIZATION, auth));
    }

    private static String json(String nome, String tipo, Instant inicio, Instant fim) {
        return """
                {"nome": "%s", "observacoes": "obs", "tipoEvento": "%s",
                 "dataHoraInicio": "%s", "dataHoraEncerramento": "%s"}"""
                .formatted(nome, tipo, inicio, fim);
    }

    private static String jsonValido(String nome) {
        return json(nome, "EVENTO", INICIO, FIM);
    }

    private Integer statusId(String codigo) {
        return jdbc.queryForObject("select id from status_evento where codigo = ?", Integer.class, codigo);
    }

    private static BigDecimal numero(String corpo, String path) {
        Object valor = JsonPath.read(corpo, path);
        return new BigDecimal(valor.toString());
    }

    @Test
    void listarSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/eventos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void listarExigeEventosLeituraEMenuNaoBasta() throws Exception {
        listar(menu())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void listarSemNenhumFiltroDeveRetornarTodos() throws Exception {
        insertEvento("A", "EVENTO", "NAO_INICIADO");
        insertEvento("B", "REUNIAO", "ENCERRADO", T0.plusDays(1), T0.plusDays(1).plusHours(2));

        listar(leitura())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listarSemEventosDeveRetornarListaVazia() throws Exception {
        listar(leitura())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarDeveColocarEmAndamentoPrimeiroEDepoisDoMaisRecenteParaOMaisAntigo() throws Exception {
        insertEvento("Antigo", "EVENTO", "ENCERRADO", T0, T0.plusHours(8));
        insertEvento("Futuro", "EVENTO", "NAO_INICIADO", T0.plusDays(10), T0.plusDays(10).plusHours(8));
        insertEvento("Atual", "EVENTO", "EM_ANDAMENTO", T0.minusDays(5), T0.minusDays(5).plusHours(8));
        insertEvento("Meio", "EVENTO", "ENCERRADO", T0.plusDays(3), T0.plusDays(3).plusHours(8));

        listar(leitura())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Atual"))
                .andExpect(jsonPath("$[1].nome").value("Futuro"))
                .andExpect(jsonPath("$[2].nome").value("Meio"))
                .andExpect(jsonPath("$[3].nome").value("Antigo"));
    }

    @Test
    void listarDeveMontarOContratoDaResposta() throws Exception {
        insertEvento("Bingo", "EVENTO", "NAO_INICIADO");

        listar(leitura())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].nome").value("Bingo"))
                .andExpect(jsonPath("$[0].tipoEvento").value("EVENTO"))
                .andExpect(jsonPath("$[0].status.codigo").value("NAO_INICIADO"))
                .andExpect(jsonPath("$[0].status.descricao").value("Evento não iniciado"))
                .andExpect(jsonPath("$[0].dataHoraInicio").isNotEmpty())
                .andExpect(jsonPath("$[0].dataHoraEncerramento").isNotEmpty())
                .andExpect(jsonPath("$[0].iniciadoEm").doesNotExist())
                .andExpect(jsonPath("$[0].encerradoEm").doesNotExist());
    }

    @Test
    void filtroQDeveIgnorarMaiusculasEEspacosExternos() throws Exception {
        insertEvento("Undoukai 2026", "EVENTO", "NAO_INICIADO");
        insertEvento("Bingo Beneficente", "EVENTO", "NAO_INICIADO");

        listar(leitura(), "q", "  UNDOUKAI ")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Undoukai 2026"));
    }

    @Test
    void filtroQEmBrancoDeveSerIgnorado() throws Exception {
        insertEvento("A", "EVENTO", "NAO_INICIADO");
        insertEvento("B", "EVENTO", "NAO_INICIADO", T0.plusDays(1), T0.plusDays(1).plusHours(1));

        listar(leitura(), "q", "   ")
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void filtroPorStatus() throws Exception {
        insertEvento("Novo", "EVENTO", "NAO_INICIADO");
        insertEvento("Velho", "EVENTO", "ENCERRADO", T0.plusDays(1), T0.plusDays(1).plusHours(1));

        listar(leitura(), "statusId", statusId("ENCERRADO").toString())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Velho"));
    }

    @Test
    void filtroPorTipo() throws Exception {
        insertEvento("Evento", "EVENTO", "NAO_INICIADO");
        insertEvento("Reuniao", "REUNIAO", "NAO_INICIADO", T0.plusDays(1), T0.plusDays(1).plusHours(1));

        listar(leitura(), "tipoEvento", "REUNIAO")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Reuniao"));
    }

    @Test
    void tipoDesconhecidoDeveRetornar400() throws Exception {
        listar(leitura(), "tipoEvento", "FESTA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.campos[0].campo").value("tipoEvento"));
    }

    @Test
    void filtroDePeriodoDeveUsarOInicioPrevistoEEhInclusivoNasDuasPontas() throws Exception {
        insertEvento("Dia 0", "EVENTO", "ENCERRADO", T0, T0.plusHours(1));
        insertEvento("Dia 10", "EVENTO", "ENCERRADO", T0.plusDays(10), T0.plusDays(10).plusHours(1));
        insertEvento("Dia 20", "EVENTO", "ENCERRADO", T0.plusDays(20), T0.plusDays(20).plusHours(1));

        listar(leitura(), "dataInicio", T0.plusDays(5).toInstant().toString())
                .andExpect(jsonPath("$", hasSize(2)));

        listar(leitura(), "dataFim", T0.plusDays(15).toInstant().toString())
                .andExpect(jsonPath("$", hasSize(2)));

        listar(leitura(),
                "dataInicio", T0.plusDays(10).toInstant().toString(),
                "dataFim", T0.plusDays(10).toInstant().toString())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Dia 10"));
    }

    @Test
    void filtrosDevemCombinarComE() throws Exception {
        insertEvento("Bingo A", "EVENTO", "NAO_INICIADO");
        insertEvento("Bingo B", "REUNIAO", "NAO_INICIADO", T0.plusDays(1), T0.plusDays(1).plusHours(1));
        insertEvento("Outro", "EVENTO", "NAO_INICIADO", T0.plusDays(2), T0.plusDays(2).plusHours(1));

        listar(leitura(), "q", "bingo", "tipoEvento", "EVENTO", "statusId", statusId("NAO_INICIADO").toString())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Bingo A"));
    }

    @Test
    void filtroSemResultadoDeveRetornarListaVazia() throws Exception {
        insertEvento("A", "EVENTO", "NAO_INICIADO");

        listar(leitura(), "q", "inexistente")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void buscarExigeEventosMenu() throws Exception {
        Long id = insertEvento("A", "EVENTO", "NAO_INICIADO");

        buscar(id, leitura())
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarInexistenteDeveRetornar404() throws Exception {
        buscar(999_999L, menu())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void buscarEventoSemMovimentoDeveRetornarResumoZerado() throws Exception {
        Long id = insertEvento("Vazio", "EVENTO", "NAO_INICIADO");

        String corpo = buscar(id, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Vazio"))
                .andExpect(jsonPath("$.observacoes").doesNotExist())
                .andExpect(jsonPath("$.status.codigo").value("NAO_INICIADO"))
                .andExpect(jsonPath("$.resumo.quantidadeProdutos").value(0))
                .andExpect(jsonPath("$.resumo.quantidadeVendas").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat(numero(corpo, "$.resumo.totalVendido")).isEqualByComparingTo("0");
        assertThat(numero(corpo, "$.resumo.totalCustosAdicionais")).isEqualByComparingTo("0");
    }

    @Test
    void buscarDeveCalcularOResumoIgnorandoVendasCanceladasEOutrosEventos() throws Exception {
        Long evento = insertEvento("Cheio", "EVENTO", "EM_ANDAMENTO");
        Long outro = insertEvento("Outro", "EVENTO", "ENCERRADO", T0.plusDays(1), T0.plusDays(1).plusHours(1));
        Long produto = insertProduto("Coca");
        insertPeEstoque(evento, produto, "Coca", bd("5.00"), 10);
        insertPeProduzido(evento, "Tempurá", bd("8.00"), bd("10.00"));

        insertVenda(evento, bd("0.00"), bd("30.00"));
        insertVenda(evento, bd("0.00"), bd("20.00"));
        Long cancelada = insertVenda(evento, bd("0.00"), bd("12.00"));
        jdbc.update("update venda set cancelada_em = now() where id = ?", cancelada);
        insertVenda(outro, bd("0.00"), bd("99.00"));

        jdbc.update("insert into custo_evento (evento_id, nome, valor) values (?, 'Limpeza', 20.00)", evento);
        jdbc.update("insert into custo_evento (evento_id, nome, valor) values (?, 'Ajudantes', 5.50)", evento);
        jdbc.update("insert into custo_evento (evento_id, nome, valor) values (?, 'De outro', 70.00)", outro);

        String corpo = buscar(evento, menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.quantidadeProdutos").value(2))
                .andExpect(jsonPath("$.resumo.quantidadeVendas").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(numero(corpo, "$.resumo.totalVendido")).isEqualByComparingTo("50.00");
        assertThat(numero(corpo, "$.resumo.totalCustosAdicionais")).isEqualByComparingTo("25.50");
    }

    @Test
    void criarSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(post("/eventos").contentType(MediaType.APPLICATION_JSON).content(jsonValido("A")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarExigeEventosMenuELeituraNaoBasta() throws Exception {
        criar(jsonValido("A"), leitura())
                .andExpect(status().isForbidden());
    }

    @Test
    void criarDeveRetornar201ComStatusNaoIniciadoEPersistir() throws Exception {
        criar(jsonValido("Undoukai"), menu())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Undoukai"))
                .andExpect(jsonPath("$.tipoEvento").value("EVENTO"))
                .andExpect(jsonPath("$.status.codigo").value("NAO_INICIADO"))
                .andExpect(jsonPath("$.iniciadoEm").doesNotExist())
                .andExpect(jsonPath("$.encerradoEm").doesNotExist());

        assertThat(count("select count(*) from evento where nome = 'Undoukai'")).isEqualTo(1);
    }

    @Test
    void criarDeveApararEspacosDoNome() throws Exception {
        criar(jsonValido("  Bingo  "), menu())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Bingo"));
    }

    @Test
    void criarReuniaoSemObservacoes() throws Exception {
        String corpo = """
                {"nome": "Reuniao", "tipoEvento": "REUNIAO",
                 "dataHoraInicio": "%s", "dataHoraEncerramento": "%s"}""".formatted(INICIO, FIM);

        criar(corpo, menu())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("REUNIAO"));
    }

    @Test
    void criarEventoNoPassadoDeveSerPermitido() throws Exception {
        Instant inicio = Instant.parse("2020-05-01T10:00:00Z");

        criar(json("Antigo", "EVENTO", inicio, inicio.plusSeconds(3600)), menu())
                .andExpect(status().isCreated());
    }

    @Test
    void criarDuplicadoDeveRetornar409() throws Exception {
        criar(jsonValido("Undoukai"), menu()).andExpect(status().isCreated());

        criar(jsonValido("Undoukai"), menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_DUPLICADO"));
    }

    @Test
    void duplicidadeConsideraONomeSemEspacosExternos() throws Exception {
        criar(jsonValido("Undoukai"), menu()).andExpect(status().isCreated());

        criar(jsonValido("  Undoukai "), menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_DUPLICADO"));
    }

    @Test
    void mesmoNomeEmOutroPeriodoOuOutroNomeNoMesmoPeriodoSaoPermitidos() throws Exception {
        criar(jsonValido("Undoukai"), menu()).andExpect(status().isCreated());

        criar(json("Undoukai", "EVENTO", INICIO.plusSeconds(86_400), FIM.plusSeconds(86_400)), menu())
                .andExpect(status().isCreated());
        criar(jsonValido("Bingo"), menu())
                .andExpect(status().isCreated());
    }

    @Test
    void encerramentoIgualAoInicioDeveRetornar400() throws Exception {
        criar(json("A", "EVENTO", INICIO, INICIO), menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("periodoValido")));

        assertThat(count("select count(*) from evento")).isZero();
    }

    @Test
    void encerramentoAnteriorAoInicioDeveRetornar400() throws Exception {
        criar(json("A", "EVENTO", INICIO, INICIO.minusSeconds(3600)), menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("periodoValido")));
    }

    @Test
    void nomeEmBrancoDeveRetornar400() throws Exception {
        criar(jsonValido("   "), menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")));
    }

    @Test
    void nomeComMaisDe150CaracteresDeveRetornar400() throws Exception {
        criar(jsonValido("x".repeat(151)), menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")));
    }

    @Test
    void camposObrigatoriosAusentesDevemRetornar400SemReclamarDoPeriodo() throws Exception {
        criar("{}", menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("tipoEvento")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("dataHoraInicio")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("dataHoraEncerramento")))
                .andExpect(jsonPath("$.campos[*].campo", not(hasItem("periodoValido"))));
    }

    @Test
    void tipoEventoDesconhecidoNoCorpoDeveRetornar400() throws Exception {
        criar(json("A", "FESTA", INICIO, FIM), menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void atualizarExigeEventosMenu() throws Exception {
        Long id = insertEvento("A", "EVENTO", "NAO_INICIADO");

        atualizar(id, jsonValido("B"), leitura())
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizarInexistenteDeveRetornar404() throws Exception {
        atualizar(999_999L, jsonValido("A"), menu())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void atualizarDeveAlterarOsCamposEPersistir() throws Exception {
        Long id = insertEvento("Antigo", "EVENTO", "NAO_INICIADO");
        Instant novoInicio = INICIO.plusSeconds(86_400);
        Instant novoFim = novoInicio.plusSeconds(7200);

        atualizar(id, json("  Novo nome ", "REUNIAO", novoInicio, novoFim), menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Novo nome"))
                .andExpect(jsonPath("$.tipoEvento").value("REUNIAO"))
                .andExpect(jsonPath("$.dataHoraInicio").value(novoInicio.toString()))
                .andExpect(jsonPath("$.dataHoraEncerramento").value(novoFim.toString()));

        assertThat(jdbc.queryForObject("select nome || '|' || tipo_evento from evento where id = ?",
                String.class, id)).isEqualTo("Novo nome|REUNIAO");
    }

    @Test
    void atualizarEventoEncerradoDeveSerPermitidoEManterOStatus() throws Exception {
        Long id = insertEvento("Velho", "EVENTO", "ENCERRADO");

        atualizar(id, jsonValido("Velho corrigido"), menu())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Velho corrigido"))
                .andExpect(jsonPath("$.status.codigo").value("ENCERRADO"));
    }

    @Test
    void atualizarComOsMesmosDadosNaoDeveConflitarComEleMesmo() throws Exception {
        Long id = insertEvento("Igual", "EVENTO", "NAO_INICIADO");

        atualizar(id, jsonValido("Igual"), menu())
                .andExpect(status().isOk());
    }

    @Test
    void atualizarParaNomeEPeriodoDeOutroEventoDeveRetornar409() throws Exception {
        insertEvento("Existente", "EVENTO", "NAO_INICIADO");
        Long id = insertEvento("Outro", "EVENTO", "NAO_INICIADO");

        atualizar(id, jsonValido("Existente"), menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_DUPLICADO"));
    }

    @Test
    void atualizarComPeriodoInvalidoDeveRetornar400() throws Exception {
        Long id = insertEvento("A", "EVENTO", "NAO_INICIADO");

        atualizar(id, json("A", "EVENTO", INICIO, INICIO), menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("periodoValido")));
    }

    @Test
    void trocarReuniaoParaEventoComOutroEmAndamentoDeveRetornar409PeloHandler() throws Exception {
        insertEvento("Atual", "EVENTO", "EM_ANDAMENTO");
        Long reuniao = insertEvento("Reuniao", "REUNIAO", "EM_ANDAMENTO");

        atualizar(reuniao, json("Reuniao", "EVENTO", INICIO, FIM), menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_EM_ANDAMENTO_EXISTENTE"));
    }

    @Test
    void excluirEventoNaoIniciadoSemDependenciasDeveRetornar204() throws Exception {
        Long id = insertEvento("Evento para excluir", "EVENTO", "NAO_INICIADO");

        assertThat(count("select count(*) from evento where id = ?", id)).isEqualTo(1);

        excluir(id, menu())
                .andExpect(status().isNoContent());

        assertThat(count("select count(*) from evento where id = ?", id))
                .isZero();
    }

    @Test
    void excluirEventoEmAndamentoDeveRetornar409() throws Exception {
        Long id = insertEvento("Evento em andamento", "EVENTO", "EM_ANDAMENTO");

        excluir(id, menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EXCLUSAO_EVENTO_STATUS_INVALIDO"));

        assertThat(count("select count(*) from evento where id = ?", id))
                .isEqualTo(1);
    }


    @Test
    void excluirEventoComVendaCanceladaDeveRetornar409() throws Exception {
        Long evento = insertEvento("Evento com venda cancelada", "EVENTO", "NAO_INICIADO");
        Long venda = insertVenda(evento, bd("0.00"), bd("30.00"));

        jdbc.update("update venda set cancelada_em = now() where id = ?", venda);

        excluir(evento, menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EXCLUSAO_EVENTO_COM_REGISTROS"));

        assertThat(count("select count(*) from evento where id = ?", evento))
                .isEqualTo(1);
    }


    @Test
    void excluirEventoComDoacaoDeveRetornar409() throws Exception {
        Long evento = insertEvento("Evento com doação", "EVENTO", "NAO_INICIADO");

        insertDoacao(
                evento,
                "DINHEIRO",
                null,
                null,
                null,
                bd("50.00"),
                "DINHEIRO",
                "Doador teste",
                null
        );

        excluir(evento, menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EXCLUSAO_EVENTO_COM_REGISTROS"));

        assertThat(count("select count(*) from evento where id = ?", evento))
                .isEqualTo(1);
    }

    @Test
    void excluirEventoComMovimentacaoEstoqueDeveRetornar409() throws Exception {
        Long evento = insertEvento("Evento com movimentação", "EVENTO", "NAO_INICIADO");
        Long produto = insertProduto("Produto movimentado");

        Long estoque = insertLote(
                produto,
                10,
                10,
                bd("5.00"),
                bd("50.00")
        );

        jdbc.update("""
        insert into movimentacao_estoque (
            estoque_id,
            tipo_movimentacao_id,
            evento_id,
            quantidade
        )
        values (
            ?, (select id from tipo_movimentacao where codigo = 'AJUSTE_ENTRADA'),
            ?, 1
        )
        """, estoque, evento);

        excluir(evento, menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EXCLUSAO_EVENTO_COM_REGISTROS"));

        assertThat(count("select count(*) from evento where id = ?", evento))
                .isEqualTo(1);
    }

    @Test
    void excluirEventoComLancamentoCaixaDeveRetornar409() throws Exception {
        Long evento = insertEvento("Evento com lançamento de caixa", "EVENTO", "NAO_INICIADO");

        jdbc.update("""
        insert into pagamento_caixa (evento_id, tipo, valor, forma_pagamento)
        values (?, 'VENDA_FICHA', 10.00, 'DINHEIRO')
        """, evento);

        excluir(evento, menu())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EXCLUSAO_EVENTO_COM_REGISTROS"));

        assertThat(count("select count(*) from evento where id = ?", evento))
                .isEqualTo(1);
    }

    @Test
    void excluirEventoInexistenteDeveRetornar404() throws Exception {
        excluir(999_999L, menu())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void excluirExigeEventosMenu() throws Exception {
        Long id = insertEvento("Evento protegido", "EVENTO", "NAO_INICIADO");

        excluir(id, leitura())
                .andExpect(status().isForbidden());

        assertThat(count("select count(*) from evento where id = ?", id)).isEqualTo(1);
    }

    @Test
    void excluirEventoDeveRemoverProdutosECustosExplicitamente() throws Exception {
        Long evento = insertEvento(
                "Evento com produtos e custos", "EVENTO", "NAO_INICIADO");

        Long produtoEvento = insertPeProduzido(
                evento, "Tempurá", bd("10.00"), bd("3.00"));

        jdbc.update("""
        insert into custo_evento (evento_id, nome, valor)
        values (?, 'Limpeza', 20.00)
        """, evento);

        assertThat(count(
                "select count(*) from produto_evento where evento_id = ?", evento))
                .isEqualTo(1);

        assertThat(count(
                "select count(*) from custo_evento where evento_id = ?", evento))
                .isEqualTo(1);

        excluir(evento, menu())
                .andExpect(status().isNoContent());

        assertThat(count(
                "select count(*) from evento where id = ?", evento))
                .isZero();

        assertThat(count(
                "select count(*) from produto_evento where id = ?", produtoEvento))
                .isZero();

        assertThat(count(
                "select count(*) from custo_evento where evento_id = ?", evento))
                .isZero();
    }


}