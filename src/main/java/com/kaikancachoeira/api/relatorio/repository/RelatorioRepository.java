package com.kaikancachoeira.api.relatorio.repository;

import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;
import com.kaikancachoeira.api.doacao.enums.TipoDoacao;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import com.kaikancachoeira.api.relatorio.dto.CustoAdicionalRelatorio;
import com.kaikancachoeira.api.relatorio.dto.DoacaoDinheiroRelatorio;
import com.kaikancachoeira.api.relatorio.dto.DoacaoItemRelatorio;
import com.kaikancachoeira.api.relatorio.dto.MovimentacaoRelatorio;
import com.kaikancachoeira.api.relatorio.dto.ProdutoRelatorio;
import com.kaikancachoeira.api.relatorio.dto.VendaHora;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RelatorioRepository {

    @PersistenceContext
    private EntityManager em;

    public BigDecimal receitaVendas(Long eventoId) {
        return somar("""
                select coalesce(sum(preco_total), 0) from venda
                where evento_id = :id and cancelada_em is null
                """, eventoId);
    }

    public BigDecimal descontosVendas(Long eventoId) {
        return somar("""
                select coalesce(sum(desconto), 0) from venda
                where evento_id = :id and cancelada_em is null
                """, eventoId);
    }

    public long contarVendas(Long eventoId) {
        Object resultado = consulta("""
                select count(*) from venda
                where evento_id = :id and cancelada_em is null
                """, eventoId).getSingleResult();
        return ((Number) resultado).longValue();
    }

    public BigDecimal custoDeMovimentacoes(Long eventoId, String codigoTipo) {
        Object resultado = consulta("""
                select coalesce(sum(abs(m.quantidade) * m.custo_unitario), 0)
                from movimentacao_estoque m
                join tipo_movimentacao t on t.id = m.tipo_movimentacao_id
                where m.evento_id = :id and t.codigo = :codigo
                """, eventoId).setParameter("codigo", codigoTipo).getSingleResult();
        return dinheiro(resultado);
    }

    public BigDecimal custoFichasAgradecimento(Long eventoId) {
        return somar("select coalesce(sum(valor_ficha), 0) from doacao where evento_id = :id", eventoId);
    }

    public BigDecimal custoIngredientes(Long eventoId) {
        return somar("""
                select coalesce(sum(custo_total_ingredientes), 0) from produto_evento
                where evento_id = :id and origem = 'PRODUZIDO'
                """, eventoId);
    }

    public BigDecimal totalCustosAdicionais(Long eventoId) {
        return somar("select coalesce(sum(valor), 0) from custo_evento where evento_id = :id", eventoId);
    }

    public BigDecimal doacoesEmDinheiro(Long eventoId) {
        return somar("""
                select coalesce(sum(valor), 0) from doacao
                where evento_id = :id and tipo = 'DINHEIRO'
                """, eventoId);
    }

    public List<CustoAdicionalRelatorio> custosAdicionais(Long eventoId) {
        return linhas("""
                select nome, descricao, valor from custo_evento
                where evento_id = :id order by id
                """, eventoId).stream()
                .map(l -> new CustoAdicionalRelatorio((String) l[0], (String) l[1], dinheiro(l[2])))
                .toList();
    }

    public List<ProdutoRelatorio> produtos(Long eventoId) {
        Map<Long, BigDecimal> custoEstoque = new HashMap<>();
        for (Object[] l : linhas("""
                select iv.produto_evento_id,
                       coalesce(sum(abs(m.quantidade) * m.custo_unitario), 0)
                from movimentacao_estoque m
                join tipo_movimentacao t on t.id = m.tipo_movimentacao_id and t.codigo = 'VENDA'
                join item_venda iv on iv.id = m.item_venda_id
                join venda v on v.id = iv.venda_id and v.cancelada_em is null
                join produto_evento pe on pe.id = iv.produto_evento_id and pe.evento_id = :id
                group by iv.produto_evento_id
                """, eventoId)) {
            custoEstoque.put(((Number) l[0]).longValue(), dinheiro(l[1]));
        }

        return linhas("""
                select pe.id, pe.nome, pe.origem, pe.quantidade_prevista, pe.preco_venda,
                       pe.custo_total_ingredientes,
                       coalesce(sum(iv.quantidade), 0),
                       coalesce(sum(iv.quantidade * iv.desconto_unitario), 0),
                       coalesce(sum(iv.quantidade * (iv.preco_unitario - iv.desconto_unitario)), 0)
                from produto_evento pe
                left join item_venda iv on iv.produto_evento_id = pe.id
                    and exists (select 1 from venda v where v.id = iv.venda_id and v.cancelada_em is null)
                where pe.evento_id = :id
                group by pe.id
                order by pe.nome, pe.id
                """, eventoId).stream().map(l -> {
            Long id = ((Number) l[0]).longValue();
            OrigemProdutoEvento origem = OrigemProdutoEvento.valueOf((String) l[2]);
            Integer prevista = l[3] == null ? null : ((Number) l[3]).intValue();
            BigDecimal preco = dinheiro(l[4]);
            long vendida = ((Number) l[6]).longValue();
            BigDecimal descontos = dinheiro(l[7]);
            BigDecimal bruto = dinheiro(l[8]);
            BigDecimal custo = origem == OrigemProdutoEvento.PRODUZIDO
                    ? dinheiro(l[5])
                    : custoEstoque.getOrDefault(id, dinheiro(null));
            return new ProdutoRelatorio(id, (String) l[1], origem, prevista, vendida, descontos,
                    custo, bruto, bruto, bruto.subtract(custo), preco);
        }).toList();
    }

    public Map<Long, String> categoriasDoCardapio(Long eventoId) {
        Map<Long, String> categorias = new HashMap<>();
        for (Object[] l : linhas("select id, categoria from produto_evento where evento_id = :id", eventoId)) {
            categorias.put(((Number) l[0]).longValue(), (String) l[1]);
        }
        return categorias;
    }

    public List<DoacaoItemRelatorio> doacoesDeItens(Long eventoId) {
        return linhas("""
                select tipo, produto_id, nome, quantidade, doador, valor_ficha from doacao
                where evento_id = :id and tipo in ('PRODUTO', 'REABASTECIMENTO')
                order by criado_em, id
                """, eventoId).stream()
                .map(l -> new DoacaoItemRelatorio(
                        TipoDoacao.valueOf((String) l[0]),
                        l[1] == null ? null : ((Number) l[1]).longValue(),
                        (String) l[2],
                        l[3] == null ? null : ((Number) l[3]).intValue(),
                        (String) l[4],
                        l[5] == null ? null : dinheiro(l[5])))
                .toList();
    }

    public List<DoacaoDinheiroRelatorio> doacoesEmDinheiroDetalhadas(Long eventoId) {
        return linhas("""
                select doador, valor, forma_pagamento, valor_ficha from doacao
                where evento_id = :id and tipo = 'DINHEIRO'
                order by criado_em, id
                """, eventoId).stream()
                .map(l -> new DoacaoDinheiroRelatorio(
                        (String) l[0],
                        dinheiro(l[1]),
                        FormaPagamentoDoacao.valueOf((String) l[2]),
                        l[3] == null ? null : dinheiro(l[3])))
                .toList();
    }

    public List<MovimentacaoRelatorio> movimentacoes(Long eventoId, Collection<String> codigosTipo) {
        return executar(consulta("""
                select m.id, t.codigo, p.id, p.nome, abs(m.quantidade), m.custo_unitario,
                       m.custo_total, m.observacoes, m.criado_em
                from movimentacao_estoque m
                join tipo_movimentacao t on t.id = m.tipo_movimentacao_id
                join estoque e on e.id = m.estoque_id
                join produto p on p.id = e.produto_id
                where m.evento_id = :id and t.codigo in (:codigos)
                order by m.criado_em, m.id
                """, eventoId).setParameter("codigos", codigosTipo))
                .stream()
                .map(l -> new MovimentacaoRelatorio(
                        ((Number) l[0]).longValue(),
                        (String) l[1],
                        ((Number) l[2]).longValue(),
                        (String) l[3],
                        ((Number) l[4]).intValue(),
                        dinheiro(l[5]),
                        dinheiro(l[6]),
                        (String) l[7],
                        instante(l[8])))
                .toList();
    }

    public List<VendaHora> vendasPorHora(Long eventoId, String fusoHorario) {
        return executar(consulta("""
                select hora, count(*) from (
                    select cast(extract(hour from (v.criado_em at time zone cast(:zona as varchar))) as integer) as hora
                    from venda v
                    where v.evento_id = :id and v.cancelada_em is null
                ) vendas_com_hora
                group by hora
                order by hora
                """, eventoId).setParameter("zona", fusoHorario))
                .stream()
                .map(l -> new VendaHora(((Number) l[0]).intValue(), ((Number) l[1]).longValue()))
                .toList();
    }

    private BigDecimal somar(String sql, Long eventoId) {
        return dinheiro(consulta(sql, eventoId).getSingleResult());
    }

    private Query consulta(String sql, Long eventoId) {
        return em.createNativeQuery(sql).setParameter("id", eventoId);
    }

    private List<Object[]> linhas(String sql, Long eventoId) {
        return executar(consulta(sql, eventoId));
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> executar(Query consulta) {
        return (List<Object[]>) consulta.getResultList();
    }

    private static BigDecimal dinheiro(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal decimal = valor instanceof BigDecimal d ? d : new BigDecimal(valor.toString());
        return decimal.setScale(2, RoundingMode.HALF_UP);
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
        if (valor instanceof Timestamp t) {
            return t.toInstant();
        }
        throw new IllegalStateException("Tipo de data não suportado: " + valor.getClass());
    }
}
