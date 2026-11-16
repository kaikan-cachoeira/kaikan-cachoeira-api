package com.kaikancachoeira.api.associados.repository;

import com.kaikancachoeira.api.associados.entity.Associado;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
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
