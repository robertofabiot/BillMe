package com.example.billme.model;

import com.example.billme.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa a un usuario del sistema (ADMIN o VENDEDOR).
 * Spring Security usará esta entidad para autenticación.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity {

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * Contraseña almacenada como hash (BCrypt).
     * Nunca se devuelve en respuestas JSON.
     */
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
