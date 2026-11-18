package com.kaikancachoeira.api.associado.controller;

import com.kaikancachoeira.api.support.ControllerIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssociadoControllerIntegrationTest extends ControllerIntegrationTest {

    private String permissaoCompleta() {
        return auth("ASSOCIADOS_MENU", "ASSOCIADOS_LEITURA", "ANUIDADES_MENU", "ANUIDADES_LEITURA");
    }

    private String semPermissao() {
        return auth("VENDAS_MENU");
    }

    @Test
    void listarSemPermissaoDeveRetornar403() throws Exception {
        enviar(get("/associados").param("ano", "2026"), semPermissao(), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void criarSemPermissaoDeveRetornar403() throws Exception {
        enviar(post("/associados"), semPermissao(), """
                {
                  "nome": "Carlos",
                  "numeroContato": "11911112222",
                  "dataNascimento": null,
                  "tipo": "TITULAR",
                  "titularId": null
                }
                """)
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizarSemPermissaoDeveRetornar403() throws Exception {
        Long id = insertAssociado("Lucas", LocalDate.of(1990, 5, 5), "TITULAR", null, "11922223333");

        enviar(put("/associados/{id}", id), semPermissao(), """
                {
                  "nome": "Lucas Atualizado",
                  "numeroContato": "11933334444",
                  "dataNascimento": "1991-06-06",
                  "tipo": "DEPENDENTE",
                  "titularId": null
                }
                """)
                .andExpect(status().isForbidden());
    }

    @Test
    void excluirSemPermissaoDeveRetornar403() throws Exception {
        Long id = insertAssociado("Para Remover", LocalDate.of(1980, 1, 1), "TITULAR", null, "11955556666");

        enviar(delete("/associados/{id}", id), semPermissao(), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void registrarPagamentoSemPermissaoDeveRetornar403() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long associado = insertAssociado("Titular", LocalDate.of(1980, 2, 2), "TITULAR", null, "11977778888");
        Long anuidade = insertAnuidade(associado, config, bd("180.00"), "PENDENTE");

        enviar(post("/associados/{id}/pagamentos", associado), semPermissao(), """
                {
                  "anuidadeId": %d,
                  "valor": 180.00
                }
                """.formatted(anuidade))
                .andExpect(status().isForbidden());
    }

    @Test
    void excluirPagamentoSemPermissaoDeveRetornar403() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long associado = insertAssociado("Titular", LocalDate.of(1980, 2, 2), "TITULAR", null, "11977778888");
        Long anuidade = insertAnuidade(associado, config, bd("180.00"), "PAGO");
        Long pagamento = insertPagamentoAnuidade(anuidade, bd("180.00"));

        enviar(delete("/associados/{idAssociado}/pagamentos/{idPagamento}", associado, pagamento), semPermissao(), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void listarDeveFiltrarPorAnoTipoStatusEIdade() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long titular = insertAssociado("Maria", LocalDate.of(1980, 2, 10), "TITULAR", null, "11999990000");
        Long dependente = insertAssociado("Pedro", LocalDate.of(2010, 3, 5), "DEPENDENTE", titular, "11999990001");
        Long outra = insertAssociado("Joana", LocalDate.of(2000, 4, 1), "TITULAR", null, "11999990002");
        Long anuidadeTitular = insertAnuidade(titular, config, bd("180.00"), "PAGO");
        insertPagamentoAnuidade(anuidadeTitular, bd("180.00"));
        insertAnuidade(dependente, config, bd("180.00"), "PENDENTE");
        insertAnuidade(outra, config, bd("180.00"), "ISENTO");

        enviar(get("/associados")
                        .param("ano", "2026")
                        .param("tipo", "DEPENDENTE")
                        .param("status", "PENDENTE")
                        .param("dataInicio", "2005-01-01")
                        .param("dataFim", "2015-12-31"),
                permissaoCompleta(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Pedro"))
                .andExpect(jsonPath("$.itens[0].tipo").value("DEPENDENTE"))
                .andExpect(jsonPath("$.itens[0].titularId").value(titular.intValue()))
                .andExpect(jsonPath("$.itens[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void criarTitularDeveRetornar201ComContrato() throws Exception {
        enviar(post("/associados"), permissaoCompleta(), """
                {
                  "nome": "Carlos",
                  "numeroContato": "11911112222",
                  "dataNascimento": null,
                  "tipo": "TITULAR",
                  "titularId": null
                }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Carlos"))
                .andExpect(jsonPath("$.numeroContato").value("11911112222"))
                .andExpect(jsonPath("$.dataNascimento").value(nullValue()))
                .andExpect(jsonPath("$.tipo").value("TITULAR"))
                .andExpect(jsonPath("$.titularId").value(nullValue()));
    }

    @Test
    void naoDevePermitirNomeDeAssociadoDuplicado() throws Exception {
        insertAssociado("Ana", LocalDate.of(1985, 1, 1), "TITULAR", null, "11900000000");

        enviar(post("/associados"), permissaoCompleta(), """
                {
                  "nome": "Ana",
                  "numeroContato": "11900000001",
                  "dataNascimento": "1990-02-02",
                  "tipo": "TITULAR",
                  "titularId": null
                }
                """)
                .andExpect(status().isConflict());
    }

    @Test
    void atualizarAssociadoDeveManterContrato() throws Exception {
        Long id = insertAssociado("Lucas", LocalDate.of(1990, 5, 5), "TITULAR", null, "11922223333");
        Long titular = insertAssociado("Responsavel", LocalDate.of(1979, 8, 8), "TITULAR", null, "11922223334");

        enviar(put("/associados/{id}", id), permissaoCompleta(), """
                {
                  "nome": "Lucas Atualizado",
                  "numeroContato": "11933334444",
                  "dataNascimento": "1991-06-06",
                  "tipo": "DEPENDENTE",
                  "titularId": %d
                }
                """.formatted(titular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.nome").value("Lucas Atualizado"))
                .andExpect(jsonPath("$.numeroContato").value("11933334444"))
                .andExpect(jsonPath("$.dataNascimento").value("1991-06-06"))
                .andExpect(jsonPath("$.tipo").value("DEPENDENTE"))
                .andExpect(jsonPath("$.titularId").value(titular.intValue()));
    }

    @Test
    void excluirAssociadoDeveRetornar204() throws Exception {
        Long id = insertAssociado("Para Remover", LocalDate.of(1980, 1, 1), "TITULAR", null, "11955556666");

        enviar(delete("/associados/{id}", id), permissaoCompleta(), null)
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(count("select count(*) from associado where id = ?", id)).isZero();
    }

    @Test
    void registrarPagamentoDeveRetornarDadosDaAnuidade() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long associado = insertAssociado("Titular", LocalDate.of(1980, 2, 2), "TITULAR", null, "11977778888");
        Long anuidade = insertAnuidade(associado, config, bd("180.00"), "PENDENTE");

        enviar(post("/associados/{id}/pagamentos", associado), permissaoCompleta(), """
                {
                  "anuidadeId": %d,
                  "valor": 180.00
                }
                """.formatted(anuidade))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.anuidadeId").value(anuidade.intValue()))
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.valor").value(180.00));
    }

    @Test
    void excluirPagamentoDeveRetornar204() throws Exception {
        Long config = insertConfiguracaoAnuidade(2026, bd("180.00"));
        Long associado = insertAssociado("Titular", LocalDate.of(1980, 2, 2), "TITULAR", null, "11977778888");
        Long anuidade = insertAnuidade(associado, config, bd("180.00"), "PAGO");
        Long pagamento = insertPagamentoAnuidade(anuidade, bd("180.00"));

        enviar(delete("/associados/{idAssociado}/pagamentos/{idPagamento}", associado, pagamento), permissaoCompleta(), null)
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(count("select count(*) from pagamento where id = ?", pagamento)).isZero();
    }
}
