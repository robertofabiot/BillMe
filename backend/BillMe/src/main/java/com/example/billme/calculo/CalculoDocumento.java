package com.example.billme.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Cálculo oficial de líneas y totales de cotizaciones y facturas. El backend es la única
 * fuente: lo que manda el frontend se recalcula aquí antes de guardar.
 *
 * <pre>
 * PU          = r2(PU)                       (precio unitario con IVA)
 * total línea = r2(cantidad × PU)
 * peso unit.  = r2(peso unit.)
 * peso total  = r2(cantidad × peso unit.)
 * documento   = Σ totales de línea · Σ pesos totales
 * </pre>
 * r2 = redondeo a 2 decimales, mitad hacia arriba.
 */
public final class CalculoDocumento {

    private CalculoDocumento() {
    }

    public record LineaCalculada(BigDecimal precioUnit, BigDecimal total, BigDecimal pesoUnitKg, BigDecimal pesoTotalKg) {
    }

    public record Totales(BigDecimal total, BigDecimal totalKg) {
    }

    public static BigDecimal r2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal enDolares(BigDecimal cordobas, BigDecimal tipoCambio) {
        return r2(cordobas.divide(tipoCambio, java.math.MathContext.DECIMAL64));
    }

    /** {@code pesoUnitKg} puede ser null (productos sin peso, servicios); entonces la línea no suma kilos. */
    public static LineaCalculada linea(BigDecimal cantidad, BigDecimal precioUnit, BigDecimal pesoUnitKg) {
        BigDecimal pu = r2(precioUnit);
        BigDecimal peso = pesoUnitKg != null ? r2(pesoUnitKg) : null;
        return new LineaCalculada(
                pu,
                r2(cantidad.multiply(pu)),
                peso,
                peso != null ? r2(cantidad.multiply(peso)) : null);
    }

    public static Totales totales(Collection<LineaCalculada> lineas) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        BigDecimal kilos = BigDecimal.ZERO.setScale(2);
        for (LineaCalculada linea : lineas) {
            total = total.add(linea.total());
            if (linea.pesoTotalKg() != null) {
                kilos = kilos.add(linea.pesoTotalKg());
            }
        }
        return new Totales(total, kilos);
    }
}
