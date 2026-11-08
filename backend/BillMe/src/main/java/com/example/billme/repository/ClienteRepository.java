package com.example.billme.repository;

import com.example.billme.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findFirstByNombreIgnoreCase(String nombre);

    /**
     * Búsqueda rápida por nombre (case-insensitive, substring).
     * Utilizada en el autocompletado de la pantalla de facturación.
     */
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    @Query("SELECT c FROM Cliente c ORDER BY lower(c.nombre)")
    List<Cliente> ordenados();

    /**
     * Búsqueda por nombre o teléfono para el buscador general de clientes.
     */
    @Query("SELECT c FROM Cliente c WHERE " +
           "cast(function('translate', lower(c.nombre), 'áéíóúüñ', 'aeiouun') as string) LIKE CONCAT('%', :q, '%') OR " +
           "c.telefono LIKE CONCAT('%', :q, '%') ORDER BY lower(c.nombre)")
    List<Cliente> buscar(@Param("q") String q);
}
