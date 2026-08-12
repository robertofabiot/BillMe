package com.example.billme.service;

import com.example.billme.dto.ReporteResponse;
import com.example.billme.enums.EstadoFactura;
import com.example.billme.model.Factura;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final FacturaRepository facturaRepository;
    private final AbonoRepository abonoRepository;

    public ReporteResponse generarReporte(UUID clienteId, UUID proyectoId, Instant desde, Instant hasta) {
        List<Factura> facturas = facturaRepository.filtrar(clienteId, proyectoId, desde, hasta, null);
        
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        BigDecimal totalCobrado = BigDecimal.ZERO;
        int totalFacturas = 0;

        BigDecimal costoTotal = BigDecimal.ZERO;

        for (Factura factura : facturas) {
            if (factura.getEstado() != EstadoFactura.COTIZACION) {
                ingresosTotales = ingresosTotales.add(factura.getTotalVenta() != null ? factura.getTotalVenta() : BigDecimal.ZERO);
                costoTotal = costoTotal.add(factura.getCostoRealEmpresa() != null ? factura.getCostoRealEmpresa() : BigDecimal.ZERO);
                BigDecimal cobrado = abonoRepository.sumMontoByFacturaId(factura.getId());
                if (cobrado != null) {
                    totalCobrado = totalCobrado.add(cobrado);
                }
                totalFacturas++;
            }
        }

        BigDecimal porCobrar = ingresosTotales.subtract(totalCobrado);
        BigDecimal gananciaNeta = ingresosTotales.subtract(costoTotal);

        return new ReporteResponse(
                ingresosTotales,
                totalCobrado,
                porCobrar,
                costoTotal,
                gananciaNeta,
                totalFacturas
        );
    }
}
