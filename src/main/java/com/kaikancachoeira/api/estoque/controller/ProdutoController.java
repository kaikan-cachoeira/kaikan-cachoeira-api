package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.estoque.dto.FotoResponse;
import com.kaikancachoeira.api.estoque.dto.ProdutoRequest;
import com.kaikancachoeira.api.estoque.dto.ProdutoResponse;
import com.kaikancachoeira.api.estoque.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/produtos")
@PreAuthorize("hasAuthority('ESTOQUE_MENU')")
public class ProdutoController implements ProdutoApi {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listar(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) List<Integer> categoriaId,
                                                        @RequestParam(required = false) Boolean abaixoDoMinimo) {
        return ResponseEntity.ok(service.listar(q, categoriaId, abaixoDoMinimo));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@RequestBody @Valid ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Override
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoResponse> enviarFoto(@PathVariable Long id,
                                                   @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.atualizarFoto(id, arquivo));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(@PathVariable Long id, @RequestBody @Valid ProdutoRequest request) {
        service.atualizar(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return service.excluir(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}