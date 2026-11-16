package com.kaikancachoeira.api.associados.controller;

import com.kaikancachoeira.api.associados.dto.AnuidadeRequest;
import com.kaikancachoeira.api.associados.dto.AnuidadeResponse;
import com.kaikancachoeira.api.associados.dto.AnuidadeValorRequest;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Anuidades", description = "Criação e atualização de anuidades dos associados.")
public interface AnuidadeApi {

    @Operation(summary = "Cria anuidades para todos os titulares", description = "Gera uma anuidade para cada associado titular com base na configuração informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anuidades criadas"),
            @ApiResponse(responseCode = "404", description = "Configuração não encontrada"),
            @ApiResponse(responseCode = "409", description = "Anuidade já existente")
    })
    ResponseEntity<Void> criarParaTodos(@RequestBody(description = "Dados da criação em massa", required = true) @Valid AnuidadeRequest request);

    @Operation(summary = "Cria anuidade para associado", description = "Cria a anuidade apenas para o associado informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anuidade criada"),
            @ApiResponse(responseCode = "404", description = "Associado ou configuração não encontrados"),
            @ApiResponse(responseCode = "409", description = "Anuidade já existente")
    })
    ResponseEntity<Void> criarParaAssociado(@Parameter(description = "ID do associado", required = true) Long idAssociado,
                                            @RequestBody(description = "Dados da criação", required = true) @Valid AnuidadeRequest request);

    @Operation(summary = "Atualiza valor da anuidade", description = "Altera somente o valor de uma anuidade específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anuidade atualizada"),
            @ApiResponse(responseCode = "404", description = "Anuidade não encontrada")
    })
    ResponseEntity<AnuidadeResponse> atualizarValor(@Parameter(description = "ID da anuidade", required = true) Long id,
                                                    @RequestBody(description = "Novo valor", required = true) @Valid AnuidadeValorRequest request);
}
