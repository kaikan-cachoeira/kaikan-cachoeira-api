package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Resumo financeiro consolidado dos eventos e reuniões iniciados.")
public record RelatorioGeralResponse(
        long quantidadeEventos,
        long quantidadeReunioes,
        long quantidadeVendas,
        BigDecimal receitaVendas,
        BigDecimal doacoesEmDinheiro,
        BigDecimal receitaTotal,
        @Schema(description = "Componente dos custos; não compõe a receita financeira.")
        BigDecimal custoFichasAgradecimento,
        BigDecimal custosTotais,
        BigDecimal lucroTotal,
        List<Item> itens
) {

    @Schema(description = "Valores consolidados de um evento ou reunião.")
    public record Item(
            Long eventoId,
            String nome,
            TipoEvento tipoEvento,
            StatusResumo status,
            Instant dataHoraInicio,
            boolean parcial,
            long quantidadeVendas,
            BigDecimal receitaVendas,
            BigDecimal doacoesEmDinheiro,
            BigDecimal receitaTotal,
            @Schema(description = "Componente dos custos; não compõe a receita financeira.")
            BigDecimal custoFichasAgradecimento,
            BigDecimal custoTotal,
            BigDecimal lucro
    ) {}
}
