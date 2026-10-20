package com.kaikancachoeira.api.support;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas só para exercitar a segurança enquanto os módulos reais não existem. */
@Profile("test")
@RestController
@RequestMapping("/test-seguranca")
public class SegurancaTestController {

    @GetMapping("/autenticado")
    public String autenticado() {
        return "ok";
    }

    @PatchMapping("/autenticado")
    public ResponseEntity<Void> patchAutenticado() {
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('VENDAS_MENU')")
    @GetMapping("/vendas")
    public String vendas() {
        return "ok";
    }

    @PreAuthorize("hasAuthority('ESTOQUE_MENU')")
    @GetMapping("/estoque")
    public String estoque() {
        return "ok";
    }
}