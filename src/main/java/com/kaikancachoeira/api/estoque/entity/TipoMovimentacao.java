package com.kaikancachoeira.api.estoque.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tipo_movimentacao")
@Getter
@Setter
@NoArgsConstructor
public class TipoMovimentacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private Direcao direcao;

    @Column(name = "gerado_pelo_sistema", nullable = false)
    private Boolean geradoPeloSistema;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(name = "exige_custo", nullable = false)
    private Boolean exigeCusto;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}