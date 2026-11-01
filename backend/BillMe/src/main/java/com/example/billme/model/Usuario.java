package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Representa a un usuario del sistema. Todos tienen los mismos permisos (no hay roles).
 * Spring Security usará esta entidad para autenticación.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity implements UserDetails {

    /** Largo mínimo de una contraseña. */
    public static final int PASSWORD_MINIMO = 8;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * Contraseña almacenada como hash (BCrypt).
     * Nunca se devuelve en respuestas JSON.
     */
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /** Un usuario inactivo no puede iniciar sesión. */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // No manejamos expiración de cuenta
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // No manejamos bloqueo de cuenta
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // No manejamos expiración de credenciales
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
