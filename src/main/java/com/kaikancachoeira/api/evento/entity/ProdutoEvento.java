package com.kaikancachoeira.api.evento.entity;

import com.kaikancachoeira.api.estoque.entity.Produto;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "produto_evento")
@Getter
@Setter
@NoArgsConstructor
public class ProdutoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrigemProdutoEvento origem;

    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "quantidade_prevista")
    private Integer quantidadePrevista;

    @Column(name = "custo_total_ingredientes", precision = 10, scale = 2)
    private BigDecimal custoTotalIngredientes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "desconto_aplicado_em")
    private Instant descontoAplicadoEm;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 100)
    private String categoria;

    @Column(name = "foto_url", length = 64)
    private String fotoUrl;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public BigDecimal getPrecoComDesconto() {
        return precoVenda.subtract(desconto);
    }

    public boolean temDesconto() {
        return desconto.signum() > 0;
    }
}
