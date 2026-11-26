package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.TrocaSenhaRequest;
import com.kaikancachoeira.api.auth.service.TrocaSenhaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class TrocaSenhaController implements TrocaSenhaApi {

    private final TrocaSenhaService service;

    public TrocaSenhaController(TrocaSenhaService service) {
        this.service = service;
    }

    @PatchMapping("/troca-senha")
    public ResponseEntity<Void> trocarSenha(Authentication authentication, @RequestBody TrocaSenhaRequest request) {

        service.trocarSenha(Integer.valueOf(authentication.getName()), request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}