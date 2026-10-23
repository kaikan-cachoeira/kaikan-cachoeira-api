package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.estoque.dto.CategoriaRequest;
import com.kaikancachoeira.api.estoque.dto.CategoriaResponse;
import com.kaikancachoeira.api.estoque.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@PreAuthorize("hasAuthority('ESTOQUE_MENU')")
public class CategoriaController implements CategoriaApi {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@RequestBody @Valid CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(@PathVariable Integer id,
                                                       @RequestBody @Valid CategoriaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
