package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.estoque.dto.FotoResponse;
import com.kaikancachoeira.api.estoque.dto.ProdutoRequest;
import com.kaikancachoeira.api.estoque.dto.ProdutoResponse;
import com.kaikancachoeira.api.estoque.service.ProdutoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController implements ProdutoApi {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ESTOQUE_MENU', 'PRODUTOS_LEITURA')")
    public ResponseEntity<PaginaResponse<ProdutoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Integer> categoriaId,
            @RequestParam(required = false) Boolean abaixoDoMinimo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(service.listar(q, categoriaId, abaixoDoMinimo, page, size));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ESTOQUE_MENU', 'PRODUTOS_LEITURA')")
    public ResponseEntity<ProdutoResponse> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ESTOQUE_MENU')")
    public ResponseEntity<ProdutoResponse> criar(@RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ESTOQUE_MENU')")
    public ResponseEntity<ProdutoResponse> criarComFoto(
            @RequestPart("produto") @Valid ProdutoRequest request,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request, arquivo));
    }

    @Override
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ESTOQUE_MENU')")
    public ResponseEntity<FotoResponse> enviarFoto(@PathVariable Long id,
                                                   @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.atualizarFoto(id, arquivo));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTOQUE_MENU')")
    public ResponseEntity<Void> atualizar(@PathVariable Long id, @RequestBody ProdutoRequest request) {
        service.atualizar(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTOQUE_MENU')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return service.excluir(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
