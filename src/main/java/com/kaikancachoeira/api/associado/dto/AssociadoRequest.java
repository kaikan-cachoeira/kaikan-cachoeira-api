package com.kaikancachoeira.api.associado.dto;

import com.kaikancachoeira.api.associado.enums.TipoAssociado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados para cadastro/atualização de associado.")
public record AssociadoRequest(
        @Schema(example = "Maria da Silva")
        @NotBlank @Size(max = 150)
        String nome,

        @Schema(example = "11999990000", nullable = true)
        @Size(max = 20)
        String numeroContato,

        @Schema(example = "1980-01-15", nullable = true)
        LocalDate dataNascimento,

        @Schema(example = "TITULAR")
        @NotNull
        TipoAssociado tipo,

        @Schema(example = "1", nullable = true)
        Long titularId
) {}
