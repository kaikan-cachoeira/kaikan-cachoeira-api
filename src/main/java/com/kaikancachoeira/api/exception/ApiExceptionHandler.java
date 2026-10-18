package com.kaikancachoeira.api.exception;

import com.kaikancachoeira.api.auth.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> tratarApiException(ApiException ex) {
        ErroResponse erro = new ErroResponse(ex.getCodigo(), ex.getMessage(), new ArrayList<>());
        return ResponseEntity.status(ex.getStatus()).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        List<ErroResponse.CampoErro> campos = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            campos.add(new ErroResponse.CampoErro(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        ErroResponse erro = new ErroResponse("VALIDACAO", "Dados inválidos", campos);
        return ResponseEntity.badRequest().body(erro);
    }
}