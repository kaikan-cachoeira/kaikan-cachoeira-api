package com.kaikancachoeira.api.associados.entity;

import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "anuidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Anuidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associado_id", nullable = false)
    private Associado associado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configuracao_anuidade_id", nullable = false)
    private ConfiguracaoAnuidade configuracaoAnuidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusAnuidade status = StatusAnuidade.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @OneToMany(mappedBy = "anuidade", fetch = FetchType.LAZY)
    private Set<Pagamento> pagamentos = new HashSet<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

}
