package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

/** Proveedor al que se le compra (el principal, cuya lista se importa, y otros). */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor extends BaseEntity {

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
}
