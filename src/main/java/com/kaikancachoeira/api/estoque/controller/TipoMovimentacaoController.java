package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.estoque.dto.TipoMovimentacaoResponse;
import com.kaikancachoeira.api.estoque.service.MovimentacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tipos-movimentacao")
@PreAuthorize("hasAuthority('ESTOQUE_MENU')")
public class TipoMovimentacaoController {

    private final MovimentacaoService service;

    public TipoMovimentacaoController(MovimentacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TipoMovimentacaoResponse>> listar() {
        return ResponseEntity.ok(service.listarTipos());
    }
}