package com.kaikancachoeira.api.relatorio.service;

import com.kaikancachoeira.api.caixa.dto.ResumoCaixaResponse;
import com.kaikancachoeira.api.caixa.service.ResumoCaixaService;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.evento.dto.StatusResumo;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import com.kaikancachoeira.api.relatorio.dto.CustoAdicionalRelatorio;
import com.kaikancachoeira.api.relatorio.dto.DoacoesRelatorio;
import com.kaikancachoeira.api.relatorio.dto.MovimentacaoRelatorio;
import com.kaikancachoeira.api.relatorio.dto.ProdutoRanking;
import com.kaikancachoeira.api.relatorio.dto.ProdutoRelatorio;
import com.kaikancachoeira.api.relatorio.dto.RankingProdutos;
import com.kaikancachoeira.api.relatorio.dto.RelatorioEventoResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioResponse;
import com.kaikancachoeira.api.relatorio.dto.RelatorioReuniaoResponse;
import com.kaikancachoeira.api.relatorio.dto.ResumoFinanceiro;
import com.kaikancachoeira.api.relatorio.dto.ResumoReuniao;
import com.kaikancachoeira.api.relatorio.dto.VendaHora;
import com.kaikancachoeira.api.relatorio.dto.VendasPorHora;
import com.kaikancachoeira.api.relatorio.repository.RelatorioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private static final String NAO_INICIADO = "NAO_INICIADO";
    private static final String REABASTECIMENTO = "REABASTECIMENTO";
    private static final String DOACAO = "DOACAO";
    private static final String CONSUMO_INTERNO = "CONSUMO_INTERNO";
    private static final String PERDA = "PERDA";

    private final EventoGuard eventoGuard;
    private final RelatorioRepository repository;
    private final ResumoCaixaService resumoCaixaService;
    private final String fusoHorario;

    public RelatorioService(EventoGuard eventoGuard,
                            RelatorioRepository repository,
                            ResumoCaixaService resumoCaixaService,
                            @Value("${app.timezone:America/Sao_Paulo}") String fusoHorario) {
        this.eventoGuard = eventoGuard;
        this.repository = repository;
        this.resumoCaixaService = resumoCaixaService;
        this.fusoHorario = ZoneId.of(fusoHorario).getId();
    }

    @Transactional(readOnly = true)
    public RelatorioResponse gerar(Long eventoId) {
        Evento evento = eventoGuard.buscar(eventoId);
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

    private RelatorioEventoResponse relatorioDeEvento(Evento evento, boolean parcial) {
        Long id = evento.getId();

        BigDecimal receitaVendas = repository.receitaVendas(id);
        BigDecimal doacoesEmDinheiro = repository.doacoesEmDinheiro(id);
        BigDecimal custoCompras = repository.custoDeMovimentacoes(id, REABASTECIMENTO);
        BigDecimal custoFichas = repository.custoFichasAgradecimento(id);
        BigDecimal custoIngredientes = repository.custoIngredientes(id);
        BigDecimal custosAdicionais = repository.totalCustosAdicionais(id);

        BigDecimal receitaTotal = receitaVendas.add(doacoesEmDinheiro);
        BigDecimal custoTotal = custoCompras.add(custoFichas).add(custoIngredientes).add(custosAdicionais);

        ResumoFinanceiro resumo = new ResumoFinanceiro(
                receitaVendas,
                repository.descontosVendas(id),
                custoCompras,
                custoFichas,
                custoIngredientes,
                custosAdicionais,
                doacoesEmDinheiro,
                receitaTotal,
                custoTotal,
                receitaTotal.subtract(custoTotal),
                repository.contarVendas(id));

        List<ProdutoRelatorio> produtos = repository.produtos(id);
        List<CustoAdicionalRelatorio> listaCustos = repository.custosAdicionais(id);
        ResumoCaixaResponse caixa = resumoCaixaService.calcular(id);

        DoacoesRelatorio doacoes = new DoacoesRelatorio(
                repository.doacoesDeItens(id),
                repository.doacoesEmDinheiroDetalhadas(id),
                doacoesEmDinheiro);

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

        BigDecimal compras = repository.custoDeMovimentacoes(id, REABASTECIMENTO);
        BigDecimal consumo = repository.custoDeMovimentacoes(id, CONSUMO_INTERNO);
        BigDecimal custosAdicionais = repository.totalCustosAdicionais(id);

        ResumoReuniao resumo = new ResumoReuniao(
                compras,
                consumo,
                custosAdicionais,
                compras.add(consumo).add(custosAdicionais));

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
