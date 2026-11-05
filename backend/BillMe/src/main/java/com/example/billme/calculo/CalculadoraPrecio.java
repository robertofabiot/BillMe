package com.example.billme.calculo;

import java.math.BigDecimal;
import java.math.MathContext;

public final class CalculadoraPrecio {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private CalculadoraPrecio() {
    }

    public static BigDecimal porPeso(BigDecimal pesoUnitKg, BigDecimal tarifaUsd, BigDecimal tipoCambio, BigDecimal factorIva) {
        if (pesoUnitKg == null || tarifaUsd == null || tipoCambio == null || factorIva == null) {
            throw new IllegalArgumentException("Ningún parámetro para precio por peso puede ser nulo");
        }
        if (pesoUnitKg.compareTo(BigDecimal.ZERO) <= 0 || tarifaUsd.compareTo(BigDecimal.ZERO) <= 0
                || tipoCambio.compareTo(BigDecimal.ZERO) <= 0 || factorIva.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Los parámetros para precio por peso deben ser mayores a cero");
        }
        BigDecimal precio = pesoUnitKg
                .multiply(tarifaUsd, MathContext.DECIMAL64)
                .multiply(tipoCambio, MathContext.DECIMAL64)
                .multiply(factorIva, MathContext.DECIMAL64);
        return CalculoDocumento.r2(precio);
    }

    public static BigDecimal porMetro(BigDecimal precioReferencia, BigDecimal largoReferencia, BigDecimal largoNuevo) {
        if (precioReferencia == null || largoReferencia == null || largoNuevo == null) {
            throw new IllegalArgumentException("Ningún parámetro para precio por metro puede ser nulo");
        }
        if (precioReferencia.compareTo(BigDecimal.ZERO) <= 0 || largoReferencia.compareTo(BigDecimal.ZERO) <= 0
                || largoNuevo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Los parámetros para precio por metro deben ser mayores a cero");
        }
        BigDecimal precio = precioReferencia
                .divide(largoReferencia, MathContext.DECIMAL64)
                .multiply(largoNuevo, MathContext.DECIMAL64);
        return CalculoDocumento.r2(precio);
    }

    public static BigDecimal porFormula(BigDecimal lista, BigDecimal factorIva, BigDecimal descuentoPct) {
        if (lista == null || factorIva == null) {
            throw new IllegalArgumentException("Precio de lista y factor IVA son obligatorios");
        }
        if (lista.compareTo(BigDecimal.ZERO) <= 0 || factorIva.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio de lista y factor IVA deben ser mayores a cero");
        }
        BigDecimal desc = descuentoPct != null ? descuentoPct : BigDecimal.ZERO;
        if (desc.compareTo(BigDecimal.ZERO) < 0 || desc.compareTo(CIEN) >= 0) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 99.99%");
        }
        BigDecimal factorDescuento = BigDecimal.ONE.subtract(desc.divide(CIEN, MathContext.DECIMAL64));
        BigDecimal precio = lista
                .multiply(factorIva, MathContext.DECIMAL64)
                .multiply(factorDescuento, MathContext.DECIMAL64);
        return CalculoDocumento.r2(precio);
    }
}
