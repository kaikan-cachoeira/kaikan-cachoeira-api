package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventoStatusControllerIntegrationTest extends DbIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;

    // ------------------------------------------------------------- helpers

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    private String menu() {
        return auth("EVENTOS_MENU");
    }

    private Integer statusId(String codigo) {
        return jdbc.queryForObject("select id from status_evento where codigo = ?", Integer.class, codigo);
    }

    private ResultActions patchBruto(Long id, String corpo, String auth) throws Exception {
        return mockMvc.perform(patch("/eventos/{id}", id)
                .header(AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    /** Muda o status sem enviar o campo custos. */
    private ResultActions mudar(Long id, String destino) throws Exception {
        return patchBruto(id, "{\"status\": %d}".formatted(statusId(destino)), menu());
    }

    /** Muda o status enviando a lista de custos já serializada (ex.: "[" + custo(...) + "]"). */
    private ResultActions mudarComCustos(Long id, String destino, String custosJson) throws Exception {
        return patchBruto(id, "{\"status\": %d, \"custos\": %s}".formatted(statusId(destino), custosJson), menu());
    }

    private static String custo(String nome, String valor) {
        return "{\"nome\": \"%s\", \"descricao\": \"desc\", \"valor\": %s}".formatted(nome, valor);
    }

    private String statusDe(Long id) {
        return jdbc.queryForObject("""
                select s.codigo from evento e join status_evento s on s.id = e.status_id
                where e.id = ?""", String.class, id);
    }

    private boolean temIniciadoEm(Long id) {
        return count("select count(*) from evento where id = ? and iniciado_em is not null", id) == 1;
    }

    private boolean temEncerradoEm(Long id) {
        return count("select count(*) from evento where id = ? and encerrado_em is not null", id) == 1;
    }

    private int qtdCustos(Long id) {
        return count("select count(*) from custo_evento where evento_id = ?", id);
    }

    private BigDecimal somaCustos(Long id) {
        return jdbc.queryForObject("select coalesce(sum(valor), 0) from custo_evento where evento_id = ?",
                BigDecimal.class, id);
    }

    private Long evento(String nome, String tipo, String status) {
        return insertEvento(nome, tipo, status);
    }

    // ===================================================== segurança e entrada

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        mockMvc.perform(patch("/eventos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": 2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exigeEventosMenuELeituraNaoBasta() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        patchBruto(id, "{\"status\": %d}".formatted(statusId("EM_ANDAMENTO")), auth("EVENTOS_LEITURA"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        assertThat(statusDe(id)).isEqualTo("NAO_INICIADO");
    }

    @Test
    void eventoInexistenteDeveRetornar404() throws Exception {
        mudar(999_999L, "EM_ANDAMENTO")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void statusInexistenteDeveRetornar400() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        patchBruto(id, "{\"status\": 999}", menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("STATUS_INVALIDO"));

        assertThat(statusDe(id)).isEqualTo("NAO_INICIADO");
    }

    @Test
    void corpoSemStatusDeveRetornar400ComOCampo() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        patchBruto(id, "{}", menu())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("status")));
    }

    // ================================================== transições válidas

    @Test
    void iniciarDeveMudarOStatusEPreencherIniciadoEm() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        mudar(id, "EM_ANDAMENTO").andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
        assertThat(temIniciadoEm(id)).isTrue();
        assertThat(temEncerradoEm(id)).isFalse();
    }

    @Test
    void encerrarSemCustosDeveMudarOStatusEPreencherEncerradoEm() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudar(id, "ENCERRADO").andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("ENCERRADO");
        assertThat(temEncerradoEm(id)).isTrue();
        assertThat(qtdCustos(id)).isZero();
    }

    @Test
    void encerrarComCustosDeveGravarTodosNaMesmaTransacao() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO",
                "[" + custo("  Limpeza ", "20.00") + "," + custo("Ajudantes", "12.50") + "]")
                .andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("ENCERRADO");
        assertThat(qtdCustos(id)).isEqualTo(2);
        assertThat(somaCustos(id)).isEqualByComparingTo("32.50");
        assertThat(count("select count(*) from custo_evento where evento_id = ? and nome = 'Limpeza'", id))
                .as("nome é gravado sem espaços externos").isEqualTo(1);
    }

    @Test
    void encerrarReuniaoComCustosTambemEhPermitido() throws Exception {
        Long id = evento("Reuniao", "REUNIAO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO", "[" + custo("Lanche", "45.00") + "]")
                .andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("ENCERRADO");
        assertThat(somaCustos(id)).isEqualByComparingTo("45.00");
    }

    @Test
    void listaDeCustosVaziaNoEncerramentoEhValida() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO", "[]").andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("ENCERRADO");
        assertThat(qtdCustos(id)).isZero();
    }

    @Test
    void reabrirDeveLimparEncerradoEmEPreservarIniciadoEm() throws Exception {
        Long id = evento("Velho", "EVENTO", "ENCERRADO");
        OffsetDateTime iniciado = T0.plusMinutes(5);
        jdbc.update("update evento set iniciado_em = ?, encerrado_em = ? where id = ?",
                iniciado, T0.plusHours(3), id);

        mudar(id, "EM_ANDAMENTO").andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
        assertThat(temEncerradoEm(id)).isFalse();
        OffsetDateTime depois = jdbc.queryForObject(
                "select iniciado_em from evento where id = ?", OffsetDateTime.class, id);
        assertThat(depois.toInstant()).isEqualTo(iniciado.toInstant());
    }

    @Test
    void reabrirNaoApagaVendasCaixaNemCustosJaRegistrados() throws Exception {
        Long id = evento("Velho", "EVENTO", "ENCERRADO");
        insertVenda(id, bd("0.00"), bd("30.00"));
        insertPagamento(id, "VENDA_FICHA", bd("30.00"), "PIX", null);
        jdbc.update("insert into custo_evento (evento_id, nome, valor) values (?, 'Limpeza', 20.00)", id);

        mudar(id, "EM_ANDAMENTO").andExpect(status().isNoContent());

        assertThat(count("select count(*) from venda where evento_id = ?", id)).isEqualTo(1);
        assertThat(count("select count(*) from pagamento_caixa where evento_id = ?", id)).isEqualTo(1);
        assertThat(qtdCustos(id)).isEqualTo(1);
    }

    @Test
    void desfazerInicioSemMovimentoDeveVoltarParaNaoIniciadoELimparIniciadoEm() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");
        jdbc.update("update evento set iniciado_em = now() where id = ?", id);

        mudar(id, "NAO_INICIADO").andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("NAO_INICIADO");
        assertThat(temIniciadoEm(id)).isFalse();
    }

    @Test
    void fluxoCompletoIniciarEncerrarReabrirEEncerrarDeNovoAcumulandoCustos() throws Exception {
        Long id = evento("Fluxo", "EVENTO", "NAO_INICIADO");

        mudar(id, "EM_ANDAMENTO").andExpect(status().isNoContent());
        OffsetDateTime primeiroInicio = jdbc.queryForObject(
                "select iniciado_em from evento where id = ?", OffsetDateTime.class, id);

        mudarComCustos(id, "ENCERRADO", "[" + custo("Limpeza", "20.00") + "]")
                .andExpect(status().isNoContent());
        mudar(id, "EM_ANDAMENTO").andExpect(status().isNoContent());
        mudarComCustos(id, "ENCERRADO", "[" + custo("Gás extra", "10.00") + "]")
                .andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("ENCERRADO");
        assertThat(qtdCustos(id)).as("custos da primeira e da segunda vez").isEqualTo(2);
        assertThat(somaCustos(id)).isEqualByComparingTo("30.00");
        OffsetDateTime inicioFinal = jdbc.queryForObject(
                "select iniciado_em from evento where id = ?", OffsetDateTime.class, id);
        assertThat(inicioFinal.toInstant()).isEqualTo(primeiroInicio.toInstant());
    }

    // ================================================== transições inválidas

    @Test
    void naoIniciadoParaEncerradoDeveRetornar409() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        mudar(id, "ENCERRADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_EVENTO_INVALIDA"));

        assertThat(statusDe(id)).isEqualTo("NAO_INICIADO");
        assertThat(temEncerradoEm(id)).isFalse();
    }

    @Test
    void encerradoParaNaoIniciadoDeveRetornar409() throws Exception {
        Long id = evento("A", "EVENTO", "ENCERRADO");

        mudar(id, "NAO_INICIADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_EVENTO_INVALIDA"));

        assertThat(statusDe(id)).isEqualTo("ENCERRADO");
    }

    @Test
    void mudarParaOMesmoStatusDeveRetornar409() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudar(id, "EM_ANDAMENTO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_EVENTO_INVALIDA"));
    }

    // ============================================ um EVENTO em andamento por vez

    @Test
    void iniciarComOutroEventoEmAndamentoDeveRetornar409() throws Exception {
        evento("Atual", "EVENTO", "EM_ANDAMENTO");
        Long novo = evento("Novo", "EVENTO", "NAO_INICIADO");

        mudar(novo, "EM_ANDAMENTO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_EM_ANDAMENTO_EXISTENTE"));

        assertThat(statusDe(novo)).isEqualTo("NAO_INICIADO");
        assertThat(temIniciadoEm(novo)).isFalse();
    }

    @Test
    void reabrirComOutroEventoEmAndamentoDeveRetornar409ESemMexerNoEncerrado() throws Exception {
        Long velho = evento("Velho", "EVENTO", "ENCERRADO");
        jdbc.update("update evento set encerrado_em = now() where id = ?", velho);
        evento("Atual", "EVENTO", "EM_ANDAMENTO");

        mudar(velho, "EM_ANDAMENTO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_EM_ANDAMENTO_EXISTENTE"));

        assertThat(statusDe(velho)).isEqualTo("ENCERRADO");
        assertThat(temEncerradoEm(velho)).isTrue();
    }

    @Test
    void depoisDeEncerrarOPrimeiroOSegundoPodeIniciar() throws Exception {
        Long primeiro = evento("Primeiro", "EVENTO", "EM_ANDAMENTO");
        Long segundo = evento("Segundo", "EVENTO", "NAO_INICIADO");

        mudar(primeiro, "ENCERRADO").andExpect(status().isNoContent());
        mudar(segundo, "EM_ANDAMENTO").andExpect(status().isNoContent());

        assertThat(statusDe(segundo)).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void reuniaoPodeIniciarMesmoComEventoEmAndamento() throws Exception {
        evento("Evento", "EVENTO", "EM_ANDAMENTO");
        Long reuniao = evento("Reuniao", "REUNIAO", "NAO_INICIADO");

        mudar(reuniao, "EM_ANDAMENTO").andExpect(status().isNoContent());

        assertThat(statusDe(reuniao)).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void eventoPodeIniciarMesmoComReuniaoEmAndamento() throws Exception {
        evento("Reuniao", "REUNIAO", "EM_ANDAMENTO");
        Long evento = evento("Evento", "EVENTO", "NAO_INICIADO");

        mudar(evento, "EM_ANDAMENTO").andExpect(status().isNoContent());

        assertThat(statusDe(evento)).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void variasReunioesPodemEstarEmAndamentoAoMesmoTempo() throws Exception {
        evento("R1", "REUNIAO", "EM_ANDAMENTO");
        Long r2 = evento("R2", "REUNIAO", "NAO_INICIADO");

        mudar(r2, "EM_ANDAMENTO").andExpect(status().isNoContent());
    }

    // ================================================== desfazer início

    @Test
    void desfazerInicioComVendaDeveRetornar409() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");
        jdbc.update("update evento set iniciado_em = now() where id = ?", id);
        insertVenda(id, bd("0.00"), bd("10.00"));

        mudar(id, "NAO_INICIADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DESFAZER_INICIO_BLOQUEADO"));

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
        assertThat(temIniciadoEm(id)).isTrue();
    }

    @Test
    void desfazerInicioComVendaCanceladaAindaDeveBloquear() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");
        Long venda = insertVenda(id, bd("0.00"), bd("10.00"));
        jdbc.update("update venda set cancelada_em = now() where id = ?", venda);

        mudar(id, "NAO_INICIADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DESFAZER_INICIO_BLOQUEADO"));
    }

    @Test
    void desfazerInicioComLancamentoDeCaixaDeveRetornar409() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");
        insertPagamento(id, "VENDA_FICHA", bd("20.00"), "PIX", null);

        mudar(id, "NAO_INICIADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DESFAZER_INICIO_BLOQUEADO"));

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void vendaDeOutroEventoNaoImpedeDesfazerInicio() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");
        Long outro = evento("Outro", "EVENTO", "ENCERRADO");
        insertVenda(outro, bd("0.00"), bd("10.00"));

        mudar(id, "NAO_INICIADO").andExpect(status().isNoContent());
    }

    // ================================================================ custos

    @Test
    void custosForaDoEncerramentoDevemRetornar400SemMudarNada() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        mudarComCustos(id, "EM_ANDAMENTO", "[" + custo("Limpeza", "20.00") + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CUSTOS_NAO_PERMITIDOS"));

        assertThat(statusDe(id)).isEqualTo("NAO_INICIADO");
        assertThat(temIniciadoEm(id)).isFalse();
        assertThat(qtdCustos(id)).isZero();
    }

    @Test
    void listaDeCustosVaziaForaDoEncerramentoEhIgnorada() throws Exception {
        Long id = evento("A", "EVENTO", "NAO_INICIADO");

        mudarComCustos(id, "EM_ANDAMENTO", "[]").andExpect(status().isNoContent());

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void custoComValorZeroDeveRetornar400ENaoEncerrar() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO", "[" + custo("Limpeza", "0") + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("custos[0].valor")));

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
        assertThat(temEncerradoEm(id)).isFalse();
        assertThat(qtdCustos(id)).isZero();
    }

    @Test
    void custoComValorNegativoDeveRetornar400() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO", "[" + custo("Limpeza", "-5.00") + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("custos[0].valor")));
    }

    @Test
    void custoSemNomeDeveRetornar400() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO", "[" + custo("   ", "10.00") + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("custos[0].nome")));
    }

    @Test
    void custoComDescricaoAcimaDe255DeveRetornar400() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");
        String longo = "{\"nome\": \"X\", \"descricao\": \"%s\", \"valor\": 10}".formatted("d".repeat(256));

        mudarComCustos(id, "ENCERRADO", "[" + longo + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("custos[0].descricao")));
    }

    @Test
    void umCustoInvalidoNaListaImpedeQueOsValidosSejamGravadosEOEventoEncerre() throws Exception {
        Long id = evento("A", "EVENTO", "EM_ANDAMENTO");

        mudarComCustos(id, "ENCERRADO",
                "[" + custo("Valido", "10.00") + "," + custo("Invalido", "0") + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("custos[1].valor")));

        assertThat(statusDe(id)).isEqualTo("EM_ANDAMENTO");
        assertThat(qtdCustos(id)).as("nenhum custo é gravado").isZero();
    }
}