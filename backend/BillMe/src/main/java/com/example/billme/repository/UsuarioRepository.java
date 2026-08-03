package com.example.billme.repository;

import com.example.billme.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** Búsqueda por username para autenticación con Spring Security. */
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
