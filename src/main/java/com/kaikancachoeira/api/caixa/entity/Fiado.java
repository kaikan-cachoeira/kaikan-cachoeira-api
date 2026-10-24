package com.kaikancachoeira.api.caixa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fiado")
@Getter
@Setter
@NoArgsConstructor
public class Fiado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_caixa_id", nullable = false)
    private PagamentoCaixa lancamentoOrigem;

    @Column(name = "nome_devedor", nullable = false, length = 150)
    private String nomeDevedor;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private Boolean quitado = false;

    @Column(name = "quitado_em")
    private Instant quitadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 50)
    private FormaPagamento formaPagamento;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public void quitar(FormaPagamento forma, Instant agora) {
        this.quitado = true;
        this.quitadoEm = agora;
        this.formaPagamento = forma;
    }

    public void reabrir() {
        this.quitado = false;
        this.quitadoEm = null;
        this.formaPagamento = null;
    }
}
