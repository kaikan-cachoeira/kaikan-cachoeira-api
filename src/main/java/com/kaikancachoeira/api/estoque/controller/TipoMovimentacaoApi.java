
package com.kaikancachoeira.api.estoque.controller;

import com.kaikancachoeira.api.estoque.dto.TipoMovimentacaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(
        name = "Tipos de movimentação",
        description = "Consulta dos tipos de movimentação de estoque disponíveis."
)
public interface TipoMovimentacaoApi {

    @Operation(
            summary = "Lista tipos de movimentação",
            description = "Retorna os tipos de movimentação cadastrados para uso nas operações de estoque."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de tipos de movimentação retornada"
    )
    ResponseEntity<List<TipoMovimentacaoResponse>> listar();
}
