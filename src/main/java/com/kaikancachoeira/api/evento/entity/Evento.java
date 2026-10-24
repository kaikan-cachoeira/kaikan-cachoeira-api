package com.kaikancachoeira.api.evento.entity;

import com.kaikancachoeira.api.evento.enums.TipoEvento;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private StatusEvento status;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 20)
    private TipoEvento tipoEvento = TipoEvento.EVENTO;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @NotNull
    @Column(name = "inicio_previsto_em", nullable = false)
    private Instant inicioPrevistoEm;

    @NotNull
    @Column(name = "encerramento_previsto_em", nullable = false)
    private Instant encerramentoPrevistoEm;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
