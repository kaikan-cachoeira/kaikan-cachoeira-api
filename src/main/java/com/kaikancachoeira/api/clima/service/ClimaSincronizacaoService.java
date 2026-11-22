package com.kaikancachoeira.api.clima.service;

import com.kaikancachoeira.api.clima.client.ClimaClient;
import com.kaikancachoeira.api.clima.client.ClimaPrevisaoHoraria;
import com.kaikancachoeira.api.clima.config.OpenMeteoProperties;
import com.kaikancachoeira.api.clima.entity.Clima;
import com.kaikancachoeira.api.clima.repository.ClimaRepository;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ClimaSincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(ClimaSincronizacaoService.class);
    static final String EM_ANDAMENTO = "EM_ANDAMENTO";

    private final EventoGuard eventoGuard;
    private final EventoRepository eventoRepository;
    private final ClimaRepository climaRepository;
    private final ClimaClient climaClient;
    private final OpenMeteoProperties properties;

    public ClimaSincronizacaoService(
            EventoGuard eventoGuard,
            EventoRepository eventoRepository,
            ClimaRepository climaRepository,
            ClimaClient climaClient,
            OpenMeteoProperties properties
    ) {
        this.eventoGuard = eventoGuard;
        this.eventoRepository = eventoRepository;
        this.climaRepository = climaRepository;
        this.climaClient = climaClient;
        this.properties = properties;
    }

    public void sincronizarEvento(Long eventoId) {
        Evento evento = eventoGuard.buscar(eventoId);
        sincronizarEvento(evento);
    }

    @Scheduled(fixedDelayString = "${app.clima.sincronizacao.delay:PT1H}")
    public void sincronizarEventosEmAndamento() {
        eventoRepository.findByStatusCodigo(EM_ANDAMENTO).forEach(this::sincronizarEventoSeguranca);
    }

    private void sincronizarEventoSeguranca(Evento evento) {
        try {
            sincronizarEvento(evento);
        } catch (RuntimeException ex) {
            log.warn("Falha ao sincronizar clima do evento {}", evento.getId(), ex);
        }
    }

    private void sincronizarEvento(Evento evento) {
        if (!EM_ANDAMENTO.equals(evento.getStatus().getCodigo())) {
            return;
        }

        Instant inicio = floorHour(evento.getInicioPrevistoEm());
        Instant fim = floorHour(evento.getEncerramentoPrevistoEm());

        List<ClimaPrevisaoHoraria> previsoes = climaClient.consultar(inicio, fim).stream()
                .filter(p -> !p.dataHoraPrevista().isBefore(inicio) && !p.dataHoraPrevista().isAfter(fim))
                .toList();

        for (ClimaPrevisaoHoraria previsao : previsoes) {
            try {
                if (climaRepository.existsByEventoIdAndDataHoraPrevista(evento.getId(), previsao.dataHoraPrevista())) {
                    continue;
                }

                Clima clima = new Clima();
                clima.setEvento(evento);
                clima.setDataHoraPrevista(previsao.dataHoraPrevista());
                clima.setTemperatura(previsao.temperatura());
                clima.setSensacaoTermica(previsao.sensacaoTermica());
                clima.setChuvaMm(previsao.chuvaMm());
                clima.setProbabilidadeChuva(previsao.probabilidadeChuva());
                clima.setVentoVelocidade(previsao.ventoVelocidade());
                clima.setCodigoClima(previsao.codigoClima());
                clima.setDescricaoClima(previsao.descricaoClima());
                climaRepository.save(clima);
            } catch (RuntimeException ex) {
                log.warn("Falha ao persistir clima do evento {} em {}", evento.getId(), previsao.dataHoraPrevista(), ex);
            }
        }
    }

    private Instant floorHour(Instant instante) {
        ZoneId zoneId = properties.getTimeZone();
        return LocalDateTime.ofInstant(instante, zoneId)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(zoneId)
                .toInstant();
    }
}
