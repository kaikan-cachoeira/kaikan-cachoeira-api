package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Página de fiados de um evento com totais globais do filtro aplicado.")
public record FiadoListagemResponse(
        @Schema(description = "Fiados da página atual") List<FiadoResponse> itens,
        @Schema(description = "Índice da página, começando em zero", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "20") int size,
        @Schema(description = "Total de registros", example = "57") long totalItens,
        @Schema(description = "Total de páginas", example = "3") int totalPaginas,
        @Schema(description = "Quantidade de fiados em aberto no conjunto filtrado") long pendingCount,
        @Schema(description = "Valor em aberto no conjunto filtrado") BigDecimal openAmount
) {}
