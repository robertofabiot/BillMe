package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "consolidado_grupos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_grupos_consolidado_fecha", columnNames = {"consolidado_id", "fecha_entrega"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidadoGrupo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidado_id", nullable = false)
    private Consolidado consolidado;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
}
