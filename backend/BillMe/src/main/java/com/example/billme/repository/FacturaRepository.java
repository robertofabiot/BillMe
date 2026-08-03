package com.example.billme.repository;

import com.example.billme.enums.EstadoFactura;
import com.example.billme.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FacturaRepository extends JpaRepository<Factura, UUID> {

    List<Factura> findByClienteId(UUID clienteId);

    List<Factura> findByProyectoId(UUID proyectoId);

    List<Factura> findByEstado(EstadoFactura estado);

    /** Verifica unicidad del folio antes de asignarlo. */
    boolean existsByFolioInterno(String folioInterno);

    /**
     * Motor de reportes cruzados: filtra por rango de fechas, cliente y proyecto,
     * con todos los parámetros opcionales (NULL = sin filtro).
     */
    @Query("SELECT f FROM Factura f WHERE " +
           "(:clienteId IS NULL OR f.cliente.id = :clienteId) AND " +
           "(:proyectoId IS NULL OR f.proyecto.id = :proyectoId) AND " +
           "(:desde IS NULL OR f.createdAt >= :desde) AND " +
           "(:hasta IS NULL OR f.createdAt <= :hasta) AND " +
           "(:estado IS NULL OR f.estado = :estado)")
    List<Factura> filtrar(
            @Param("clienteId")  UUID clienteId,
            @Param("proyectoId") UUID proyectoId,
            @Param("desde")      Instant desde,
            @Param("hasta")      Instant hasta,
            @Param("estado")     EstadoFactura estado
    );

    /**
     * Historial de precios: todas las facturas donde un cliente compró un producto
     * específico, ordenadas por fecha descendente.
     * Usada para el autocompletado de precio en nueva factura.
     */
    @Query("SELECT f FROM Factura f JOIN f.detalles d WHERE " +
           "f.cliente.id = :clienteId AND d.producto.id = :productoId " +
           "ORDER BY f.createdAt DESC")
    List<Factura> historialPreciosPorClienteYProducto(
            @Param("clienteId")  UUID clienteId,
            @Param("productoId") UUID productoId
    );
}
