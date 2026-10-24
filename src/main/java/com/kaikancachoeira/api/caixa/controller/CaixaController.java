package com.kaikancachoeira.api.caixa.controller;

import com.kaikancachoeira.api.caixa.dto.CaixaRequest;
import com.kaikancachoeira.api.caixa.dto.CaixaResponse;
import com.kaikancachoeira.api.caixa.dto.ExclusaoCaixaResponse;
import com.kaikancachoeira.api.caixa.dto.RegistroCaixaResponse;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import com.kaikancachoeira.api.caixa.service.CaixaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos/{eventoId}/caixa")
@PreAuthorize("hasAuthority('CAIXA_MENU')")
public class CaixaController {

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RegistroCaixaResponse> registrar(@PathVariable Long eventoId,
                                                           @RequestBody @Valid CaixaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(eventoId, request));
    }

    @GetMapping
    public ResponseEntity<CaixaResponse> listar(@PathVariable Long eventoId,
                                                @RequestParam(required = false) TipoLancamento tipo,
                                                @RequestParam(required = false) FormaPagamento formaPagamento,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listar(eventoId, tipo, formaPagamento, page, size));
    }

    @DeleteMapping("/{lancamentoId}")
    public ResponseEntity<ExclusaoCaixaResponse> excluir(@PathVariable Long eventoId,
                                                         @PathVariable Long lancamentoId) {
        return ResponseEntity.ok(service.excluir(eventoId, lancamentoId));
    }
}
