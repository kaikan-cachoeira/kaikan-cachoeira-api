package com.kaikancachoeira.api.clima.controller;

import com.kaikancachoeira.api.auth.security.TokenService;
import com.kaikancachoeira.api.clima.service.ClimaSincronizacaoService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClimaControllerIntegrationTest extends DbIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @MockitoBean private ClimaSincronizacaoService sincronizacaoService;

    private String auth(String... permissoes) {
        return "Bearer " + tokenService.gerarAccessToken("1", "TESTE", List.of(permissoes));
    }

    private Long insertClima(Long eventoId, OffsetDateTime dataHora, String temperatura, String sensacao, String chuva,
                             Integer probabilidade, String vento, Integer codigo, String descricao) {
        return jdbc.queryForObject(
                """
                insert into clima (
                    evento_id,
                    data_hora_prevista,
                    temperatura,
                    sensacao_termica,
                    chuva_mm,
                    probabilidade_chuva,
                    vento_velocidade,
                    codigo_clima,
                    descricao_clima
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
                Long.class,
                eventoId,
                dataHora,
                new BigDecimal(temperatura),
                new BigDecimal(sensacao),
                new BigDecimal(chuva),
                probabilidade,
                new BigDecimal(vento),
                codigo,
                descricao
        );
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        Long evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        mockMvc.perform(get("/eventos/{id}/climas", evento))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void semPermissaoDeveRetornar403() throws Exception {
        Long evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        mockMvc.perform(get("/eventos/{id}/climas", evento).header(AUTHORIZATION, auth("EVENTOS_MENU")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void eventoInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(get("/eventos/{id}/climas", 999999L).header(AUTHORIZATION, auth("RELATORIOS_MENU")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void deveRetornarClimaPersistidoEResumoCalculado() throws Exception {
        Long evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");
        insertClima(evento, OffsetDateTime.parse("2026-11-21T14:00:00Z"), "28.00", "29.00", "0.20", 30, "4.00", 63, "Chuva fraca");
        insertClima(evento, OffsetDateTime.parse("2026-11-21T15:00:00Z"), "30.00", "31.00", "0.00", 10, "6.00", 1, "Céu limpo");

        mockMvc.perform(get("/eventos/{id}/climas", evento).header(AUTHORIZATION, auth("RELATORIOS_MENU")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.temperaturaMedia").value(29.00))
                .andExpect(jsonPath("$.resumo.temperaturaMinima").value(28.00))
                .andExpect(jsonPath("$.resumo.temperaturaMaxima").value(30.00))
                .andExpect(jsonPath("$.resumo.chuvaAcumulada").value(0.20))
                .andExpect(jsonPath("$.resumo.ventoMaximo").value(6.00))
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.itens[0].descricaoClima").value("Chuva fraca"))
                .andExpect(jsonPath("$.itens[1].codigoClima").value(1));
    }

    @Test
    void deveDispararSincronizacaoManual() throws Exception {
        Long evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        mockMvc.perform(post("/eventos/{id}/climas/sincronizar", evento).header(AUTHORIZATION, auth("RELATORIOS_MENU")))
                .andExpect(status().isNoContent());

        verify(sincronizacaoService).sincronizarEvento(evento);
    }
}
