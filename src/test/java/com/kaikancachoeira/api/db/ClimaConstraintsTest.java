package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClimaConstraintsTest extends DbIntegrationTest {

    private Long evento;

    @BeforeEach
    void setUp() {
        evento = insertEvento("Evento", "EVENTO", "EM_ANDAMENTO");
    }

    private Long insertClima(
            Long eventoId,
            OffsetDateTime dataHoraPrevista,
            BigDecimal temperatura,
            BigDecimal sensacaoTermica,
            BigDecimal chuvaMm,
            Integer probabilidadeChuva,
            BigDecimal ventoVelocidade,
            Integer codigoClima,
            String descricaoClima
    ) {
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
                dataHoraPrevista,
                temperatura,
                sensacaoTermica,
                chuvaMm,
                probabilidadeChuva,
                ventoVelocidade,
                codigoClima,
                descricaoClima
        );
    }

    @Test
    void climaValidoDeveSerInserido() {
        assertThatCode(() -> insertClima(
                evento,
                T0,
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        )).doesNotThrowAnyException();
    }

    @Test
    void eventoInexistenteDeveRejeitar() {
        assertViolacao(() -> insertClima(
                999_999L,
                T0,
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        ), "fk_clima_evento");
    }

    @Test
    void mesmoEventoMesmaHoraDeveSerRejeitado() {
        insertClima(
                evento,
                T0,
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        );

        assertThatThrownBy(() -> insertClima(
                evento,
                T0,
                bd("27.00"),
                bd("27.50"),
                bd("0.00"),
                20,
                bd("2.10"),
                80,
                "Nublado"
        )).isInstanceOf(org.springframework.dao.DuplicateKeyException.class)
                .hasMessageContaining("uq_evento_data_hora_prevista");
    }

    @Test
    void probabilidadeAbaixoDeZeroDeveSerRejeitada() {
        assertViolacao(() -> insertClima(
                evento,
                T0,
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                -1,
                bd("3.40"),
                63,
                "Chuva fraca"
        ), "chk_probabilidade_chuva");
    }

    @Test
    void probabilidadeAcimaDeCemDeveSerRejeitada() {
        assertViolacao(() -> insertClima(
                evento,
                T0.plusHours(1),
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                101,
                bd("3.40"),
                63,
                "Chuva fraca"
        ), "chk_probabilidade_chuva");
    }

    @Test
    void chuvaNegativaDeveSerRejeitada() {
        assertViolacao(() -> insertClima(
                evento,
                T0,
                bd("28.50"),
                bd("29.10"),
                bd("-0.01"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        ), "chk_chuva_mm");
    }

    @Test
    void ventoNegativoDeveSerRejeitado() {
        assertViolacao(() -> insertClima(
                evento,
                T0,
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("-0.01"),
                63,
                "Chuva fraca"
        ), "chk_vento_velocidade");
    }

    @Test
    void dataHoraPrevistaNaoPodeSerNula() {
        assertThatThrownBy(() -> jdbc.update(
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
                """,
                evento,
                null,
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("null value in column");
    }

    @Test
    void temperaturaNaoPodeSerNula() {
        assertThatThrownBy(() -> jdbc.update(
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
                """,
                evento,
                T0.plusHours(2),
                null,
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("null value in column");
    }

    @Test
    void sensacaoTermicaNaoPodeSerNula() {
        assertThatThrownBy(() -> jdbc.update(
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
                """,
                evento,
                T0.plusHours(3),
                bd("28.50"),
                null,
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                "Chuva fraca"
        )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("null value in column");
    }

    @Test
    void codigoClimaNaoPodeSerNulo() {
        assertThatThrownBy(() -> jdbc.update(
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
                """,
                evento,
                T0.plusHours(4),
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                null,
                "Chuva fraca"
        )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("null value in column");
    }

    @Test
    void descricaoClimaNaoPodeSerNula() {
        assertThatThrownBy(() -> jdbc.update(
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
                """,
                evento,
                T0.plusHours(5),
                bd("28.50"),
                bd("29.10"),
                bd("0.00"),
                10,
                bd("3.40"),
                63,
                null
        )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("null value in column");
    }
}
