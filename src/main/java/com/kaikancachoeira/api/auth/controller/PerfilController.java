package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.AtualizarNomeRequest;
import com.kaikancachoeira.api.auth.dto.PerfilResponse;
import com.kaikancachoeira.api.auth.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/me")
public class PerfilController implements PerfilApi {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public ResponseEntity<PerfilResponse> consultarPerfil(Authentication authentication) {
        return ResponseEntity.ok(service.consultarPerfil(Integer.valueOf(authentication.getName())));
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> atualizarNome(Authentication authentication,
                                              @Valid @RequestBody AtualizarNomeRequest request) {
        service.atualizarNome(Integer.valueOf(authentication.getName()), request.nome());
        return ResponseEntity.accepted().build();
    }
}
