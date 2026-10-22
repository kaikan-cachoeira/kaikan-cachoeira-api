package com.kaikancachoeira.api.estoque.repository;

import com.kaikancachoeira.api.estoque.entity.Produto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Query("""
            select p from Produto p left join fetch p.categoria
            where p.ativo = true and lower(p.nome) like lower(concat('%', :q, '%'))
            order by p.nome
            """)
    List<Produto> buscarAtivos(@Param("q") String q);

    Optional<Produto> findByIdAndAtivoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id and p.ativo = true")
    Optional<Produto> findByIdParaAtualizacao(@Param("id") Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByCategoriaId(Integer categoriaId);

    @Query(value = """
            select exists (select 1 from produto_evento where produto_id = :id)
                or exists (select 1 from doacao where produto_id = :id)
            """, nativeQuery = true)
    boolean possuiVinculoComEventoOuDoacao(@Param("id") Long id);
}