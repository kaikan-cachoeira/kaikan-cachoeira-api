package com.kaikancachoeira.api.doacao.repository;

import com.kaikancachoeira.api.doacao.entity.Doacao;
import com.kaikancachoeira.api.estoque.entity.Estoque;
import com.kaikancachoeira.api.estoque.entity.MovimentacaoEstoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoacaoRepository extends JpaRepository<Doacao, Long>, JpaSpecificationExecutor<Doacao> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doacao d where d.id = :id")
    Optional<Doacao> findByIdParaAtualizacao(@Param("id") Long id);

    @Query("select m from MovimentacaoEstoque m where m.doacaoId = :doacaoId")
    Optional<MovimentacaoEstoque> movimentacaoDaDoacao(@Param("doacaoId") Long doacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from Estoque e
            where e.id = (select m.estoque.id from MovimentacaoEstoque m where m.doacaoId = :doacaoId)
            """)
    Optional<Estoque> lotePorDoacaoParaAtualizacao(@Param("doacaoId") Long doacaoId);
}
