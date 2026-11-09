package com.kaikancachoeira.api.dashboard.controller;

import com.kaikancachoeira.api.dashboard.dto.DashboardEstoqueResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardEventoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardProdutoMaisVendidoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardResumoResponse;
import com.kaikancachoeira.api.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasAuthority('DASHBOARD_MENU')")
public class DashboardController implements DashboardApi {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/resumo")
    public ResponseEntity<DashboardResumoResponse> consultarResumo(
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return ResponseEntity.ok(service.consultarResumo(dataInicio, dataFim));
    }

    @Override
    @GetMapping("/eventos")
    public ResponseEntity<List<DashboardEventoResponse>> listarEventos(
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return ResponseEntity.ok(service.listarEventos(dataInicio, dataFim));
    }

    @Override
    @GetMapping("/produtos-mais-vendidos")
    public ResponseEntity<List<DashboardProdutoMaisVendidoResponse>> listarProdutosMaisVendidos(
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim,
            @RequestParam(required = false) Integer limite
    ) {
        return ResponseEntity.ok(service.listarProdutosMaisVendidos(dataInicio, dataFim, limite));
    }

    @Override
    @GetMapping("/estoque")
    public ResponseEntity<DashboardEstoqueResponse> consultarEstoque() {
        return ResponseEntity.ok(service.consultarEstoque());
    }
}
