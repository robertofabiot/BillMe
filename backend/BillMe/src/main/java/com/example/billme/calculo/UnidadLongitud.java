package com.example.billme.calculo;

import java.math.BigDecimal;
import java.math.MathContext;

public enum UnidadLongitud {
    PULGADA,
    PIE,
    METRO,
    CM,
    MM;

    private static final BigDecimal FACTOR_PIE = new BigDecimal("12");
    private static final BigDecimal FACTOR_METRO = new BigDecimal("0.0254");
    private static final BigDecimal FACTOR_CM = new BigDecimal("2.54");
    private static final BigDecimal FACTOR_MM = new BigDecimal("25.4");

    public BigDecimal aPulgadas(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El valor no puede ser nulo");
        }
        return switch (this) {
            case PULGADA -> valor;
            case PIE -> valor.multiply(FACTOR_PIE, MathContext.DECIMAL64);
            case METRO -> valor.divide(FACTOR_METRO, MathContext.DECIMAL64);
            case CM -> valor.divide(FACTOR_CM, MathContext.DECIMAL64);
            case MM -> valor.divide(FACTOR_MM, MathContext.DECIMAL64);
        };
    }
}
