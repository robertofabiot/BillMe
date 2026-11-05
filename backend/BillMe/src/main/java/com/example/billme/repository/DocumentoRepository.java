package com.example.billme.repository;

import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Documento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<Documento> findByImportKey(String importKey);

    /**
     * Listado con filtros opcionales (null = sin filtro), por fecha del documento,
     * los más recientes primero.
     */
    @Query("SELECT d FROM Documento d LEFT JOIN d.proyecto p WHERE " +
           "(:tipo IS NULL OR d.tipo = :tipo) AND " +
           "(:estado IS NULL OR d.estado = :estado) AND " +
           "(cast(:clienteId as uuid) IS NULL OR d.cliente.id = :clienteId) AND " +
           "(cast(:proyectoId as uuid) IS NULL OR d.proyecto.id = :proyectoId) AND " +
           "(cast(:desde as date) IS NULL OR d.fecha >= :desde) AND " +
           "(cast(:hasta as date) IS NULL OR d.fecha <= :hasta) AND " +
           "(cast(:productoId as uuid) IS NULL OR EXISTS " +
           "  (SELECT 1 FROM DocumentoLinea l WHERE l.documento = d AND l.producto.id = :productoId)) AND " +
           "(:q = '' OR lower(d.folio) LIKE :contiene OR lower(d.cliente.nombre) LIKE :contiene OR lower(p.nombre) LIKE :contiene) " +
           "ORDER BY d.fecha DESC, d.createdAt DESC")
    Page<Documento> filtrar(
            @Param("tipo") TipoDocumento tipo,
            @Param("estado") EstadoDocumento estado,
            @Param("clienteId") UUID clienteId,
            @Param("proyectoId") UUID proyectoId,
            @Param("productoId") UUID productoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("q") String q,
            @Param("contiene") String contiene,
            Pageable pageable);

    @Query(value = "select nextval('seq_folio_cot')", nativeQuery = true)
    long siguienteFolioCotizacion();

    @Query(value = "select nextval('seq_folio_fac')", nativeQuery = true)
    long siguienteFolioFactura();

    @Query("SELECT d FROM Documento d WHERE d.cliente.id = :clienteId " +
           "AND d.tipo = com.example.billme.enums.TipoDocumento.FACTURA " +
           "AND d.estado = com.example.billme.enums.EstadoDocumento.ACTIVA AND d.importKey IS NULL " +
           "AND (cast(:proyectoId as uuid) IS NULL OR d.proyecto.id = :proyectoId) " +
           "AND (cast(:desde as date) IS NULL OR d.fecha >= :desde) " +
           "AND (cast(:hasta as date) IS NULL OR d.fecha <= :hasta) " +
           "ORDER BY d.fecha ASC, d.createdAt ASC")
    List<Documento> facturasDeCuenta(@Param("clienteId") UUID clienteId, @Param("proyectoId") UUID proyectoId,
                                     @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT d.cliente_id AS clienteid, COALESCE(SUM(d.total), 0) AS suma " +
                   "FROM documentos d JOIN clientes c ON c.id = d.cliente_id " +
                   "WHERE d.tipo = 'FACTURA' AND d.estado = 'ACTIVA' AND d.import_key IS NULL " +
                   "AND (c.fecha_inicio_cuenta IS NULL OR d.fecha >= c.fecha_inicio_cuenta) " +
                   "GROUP BY d.cliente_id", nativeQuery = true)
    List<SumaPorCliente> sumaFacturasPorCliente();

    @Query(value = "SELECT d.proyecto_id AS proyectoid, COALESCE(SUM(d.total), 0) AS suma " +
                   "FROM documentos d " +
                   "WHERE d.tipo = 'FACTURA' AND d.estado = 'ACTIVA' AND d.proyecto_id IS NOT NULL " +
                   "GROUP BY d.proyecto_id", nativeQuery = true)
    List<SumaPorProyecto> sumaFacturasPorProyecto();

    @Query(value = """
            select d.id as id, d.folio as folio, d.fecha as fecha, c.nombre as cliente, d.total as total,
                   coalesce(sum(l.cantidad * l.costo_unit_con_iva), 0) as costo,
                   coalesce(sum(l.total) filter (where l.costo_unit_con_iva is not null), 0) as ventasconcosto,
                   count(*) filter (where l.costo_unit_con_iva is null) as lineassincosto
            from documentos d
            join clientes c on c.id = d.cliente_id
            join documento_lineas l on l.documento_id = d.id
            where d.tipo = 'FACTURA' and d.estado = 'ACTIVA'
              and (cast(:clienteId as uuid) is null or d.cliente_id = cast(:clienteId as uuid))
              and (cast(:proyectoId as uuid) is null or d.proyecto_id = cast(:proyectoId as uuid))
              and (cast(:desde as date) is null or d.fecha >= cast(:desde as date))
              and (cast(:hasta as date) is null or d.fecha <= cast(:hasta as date))
            group by d.id, d.folio, d.fecha, c.nombre, d.total
            order by d.fecha desc, d.folio desc
            """, nativeQuery = true)
    List<GananciaPorFactura> reporteGanancias(
            @Param("clienteId") UUID clienteId,
            @Param("proyectoId") UUID proyectoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
