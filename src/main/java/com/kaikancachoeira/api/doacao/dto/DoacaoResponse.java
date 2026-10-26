package com.kaikancachoeira.api.doacao.dto;

import com.kaikancachoeira.api.doacao.entity.Doacao;
import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;
import com.kaikancachoeira.api.doacao.enums.TipoDoacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record DoacaoResponse(
        Long id,
        Long eventoId,
        String eventoNome,
        TipoDoacao tipo,
        @Schema(description = "Preenchido só em DINHEIRO", nullable = true) BigDecimal valor,
        @Schema(description = "Preenchido só em DINHEIRO", nullable = true) FormaPagamentoDoacao formaPagamento,
        @Schema(description = "Preenchido só em REABASTECIMENTO", nullable = true) Long produtoId,
        @Schema(description = "Descrição do item (PRODUTO) ou nome do produto (REABASTECIMENTO)", nullable = true) String nome,
        @Schema(description = "Preenchido só em REABASTECIMENTO", nullable = true) Integer quantidade,
        String doador,
        @Schema(nullable = true) BigDecimal valorFicha,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static DoacaoResponse de(Doacao d, String eventoNome) {
        return new DoacaoResponse(
                d.getId(),
                d.getEventoId(),
                eventoNome,
                d.getTipo(),
                d.getValor(),
                d.getFormaPagamento(),
                d.getProdutoId(),
                d.getNome(),
                d.getQuantidade(),
                d.getDoador(),
                d.getValorFicha(),
                d.getCriadoEm(),
                d.getAtualizadoEm());
    }
}
