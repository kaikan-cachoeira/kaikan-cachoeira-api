package com.kaikancachoeira.api.relatorio.controller;

import com.kaikancachoeira.api.relatorio.dto.RelatorioGeralResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioResponse;
import com.kaikancachoeira.api.relatorio.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@PreAuthorize("hasAuthority('RELATORIOS_MENU')")
public class RelatorioController implements RelatorioApi {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/eventos/{id}/relatorio")
    public ResponseEntity<RelatorioResponse> gerar(@PathVariable Long id) {
        return ResponseEntity.ok(service.gerar(id));
    }

    @Override
    @GetMapping("/relatorios/geral")
    public ResponseEntity<RelatorioGeralResponse> gerarGeral(
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim,
            @RequestParam(required = false) String tipos) {
        return ResponseEntity.ok(service.gerarGeral(dataInicio, dataFim, tipos));
    }
}
