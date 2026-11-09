package com.kaikancachoeira.api.dashboard.service;

import com.kaikancachoeira.api.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private DashboardRepository repository;

    @Test
    void deveAplicarPeriodoPadraoDeSeisMesesQuandoNaoInformado() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
        DashboardService service = new DashboardService(repository, clock);

        when(repository.consultarResumo(any(), any()))
                .thenReturn(new com.kaikancachoeira.api.dashboard.dto.DashboardResumoResponse(
                        0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        service.consultarResumo(null, null);

        ArgumentCaptor<Instant> inicio = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> fim = ArgumentCaptor.forClass(Instant.class);
        verify(repository).consultarResumo(inicio.capture(), fim.capture());

        assertEquals(Instant.parse("2026-05-09T10:00:00Z"), inicio.getValue());
        assertEquals(Instant.parse("2026-11-09T10:00:00Z"), fim.getValue());
    }

    @Test
    void deveRejeitarLimiteInvalido() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
        DashboardService service = new DashboardService(repository, clock);

        assertThrows(com.kaikancachoeira.api.common.exception.ValidacaoException.class,
                () -> service.listarProdutosMaisVendidos(null, null, 0));

        verifyNoInteractions(repository);
    }
}
