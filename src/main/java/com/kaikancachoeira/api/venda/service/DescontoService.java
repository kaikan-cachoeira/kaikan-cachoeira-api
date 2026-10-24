package com.kaikancachoeira.api.venda.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import com.kaikancachoeira.api.evento.repository.ProdutoEventoRepository;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import com.kaikancachoeira.api.venda.dto.DescontoProdutoResponse;
import com.kaikancachoeira.api.venda.dto.DescontoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

@Service
public class DescontoService {

    private final EventoGuard eventoGuard;
    private final ProdutoEventoRepository produtoEventoRepository;
    private final Clock clock;

    public DescontoService(EventoGuard eventoGuard, ProdutoEventoRepository produtoEventoRepository, Clock clock) {
        this.eventoGuard = eventoGuard;
        this.produtoEventoRepository = produtoEventoRepository;
        this.clock = clock;
    }

    @Transactional
    public DescontoProdutoResponse aplicar(Long eventoId, Long produtoEventoId, DescontoRequest request) {
        eventoGuard.exigirEmAndamento(eventoId);
        ProdutoEvento produto = buscar(eventoId, produtoEventoId);

        BigDecimal novoPreco = request.novoPreco();
        if (novoPreco.compareTo(produto.getPrecoVenda()) > 0) {
            throw ValidacaoException.campo("novoPreco", "Novo preço não pode ser maior que o preço de venda");
        }
        if (produto.temDesconto()) {
            throw new ConflitoException("DESCONTO_JA_APLICADO",
                    "O produto já tem desconto aplicado. Remova o desconto antes de aplicar outro.");
        }
        if (novoPreco.compareTo(produto.getPrecoVenda()) == 0) {
            return DescontoProdutoResponse.de(produto);
        }

        produto.setDesconto(produto.getPrecoVenda().subtract(novoPreco).setScale(2, RoundingMode.HALF_UP));
        produto.setDescontoAplicadoEm(Instant.now(clock));
        return DescontoProdutoResponse.de(produto);
    }

    @Transactional
    public DescontoProdutoResponse remover(Long eventoId, Long produtoEventoId) {
        eventoGuard.exigirEmAndamento(eventoId);
        ProdutoEvento produto = buscar(eventoId, produtoEventoId);

        if (produto.temDesconto()) {
            produto.setDesconto(BigDecimal.ZERO);
            produto.setDescontoAplicadoEm(null);
        }
        return DescontoProdutoResponse.de(produto);
    }

    private ProdutoEvento buscar(Long eventoId, Long produtoEventoId) {
        return produtoEventoRepository.findByIdAndEventoId(produtoEventoId, eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto do evento", produtoEventoId));
    }
}
