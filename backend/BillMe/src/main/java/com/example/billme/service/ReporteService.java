package com.example.billme.service;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.dto.GananciasResponse;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.GananciaPorFactura;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final DocumentoRepository documentoRepository;

    public GananciasResponse ganancias(UUID clienteId, UUID proyectoId, LocalDate desde, LocalDate hasta) {
        List<GananciaPorFactura> rows = documentoRepository.reporteGanancias(clienteId, proyectoId, desde, hasta);

        BigDecimal facturado = BigDecimal.ZERO.setScale(2);
        BigDecimal ventasConCosto = BigDecimal.ZERO.setScale(2);
        BigDecimal costo = BigDecimal.ZERO.setScale(2);
        BigDecimal ganancia = BigDecimal.ZERO.setScale(2);
        int lineasSinCosto = 0;

        List<GananciasResponse.FilaGanancia> facturas = new ArrayList<>(rows.size());

        for (GananciaPorFactura row : rows) {
            BigDecimal fTotal = CalculoDocumento.r2(row.getTotal());
            BigDecimal fCosto = CalculoDocumento.r2(row.getCosto());
            BigDecimal fVentasConCosto = CalculoDocumento.r2(row.getVentasConCosto());
            BigDecimal fGanancia = fVentasConCosto.subtract(fCosto);
            int fSinCosto = row.getLineasSinCosto();

            facturas.add(new GananciasResponse.FilaGanancia(
                    row.getId(),
                    row.getFolio(),
                    row.getFecha(),
                    row.getCliente(),
                    fTotal,
                    fCosto,
                    fGanancia,
                    fSinCosto
            ));

            facturado = facturado.add(fTotal);
            ventasConCosto = ventasConCosto.add(fVentasConCosto);
            costo = costo.add(fCosto);
            ganancia = ganancia.add(fGanancia);
            lineasSinCosto += fSinCosto;
        }

        BigDecimal ventasSinCosto = facturado.subtract(ventasConCosto);

        return new GananciasResponse(
                facturado,
                ventasConCosto,
                costo,
                ganancia,
                lineasSinCosto,
                ventasSinCosto,
                facturas
        );
    }
}
