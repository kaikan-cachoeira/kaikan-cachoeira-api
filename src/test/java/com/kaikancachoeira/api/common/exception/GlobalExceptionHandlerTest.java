package com.kaikancachoeira.api.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    record CorpoFake(@NotBlank String nome, @Min(1) Integer idade) {}

    @Controller
    @RequestMapping
    static class ControllerFake {

        @PostMapping("/corpo")
        @ResponseBody
        String corpo(@Valid @RequestBody CorpoFake corpo) { return "ok"; }

        @GetMapping("/param-tipo")
        @ResponseBody
        String paramTipo(@RequestParam Integer id) { return "ok"; }

        @GetMapping("/param-obrigatorio")
        @ResponseBody
        String paramObrigatorio(@RequestParam String filtro) { return "ok"; }

        @GetMapping("/param-validado")
        @ResponseBody
        String paramValidado(@RequestParam @Min(1) int n) { return "ok"; }

        @GetMapping("/api-exception")
        @ResponseBody
        String apiException() { throw new ConflitoException("X_CONFLITO", "Já existe"); }

        @GetMapping("/inesperado")
        @ResponseBody
        String inesperado() { throw new IllegalStateException("segredo-interno-do-servidor"); }

        @GetMapping("/nao-autenticado")
        @ResponseBody
        String naoAutenticado() { throw new BadCredentialsException("x"); }

        @GetMapping("/negado")
        @ResponseBody
        String negado() { throw new AccessDeniedException("x"); }

        @GetMapping("/so-get")
        @ResponseBody
        String soGet() { return "ok"; }

        @PostMapping(value = "/so-json", consumes = "application/json")
        @ResponseBody
        String soJson() { return "ok"; }

        // usado só para obter um MethodParameter real
        void alvo(CorpoFake corpo) {}
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockMvc mockMvc;

    private DataIntegrityViolationException violacao(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("ERROR: violates constraint \"" + constraint + "\""));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerFake())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void nomeEPeriodoDuplicadosDevemVirar409() {
        ResponseEntity<ErroResponse> resposta = handler.handleIntegridade(violacao("uq_evento_nome_periodo"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().codigo()).isEqualTo("EVENTO_DUPLICADO");
    }

    @Test
    void segundoEventoEmAndamentoDeveVirar409() {
        ResponseEntity<ErroResponse> resposta = handler.handleIntegridade(violacao("uq_evento_em_andamento"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().codigo()).isEqualTo("EVENTO_EM_ANDAMENTO_EXISTENTE");
    }

    @Test
    void periodoInvalidoDeveVirar400ComOCampoPeriodoValido() {
        ResponseEntity<ErroResponse> resposta = handler.handleIntegridade(violacao("chk_evento_periodo"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().codigo()).isEqualTo("VALIDACAO");
        assertThat(resposta.getBody().campos())
                .extracting(CampoErro::campo)
                .containsExactly("periodoValido");
    }

    @Test
    void constraintDesconhecidaDeveVirar409GenericoSemVazarAMensagemDoBanco() {
        ResponseEntity<ErroResponse> resposta = handler.handleIntegridade(violacao("fk_segredo_interno"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().codigo()).isEqualTo("CONFLITO_DADOS");
        assertThat(resposta.getBody().mensagem()).doesNotContain("fk_segredo_interno");
    }

    @Test
    void excecaoSemCausaNaoDeveQuebrarOHandler() {
        ResponseEntity<ErroResponse> resposta =
                handler.handleIntegridade(new DataIntegrityViolationException("sem causa"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().codigo()).isEqualTo("CONFLITO_DADOS");
    }

    // ------------------------------------------------------- ApiException

    @Test
    void apiExceptionDeveUsarStatusCodigoEMensagemDaExcecao() throws Exception {
        mockMvc.perform(get("/api-exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("X_CONFLITO"))
                .andExpect(jsonPath("$.mensagem").value("Já existe"))
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    // ------------------------------------------------------ validação 400

    @Test
    void corpoInvalidoDeveListarUmCampoErroPorViolacao() throws Exception {
        mockMvc.perform(post("/corpo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": " ", "idade": 0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.campos.length()").value(2))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("idade")))
                .andExpect(jsonPath("$.campos[0].mensagem").isNotEmpty());
    }

    @Test
    void erroGlobalDeObjetoDeveEntrarEmCamposComONomeDoObjeto() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new CorpoFake("a", 1), "corpoFake");
        binding.addError(new ObjectError("corpoFake", "regra entre campos violada"));
        MethodParameter parametro = new MethodParameter(
                ControllerFake.class.getDeclaredMethod("alvo", CorpoFake.class), 0);

        ResponseEntity<ErroResponse> resposta = handler.handleValidacaoCorpo(
                new MethodArgumentNotValidException(parametro, binding));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().codigo()).isEqualTo("VALIDACAO");
        assertThat(resposta.getBody().campos())
                .containsExactly(new CampoErro("corpoFake", "regra entre campos violada"));
    }

    @Test
    void parametroComConstraintVioladaDeveRetornarValidacaoComNomeDoParametro() throws Exception {
        mockMvc.perform(get("/param-validado").param("n", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("n")));
    }

    // -------------------------------------------------- requisição inválida

    @Test
    void jsonMalformadoDeveRetornarRequisicaoInvalidaSemVazarDetalhes() throws Exception {
        mockMvc.perform(post("/corpo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido ou malformado"))
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    void tipoErradoEmParametroDeveIndicarOCampo() throws Exception {
        mockMvc.perform(get("/param-tipo").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro"))
                .andExpect(jsonPath("$.campos[0].campo").value("id"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("Valor inválido"));
    }

    @Test
    void parametroObrigatorioAusenteDeveIndicarOCampo() throws Exception {
        mockMvc.perform(get("/param-obrigatorio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Parâmetro obrigatório ausente"))
                .andExpect(jsonPath("$.campos[0].campo").value("filtro"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("Parâmetro obrigatório"));
    }

    // ---------------------------------------------------- segurança no advice

    @Test
    void authenticationExceptionDeveVirar401() throws Exception {
        mockMvc.perform(get("/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária"));
    }

    @Test
    void accessDeniedExceptionDeveVirar403() throws Exception {
        mockMvc.perform(get("/negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Você não tem permissão para realizar esta operação."));
    }

    // ------------------------------------------- exceções do framework (ErrorResponse)

    @Test
    void metodoNaoSuportadoDeveVirar405PreservandoOHeaderAllow() throws Exception {
        mockMvc.perform(post("/so-get"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.codigo").value("METODO_NAO_SUPORTADO"))
                .andExpect(jsonPath("$.mensagem").value("Método HTTP não permitido"));
    }

    @Test
    void mediaTypeNaoSuportadoDeveVirar415() throws Exception {
        mockMvc.perform(post("/so-json")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("MEDIA_TYPE_NAO_SUPORTADO"))
                .andExpect(jsonPath("$.mensagem").value("Tipo de conteúdo não suportado"));
    }

    @ParameterizedTest
    @CsvSource({
            "404, RECURSO_NAO_ENCONTRADO, Recurso não encontrado",
            "400, REQUISICAO_INVALIDA,    Requisição inválida",
            "422, REQUISICAO_INVALIDA,    Requisição inválida",
            "500, ERRO_INTERNO,           Erro interno no servidor",
            "503, ERRO_INTERNO,           Erro interno no servidor"
    })
    void errorResponseGenericoDeveSerTraduzidoPeloStatus(int status, String codigo, String mensagem) {
        ResponseEntity<ErroResponse> resposta = handler.handleGenerica(
                new ResponseStatusException(HttpStatus.valueOf(status), "detalhe interno"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(status);
        assertThat(resposta.getBody().codigo()).isEqualTo(codigo);
        assertThat(resposta.getBody().mensagem()).isEqualTo(mensagem).doesNotContain("detalhe interno");
        assertThat(resposta.getBody().campos()).isEmpty();
    }

    // ------------------------------------------------------ fallback genérico

    @Test
    void excecaoInesperadaDeveVirar500SemVazarAMensagemInterna() throws Exception {
        mockMvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Erro interno no servidor"))
                .andExpect(jsonPath("$.campos").isEmpty())
                .andExpect(content().string(not(containsString("segredo-interno-do-servidor"))));
    }
}