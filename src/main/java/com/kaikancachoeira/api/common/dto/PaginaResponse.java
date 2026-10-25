package com.kaikancachoeira.api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Página de resultados. page começa em 0")
public record PaginaResponse<T>(
        @Schema(description = "Registros da página") List<T> itens,
        @Schema(description = "Índice da página, começando em zero", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "20") int size,
        @Schema(description = "Total de registros", example = "57") long totalItens,
        @Schema(description = "Total de páginas", example = "3") int totalPaginas
) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}