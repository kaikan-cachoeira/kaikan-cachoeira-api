package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.caixa.dto.ResumoCaixaResponse;
import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.enums.TipoEvento;

import java.util.List;

public record RelatorioEventoResponse(
        Long eventoId,
        String nome,
        TipoEvento tipoEvento,
        StatusResumo status,
        boolean parcial,
        ResumoFinanceiro resumo,
        List<ProdutoRelatorio> produtos,
        List<CustoAdicionalRelatorio> custosAdicionais,
        ResumoCaixaResponse caixa,
        DoacoesRelatorio doacoes,
        List<MovimentacaoRelatorio> movimentacoesEvento,
        RankingProdutos produtosMaisVendidos,
        VendasPorHora vendasPorHora
) implements RelatorioResponse {
}
