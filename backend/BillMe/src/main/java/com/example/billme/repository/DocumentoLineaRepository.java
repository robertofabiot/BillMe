package com.example.billme.repository;

import com.example.billme.enums.EstadoDocumento;
import com.example.billme.model.DocumentoLinea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentoLineaRepository extends JpaRepository<DocumentoLinea, UUID> {

    boolean existsByProductoId(UUID productoId);

    /** Precios dados a un cliente por un producto (cotizaciones y facturas no anuladas), los más recientes primero. */
    @Query("SELECT l FROM DocumentoLinea l JOIN FETCH l.documento d " +
           "WHERE d.cliente.id = :clienteId AND l.producto.id = :productoId AND d.estado <> :anulada " +
           "ORDER BY d.fecha DESC, d.createdAt DESC")
    List<DocumentoLinea> historialDeCliente(@Param("clienteId") UUID clienteId,
                                             @Param("productoId") UUID productoId,
                                             @Param("anulada") EstadoDocumento anulada);
}
