package com.kaikancachoeira.api.doacao.entity;

import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;
import com.kaikancachoeira.api.doacao.enums.TipoDoacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "doacao")
@Getter
@Setter
@NoArgsConstructor
public class Doacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "produto_id")
    private Long produtoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoDoacao tipo;

    @Column(length = 150)
    private String nome;

    private Integer quantidade;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 50)
    private FormaPagamentoDoacao formaPagamento;

    @Column(nullable = false, length = 150)
    private String doador;

    @Column(name = "valor_ficha", precision = 10, scale = 2)
    private BigDecimal valorFicha;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
