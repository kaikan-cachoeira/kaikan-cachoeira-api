package com.kaikancachoeira.api.associado.controller;

import com.kaikancachoeira.api.associado.dto.ConfiguracaoAnuidadeRequest;
import com.kaikancachoeira.api.associado.dto.ConfiguracaoAnuidadeResponse;
import com.kaikancachoeira.api.associado.service.ConfiguracaoAnuidadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/config-anuidades")
public class ConfiguracaoAnuidadeController implements ConfiguracaoAnuidadeApi {

    private final ConfiguracaoAnuidadeService service;

    public ConfiguracaoAnuidadeController(ConfiguracaoAnuidadeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANUIDADES_LEITURA')")
    @Override
    public ResponseEntity<List<ConfiguracaoAnuidadeResponse>> listar() {
        return ResponseEntity.status(HttpStatus.OK).body(service.listar());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<ConfiguracaoAnuidadeResponse> criar(@RequestBody @Valid ConfiguracaoAnuidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<ConfiguracaoAnuidadeResponse> atualizar(@PathVariable Integer id,
                                                                  @RequestBody @Valid ConfiguracaoAnuidadeRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        service.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
