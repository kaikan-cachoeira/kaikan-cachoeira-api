package com.kaikancachoeira.api.evento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AlteracaoStatusEventoRequest(
        @NotNull
        Integer status,
        List<@Valid CustoEventoRequest> custos
) {}
