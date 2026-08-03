package com.example.billme.repository;

import com.example.billme.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    Optional<Producto> findByCodigoInterno(String codigoInterno);

    boolean existsByCodigoInterno(String codigoInterno);

    /**
     * Búsqueda multi-campo: código interno, nombre principal o cualquiera de sus alias.
     * Usada en la barra de búsqueda de la pantalla de facturación.
     */
    @Query("SELECT DISTINCT p FROM Producto p LEFT JOIN p.aliases a WHERE " +
           "LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.nombrePrincipal) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(a.nombreAlias) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Producto> buscar(@Param("q") String q);
}
