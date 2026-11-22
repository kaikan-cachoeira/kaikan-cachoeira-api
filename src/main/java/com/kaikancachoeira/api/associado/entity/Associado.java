package com.kaikancachoeira.api.associado.entity;

import com.kaikancachoeira.api.associado.enums.TipoAssociado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "associado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Associado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id")
    private Associado titular;

    @OneToMany(mappedBy = "titular", fetch = FetchType.LAZY)
    private Set<Associado> dependentes = new HashSet<>();

    @Column(nullable = false, unique = true, length = 150)
    private String nome;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAssociado tipo;

    @Column(length = 20)
    private String numeroContato;

    @OneToMany(mappedBy = "associado", fetch = FetchType.LAZY)
    private Set<Anuidade> anuidades = new HashSet<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

}
