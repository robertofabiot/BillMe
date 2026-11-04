package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

/** Cómo estaba un documento antes de una edición (copia en JSON). */
@Entity
@Table(name = "documento_revisiones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoRevision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "snapshot", nullable = false, columnDefinition = "TEXT")
    private String snapshot;

    /** Quién hizo la edición. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
