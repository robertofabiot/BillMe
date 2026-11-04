package com.example.billme.repository;

import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    boolean existsByClienteId(UUID clienteId);

    /** La factura que salió de una cotización. */
    Optional<Documento> findFirstByCotizacionOrigenId(UUID cotizacionId);

    /**
     * Listado con filtros opcionales (null = sin filtro), por fecha del documento,
     * los más recientes primero.
     */
    @Query("SELECT d FROM Documento d WHERE " +
           "(:tipo IS NULL OR d.tipo = :tipo) AND " +
           "(:estado IS NULL OR d.estado = :estado) AND " +
           "(cast(:clienteId as uuid) IS NULL OR d.cliente.id = :clienteId) AND " +
           "(cast(:proyectoId as uuid) IS NULL OR d.proyecto.id = :proyectoId) AND " +
           "(cast(:desde as date) IS NULL OR d.fecha >= :desde) AND " +
           "(cast(:hasta as date) IS NULL OR d.fecha <= :hasta) AND " +
           "(cast(:productoId as uuid) IS NULL OR EXISTS " +
           "  (SELECT 1 FROM DocumentoLinea l WHERE l.documento = d AND l.producto.id = :productoId)) " +
           "ORDER BY d.fecha DESC, d.createdAt DESC")
    List<Documento> filtrar(
            @Param("tipo") TipoDocumento tipo,
            @Param("estado") EstadoDocumento estado,
            @Param("clienteId") UUID clienteId,
            @Param("proyectoId") UUID proyectoId,
            @Param("productoId") UUID productoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "select nextval('seq_folio_cot')", nativeQuery = true)
    long siguienteFolioCotizacion();

    @Query(value = "select nextval('seq_folio_fac')", nativeQuery = true)
    long siguienteFolioFactura();
}
