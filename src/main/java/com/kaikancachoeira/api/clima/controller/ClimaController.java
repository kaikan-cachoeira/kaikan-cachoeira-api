package com.kaikancachoeira.api.clima.controller;

import com.kaikancachoeira.api.clima.dto.ClimaResponse;
import com.kaikancachoeira.api.clima.service.ClimaSincronizacaoService;
import com.kaikancachoeira.api.clima.service.ClimaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
    @RequestMapping("/eventos/{id}/climas")
    @PreAuthorize("hasAuthority('RELATORIOS_MENU')")
public class ClimaController implements ClimaApi {

    private final ClimaService service;
    private final ClimaSincronizacaoService sincronizacaoService;

    public ClimaController(ClimaService service, ClimaSincronizacaoService sincronizacaoService) {
        this.service = service;
        this.sincronizacaoService = sincronizacaoService;
    }

    @Override
    @GetMapping
    public ResponseEntity<ClimaResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }

    @Override
    @PostMapping("/sincronizar")
    public ResponseEntity<Void> sincronizar(@PathVariable Long id) {
        sincronizacaoService.sincronizarEvento(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
