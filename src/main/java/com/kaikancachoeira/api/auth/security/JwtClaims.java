package com.kaikancachoeira.api.auth.security;

public class JwtClaims {

    public static final String CARGO = "cargo";
    public static final String PERMISSOES = "permissoes";

    private JwtClaims() {
        throw new UnsupportedOperationException(
                "Classe utilitária não pode ser instanciada"
        );
    }

}
