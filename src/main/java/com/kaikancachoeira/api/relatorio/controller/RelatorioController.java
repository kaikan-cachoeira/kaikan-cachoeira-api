package com.kaikancachoeira.api.relatorio.controller;

import com.kaikancachoeira.api.relatorio.dto.RelatorioResponse;
import com.kaikancachoeira.api.relatorio.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eventos/{id}/relatorio")
@PreAuthorize("hasAuthority('RELATORIOS_MENU')")
public class RelatorioController implements RelatorioApi {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public ResponseEntity<RelatorioResponse> gerar(@PathVariable Long id) {
        return ResponseEntity.ok(service.gerar(id));
    }
}
