package com.example.billme.calculo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.MathContext;

public record Medida(
        @NotBlank String texto,
        @NotNull UnidadLongitud unidad
) {

    public BigDecimal valorNumerico() {
        if (texto == null) {
            throw error();
        }
        String t = texto.trim();
        if (t.isEmpty()) {
            throw error();
        }

        BigDecimal valor = null;
        try {
            if (t.equals("½")) {
                valor = new BigDecimal("0.5");
            } else if (t.equals("¼")) {
                valor = new BigDecimal("0.25");
            } else if (t.equals("¾")) {
                valor = new BigDecimal("0.75");
            } else if (t.matches("^[0-9]+(\\.[0-9]+)?$")) {
                valor = new BigDecimal(t);
            } else if (t.matches("^[0-9]+/[0-9]+$")) {
                String[] partes = t.split("/");
                BigDecimal num = new BigDecimal(partes[0]);
                BigDecimal den = new BigDecimal(partes[1]);
                if (den.compareTo(BigDecimal.ZERO) == 0) {
                    throw error();
                }
                valor = num.divide(den, MathContext.DECIMAL64);
            } else if (t.matches("^[0-9]+\\s+[0-9]+/[0-9]+$")) {
                String[] partes = t.split("\\s+");
                BigDecimal entero = new BigDecimal(partes[0]);
                String[] frac = partes[1].split("/");
                BigDecimal num = new BigDecimal(frac[0]);
                BigDecimal den = new BigDecimal(frac[1]);
                if (den.compareTo(BigDecimal.ZERO) == 0) {
                    throw error();
                }
                valor = entero.add(num.divide(den, MathContext.DECIMAL64));
            } else if (t.matches("^[0-9]+\\s*[½¼¾]$")) {
                char last = t.charAt(t.length() - 1);
                String enteroStr = t.substring(0, t.length() - 1).trim();
                BigDecimal entero = new BigDecimal(enteroStr);
                BigDecimal frac = switch (last) {
                    case '½' -> new BigDecimal("0.5");
                    case '¼' -> new BigDecimal("0.25");
                    case '¾' -> new BigDecimal("0.75");
                    default -> throw error();
                };
                valor = entero.add(frac);
            }
        } catch (Exception e) {
            throw error();
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw error();
        }
        return valor;
    }

    public BigDecimal enPulgadas() {
        if (unidad == null) {
            throw new IllegalArgumentException("La unidad no puede ser nula");
        }
        return unidad.aPulgadas(valorNumerico());
    }

    public String paraDescripcion() {
        if (unidad == null) {
            throw new IllegalArgumentException("La unidad no puede ser nula");
        }
        String t = texto != null ? texto.trim() : "";
        return switch (unidad) {
            case PULGADA -> t + "\"";
            case PIE -> t + "'";
            case METRO -> t + " m";
            case CM -> t + " cm";
            case MM -> t + " mm";
        };
    }

    private IllegalArgumentException error() {
        return new IllegalArgumentException(
                "No se entiende la medida \"" + texto + "\". Escribe por ejemplo 10, 3.05, 1/4 o 1 1/2.");
    }
}
