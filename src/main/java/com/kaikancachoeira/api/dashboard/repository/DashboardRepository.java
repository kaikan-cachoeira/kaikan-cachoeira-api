package com.kaikancachoeira.api.dashboard.repository;

import com.kaikancachoeira.api.dashboard.dto.DashboardEstoqueResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardEventoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardProdutoMaisVendidoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardResumoResponse;

import java.time.Instant;
import java.util.List;

public interface DashboardRepository {

    DashboardResumoResponse consultarResumo(Instant dataInicio, Instant dataFim);

    List<DashboardEventoResponse> listarEventos(Instant dataInicio, Instant dataFim);

    List<DashboardProdutoMaisVendidoResponse> listarProdutosMaisVendidos(
            Instant dataInicio,
            Instant dataFim,
            int limite
    );

    DashboardEstoqueResponse consultarEstoque();
}
