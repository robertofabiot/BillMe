package com.example.billme.service;

import com.example.billme.dto.ReporteResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final DocumentoRepository documentoRepository;
    private final AbonoRepository abonoRepository;

    /** Solo las facturas activas cuentan: las cotizaciones y las anuladas no son ingresos. */
    public ReporteResponse generarReporte(UUID clienteId, UUID proyectoId, LocalDate desde, LocalDate hasta) {
        List<Documento> facturas = documentoRepository.filtrar(TipoDocumento.FACTURA, EstadoDocumento.ACTIVA,
                clienteId, proyectoId, null, desde, hasta);

        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        for (Documento factura : facturas) {
            ingresos = ingresos.add(factura.getTotal());
            for (DocumentoLinea linea : factura.getLineas()) {
                if (linea.getCostoUnitConIva() != null) {
                    costo = costo.add(linea.getCantidad().multiply(linea.getCostoUnitConIva()));
                }
            }
        }
        BigDecimal cobrado = abonoRepository.sumar(clienteId, proyectoId, desde, hasta);
        return new ReporteResponse(ingresos, cobrado, ingresos.subtract(cobrado), costo,
                ingresos.subtract(costo), facturas.size());
    }
}
