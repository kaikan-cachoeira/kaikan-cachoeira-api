package com.kaikancachoeira.api.clima.service;

import com.kaikancachoeira.api.clima.entity.Clima;
import com.kaikancachoeira.api.clima.repository.ClimaRepository;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClimaServiceTest {

    @Mock private EventoGuard eventoGuard;
    @Mock private ClimaRepository climaRepository;

    private ClimaService service;

    @BeforeEach
    void setUp() {
        service = new ClimaService(eventoGuard, climaRepository);
    }

    private Clima clima(
            Long id,
            Instant dataHora,
            String temperatura,
            String sensacao,
            String chuva,
            Integer probabilidade,
            String vento,
            Integer codigo,
            String descricao
    ) {
        Clima clima = new Clima();
        clima.setId(id);
        clima.setEvento(new Evento());
        clima.setDataHoraPrevista(dataHora);
        clima.setTemperatura(new BigDecimal(temperatura));
        clima.setSensacaoTermica(new BigDecimal(sensacao));
        clima.setChuvaMm(new BigDecimal(chuva));
        clima.setProbabilidadeChuva(probabilidade);
        clima.setVentoVelocidade(new BigDecimal(vento));
        clima.setCodigoClima(codigo);
        clima.setDescricaoClima(descricao);
        return clima;
    }

    @Test
    void consultarSemDadosDeveRetornarResumoZero() {
        Long eventoId = 10L;
        when(eventoGuard.buscar(eventoId)).thenReturn(new Evento());
        when(climaRepository.findByEventoIdOrderByDataHoraPrevistaAsc(eventoId)).thenReturn(List.of());

        var response = service.consultar(eventoId);

        assertThat(response.itens()).isEmpty();
        assertThat(response.resumo().temperaturaMedia()).isEqualByComparingTo("0.00");
        assertThat(response.resumo().temperaturaMinima()).isEqualByComparingTo("0.00");
        assertThat(response.resumo().temperaturaMaxima()).isEqualByComparingTo("0.00");
        assertThat(response.resumo().chuvaAcumulada()).isEqualByComparingTo("0.00");
        assertThat(response.resumo().ventoMaximo()).isEqualByComparingTo("0.00");
    }

    @Test
    void consultarDeveListarItensECalcularResumo() {
        Long eventoId = 20L;
        when(eventoGuard.buscar(eventoId)).thenReturn(new Evento());

        List<Clima> climas = List.of(
                clima(1L, Instant.parse("2026-11-21T14:00:00Z"), "28.00", "29.00", "0.20", 30, "4.00", 63, "Chuva fraca"),
                clima(2L, Instant.parse("2026-11-21T15:00:00Z"), "30.00", "31.00", "0.00", 10, "6.00", 1, "Céu limpo")
        );
        when(climaRepository.findByEventoIdOrderByDataHoraPrevistaAsc(eventoId)).thenReturn(climas);

        var response = service.consultar(eventoId);

        assertThat(response.itens()).hasSize(2);
        assertThat(response.itens().get(0).descricaoClima()).isEqualTo("Chuva fraca");
        assertThat(response.itens().get(1).codigoClima()).isEqualTo(1);
        assertThat(response.resumo().temperaturaMedia()).isEqualByComparingTo("29.00");
        assertThat(response.resumo().temperaturaMinima()).isEqualByComparingTo("28.00");
        assertThat(response.resumo().temperaturaMaxima()).isEqualByComparingTo("30.00");
        assertThat(response.resumo().chuvaAcumulada()).isEqualByComparingTo("0.20");
        assertThat(response.resumo().ventoMaximo()).isEqualByComparingTo("6.00");
    }
}
