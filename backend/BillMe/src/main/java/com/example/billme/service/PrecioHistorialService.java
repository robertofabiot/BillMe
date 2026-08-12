package com.example.billme.service;

import com.example.billme.dto.PrecioHistorialResponse;
import com.example.billme.model.DetalleFactura;
import com.example.billme.model.Factura;
import com.example.billme.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrecioHistorialService {

    private final FacturaRepository facturaRepository;

    public List<PrecioHistorialResponse> obtenerHistorial(UUID clienteId, UUID productoId) {
        List<Factura> facturas = facturaRepository.historialPreciosPorClienteYProducto(clienteId, productoId);
        List<PrecioHistorialResponse> historial = new ArrayList<>();

        for (Factura factura : facturas) {
            if (factura.getDetalles() != null) {
                for (DetalleFactura detalle : factura.getDetalles()) {
                    if (productoId.equals(detalle.getProducto().getId())) {
                        historial.add(new PrecioHistorialResponse(
                                factura.getFolioInterno(),
                                factura.getCreatedAt(),
                                detalle.getPrecioUnitarioVenta(),
                                detalle.getCantidad()
                        ));
                    }
                }
            }
        }

        return historial;
    }
}
