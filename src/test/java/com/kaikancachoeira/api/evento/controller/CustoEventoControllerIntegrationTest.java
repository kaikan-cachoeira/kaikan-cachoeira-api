
package com.kaikancachoeira.api.evento.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.MediaType;

class CustoEventoControllerIntegrationTest extends ControllerIntegrationTest {

    private static final String ROTA = "/eventos/{eventoId}/custos";

    private Long inserirCusto(Long eventoId, String nome, String descricao, String valor) {
        return jdbc.queryForObject("""
                INSERT INTO custo_evento (evento_id, nome, descricao, valor)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, Long.class, eventoId, nome, descricao, bd(valor));
    }

    private org.springframework.test.web.servlet.ResultActions listar(
            Long eventoId, String token
    ) throws Exception {
        return mockMvc.perform(get(ROTA, eventoId)
                .header(AUTHORIZATION, token));
    }

    @Test
    void deveRetornarListaVaziaQuandoEventoNaoPossuiCustos() throws Exception {
        Long eventoId = insertEvento("Evento sem custos", "EVENTO", "NAO_INICIADO");

        listar(eventoId, auth("EVENTOS_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveListarCustosDoEventoComContratoDaResposta() throws Exception {
        Long eventoId = insertEvento("Evento com custos", "EVENTO", "EM_ANDAMENTO");
        inserirCusto(eventoId, "Aluguel", "Aluguel do espaço", "250.00");
        inserirCusto(eventoId, "Limpeza", null, "120.00");

        listar(eventoId, auth("EVENTOS_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].nome").value("Aluguel"))
                .andExpect(jsonPath("$[0].descricao").value("Aluguel do espaço"))
                .andExpect(jsonPath("$[0].valor").value(250.00))
                .andExpect(jsonPath("$[0].criadoEm").isNotEmpty())
                .andExpect(jsonPath("$[0].atualizadoEm").isNotEmpty())
                .andExpect(jsonPath("$[1].nome").value("Limpeza"))
                .andExpect(jsonPath("$[1].descricao").doesNotExist())
                .andExpect(jsonPath("$[1].valor").value(120.00));
    }

    @Test
    void deveRetornar404QuandoEventoNaoExiste() throws Exception {
        listar(999_999L, auth("EVENTOS_MENU"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void deveExigirPermissaoDeGerenciamentoDeEventos() throws Exception {
        Long eventoId = insertEvento("Evento protegido", "EVENTO", "NAO_INICIADO");

        listar(eventoId, auth("EVENTOS_LEITURA"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void deveRetornar401QuandoNaoHaToken() throws Exception {
        Long eventoId = insertEvento("Evento protegido", "EVENTO", "NAO_INICIADO");

        mockMvc.perform(get(ROTA, eventoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void deveAdicionarCustoQuandoEventoEstaEmAndamento() throws Exception {
        Long eventoId = insertEvento(
                "Evento com custo",
                "EVENTO",
                "EM_ANDAMENTO"
        );

        mockMvc.perform(post("/eventos/{eventoId}/custos", eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Aluguel de equipamentos",
                                "descricao": "Equipamentos de som",
                                "valor": 250.00
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Aluguel de equipamentos"))
                .andExpect(jsonPath("$.descricao").value("Equipamentos de som"))
                .andExpect(jsonPath("$.valor").value(250.00))
                .andExpect(jsonPath("$.criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.atualizadoEm").isNotEmpty());
    }

    @Test
    void deveRetornar409AoAdicionarCustoEmEventoNaoIniciado() throws Exception {
        Long eventoId = insertEvento(
                "Evento futuro",
                "EVENTO",
                "NAO_INICIADO"
        );

        mockMvc.perform(post("/eventos/{eventoId}/custos", eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Aluguel de equipamentos",
                                "descricao": "Equipamentos de som",
                                "valor": 250.00
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUSTO_EVENTO_STATUS_INVALIDO"));
    }

    @Test
    void deveRetornar400AoAdicionarCustoComValorInvalido() throws Exception {
        Long eventoId = insertEvento(
                "Evento em andamento",
                "EVENTO",
                "EM_ANDAMENTO"
        );

        mockMvc.perform(post("/eventos/{eventoId}/custos", eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Aluguel de equipamentos",
                                "descricao": "Equipamentos de som",
                                "valor": 0
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404AoAdicionarCustoEmEventoInexistente() throws Exception {
        mockMvc.perform(post("/eventos/{eventoId}/custos", 999999L)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Aluguel de equipamentos",
                                "descricao": "Equipamentos de som",
                                "valor": 250.00
                            }
                            """))
                .andExpect(status().isNotFound());
    }


    @Test
    void atualizarDeveAlterarOsCamposEPersistir() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        Long custoId = inserirCusto(eventoId, "Custo antigo", "Descrição antiga", "10.00");

        String corpo = """
            {
                "nome": "Custo atualizado",
                "descricao": "Nova descrição",
                "valor": 25.50
            }
            """;

        mockMvc.perform(put(ROTA + "/{custoId}", eventoId, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(custoId))
                .andExpect(jsonPath("$.nome").value("Custo atualizado"))
                .andExpect(jsonPath("$.descricao").value("Nova descrição"))
                .andExpect(jsonPath("$.valor").value(25.50));

        assertThat(jdbc.queryForObject(
                "SELECT nome FROM custo_evento WHERE id = ?",
                String.class,
                custoId
        )).isEqualTo("Custo atualizado");
    }

    @Test
    void atualizarCustoInexistenteDeveRetornar404() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        mockMvc.perform(put(ROTA + "/{custoId}", eventoId, 999_999L)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Custo",
                                "descricao": "Descrição",
                                "valor": 10.00
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void atualizarCustoDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoA = insertEvento("Evento A", "EVENTO", "EM_ANDAMENTO");
        Long eventoB = insertEvento("Evento B", "EVENTO", "NAO_INICIADO");

        Long custoId = inserirCusto(
                eventoA, "Custo", "Descrição", "10.00"
        );

        mockMvc.perform(put(ROTA + "/{custoId}", eventoB, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Custo atualizado",
                                "descricao": "Descrição",
                                "valor": 20.00
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void atualizarCustoDeEventoNaoIniciadoDeveRetornar409() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "NAO_INICIADO");

        Long custoId = inserirCusto(
                eventoId, "Custo", "Descrição", "10.00"
        );

        mockMvc.perform(put(ROTA + "/{custoId}", eventoId, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Custo atualizado",
                                "descricao": "Descrição",
                                "valor": 20.00
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo")
                        .value("CUSTO_EVENTO_STATUS_INVALIDO"));
    }

    @Test
    void atualizarCustoComValorInvalidoDeveRetornar400() throws Exception {
        Long eventoId = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        Long custoId = inserirCusto(
                eventoId, "Custo", "Descrição", "10.00"
        );

        mockMvc.perform(put(ROTA + "/{custoId}", eventoId, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nome": "Custo atualizado",
                                "descricao": "Descrição",
                                "valor": 0
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removerCustoDeveRetornar204() throws Exception {
        Long eventoId = insertEvento(
                "Evento para remover custo",
                "EVENTO",
                "NAO_INICIADO"
        );

        Long custoId = inserirCusto(
                eventoId,
                "Aluguel do espaço",
                "Espaço para realização do evento",
                "500.00"
        );

        mockMvc.perform(delete(ROTA + "/{custoId}", eventoId, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU")))
                .andExpect(status().isNoContent());

        Integer quantidade = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custo_evento WHERE id = ?",
                Integer.class,
                custoId
        );

        assertThat(quantidade).isZero();
    }

    @Test
    void removerCustoDeEventoInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(delete(ROTA + "/{custoId}", 999_999L, 999_999L)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void removerCustoInexistenteDeveRetornar404() throws Exception {
        Long eventoId = insertEvento(
                "Evento sem esse custo",
                "EVENTO",
                "NAO_INICIADO"
        );

        mockMvc.perform(delete(ROTA + "/{custoId}", eventoId, 999_999L)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void removerCustoDeOutroEventoDeveRetornar404() throws Exception {
        Long eventoA = insertEvento(
                "Evento A",
                "EVENTO",
                "EM_ANDAMENTO"
        );

        Long eventoB = insertEvento(
                "Evento B",
                "EVENTO",
                "NAO_INICIADO"
        );

        Long custoId = inserirCusto(
                eventoA,
                "Custo do evento A",
                "Descrição",
                "10.00"
        );

        mockMvc.perform(delete(ROTA + "/{custoId}", eventoB, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM custo_evento WHERE id = ?",
                Integer.class,
                custoId
        )).isEqualTo(1);
    }

    @Test
    void deveAdicionarCustoQuandoEventoEstaEncerrado() throws Exception {
        Long eventoId = insertEvento(
                "Evento encerrado",
                "EVENTO",
                "ENCERRADO"
        );

        mockMvc.perform(post(ROTA, eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "Custo final",
                            "descricao": "Despesa adicional",
                            "valor": 125.50
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Custo final"))
                .andExpect(jsonPath("$.valor").value(125.50));
    }

    @Test
    void deveAtualizarCustoQuandoEventoEstaEncerrado() throws Exception {
        Long eventoId = insertEvento(
                "Evento encerrado",
                "EVENTO",
                "ENCERRADO"
        );

        Long custoId = inserirCusto(
                eventoId,
                "Custo antigo",
                "Descrição antiga",
                "10.00"
        );

        mockMvc.perform(put(ROTA + "/{custoId}", eventoId, custoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "Custo atualizado",
                            "descricao": "Nova descrição",
                            "valor": 25.50
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(custoId))
                .andExpect(jsonPath("$.nome").value("Custo atualizado"))
                .andExpect(jsonPath("$.valor").value(25.50));
    }


    @Test
    void atualizarCustoDeEventoInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(put(ROTA + "/{custoId}", 999_999L, 1L)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "Custo atualizado",
                            "descricao": "Descrição",
                            "valor": 20.00
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void deveAdicionarCustoQuandoEventoEhReuniao() throws Exception {
        Long eventoId = insertEvento(
                "Reunião com custos",
                "REUNIAO",
                "EM_ANDAMENTO"
        );

        mockMvc.perform(post(ROTA, eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "Alimentação",
                            "descricao": "Lanche para participantes",
                            "valor": 150.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Alimentação"))
                .andExpect(jsonPath("$.valor").value(150.00));
    }

    @Test
    void deveRetornar400AoAdicionarCustoComNomeEmBranco() throws Exception {
        Long eventoId = insertEvento(
                "Evento com custo inválido",
                "EVENTO",
                "EM_ANDAMENTO"
        );

        mockMvc.perform(post(ROTA, eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "   ",
                            "descricao": "Descrição",
                            "valor": 10.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400AoAdicionarCustoComNomeMaiorQue150Caracteres()
            throws Exception {
        Long eventoId = insertEvento(
                "Evento com nome muito longo",
                "EVENTO",
                "EM_ANDAMENTO"
        );

        String nome = "a".repeat(151);

        mockMvc.perform(post(ROTA, eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "%s",
                            "descricao": "Descrição",
                            "valor": 10.00
                        }
                        """.formatted(nome)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400AoAdicionarCustoComDescricaoMaiorQue255Caracteres()
            throws Exception {
        Long eventoId = insertEvento(
                "Evento com descrição muito longa",
                "EVENTO",
                "EM_ANDAMENTO"
        );

        String descricao = "a".repeat(256);

        mockMvc.perform(post(ROTA, eventoId)
                        .header(AUTHORIZATION, auth("EVENTOS_MENU"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "nome": "Custo válido",
                            "descricao": "%s",
                            "valor": 10.00
                        }
                        """.formatted(descricao)))
                .andExpect(status().isBadRequest());
    }


}
