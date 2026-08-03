package com.example.billme.repository;

import com.example.billme.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Búsqueda rápida por nombre (case-insensitive, substring).
     * Utilizada en el autocompletado de la pantalla de facturación.
     */
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Búsqueda por nombre o teléfono para el buscador general de clientes.
     */
    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "c.telefono LIKE CONCAT('%', :q, '%')")
    List<Cliente> buscar(@Param("q") String q);
}
