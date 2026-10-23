package com.kaikancachoeira.api.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaResponse<T>(List<T> itens, int page, int size, long totalItens, int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}