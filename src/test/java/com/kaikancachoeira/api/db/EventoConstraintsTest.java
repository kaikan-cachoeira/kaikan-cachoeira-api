package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoConstraintsTest extends DbIntegrationTest {

    // ------------------------------------------------------------ defaults

    @Test
    void deveAplicarStatusNaoIniciadoETipoEventoPorPadrao() {
        jdbc.update("""
                insert into evento (nome, inicio_previsto_em, encerramento_previsto_em)
                values ('Padrao', ?, ?)""", T0, T0.plusHours(2));

        assertThat(jdbc.queryForObject("""
                select s.codigo || '|' || e.tipo_evento from evento e
                join status_evento s on s.id = e.status_id where e.nome = 'Padrao'""", String.class))
                .isEqualTo("NAO_INICIADO|EVENTO");
    }

    // ------------------------------------------------------------- período

    @Test
    void encerramentoPosteriorAoInicioEhValido() {
        assertThatCode(() -> insertEvento("Ok", "EVENTO", "NAO_INICIADO", T0, T0.plusMinutes(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void encerramentoAnteriorAoInicioDeveSerRejeitado() {
        assertThatThrownBy(() -> insertEvento("Ruim", "EVENTO", "NAO_INICIADO", T0, T0.minusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_evento_periodo");
    }

    @Test
    void encerramentoIgualAoInicioDeveSerRejeitado() {
        assertThatThrownBy(() -> insertEvento("Igual", "EVENTO", "NAO_INICIADO", T0, T0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_evento_periodo");
    }

    // ---------------------------------------------------------------- tipo

    @Test
    void deveAceitarEventoEReuniao() {
        assertThatCode(() -> {
            insertEvento("E", "EVENTO", "NAO_INICIADO");
            insertEvento("R", "REUNIAO", "NAO_INICIADO");
        }).doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarTipoDesconhecido() {
        assertThatThrownBy(() -> insertEvento("X", "FESTA", "NAO_INICIADO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_evento_tipo_evento");
    }

    // ---------------------------------------------------------- unicidade

    @Test
    void mesmoNomeEPeriodoDeveSerRejeitado() {
        insertEvento("Undoukai", "EVENTO", "NAO_INICIADO");

        assertThatThrownBy(() -> insertEvento("Undoukai", "EVENTO", "NAO_INICIADO"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_evento_nome_periodo");
    }

    @Test
    void mesmoNomeEmPeriodoDiferenteEhPermitido() {
        insertEvento("Undoukai", "EVENTO", "ENCERRADO", T0, T0.plusHours(8));

        assertThatCode(() -> insertEvento("Undoukai", "EVENTO", "NAO_INICIADO",
                T0.plusYears(1), T0.plusYears(1).plusHours(8)))
                .doesNotThrowAnyException();
    }

    @Test
    void periodoIgualComNomeDiferenteEhPermitido() {
        insertEvento("Bingo", "EVENTO", "NAO_INICIADO");

        assertThatCode(() -> insertEvento("Karaokê", "EVENTO", "NAO_INICIADO"))
                .doesNotThrowAnyException();
    }

    // ------------------------------------------ um evento em andamento por vez

    @Test
    void naoDevePermitirDoisEventosEmAndamento() {
        insertEvento("A", "EVENTO", "EM_ANDAMENTO");

        assertThatThrownBy(() -> insertEvento("B", "EVENTO", "EM_ANDAMENTO"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_evento_em_andamento");
    }

    @Test
    void naoDevePermitirIniciarSegundoEventoPorUpdate() {
        insertEvento("A", "EVENTO", "EM_ANDAMENTO");
        Long b = insertEvento("B", "EVENTO", "NAO_INICIADO");

        assertThatThrownBy(() -> jdbc.update(
                "update evento set status_id = 2 where id = ?", b))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_evento_em_andamento");
    }

    @Test
    void depoisDeEncerrarOPrimeiroOutroPodeIniciar() {
        Long a = insertEvento("A", "EVENTO", "EM_ANDAMENTO");
        jdbc.update("update evento set status_id = 3 where id = ?", a);

        assertThatCode(() -> insertEvento("B", "EVENTO", "EM_ANDAMENTO"))
                .doesNotThrowAnyException();
    }

    @Test
    void reabrirEventoEncerradoBarradoSeJaHaOutroEmAndamento() {
        Long encerrado = insertEvento("Velho", "EVENTO", "ENCERRADO");
        insertEvento("Atual", "EVENTO", "EM_ANDAMENTO");

        assertThatThrownBy(() -> jdbc.update("update evento set status_id = 2 where id = ?", encerrado))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_evento_em_andamento");
    }

    @Test
    void variosEventosEncerradosOuNaoIniciadosSaoPermitidos() {
        assertThatCode(() -> {
            insertEvento("E1", "EVENTO", "ENCERRADO");
            insertEvento("E2", "EVENTO", "ENCERRADO");
            insertEvento("E3", "EVENTO", "NAO_INICIADO");
            insertEvento("E4", "EVENTO", "NAO_INICIADO");
        }).doesNotThrowAnyException();
    }

    @Test
    void reuniaoEmAndamentoNaoConflitaComEventoEmAndamento() {
        insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");

        assertThatCode(() -> insertEvento("Reuniao", "REUNIAO", "EM_ANDAMENTO"))
                .doesNotThrowAnyException();
    }

    @Test
    void variasReunioesEmAndamentoSaoPermitidas() {
        insertEvento("R1", "REUNIAO", "EM_ANDAMENTO");

        assertThatCode(() -> insertEvento("R2", "REUNIAO", "EM_ANDAMENTO"))
                .doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------ FK

    @Test
    void statusInexistenteDeveSerRejeitado() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into evento (status_id, nome, inicio_previsto_em, encerramento_previsto_em)
                values (99, 'X', ?, ?)""", T0, T0.plusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_evento_status");
    }
}