package com.kaikancachoeira.api.associados.controller;

import com.kaikancachoeira.api.associados.dto.*;
import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
import com.kaikancachoeira.api.associados.service.AssociadoService;
import com.kaikancachoeira.api.associados.service.PagamentoService;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/associados")
public class AssociadoController implements AssociadoApi {

    private final AssociadoService associadoService;
    private final PagamentoService pagamentoService;

    public AssociadoController(AssociadoService associadoService, PagamentoService pagamentoService) {
        this.associadoService = associadoService;
        this.pagamentoService = pagamentoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSOCIADOS_LEITURA')")
    @Override
    public ResponseEntity<PaginaResponse<AssociadoListaResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) TipoAssociado tipo,
            @RequestParam Integer ano,
            @RequestParam(required = false) StatusAnuidade status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(associadoService.listar(q, dataInicio, dataFim, tipo, ano, status, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ASSOCIADOS_MENU')")
    @Override
    public ResponseEntity<AssociadoResponse> criar(@RequestBody @Valid AssociadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(associadoService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSOCIADOS_MENU')")
    @Override
    public ResponseEntity<AssociadoResponse> atualizar(@PathVariable Long id, @RequestBody @Valid AssociadoRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(associadoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSOCIADOS_MENU')")
    @Override
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        associadoService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/pagamentos")
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<PagamentoResponse> registrarPagamento(@PathVariable Long id, @RequestBody @Valid PagamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoService.registrar(id, request));
    }

    @DeleteMapping("/{idAssociado}/pagamentos/{idPagamento}")
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<Void> excluirPagamento(@PathVariable Long idAssociado, @PathVariable Long idPagamento) {
        pagamentoService.excluir(idAssociado, idPagamento);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
