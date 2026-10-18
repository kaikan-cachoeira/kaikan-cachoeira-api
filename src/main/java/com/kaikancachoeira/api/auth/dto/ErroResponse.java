package com.kaikancachoeira.api.auth.dto;

import java.util.List;

public record ErroResponse(String codigo, String mensagem, List<CampoErro> campos) {

    public record CampoErro(String campo, String mensagem) {
    }
}