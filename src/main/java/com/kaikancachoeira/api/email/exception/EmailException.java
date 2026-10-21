package com.kaikancachoeira.api.email.exception;

public class EmailException extends RuntimeException {

    private final boolean transitorio;

    public EmailException(String mensagem, Throwable causa, boolean transitorio) {
        super(mensagem, causa);
        this.transitorio = transitorio;
    }

    /** true: falha passageira (timeout, indisponibilidade, limite de taxa); false: erro de configuração ou de requisição. */
    public boolean isTransitorio() {
        return transitorio;
    }
}
