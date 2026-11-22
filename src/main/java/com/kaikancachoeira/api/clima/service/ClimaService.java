package com.kaikancachoeira.api.clima.service;

import com.kaikancachoeira.api.clima.dto.ClimaItemResponse;
import com.kaikancachoeira.api.clima.dto.ClimaResponse;
import com.kaikancachoeira.api.clima.dto.ClimaResumoResponse;
import com.kaikancachoeira.api.clima.entity.Clima;
import com.kaikancachoeira.api.clima.repository.ClimaRepository;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ClimaService {

    private final EventoGuard eventoGuard;
    private final ClimaRepository climaRepository;

    public ClimaService(EventoGuard eventoGuard, ClimaRepository climaRepository) {
        this.eventoGuard = eventoGuard;
        this.climaRepository = climaRepository;
    }

    @Transactional(readOnly = true)
    public ClimaResponse consultar(Long eventoId) {
        eventoGuard.buscar(eventoId);

        List<Clima> climas = climaRepository.findByEventoIdOrderByDataHoraPrevistaAsc(eventoId);
        List<ClimaItemResponse> itens = climas.stream()
                .map(this::mapearItem)
                .toList();

        return new ClimaResponse(resumo(climas), itens);
    }

    private ClimaItemResponse mapearItem(Clima clima) {
        return new ClimaItemResponse(
                clima.getDataHoraPrevista(),
                clima.getTemperatura(),
                clima.getSensacaoTermica(),
                clima.getChuvaMm(),
                clima.getProbabilidadeChuva(),
                clima.getVentoVelocidade(),
                clima.getCodigoClima(),
                clima.getDescricaoClima()
        );
    }

    private ClimaResumoResponse resumo(List<Clima> climas) {
        if (climas.isEmpty()) {
            BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new ClimaResumoResponse(zero, zero, zero, zero, zero);
        }

        BigDecimal totalTemperatura = BigDecimal.ZERO;
        BigDecimal minima = null;
        BigDecimal maxima = null;
        BigDecimal chuvaAcumulada = BigDecimal.ZERO;
        BigDecimal ventoMaximo = null;

        for (Clima clima : climas) {
            BigDecimal temperatura = clima.getTemperatura();
            totalTemperatura = totalTemperatura.add(temperatura);
            minima = (minima == null || temperatura.compareTo(minima) < 0) ? temperatura : minima;
            maxima = (maxima == null || temperatura.compareTo(maxima) > 0) ? temperatura : maxima;
            chuvaAcumulada = chuvaAcumulada.add(clima.getChuvaMm());
            BigDecimal vento = clima.getVentoVelocidade();
            ventoMaximo = (ventoMaximo == null || vento.compareTo(ventoMaximo) > 0) ? vento : ventoMaximo;
        }

        BigDecimal media = totalTemperatura.divide(BigDecimal.valueOf(climas.size()), 2, RoundingMode.HALF_UP);
        return new ClimaResumoResponse(
                media,
                minima.setScale(2, RoundingMode.HALF_UP),
                maxima.setScale(2, RoundingMode.HALF_UP),
                chuvaAcumulada.setScale(2, RoundingMode.HALF_UP),
                ventoMaximo.setScale(2, RoundingMode.HALF_UP)
        );
    }
}
