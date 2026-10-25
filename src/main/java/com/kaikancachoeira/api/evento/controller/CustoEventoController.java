package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.evento.dto.CustoEventoRequest;
import com.kaikancachoeira.api.evento.dto.CustoEventoResponse;
import com.kaikancachoeira.api.evento.service.CustoEventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<CustoEventoResponse>> listar(
            @PathVariable Long eventoId
    ) {
        List<CustoEventoResponse> response = service.listar(eventoId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<CustoEventoResponse> adicionar(
            @PathVariable Long eventoId,
            @RequestBody @Valid CustoEventoRequest request
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
            @RequestBody @Valid CustoEventoRequest request
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
