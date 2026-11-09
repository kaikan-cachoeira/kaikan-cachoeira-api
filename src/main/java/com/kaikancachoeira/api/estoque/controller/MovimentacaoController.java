package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.estoque.dto.EstoqueResponse;
import com.kaikancachoeira.api.estoque.dto.MovimentacaoResponse;
import com.kaikancachoeira.api.estoque.dto.MovimentacaoResultadoResponse;
import com.kaikancachoeira.api.estoque.dto.MovimentarEstoqueRequest;
import com.kaikancachoeira.api.estoque.service.MovimentacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@PreAuthorize("hasAuthority('ESTOQUE_MENU')")
public class MovimentacaoController implements MovimentacaoApi {

    private final MovimentacaoService service;

    public MovimentacaoController(MovimentacaoService service) {
        this.service = service;
    }

    @PostMapping("/produtos/{id}/movimentacoes")
    public ResponseEntity<MovimentacaoResultadoResponse> movimentar(@PathVariable Long id,
                                                                    @RequestBody MovimentarEstoqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.movimentar(id, request));
    }

    @Override
    @GetMapping("/produtos/{id}/movimentacoes")
    public ResponseEntity<PaginaResponse<MovimentacaoResponse>> historicoDoProduto(
            @PathVariable Long id,
            @RequestParam(required = false) Integer tipoMovimentacaoId,
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.historicoDoProduto(id, tipoMovimentacaoId, eventoId,
                dataInicio, dataFim, page, size));
    }

    @Override
    @GetMapping("/produtos/{id}/estoques")
    public ResponseEntity<List<EstoqueResponse>> listarLotes(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarLotes(id));
    }

    @Override
    @GetMapping("/estoques/{id}/movimentacoes")
    public ResponseEntity<PaginaResponse<MovimentacaoResponse>> historicoDoLote(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.historicoDoLote(id, page, size));
    }
}
