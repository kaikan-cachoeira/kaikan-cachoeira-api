package com.kaikancachoeira.api.relatorio.service;

import com.kaikancachoeira.api.caixa.dto.ResumoCaixaResponse;
import com.kaikancachoeira.api.caixa.service.ResumoCaixaService;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.evento.repository.EventoSpecifications;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import com.kaikancachoeira.api.relatorio.dto.CustoAdicionalRelatorio;
import com.kaikancachoeira.api.relatorio.dto.DoacoesRelatorio;
import com.kaikancachoeira.api.relatorio.dto.MovimentacaoRelatorio;
import com.kaikancachoeira.api.relatorio.dto.ProdutoRanking;
import com.kaikancachoeira.api.relatorio.dto.ProdutoRelatorio;
import com.kaikancachoeira.api.relatorio.dto.RankingProdutos;
import com.kaikancachoeira.api.relatorio.dto.RelatorioEventoResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioGeralResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioReuniaoResponse;
import com.kaikancachoeira.api.relatorio.dto.ResumoFinanceiro;
import com.kaikancachoeira.api.relatorio.dto.ResumoReuniao;
import com.kaikancachoeira.api.relatorio.dto.VendaHora;
import com.kaikancachoeira.api.relatorio.dto.VendasPorHora;
import com.kaikancachoeira.api.relatorio.repository.RelatorioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class RelatorioService {

    private static final String NAO_INICIADO = "NAO_INICIADO";
    private static final String ENCERRADO = "ENCERRADO";
    private static final String REABASTECIMENTO = "REABASTECIMENTO";
    private static final String DOACAO = "DOACAO";
    private static final String CONSUMO_INTERNO = "CONSUMO_INTERNO";
    private static final String PERDA = "PERDA";

    private final EventoGuard eventoGuard;
    private final EventoRepository eventoRepository;
    private final RelatorioRepository repository;
    private final ResumoCaixaService resumoCaixaService;
    private final String fusoHorario;

    public RelatorioService(EventoGuard eventoGuard,
                            EventoRepository eventoRepository,
                            RelatorioRepository repository,
                            ResumoCaixaService resumoCaixaService,
                            @Value("${app.timezone:America/Sao_Paulo}") String fusoHorario) {
        this.eventoGuard = eventoGuard;
        this.eventoRepository = eventoRepository;
        this.repository = repository;
        this.resumoCaixaService = resumoCaixaService;
        this.fusoHorario = ZoneId.of(fusoHorario).getId();
    }

    @Transactional(readOnly = true)
    public RelatorioResponse gerar(Long eventoId) {
        return gerar(eventoGuard.buscar(eventoId));
    }

    @Transactional(readOnly = true)
    public RelatorioGeralResponse gerarGeral(Instant dataInicio, Instant dataFim, String tipos) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw ValidacaoException.campo("dataInicio", "deve ser anterior ou igual a dataFim");
        }

        Specification<Evento> spec = Specification.<Evento>unrestricted()
                .and(EventoSpecifications.comStatus(Set.of(EventoGuard.EM_ANDAMENTO, ENCERRADO)))
                .and(EventoSpecifications.comTipos(parseTipos(tipos)));

        if (dataInicio != null) {
            spec = spec.and(EventoSpecifications.inicioA_partirDe(dataInicio));
        }
        if (dataFim != null) {
            spec = spec.and(EventoSpecifications.inicioAte(dataFim));
        }

        List<Evento> eventos = eventoRepository.findAll(
                spec,
                Sort.by(Sort.Order.asc("inicioPrevistoEm"), Sort.Order.asc("id")));
        List<RelatorioGeralResponse.Item> itens = eventos.stream()
                .map(this::resumirEvento)
                .toList();

        return new RelatorioGeralResponse(
                itens.stream().filter(item -> item.tipoEvento() == TipoEvento.EVENTO).count(),
                itens.stream().filter(item -> item.tipoEvento() == TipoEvento.REUNIAO).count(),
                itens.stream().mapToLong(RelatorioGeralResponse.Item::quantidadeVendas).sum(),
                somar(itens, RelatorioGeralResponse.Item::receitaVendas),
                somar(itens, RelatorioGeralResponse.Item::doacoesEmDinheiro),
                somar(itens, RelatorioGeralResponse.Item::receitaTotal),
                somar(itens, RelatorioGeralResponse.Item::custoFichasAgradecimento),
                somar(itens, RelatorioGeralResponse.Item::custoTotal),
                somar(itens, RelatorioGeralResponse.Item::lucro),
                itens);
    }

    private RelatorioResponse gerar(Evento evento) {
        String status = evento.getStatus().getCodigo();

        if (NAO_INICIADO.equals(status)) {
            throw new ConflitoException("EVENTO_NAO_INICIADO",
                    "O relatório só fica disponível depois que o evento é iniciado.");
        }

        boolean parcial = EventoGuard.EM_ANDAMENTO.equals(status);

        return evento.getTipoEvento() == TipoEvento.REUNIAO
                ? relatorioDeReuniao(evento, parcial)
                : relatorioDeEvento(evento, parcial);
    }

    private RelatorioGeralResponse.Item resumirEvento(Evento evento) {
        boolean parcial = EventoGuard.EM_ANDAMENTO.equals(evento.getStatus().getCodigo());
        StatusResumo status = StatusResumo.de(evento.getStatus());
        if (evento.getTipoEvento() == TipoEvento.EVENTO) {
            ResumoFinanceiro resumo = calcularResumoFinanceiro(evento.getId());
            return new RelatorioGeralResponse.Item(
                    evento.getId(),
                    evento.getNome(),
                    TipoEvento.EVENTO,
                    status,
                    evento.getInicioPrevistoEm(),
                    parcial,
                    resumo.vendas(),
                    resumo.receitaVendas(),
                    resumo.doacoesEmDinheiro(),
                    resumo.receitaTotal(),
                    resumo.custoFichasAgradecimento(),
                    resumo.custoTotal(),
                    resumo.lucro());
        }

        BigDecimal custos = calcularResumoReuniao(evento.getId()).custoTotal();
        return new RelatorioGeralResponse.Item(
                evento.getId(),
                evento.getNome(),
                TipoEvento.REUNIAO,
                status,
                evento.getInicioPrevistoEm(),
                parcial,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                custos,
                custos.negate());
    }

    private Set<TipoEvento> parseTipos(String tipos) {
        if (tipos == null || tipos.isBlank()) {
            return EnumSet.allOf(TipoEvento.class);
        }

        Set<TipoEvento> resultado = EnumSet.noneOf(TipoEvento.class);
        for (String tipo : tipos.split(",")) {
            if (tipo.isBlank()) {
                continue;
            }
            try {
                resultado.add(TipoEvento.valueOf(tipo.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw ValidacaoException.campo("tipos", "deve conter EVENTO e/ou REUNIAO");
            }
        }
        return resultado.isEmpty() ? EnumSet.allOf(TipoEvento.class) : resultado;
    }

    private BigDecimal somar(
            List<RelatorioGeralResponse.Item> itens,
            Function<RelatorioGeralResponse.Item, BigDecimal> valor) {
        return itens.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RelatorioEventoResponse relatorioDeEvento(Evento evento, boolean parcial) {
        Long id = evento.getId();

        ResumoFinanceiro resumo = calcularResumoFinanceiro(id);

        List<ProdutoRelatorio> produtos = repository.produtos(id);
        List<CustoAdicionalRelatorio> listaCustos = repository.custosAdicionais(id);
        ResumoCaixaResponse caixa = resumoCaixaService.calcular(id);

        DoacoesRelatorio doacoes = new DoacoesRelatorio(
                repository.doacoesDeItens(id),
                repository.doacoesEmDinheiroDetalhadas(id),
                resumo.doacoesEmDinheiro());

        List<MovimentacaoRelatorio> consumoEPerdas =
                repository.movimentacoes(id, List.of(CONSUMO_INTERNO, PERDA));

        return new RelatorioEventoResponse(
                id,
                evento.getNome(),
                evento.getTipoEvento(),
                StatusResumo.de(evento.getStatus()),
                parcial,
                resumo,
                produtos,
                listaCustos,
                caixa,
                doacoes,
                consumoEPerdas,
                ranking(produtos, repository.categoriasDoCardapio(id)),
                vendasPorHora(id));
    }

    private RelatorioReuniaoResponse relatorioDeReuniao(Evento evento, boolean parcial) {
        Long id = evento.getId();

        ResumoReuniao resumo = calcularResumoReuniao(id);

        return new RelatorioReuniaoResponse(
                id,
                evento.getNome(),
                evento.getTipoEvento(),
                StatusResumo.de(evento.getStatus()),
                parcial,
                resumo,
                repository.movimentacoes(id, List.of(REABASTECIMENTO, DOACAO, CONSUMO_INTERNO)),
                repository.custosAdicionais(id));
    }

    private ResumoFinanceiro calcularResumoFinanceiro(Long eventoId) {
        BigDecimal receitaVendas = repository.receitaVendas(eventoId);
        BigDecimal doacoesEmDinheiro = repository.doacoesEmDinheiro(eventoId);
        BigDecimal custoCompras = repository.custoDeMovimentacoes(eventoId, REABASTECIMENTO);
        BigDecimal custoFichas = repository.custoFichasAgradecimento(eventoId);
        BigDecimal custoIngredientes = repository.custoIngredientes(eventoId);
        BigDecimal custosAdicionais = repository.totalCustosAdicionais(eventoId);

        BigDecimal receitaTotal = receitaVendas.add(doacoesEmDinheiro);
        BigDecimal custoTotal = custoCompras.add(custoFichas).add(custoIngredientes).add(custosAdicionais);

        return new ResumoFinanceiro(
                receitaVendas,
                repository.descontosVendas(eventoId),
                custoCompras,
                custoFichas,
                custoIngredientes,
                custosAdicionais,
                doacoesEmDinheiro,
                receitaTotal,
                custoTotal,
                receitaTotal.subtract(custoTotal),
                repository.contarVendas(eventoId));
    }

    private ResumoReuniao calcularResumoReuniao(Long eventoId) {
        BigDecimal compras = repository.custoDeMovimentacoes(eventoId, REABASTECIMENTO);
        BigDecimal consumo = repository.custoDeMovimentacoes(eventoId, CONSUMO_INTERNO);
        BigDecimal custosAdicionais = repository.totalCustosAdicionais(eventoId);

        return new ResumoReuniao(
                compras,
                consumo,
                custosAdicionais,
                compras.add(consumo).add(custosAdicionais));
    }

    private RankingProdutos ranking(List<ProdutoRelatorio> produtos, Map<Long, String> categorias) {
        List<ProdutoRanking> ranking = produtos.stream()
                .filter(p -> p.quantidadeVendida() > 0)
                .sorted(Comparator.comparingLong(ProdutoRelatorio::quantidadeVendida).reversed()
                        .thenComparing(ProdutoRelatorio::nome))
                .map(p -> new ProdutoRanking(
                        p.produtoEventoId(),
                        p.nome(),
                        categorias.get(p.produtoEventoId()),
                        p.quantidadeVendida()))
                .toList();

        long total = produtos.stream().mapToLong(ProdutoRelatorio::quantidadeVendida).sum();
        return new RankingProdutos(total, ranking);
    }

    private VendasPorHora vendasPorHora(Long eventoId) {
        List<VendaHora> horas = repository.vendasPorHora(eventoId, fusoHorario);
        long total = horas.stream().mapToLong(VendaHora::quantidadeVendas).sum();
        return new VendasPorHora(total, horas);
    }
}
