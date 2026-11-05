package com.example.billme.repository;

import com.example.billme.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    Optional<Proveedor> findByNombre(String nombre);

    List<Proveedor> findByActivoTrueOrderByNombre();

    boolean existsByNombreIgnoreCase(String nombre);
}
