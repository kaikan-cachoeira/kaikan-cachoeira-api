package com.kaikancachoeira.api.associados.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "configuracao_anuidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoAnuidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer ano;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @OneToMany(mappedBy = "configuracaoAnuidade", fetch = FetchType.LAZY)
    private Set<Anuidade> anuidades = new HashSet<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

}
