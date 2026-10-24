package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.common.dto.PaginaResponse;

public record CaixaResponse(ResumoCaixaResponse resumo, PaginaResponse<LancamentoResponse> lancamentos) {}
