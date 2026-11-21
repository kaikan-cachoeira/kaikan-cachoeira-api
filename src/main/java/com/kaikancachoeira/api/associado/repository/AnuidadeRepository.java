package com.kaikancachoeira.api.associado.repository;

import com.kaikancachoeira.api.associado.entity.Anuidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnuidadeRepository extends JpaRepository<Anuidade, Long> {

    Optional<Anuidade> findByIdAndAssociadoId(Long id, Long associadoId);

    Optional<Anuidade> findByAssociadoIdAndConfiguracaoAnuidadeId(Long associadoId, Integer configuracaoAnuidadeId);

    List<Anuidade> findAllByConfiguracaoAnuidadeIdOrderByIdAsc(Integer configuracaoAnuidadeId);

    List<Anuidade> findAllByAssociadoIdOrderByIdAsc(Long associadoId);
}
