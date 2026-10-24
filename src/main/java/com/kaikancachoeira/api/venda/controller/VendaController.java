package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.venda.dto.VendaRequest;
import com.kaikancachoeira.api.venda.dto.VendaResponse;
import com.kaikancachoeira.api.venda.service.VendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos/{eventoId}/vendas")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VENDAS_MENU')")
    public ResponseEntity<VendaResponse> registrar(@PathVariable Long eventoId,
                                                   @RequestBody @Valid VendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(eventoId, request));
    }

    @PostMapping("/{vendaId}/cancelamento")
    @PreAuthorize("hasAnyAuthority('VENDAS_MENU', 'EVENTOS_MENU')")
    public ResponseEntity<Void> cancelar(@PathVariable Long eventoId, @PathVariable Long vendaId) {
        service.cancelar(eventoId, vendaId);
        return ResponseEntity.noContent().build();
    }
}
