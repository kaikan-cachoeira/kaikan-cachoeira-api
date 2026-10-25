package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.caixa.dto.ResumoCaixaResponse;
import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Relatório de um evento do tipo EVENTO")
public record RelatorioEventoResponse(
        Long eventoId,
        String nome,
        TipoEvento tipoEvento,
        StatusResumo status,
        @Schema(description = "true com o evento EM_ANDAMENTO: valores parciais") boolean parcial,
        ResumoFinanceiro resumo,
        @Schema(description = "Todos os itens do cardápio, inclusive sem venda") List<ProdutoRelatorio> produtos,
        List<CustoAdicionalRelatorio> custosAdicionais,
        ResumoCaixaResponse caixa,
        DoacoesRelatorio doacoes,
        @Schema(description = "Consumo e perdas vinculados ao evento. Não existe registro separado de sobras")
        List<MovimentacaoRelatorio> movimentacoesEvento,
        RankingProdutos produtosMaisVendidos,
        VendasPorHora vendasPorHora
) implements RelatorioResponse {}
