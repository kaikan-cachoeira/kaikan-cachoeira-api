package com.kaikancachoeira.api.clima.service;

import com.kaikancachoeira.api.clima.client.ClimaClient;
import com.kaikancachoeira.api.clima.client.ClimaPrevisaoHoraria;
import com.kaikancachoeira.api.clima.config.OpenMeteoProperties;
import com.kaikancachoeira.api.clima.entity.Clima;
import com.kaikancachoeira.api.clima.repository.ClimaRepository;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.entity.StatusEvento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClimaSincronizacaoServiceTest {

    @Mock private EventoGuard eventoGuard;
    @Mock private EventoRepository eventoRepository;
    @Mock private ClimaRepository climaRepository;
    @Mock private ClimaClient climaClient;
    private final OpenMeteoProperties properties = new OpenMeteoProperties();

    private ClimaSincronizacaoService service() {
        properties.setTimeZone(ZoneId.of("UTC"));
        return new ClimaSincronizacaoService(eventoGuard, eventoRepository, climaRepository, climaClient, properties);
    }

    private Evento eventoEmAndamento() {
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setStatus(new StatusEvento(2, "EM_ANDAMENTO", "Em andamento", null, null));
        evento.setInicioPrevistoEm(Instant.parse("2026-11-21T13:20:00Z"));
        evento.setEncerramentoPrevistoEm(Instant.parse("2026-11-21T15:45:00Z"));
        return evento;
    }

    private ClimaPrevisaoHoraria previsao(String hora, String temperatura) {
        return new ClimaPrevisaoHoraria(
                Instant.parse(hora),
                new BigDecimal(temperatura),
                new BigDecimal(temperatura).add(BigDecimal.ONE),
                new BigDecimal("0.20"),
                20,
                new BigDecimal("4.00"),
                63,
                "Chuva moderada"
        );
    }

    @Test
    void sincronizarEventoDevePersistirSomenteOsNovosRegistros() {
        Evento evento = eventoEmAndamento();
        when(eventoGuard.buscar(1L)).thenReturn(evento);
        when(climaClient.consultar(Instant.parse("2026-11-21T13:00:00Z"), Instant.parse("2026-11-21T15:00:00Z")))
                .thenReturn(List.of(
                        previsao("2026-11-21T13:00:00Z", "27.00"),
                        previsao("2026-11-21T14:00:00Z", "28.00")
                ));
        when(climaRepository.existsByEventoIdAndDataHoraPrevista(eq(1L), eq(Instant.parse("2026-11-21T13:00:00Z"))))
                .thenReturn(true);
        when(climaRepository.existsByEventoIdAndDataHoraPrevista(eq(1L), eq(Instant.parse("2026-11-21T14:00:00Z"))))
                .thenReturn(false);

        service().sincronizarEvento(1L);

        verify(climaRepository, org.mockito.Mockito.times(1)).save(any(Clima.class));
    }

    @Test
    void sincronizarEventoDeveContinuarMesmoSeUmInsertFalhar() {
        Evento evento = eventoEmAndamento();
        when(eventoGuard.buscar(1L)).thenReturn(evento);
        when(climaClient.consultar(Instant.parse("2026-11-21T13:00:00Z"), Instant.parse("2026-11-21T15:00:00Z")))
                .thenReturn(List.of(
                        previsao("2026-11-21T13:00:00Z", "27.00"),
                        previsao("2026-11-21T14:00:00Z", "28.00")
                ));
        when(climaRepository.existsByEventoIdAndDataHoraPrevista(eq(1L), any())).thenReturn(false);
        doAnswer(inv -> {
            Clima clima = inv.getArgument(0);
            if (clima.getDataHoraPrevista().equals(Instant.parse("2026-11-21T13:00:00Z"))) {
                throw new RuntimeException("falha");
            }
            return clima;
        }).when(climaRepository).save(any(Clima.class));

        service().sincronizarEvento(1L);

        verify(climaRepository, org.mockito.Mockito.times(2)).save(any(Clima.class));
    }

    @Test
    void sincronizarEventosEmAndamentoDevePercorrerOsEventos() {
        Evento evento = eventoEmAndamento();
        when(eventoRepository.findByStatusCodigo("EM_ANDAMENTO")).thenReturn(List.of(evento));
        when(climaClient.consultar(any(), any())).thenReturn(List.of());

        service().sincronizarEventosEmAndamento();

        verify(climaClient).consultar(any(), any());
    }
}
