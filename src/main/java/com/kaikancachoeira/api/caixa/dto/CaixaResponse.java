package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.common.dto.PaginaResponse;

@Schema(description = "Resposta da consulta do caixa, com resumo financeiro e lançamentos paginados.")
public record CaixaResponse(
        @Schema(description = "Resumo financeiro do caixa.")
        ResumoCaixaResponse resumo,
        @Schema(description = "Lançamentos de caixa da página solicitada.")
        PaginaResponse<LancamentoResponse> lancamentos
) {}