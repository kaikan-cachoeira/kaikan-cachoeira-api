package com.kaikancachoeira.api.doacao.dto;

import com.kaikancachoeira.api.doacao.entity.Doacao;
import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;
import com.kaikancachoeira.api.doacao.enums.TipoDoacao;

import java.math.BigDecimal;
import java.time.Instant;

public record DoacaoResponse(
        Long id,
        Long eventoId,
        TipoDoacao tipo,
        BigDecimal valor,
        FormaPagamentoDoacao formaPagamento,
        Long produtoId,
        String nome,
        Integer quantidade,
        String doador,
        BigDecimal valorFicha,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static DoacaoResponse de(Doacao d) {
        return new DoacaoResponse(
                d.getId(),
                d.getEventoId(),
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
