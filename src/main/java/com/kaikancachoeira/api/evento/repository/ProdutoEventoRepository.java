package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoEventoRepository extends JpaRepository<ProdutoEvento, Long> {

    Optional<ProdutoEvento> findByIdAndEventoId(Long id, Long eventoId);

    List<ProdutoEvento> findByEventoIdAndIdIn(Long eventoId, Collection<Long> ids);

    List<ProdutoEvento> findByEventoIdOrderById(Long eventoId);

    @Query("""
            select pe from ProdutoEvento pe
            where pe.evento.id = :eventoId
              and (:q = '' or lower(pe.nome) like lower(concat('%', :q, '%')))
              and (:#{#categorias == null || #categorias.isEmpty()} = true
                   or pe.categoria in :categorias)
            order by pe.id
            """)
    List<ProdutoEvento> buscarPorEvento(
            @Param("eventoId") Long eventoId,
            @Param("q") String q,
            @Param("categorias") List<String> categorias);

    @Query("select distinct pe.categoria from ProdutoEvento pe where pe.evento.id = :eventoId and pe.categoria is not null order by pe.categoria")
    List<String> findCategoriasDistintas(@Param("eventoId") Long eventoId);

    boolean existsByEventoIdAndProdutoId(Long eventoId, Long produtoId);

    boolean existsByEventoIdAndNomeIgnoreCase(Long eventoId, String nome);

    boolean existsByEventoIdAndNomeIgnoreCaseAndIdNot(Long eventoId, String nome, Long id);

    @Query(value = """
            select coalesce(sum(iv.quantidade), 0)
            from item_venda iv
            join venda v on v.id = iv.venda_id
            where iv.produto_evento_id = :peId
              and v.cancelada_em is null
            """, nativeQuery = true)
    Integer quantidadeVendida(@Param("peId") Long peId);

    @Query(value = """
            select exists (
                select 1 from item_venda where produto_evento_id = :peId
            )
            """, nativeQuery = true)
    boolean foiVendido(@Param("peId") Long peId);

}
