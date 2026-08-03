package com.example.billme.repository;

import com.example.billme.model.DetalleFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetalleFacturaRepository extends JpaRepository<DetalleFactura, UUID> {

    List<DetalleFactura> findByFacturaId(UUID facturaId);

    /** Todos los detalles de un producto en particular — útil para reportes de margen. */
    List<DetalleFactura> findByProductoId(UUID productoId);
}
