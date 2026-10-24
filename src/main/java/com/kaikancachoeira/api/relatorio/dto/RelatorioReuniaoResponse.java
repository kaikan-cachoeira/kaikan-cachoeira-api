package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.enums.TipoEvento;

import java.util.List;

public record RelatorioReuniaoResponse(
        Long eventoId,
        String nome,
        TipoEvento tipoEvento,
        StatusResumo status,
        boolean parcial,
        ResumoReuniao resumo,
        List<MovimentacaoRelatorio> itens,
        List<CustoAdicionalRelatorio> custosAdicionais
) implements RelatorioResponse {
}
