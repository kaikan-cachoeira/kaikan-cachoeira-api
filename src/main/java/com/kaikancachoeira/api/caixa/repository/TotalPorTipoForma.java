package com.kaikancachoeira.api.caixa.repository;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;

import java.math.BigDecimal;

public record TotalPorTipoForma(TipoLancamento tipo, FormaPagamento formaPagamento, BigDecimal total) {}
