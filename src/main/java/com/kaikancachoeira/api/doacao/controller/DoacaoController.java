package com.kaikancachoeira.api.doacao.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.doacao.dto.DoacaoRequest;
import com.kaikancachoeira.api.doacao.dto.DoacaoResponse;
import com.kaikancachoeira.api.doacao.service.DoacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doacoes")
@PreAuthorize("hasAuthority('DOACOES_MENU')")
public class DoacaoController implements DoacaoApi {

    private final DoacaoService service;

    public DoacaoController(DoacaoService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public ResponseEntity<PaginaResponse<DoacaoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> tipo,
            @RequestParam(required = false) List<Long> eventoId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.listar(q, tipo, eventoId, page, size));
    }

    @Override
    @PostMapping
    public ResponseEntity<DoacaoResponse> criar(@RequestBody @Valid DoacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(@PathVariable Long id, @RequestBody @Valid DoacaoRequest request) {
        service.atualizar(id, request);
        return ResponseEntity.noContent().build();
    }
}
