package com.kaikancachoeira.api.associado.repository;

import com.kaikancachoeira.api.associado.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByIdAndAnuidadeId(Long id, Long anuidadeId);

    List<Pagamento> findAllByAnuidadeIdOrderByCriadoEmDesc(Long anuidadeId);

    List<Pagamento> findAllByAnuidade_AssociadoIdAndAnuidade_ConfiguracaoAnuidadeIdOrderByCriadoEmDesc(
            Long associadoId,
            Integer configuracaoAnuidadeId
    );

    boolean existsByAnuidade_ConfiguracaoAnuidadeId(Integer configuracaoAnuidadeId);

}
