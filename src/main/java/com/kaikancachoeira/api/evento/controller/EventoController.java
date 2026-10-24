package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.evento.dto.EventoDetalheResponse;
import com.kaikancachoeira.api.evento.dto.EventoRequest;
import com.kaikancachoeira.api.evento.dto.EventoResponse;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EVENTOS_LEITURA')")
    public ResponseEntity<List<EventoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) TipoEvento tipoEvento,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        List<EventoResponse> response = service.listar(q, statusId, tipoEvento, dataInicio, dataFim);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENTOS_MENU')")
    public ResponseEntity<EventoDetalheResponse> buscar(@PathVariable Long id) {
        EventoDetalheResponse response = service.buscar(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EVENTOS_MENU')")
    public ResponseEntity<EventoResponse> criar(@RequestBody @Valid EventoRequest request) {
        EventoResponse response = service.criar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENTOS_MENU')")
    public ResponseEntity<EventoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid EventoRequest request
    ) {
        EventoResponse response = service.atualizar(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
