package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.evento.dto.CustoEventoRequest;
import com.kaikancachoeira.api.evento.dto.CustoEventoResponse;
import com.kaikancachoeira.api.evento.service.CustoEventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos/{eventoId}/custos")
@PreAuthorize("hasAuthority('EVENTOS_MENU')")
public class CustoEventoController implements CustoEventoApi {

    private final CustoEventoService service;

    public CustoEventoController(CustoEventoService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public ResponseEntity<PaginaResponse<CustoEventoResponse>> listar(
            @PathVariable Long eventoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PaginaResponse<CustoEventoResponse> response = service.listar(eventoId, page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<CustoEventoResponse> adicionar(
            @PathVariable Long eventoId,
            @RequestBody CustoEventoRequest request
    ) {
        CustoEventoResponse response = service.adicionar(eventoId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Override
    @PutMapping("/{custoId}")
    public ResponseEntity<CustoEventoResponse> atualizar(
            @PathVariable Long eventoId,
            @PathVariable Long custoId,
            @RequestBody CustoEventoRequest request
    ) {
        CustoEventoResponse response = service.atualizar(eventoId, custoId, request);

        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{custoId}")
    public ResponseEntity<Void> remover(
            @PathVariable Long eventoId,
            @PathVariable Long custoId
    ) {
        service.remover(eventoId, custoId);

        return ResponseEntity.noContent().build();
    }

}
