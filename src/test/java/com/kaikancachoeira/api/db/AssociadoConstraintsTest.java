package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;

class AssociadoConstraintsTest extends DbIntegrationTest {

    @Test
    void titularPodeSerCadastradoSemTitularResponsavel() {
        assertThatCode(() -> insertAssociado("Maria", LocalDate.of(1980, 1, 15), "TITULAR", null, "11999999999"))
                .doesNotThrowAnyException();
    }

    @Test
    void dataNascimentoPodeSerNula() {
        assertThatCode(() -> jdbc.update("insert into associado (nome, data_nascimento, tipo, titular_id, numero_contato) values (?, ?, ?, ?, ?)",
                "Sem Data", null, "TITULAR", null, "11900000000"))
                .doesNotThrowAnyException();
    }

    @Test
    void dependentePodeReferenciarUmTitularExistente() {
        Long titular = insertAssociado("João", LocalDate.of(1975, 5, 20), "TITULAR", null, "11988888888");

        assertThatCode(() -> insertAssociado("Pedro", LocalDate.of(2010, 8, 2), "DEPENDENTE", titular, "11977777777"))
                .doesNotThrowAnyException();
    }

    @Test
    void titularNaoPodeInformarTitularResponsavel() {
        Long titular = insertAssociado("João", LocalDate.of(1975, 5, 20), "TITULAR", null, "11988888888");

        assertViolacao(() -> jdbc.update("insert into associado (nome, data_nascimento, tipo, titular_id, numero_contato) values (?, ?, ?, ?, ?)",
                "Titular Invalido", LocalDate.of(1980, 1, 1), "TITULAR", titular, "11977777777"),
                "chk_titular");
    }

    @Test
    void dependenteDeveInformarTitularResponsavel() {
        assertViolacao(() -> jdbc.update("insert into associado (nome, data_nascimento, tipo, titular_id, numero_contato) values (?, ?, ?, ?, ?)",
                "Dependente Invalido", LocalDate.of(2010, 1, 1), "DEPENDENTE", null, "11966666666"),
                "chk_titular");
    }

    @Test
    void nomeDeveSerUnico() {
        insertAssociado("Ana", LocalDate.of(1985, 3, 10), "TITULAR", null, "11966666666");

        assertViolacao(() -> insertAssociado("Ana", LocalDate.of(1990, 4, 11), "TITULAR", null, "11955555555"),
                "associado_nome_key");
    }

    @Test
    void tipoDeveSerLimitadoAosValoresAceitos() {
        assertViolacao(() -> insertAssociado("Carlos", LocalDate.of(1988, 12, 1), "VISITANTE", null, "11944444444"),
                "chk_tipo");
    }

    @Test
    void titularIdDevePermitirNuloNoBanco() {
        assertThatCode(() -> jdbc.update("insert into associado (nome, data_nascimento, tipo, titular_id, numero_contato) values (?, ?, ?, ?, ?)",
                "Sem Titular", LocalDate.of(1970, 1, 1), "TITULAR", null, "11933333333"))
                .doesNotThrowAnyException();
    }
}
