package com.kaikancachoeira.api.venda.controller;

import com.kaikancachoeira.api.venda.dto.VendaRequest;
import com.kaikancachoeira.api.venda.dto.VendaResponse;
import com.kaikancachoeira.api.venda.service.VendaService;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.venda.dto.VendaDetalheResponse;
import com.kaikancachoeira.api.venda.dto.VendaHistoricoResponse;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

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

    @GetMapping
    @PreAuthorize("hasAuthority('EVENTOS_MENU')")
    public ResponseEntity<PaginaResponse<VendaHistoricoResponse>> listarHistorico(
            @PathVariable Long eventoId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Instant dataHoraInicio,
            @RequestParam(required = false) Instant dataHoraFim,
            @RequestParam(required = false) Boolean cancelada,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validarPaginacao(page, size);

        PaginaResponse<VendaHistoricoResponse> response = service.listarHistorico(
                eventoId, q, dataHoraInicio, dataHoraFim, cancelada, page, size
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{vendaId}")
    @PreAuthorize("hasAuthority('EVENTOS_MENU')")
    public ResponseEntity<VendaDetalheResponse> detalhar(
            @PathVariable Long eventoId,
            @PathVariable Long vendaId
    ) {
        VendaDetalheResponse response = service.detalhar(eventoId, vendaId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    private void validarPaginacao(int page, int size) {
        if (page < 0) {
            throw ValidacaoException.campo(
                    "page",
                    "A página não pode ser negativa"
            );
        }

        if (size < 1 || size > 100) {
            throw ValidacaoException.campo(
                    "size",
                    "O tamanho da página deve estar entre 1 e 100"
            );
        }
    }
}
