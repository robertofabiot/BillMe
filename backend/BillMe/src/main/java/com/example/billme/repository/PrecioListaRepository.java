package com.example.billme.repository;

import com.example.billme.enums.FuentePrecio;
import com.example.billme.model.PrecioLista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrecioListaRepository extends JpaRepository<PrecioLista, UUID> {

    /** Precio vigente: el registro más reciente del producto. */
    Optional<PrecioLista> findFirstByProductoIdOrderByFechaDescCreatedAtDesc(UUID productoId);

    List<PrecioLista> findByProductoIdOrderByFechaDescCreatedAtDesc(UUID productoId);

    /** Precio vigente de varios productos en una sola consulta (para listados). */
    @Query(value = "select distinct on (producto_id) * from precios_lista "
            + "where producto_id in (:ids) order by producto_id, fecha desc, created_at desc",
            nativeQuery = true)
    List<PrecioLista> vigentes(@Param("ids") Collection<UUID> ids);

    /** Último descuento usado con una fuente en una familia (el descuento suele ser el mismo por familia). */
    Optional<PrecioLista> findFirstByProductoFamiliaAndFuenteAndDescuentoPctNotNullOrderByFechaDescCreatedAtDesc(
            String familia, FuentePrecio fuente);

    void deleteByProductoId(UUID productoId);
}
