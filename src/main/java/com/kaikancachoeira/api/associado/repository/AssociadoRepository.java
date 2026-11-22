package com.kaikancachoeira.api.associado.repository;

import com.kaikancachoeira.api.associado.entity.Associado;
import com.kaikancachoeira.api.associado.enums.TipoAssociado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssociadoRepository extends JpaRepository<Associado, Long>, AssociadoRepositoryCustom {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    Optional<Associado> findByIdAndTipo(Long id, TipoAssociado tipo);

    List<Associado> findAllByTipoOrderByNomeAsc(TipoAssociado tipo);

    List<Associado> findAllByOrderByNomeAsc();
}
