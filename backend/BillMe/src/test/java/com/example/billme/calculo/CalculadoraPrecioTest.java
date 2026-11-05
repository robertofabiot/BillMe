package com.example.billme.calculo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraPrecioTest {

    private static final BigDecimal TIPO_CAMBIO = new BigDecimal("36.6243");
    private static final BigDecimal FACTOR_IVA = new BigDecimal("1.15");

    @Test
    void calculaPrecioPorPeso() {
        // Placa 10"x8"x1/4", peso 2.58, tarifa 1.80 -> PU 195.60
        BigDecimal pesoUnitKg = new BigDecimal("2.58");
        BigDecimal tarifaUsd = new BigDecimal("1.80");

        BigDecimal pu = CalculadoraPrecio.porPeso(pesoUnitKg, tarifaUsd, TIPO_CAMBIO, FACTOR_IVA);
        assertThat(pu).isEqualByComparingTo("195.60");
    }

    @Test
    void calculaPrecioPorFormula() {
        BigDecimal lista = new BigDecimal("1000.00");

        // Descuento 4% -> PU 1,104.00
        BigDecimal pu4 = CalculadoraPrecio.porFormula(lista, FACTOR_IVA, new BigDecimal("4"));
        assertThat(pu4).isEqualByComparingTo("1104.00");

        // Descuento 6% -> PU 1,081.00
        BigDecimal pu6 = CalculadoraPrecio.porFormula(lista, FACTOR_IVA, new BigDecimal("6"));
        assertThat(pu6).isEqualByComparingTo("1081.00");

        // Sin descuento -> PU 1,150.00
        BigDecimal pu0 = CalculadoraPrecio.porFormula(lista, FACTOR_IVA, BigDecimal.ZERO);
        assertThat(pu0).isEqualByComparingTo("1150.00");
    }

    @Test
    void calculaPrecioPorMetro() {
        // 2,700.00 / 6.75 m x 5.40 m -> PU 2,160.00
        BigDecimal precioReferencia = new BigDecimal("2700.00");
        BigDecimal largoReferencia = new BigDecimal("6.75");
        BigDecimal largoNuevo = new BigDecimal("5.40");

        BigDecimal pu = CalculadoraPrecio.porMetro(precioReferencia, largoReferencia, largoNuevo);
        assertThat(pu).isEqualByComparingTo("2160.00");
    }

    @Test
    void validaParametrosIncorrectos() {
        assertThatThrownBy(() -> CalculadoraPrecio.porPeso(null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CalculadoraPrecio.porMetro(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CalculadoraPrecio.porFormula(BigDecimal.ONE, FACTOR_IVA, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
