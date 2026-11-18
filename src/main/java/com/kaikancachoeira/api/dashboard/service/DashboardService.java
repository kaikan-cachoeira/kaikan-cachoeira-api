package com.kaikancachoeira.api.dashboard.service;

import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.dashboard.dto.DashboardEstoqueResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardEventoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardProdutoMaisVendidoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardResumoResponse;
import com.kaikancachoeira.api.dashboard.repository.DashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class DashboardService {

    private static final int LIMITE_PADRAO = 10;

    private final DashboardRepository repository;
    private final Clock clock;

    public DashboardService(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponse consultarResumo(Instant dataInicio, Instant dataFim) {
        Periodo periodo = normalizarPeriodo(dataInicio, dataFim);
        return repository.consultarResumo(periodo.inicio(), periodo.fim());
    }

    @Transactional(readOnly = true)
    public List<DashboardEventoResponse> listarEventos(Instant dataInicio, Instant dataFim) {
        Periodo periodo = normalizarPeriodo(dataInicio, dataFim);
        return repository.listarEventos(periodo.inicio(), periodo.fim());
    }

    @Transactional(readOnly = true)
    public List<DashboardProdutoMaisVendidoResponse> listarProdutosMaisVendidos(
            Instant dataInicio,
            Instant dataFim,
            Integer limite
    ) {
        Periodo periodo = normalizarPeriodo(dataInicio, dataFim);
        int limiteNormalizado = limite == null ? LIMITE_PADRAO : limite;

        if (limiteNormalizado < 1) {
            throw ValidacaoException.campo("limite", "O limite deve ser maior que zero");
        }

        return repository.listarProdutosMaisVendidos(periodo.inicio(), periodo.fim(), limiteNormalizado);
    }

    @Transactional(readOnly = true)
    public DashboardEstoqueResponse consultarEstoque() {
        return repository.consultarEstoque();
    }

    private Periodo normalizarPeriodo(Instant dataInicio, Instant dataFim) {
        Instant agora = Instant.now(clock);
        Instant inicioPadrao = agora.atZone(ZoneOffset.UTC).minusMonths(6).toInstant();
        Instant fimPadrao = agora;

        Instant inicio = dataInicio != null ? dataInicio : inicioPadrao;
        Instant fim = dataFim != null ? dataFim : fimPadrao;

        if (inicio.isAfter(fim)) {
            throw ValidacaoException.campo("dataFim", "A data fim deve ser posterior ou igual à data início");
        }

        return new Periodo(inicio, fim);
    }

    private record Periodo(Instant inicio, Instant fim) {}
}
