package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.evento.dto.FotoEventoResponse;
import com.kaikancachoeira.api.evento.dto.ProdutoEventoRequest;
import com.kaikancachoeira.api.evento.dto.ProdutoEventoResponse;
import com.kaikancachoeira.api.evento.service.ProdutoEventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/eventos/{eventoId}/produtos")
@PreAuthorize("hasAuthority('EVENTOS_MENU')")
public class ProdutoEventoController implements ProdutoEventoApi {

    private final ProdutoEventoService service;

    public ProdutoEventoController(ProdutoEventoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_MENU', 'CARDAPIO_EVENTO_LEITURA')")
    public ResponseEntity<List<ProdutoEventoResponse>> listar(
            @PathVariable Long eventoId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> categoria
    ) {
        List<ProdutoEventoResponse> response = service.listar(eventoId, q, categoria);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAnyAuthority('EVENTOS_MENU', 'CARDAPIO_EVENTO_LEITURA')")
    public ResponseEntity<List<String>> listarCategorias(@PathVariable Long eventoId) {
        List<String> response = service.listarCategorias(eventoId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping
    public ResponseEntity<ProdutoEventoResponse> adicionar(
            @PathVariable Long eventoId,
            @RequestBody @Valid ProdutoEventoRequest request
    ) {
        ProdutoEventoResponse response = service.adicionar(eventoId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping(value = "/{produtoEventoId}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoEventoResponse> atualizarFoto(
            @PathVariable Long eventoId,
            @PathVariable Long produtoEventoId,
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(service.atualizarFoto(eventoId, produtoEventoId, arquivo));
    }

    @PutMapping("/{produtoEventoId}")
    public ResponseEntity<ProdutoEventoResponse> atualizar(
            @PathVariable Long eventoId,
            @PathVariable Long produtoEventoId,
            @RequestBody @Valid ProdutoEventoRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(eventoId, produtoEventoId, request));
    }

    @DeleteMapping("/{produtoEventoId}")
    public ResponseEntity<Void> remover(
            @PathVariable Long eventoId,
            @PathVariable Long produtoEventoId
    ) {
        service.remover(eventoId, produtoEventoId);
        return ResponseEntity.noContent().build();
    }
}