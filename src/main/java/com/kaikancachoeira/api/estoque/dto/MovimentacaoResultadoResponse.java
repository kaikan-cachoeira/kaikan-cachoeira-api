package com.kaikancachoeira.api.estoque.dto;

import java.util.List;

public record MovimentacaoResultadoResponse(List<MovimentacaoResponse> movimentacoes, Integer quantidadeAtual) {}
