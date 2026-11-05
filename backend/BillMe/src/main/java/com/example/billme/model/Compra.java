package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Compra al proveedor ligada a una factura: da el costo real de sus líneas. */
@Entity
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_factura", length = 50)
    private String numeroFactura;

    @Column(name = "nota", columnDefinition = "TEXT")
    private String nota;
}
