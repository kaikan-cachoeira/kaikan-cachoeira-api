package com.kaikancachoeira.api.clima.entity;

import com.kaikancachoeira.api.evento.entity.Evento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "clima")
@Getter
@Setter
@NoArgsConstructor
public class Clima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "data_hora_prevista", nullable = false)
    private Instant dataHoraPrevista;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal temperatura;

    @Column(name = "sensacao_termica", nullable = false, precision = 5, scale = 2)
    private BigDecimal sensacaoTermica;

    @Column(name = "chuva_mm", nullable = false, precision = 7, scale = 2)
    private BigDecimal chuvaMm = BigDecimal.ZERO;

    @Column(name = "probabilidade_chuva", nullable = false)
    private Integer probabilidadeChuva = 0;

    @Column(name = "vento_velocidade", nullable = false, precision = 7, scale = 2)
    private BigDecimal ventoVelocidade = BigDecimal.ZERO;

    @Column(name = "codigo_clima", nullable = false)
    private Integer codigoClima;

    @Column(name = "descricao_clima", nullable = false, length = 255)
    private String descricaoClima;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
