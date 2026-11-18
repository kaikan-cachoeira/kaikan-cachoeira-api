package com.kaikancachoeira.api.dashboard.repository;

import com.kaikancachoeira.api.dashboard.dto.DashboardEstoqueResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardEventoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardProdutoAlertaResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardProdutoMaisVendidoResponse;
import com.kaikancachoeira.api.dashboard.dto.DashboardResumoResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    private static final String SQL_RESUMO = """
            with eventos_periodo as (
                select e.id
                from evento e
                where e.tipo_evento = 'EVENTO'
                  and e.iniciado_em >= :dataInicio
                  and e.iniciado_em <= :dataFim
            ),
            total_eventos as (
                select count(*) as total from eventos_periodo
            ),
            receita_vendas as (
                select coalesce(sum(v.preco_total), 0) as total
                from venda v
                join eventos_periodo ep on ep.id = v.evento_id
                where v.cancelada_em is null
            ),
            receita_doacoes as (
                select coalesce(sum(d.valor), 0) as total
                from doacao d
                join eventos_periodo ep on ep.id = d.evento_id
                where d.tipo = 'DINHEIRO'
            ),
            custo_compras as (
                select coalesce(sum(abs(m.quantidade) * m.custo_unitario), 0) as total
                from movimentacao_estoque m
                join eventos_periodo ep on ep.id = m.evento_id
                join tipo_movimentacao tm on tm.id = m.tipo_movimentacao_id
                where tm.codigo = 'REABASTECIMENTO'
            ),
            custo_fichas as (
                select coalesce(sum(d.valor_ficha), 0) as total
                from doacao d
                join eventos_periodo ep on ep.id = d.evento_id
            ),
            custo_ingredientes as (
                select coalesce(sum(pe.custo_total_ingredientes), 0) as total
                from produto_evento pe
                join eventos_periodo ep on ep.id = pe.evento_id
                where pe.origem = 'PRODUZIDO'
            ),
            custos_adicionais as (
                select coalesce(sum(ce.valor), 0) as total
                from custo_evento ce
                join eventos_periodo ep on ep.id = ce.evento_id
            )
            select
                te.total as total_eventos,
                (rv.total + rd.total) as receita_total,
                (cc.total + cf.total + ci.total + ca.total) as custo_total,
                (rv.total + rd.total - cc.total - cf.total - ci.total - ca.total) as lucro_total
            from total_eventos te
            cross join receita_vendas rv
            cross join receita_doacoes rd
            cross join custo_compras cc
            cross join custo_fichas cf
            cross join custo_ingredientes ci
            cross join custos_adicionais ca
            """;

    private static final String SQL_EVENTOS = """
            with eventos_periodo as (
                select e.id, e.nome, e.iniciado_em
                from evento e
                where e.tipo_evento = 'EVENTO'
                  and e.iniciado_em >= :dataInicio
                  and e.iniciado_em <= :dataFim
            )
            select
                ep.id as evento_id,
                ep.nome,
                ep.iniciado_em,
                coalesce((
                    select sum(v.preco_total)
                    from venda v
                    where v.evento_id = ep.id
                      and v.cancelada_em is null
                ), 0)
                + coalesce((
                    select sum(d.valor)
                    from doacao d
                    where d.evento_id = ep.id
                      and d.tipo = 'DINHEIRO'
                ), 0) as receita,
                coalesce((
                    select sum(abs(m.quantidade) * m.custo_unitario)
                    from movimentacao_estoque m
                    join tipo_movimentacao tm on tm.id = m.tipo_movimentacao_id
                    where m.evento_id = ep.id
                      and tm.codigo = 'REABASTECIMENTO'
                ), 0)
                + coalesce((
                    select sum(d.valor_ficha)
                    from doacao d
                    where d.evento_id = ep.id
                ), 0)
                + coalesce((
                    select sum(pe.custo_total_ingredientes)
                    from produto_evento pe
                    where pe.evento_id = ep.id
                      and pe.origem = 'PRODUZIDO'
                ), 0)
                + coalesce((
                    select sum(ce.valor)
                    from custo_evento ce
                    where ce.evento_id = ep.id
                ), 0) as custo
            from eventos_periodo ep
            order by ep.iniciado_em asc, ep.id asc
            """;

    private static final String SQL_PRODUTOS = """
            with eventos_periodo as (
                select e.id
                from evento e
                where e.tipo_evento = 'EVENTO'
                  and e.iniciado_em >= :dataInicio
                  and e.iniciado_em <= :dataFim
            )
            select
                pe.id as produto_id,
                pe.nome,
                coalesce(sum(iv.quantidade), 0) as quantidade,
                coalesce(sum(iv.quantidade * (iv.preco_unitario - iv.desconto_unitario)), 0) as receita
            from item_venda iv
            join venda v on v.id = iv.venda_id and v.cancelada_em is null
            join produto_evento pe on pe.id = iv.produto_evento_id
            join eventos_periodo ep on ep.id = v.evento_id
            group by pe.id, pe.nome
            order by receita desc, pe.nome asc, pe.id asc
            limit :limite
            """;

    private static final String SQL_VALOR_ESTOQUE = """
            select coalesce(sum(e.quantidade_atual * e.custo_unitario), 0)
            from estoque e
            where e.custo_unitario > 0
            """;

    private static final String SQL_ABAIXO_MINIMO = """
            select p.id, p.nome, p.quantidade_atual, p.estoque_minimo
            from produto p
            where p.ativo = true
              and p.estoque_minimo is not null
              and p.quantidade_atual < p.estoque_minimo
            order by p.quantidade_atual asc, p.nome asc, p.id asc
            """;

    private static final String SQL_NEGATIVOS = """
            select p.id, p.nome, p.quantidade_atual, p.estoque_minimo
            from produto p
            where p.ativo = true
              and p.quantidade_atual < 0
            order by p.quantidade_atual asc, p.nome asc, p.id asc
            """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public DashboardResumoResponse consultarResumo(Instant dataInicio, Instant dataFim) {
        Object[] linha = linhaUnica(SQL_RESUMO, dataInicio, dataFim);

        long totalEventos = numero(linha[0]).longValue();
        BigDecimal receitaTotal = dinheiro(linha[1]);
        BigDecimal custoTotal = dinheiro(linha[2]);
        BigDecimal lucroTotal = dinheiro(linha[3]);

        return new DashboardResumoResponse(totalEventos, receitaTotal, custoTotal, lucroTotal);
    }

    @Override
    public List<DashboardEventoResponse> listarEventos(Instant dataInicio, Instant dataFim) {
        return linhas(SQL_EVENTOS, dataInicio, dataFim).stream()
                .map(l -> {
                    Long eventoId = numero(l[0]).longValue();
                    String nome = (String) l[1];
                    Instant data = instante(l[2]);
                    BigDecimal receita = dinheiro(l[3]);
                    BigDecimal custo = dinheiro(l[4]);
                    BigDecimal lucro = receita.subtract(custo).setScale(2, RoundingMode.HALF_UP);
                    return new DashboardEventoResponse(eventoId, nome, data, receita, custo, lucro);
                })
                .toList();
    }

    @Override
    public List<DashboardProdutoMaisVendidoResponse> listarProdutosMaisVendidos(
            Instant dataInicio,
            Instant dataFim,
            int limite
    ) {
        List<Object[]> linhas = linhas(SQL_PRODUTOS, dataInicio, dataFim, limite);
        if (linhas.isEmpty()) {
            return List.of();
        }

        BigDecimal receitaTotal = linhas.stream()
                .map(l -> dinheiro(l[3]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receitaAcumulada = BigDecimal.ZERO;
        java.util.ArrayList<DashboardProdutoMaisVendidoResponse> resposta = new java.util.ArrayList<>();

        for (Object[] linha : linhas) {
            Long produtoId = numero(linha[0]).longValue();
            String nome = (String) linha[1];
            long quantidade = numero(linha[2]).longValue();
            BigDecimal receita = dinheiro(linha[3]);

            receitaAcumulada = receitaAcumulada.add(receita);
            BigDecimal participacao = percentual(receita, receitaTotal);
            BigDecimal participacaoAcumulada = percentual(receitaAcumulada, receitaTotal);

            resposta.add(new DashboardProdutoMaisVendidoResponse(
                    produtoId,
                    nome,
                    quantidade,
                    receita,
                    participacao,
                    participacaoAcumulada
            ));
        }

        return resposta;
    }

    @Override
    public DashboardEstoqueResponse consultarEstoque() {
        BigDecimal valorEstoque = dinheiro(consulta(SQL_VALOR_ESTOQUE).getSingleResult());

        List<DashboardProdutoAlertaResponse> abaixoDoMinimo = linhas(SQL_ABAIXO_MINIMO).stream()
                .map(this::mapearAlertaEstoque)
                .toList();

        List<DashboardProdutoAlertaResponse> negativos = linhas(SQL_NEGATIVOS).stream()
                .map(this::mapearAlertaEstoque)
                .toList();

        return new DashboardEstoqueResponse(valorEstoque, abaixoDoMinimo, negativos);
    }

    private DashboardProdutoAlertaResponse mapearAlertaEstoque(Object[] linha) {
        return new DashboardProdutoAlertaResponse(
                numero(linha[0]).longValue(),
                (String) linha[1],
                linha[2] == null ? null : numero(linha[2]).intValue(),
                linha[3] == null ? null : numero(linha[3]).intValue()
        );
    }

    private Query consulta(String sql, Object... parametros) {
        Query query = em.createNativeQuery(sql);
        if (parametros.length > 0) {
            query.setParameter("dataInicio", Timestamp.from((Instant) parametros[0]));
        }
        if (parametros.length > 1) {
            query.setParameter("dataFim", Timestamp.from((Instant) parametros[1]));
        }
        if (parametros.length > 2) {
            query.setParameter("limite", parametros[2]);
        }
        return query;
    }

    private Object[] linhaUnica(String sql, Instant dataInicio, Instant dataFim) {
        return (Object[]) consulta(sql, dataInicio, dataFim).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> linhas(String sql, Instant dataInicio, Instant dataFim, int limite) {
        return (List<Object[]>) consulta(sql, dataInicio, dataFim, limite).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> linhas(String sql, Instant dataInicio, Instant dataFim) {
        return (List<Object[]>) consulta(sql, dataInicio, dataFim).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> linhas(String sql) {
        return (List<Object[]>) consulta(sql).getResultList();
    }

    private static BigDecimal percentual(BigDecimal valor, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return valor.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinheiro(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal decimal = valor instanceof BigDecimal d ? d : new BigDecimal(valor.toString());
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    private static Number numero(Object valor) {
        return (Number) valor;
    }

    private static Instant instante(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Instant i) {
            return i;
        }
        if (valor instanceof OffsetDateTime o) {
            return o.toInstant();
        }
        if (valor instanceof ZonedDateTime z) {
            return z.toInstant();
        }
        if (valor instanceof LocalDateTime l) {
            return l.toInstant(java.time.ZoneOffset.UTC);
        }
        if (valor instanceof Timestamp t) {
            return t.toInstant();
        }
        throw new IllegalStateException("Tipo de data não suportado: " + valor.getClass());
    }
}
