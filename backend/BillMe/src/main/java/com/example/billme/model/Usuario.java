package com.example.billme.model;

import com.example.billme.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

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
public class Usuario extends BaseEntity implements UserDetails {

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
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
        return true; // Siempre habilitado por defecto
    }
}
