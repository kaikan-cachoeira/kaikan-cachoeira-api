package com.kaikancachoeira.api.associado.controller;

import com.kaikancachoeira.api.associado.dto.AnuidadeRequest;
import com.kaikancachoeira.api.associado.dto.AnuidadeResponse;
import com.kaikancachoeira.api.associado.dto.AnuidadeValorRequest;
import com.kaikancachoeira.api.associado.service.AnuidadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/anuidades")
@Validated
public class AnuidadeController implements AnuidadeApi {

    private final AnuidadeService service;

    public AnuidadeController(AnuidadeService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<Void> criarParaTodos(@RequestBody @Valid AnuidadeRequest request) {
        service.criarParaTodosTitulares(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/associados/{idAssociado}")
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<Void> criarParaAssociado(@PathVariable Long idAssociado,
                                                   @RequestBody @Valid AnuidadeRequest request) {
        service.criarParaAssociado(idAssociado, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ANUIDADES_MENU')")
    @Override
    public ResponseEntity<AnuidadeResponse> atualizarValor(@PathVariable Long id,
                                                           @RequestBody @Valid AnuidadeValorRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(service.atualizarValor(id, request));
    }
}
