package com.kaikancachoeira.api.caixa.controller;

import com.kaikancachoeira.api.caixa.dto.FiadoResponse;
import com.kaikancachoeira.api.caixa.dto.QuitacaoRequest;
import com.kaikancachoeira.api.caixa.dto.QuitacaoResponse;
import com.kaikancachoeira.api.caixa.service.FiadoService;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos/{eventoId}/fiados")
@PreAuthorize("hasAuthority('CAIXA_MENU')")
public class FiadoController implements FiadoApi {

    private final FiadoService service;

    public FiadoController(FiadoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<FiadoResponse>> listar(
            @PathVariable Long eventoId,
            @RequestParam(required = false) String situacao,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.listar(eventoId, situacao, page, size));
    }

    @PatchMapping("/{fiadoId}/quitacao")
    public ResponseEntity<QuitacaoResponse> quitar(@PathVariable Long eventoId,
                                                   @PathVariable Long fiadoId,
                                                   @RequestBody QuitacaoRequest request) {
        return ResponseEntity.ok(service.quitar(eventoId, fiadoId, request));
    }
}
