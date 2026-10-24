package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.venda.dto.DescontoProdutoResponse;
import com.kaikancachoeira.api.venda.dto.DescontoRequest;
import com.kaikancachoeira.api.venda.service.DescontoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos/{eventoId}/produtos/{produtoEventoId}/desconto")
@PreAuthorize("hasAuthority('VENDAS_MENU')")
public class DescontoController {

    private final DescontoService service;

    public DescontoController(DescontoService service) {
        this.service = service;
    }

    @PatchMapping
    public ResponseEntity<DescontoProdutoResponse> aplicar(@PathVariable Long eventoId,
                                                           @PathVariable Long produtoEventoId,
                                                           @RequestBody @Valid DescontoRequest request) {
        return ResponseEntity.ok(service.aplicar(eventoId, produtoEventoId, request));
    }

    @DeleteMapping
    public ResponseEntity<DescontoProdutoResponse> remover(@PathVariable Long eventoId,
                                                           @PathVariable Long produtoEventoId) {
        return ResponseEntity.ok(service.remover(eventoId, produtoEventoId));
    }
}
