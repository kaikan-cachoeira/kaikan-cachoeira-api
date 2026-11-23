package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.RedefinicaoEmailRequest;
import com.kaikancachoeira.api.auth.service.RedefinicaoEmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class RedefinicaoEmailController implements RedefinicaoEmailApi {

    private final RedefinicaoEmailService service;

    public RedefinicaoEmailController(RedefinicaoEmailService service) {
        this.service = service;
    }

    @PostMapping("/redefinicao-email")
    public ResponseEntity<Void> redefinirEmail(@RequestBody RedefinicaoEmailRequest request) {
        service.redefinirEmail(request);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}