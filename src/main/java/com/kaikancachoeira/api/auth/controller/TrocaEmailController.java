package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.TrocaEmailRequest;
import com.kaikancachoeira.api.auth.service.TrocaEmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class TrocaEmailController implements TrocaEmailApi {

    private final TrocaEmailService service;

    public TrocaEmailController(TrocaEmailService service) {
        this.service = service;
    }

    @PostMapping("/troca-email")
    public ResponseEntity<Void> trocarEmail(Authentication authentication, @RequestBody TrocaEmailRequest request) {
        // o subject do JWT é o id do usuário
        service.trocarEmail(Integer.valueOf(authentication.getName()), request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}