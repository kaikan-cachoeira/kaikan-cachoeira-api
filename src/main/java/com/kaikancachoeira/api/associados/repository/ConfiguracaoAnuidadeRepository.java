package com.kaikancachoeira.api.associados.repository;

import com.kaikancachoeira.api.associados.entity.ConfiguracaoAnuidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfiguracaoAnuidadeRepository extends JpaRepository<ConfiguracaoAnuidade, Integer> {

    boolean existsByAno(Integer ano);

    boolean existsByAnoAndIdNot(Integer ano, Integer id);

    List<ConfiguracaoAnuidade> findAllByOrderByAnoAsc();
}
