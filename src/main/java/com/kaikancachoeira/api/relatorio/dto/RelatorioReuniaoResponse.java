package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Relatório de uma REUNIAO: só custos, sem receita nem lucro")
public record RelatorioReuniaoResponse(
        Long eventoId,
        String nome,
        TipoEvento tipoEvento,
        StatusResumo status,
        boolean parcial,
        ResumoReuniao resumo,
        @Schema(description = "Compras, doações e consumo interno da reunião") List<MovimentacaoRelatorio> itens,
        List<CustoAdicionalRelatorio> custosAdicionais
) implements RelatorioResponse {}