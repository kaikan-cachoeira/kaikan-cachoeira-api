package com.kaikancachoeira.api.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.core.AuthenticationException;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String VALIDACAO = "VALIDACAO";
    public static final String REQUISICAO_INVALIDA = "REQUISICAO_INVALIDA";
    public static final String ERRO_INTERNO = "ERRO_INTERNO";
    public static final String NAO_AUTENTICADO = "NAO_AUTENTICADO";
    public static final String ACESSO_NEGADO = "ACESSO_NEGADO";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErroResponse.de(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroResponse> handleValidacaoNegocio(ValidacaoException ex) {
        return resposta(HttpStatus.BAD_REQUEST, VALIDACAO, ex.getMessage(), ex.getCampos());
    }

    // @Valid em corpo de requisição
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacaoCorpo(MethodArgumentNotValidException ex) {
        List<CampoErro> campos = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(e ->
                campos.add(new CampoErro(e.getField(), e.getDefaultMessage())));

        ex.getBindingResult().getGlobalErrors().forEach(e ->
                campos.add(new CampoErro(e.getObjectName(), e.getDefaultMessage())));

        return resposta(HttpStatus.BAD_REQUEST, VALIDACAO, "Dados inválidos", campos);
    }

    // Validação de @RequestParam / @PathVariable (ex.: @Min, @Positive)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> handleValidacaoParametros(HandlerMethodValidationException ex) {
        List<CampoErro> campos = new ArrayList<>();

        ex.getParameterValidationResults().forEach(resultado -> {
            String nome = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors().forEach(e ->
                    campos.add(new CampoErro(nome, e.getDefaultMessage())));
        });

        return resposta(HttpStatus.BAD_REQUEST, VALIDACAO, "Dados inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, REQUISICAO_INVALIDA,
                "Corpo da requisição inválido ou malformado", List.of());
    }

    // Tipo errado em parâmetro (ex.: id="abc", enum inexistente)
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(TypeMismatchException ex) {
        String campo = ex instanceof MethodArgumentTypeMismatchException m
                ? m.getName()
                : ex.getPropertyName();

        return resposta(HttpStatus.BAD_REQUEST, REQUISICAO_INVALIDA,
                "Valor inválido para o parâmetro",
                campo == null ? List.of() : List.of(new CampoErro(campo, "Valor inválido")));
    }

    // Query param ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> handleParametroAusente(MissingServletRequestParameterException ex) {
        return resposta(HttpStatus.BAD_REQUEST, REQUISICAO_INVALIDA,
                "Parâmetro obrigatório ausente",
                List.of(new CampoErro(ex.getParameterName(), "Parâmetro obrigatório")));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAutenticacao(AuthenticationException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO,
                "Autenticação necessária", List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, ACESSO_NEGADO,
                "Você não tem permissão para realizar esta operação.", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleIntegridade(DataIntegrityViolationException ex) {
        String detalhe = mensagemRaiz(ex);

        if (detalhe.contains("uq_evento_nome_periodo")) {
            return resposta(HttpStatus.CONFLICT, "EVENTO_DUPLICADO",
                    "Já existe um evento com este nome e período.", List.of());
        }

        if (detalhe.contains("uq_evento_em_andamento")) {
            return resposta(HttpStatus.CONFLICT, "EVENTO_EM_ANDAMENTO_EXISTENTE",
                    "Já existe um evento em andamento.", List.of());
        }

        if (detalhe.contains("chk_evento_periodo")) {
            return resposta(HttpStatus.BAD_REQUEST, VALIDACAO, "Dados inválidos",
                    List.of(new CampoErro("periodoValido",
                            "A data de encerramento deve ser posterior à data de início")));
        }

        log.warn("Violação de integridade não mapeada: {}", detalhe);
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DADOS",
                "A operação viola uma regra de integridade dos dados.", List.of());
    }

    private String mensagemRaiz(Throwable ex) {
        String mensagem = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        return mensagem == null ? "" : mensagem;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerica(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());

            if (status.is5xxServerError()) {
                log.error("Erro do framework: ", ex);
            }

            return ResponseEntity
                    .status(status)
                    .headers(er.getHeaders())
                    .body(ErroResponse.de(codigoPara(status), mensagemPara(status)));
        }

        log.error("Erro inesperado: ", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, ERRO_INTERNO,
                "Erro interno no servidor", List.of());
    }

    private String codigoPara(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> RecursoNaoEncontradoException.CODIGO_PADRAO;
            case METHOD_NOT_ALLOWED -> "METODO_NAO_SUPORTADO";
            case UNSUPPORTED_MEDIA_TYPE -> "MEDIA_TYPE_NAO_SUPORTADO";
            default -> status.is5xxServerError() ? ERRO_INTERNO : REQUISICAO_INVALIDA;
        };
    }

    private String mensagemPara(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado";
            case METHOD_NOT_ALLOWED -> "Método HTTP não permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado";
            default -> status.is5xxServerError() ? "Erro interno no servidor" : "Requisição inválida";
        };
    }

    private ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String codigo, String mensagem, List<CampoErro> campos) {
        return ResponseEntity.status(status).body(new ErroResponse(codigo, mensagem, campos));
    }
}
