package com.kaikancachoeira.api.associado.dto;

import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import com.kaikancachoeira.api.associado.enums.TipoAssociado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Linha da listagem de associados com status da anuidade do ano consultado.")
public record AssociadoListaResponse(
        @Schema(example = "12")
        Long id,
        @Schema(example = "Maria Silva")
        String nome,
        @Schema(example = "11999990000")
        String numeroContato,
        @Schema(example = "46")
        Integer idade,
        @Schema(example = "1980-02-10")
        LocalDate dataNascimento,
        @Schema(example = "TITULAR")
        TipoAssociado tipo,
        @Schema(description = "ID do titular quando o associado é dependente.", example = "1", nullable = true)
        Long titularId,
        @Schema(description = "Nome do titular quando o associado é dependente.", example = "Maria Silva", nullable = true)
        String titularNome,
        @Schema(example = "PENDENTE")
        StatusAnuidade status,
        @Schema(description = "Resumo da anuidade do ano consultado para titulares.", nullable = true)
        AnuidadeListaResponse anuidade
) {
    public static AssociadoListaResponse de(
            Long id,
            String nome,
            String numeroContato,
            Integer idade,
            LocalDate dataNascimento,
            TipoAssociado tipo,
            Long titularId,
            String titularNome,
            StatusAnuidade status,
            Long anuidadeId,
            BigDecimal anuidadeValor,
            BigDecimal totalPago,
            StatusAnuidade anuidadeStatus
    ) {
        return new AssociadoListaResponse(
                id,
                nome,
                numeroContato,
                idade,
                dataNascimento,
                tipo,
                titularId,
                titularNome,
                status,
                tipo == TipoAssociado.TITULAR && anuidadeId != null
                        ? new AnuidadeListaResponse(anuidadeId, anuidadeValor, totalPago, anuidadeStatus)
                        : null
        );
    }
}
