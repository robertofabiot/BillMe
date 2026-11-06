package com.example.billme.calculo;

import com.example.billme.calculo.CalculoDocumento.LineaCalculada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoDocumentoTest {

    @Test
    void redondeaElPrecioUnitarioAntesDeMultiplicar() {
        // Placa 10" x 8" x 1/4": 2.58 kg × 1.80 USD × 36.6243 × 1.15 = 195.5957…
        LineaCalculada linea = CalculoDocumento.linea(new BigDecimal("10"), new BigDecimal("195.5957"), new BigDecimal("2.58"));

        assertThat(linea.precioUnit()).isEqualByComparingTo("195.60");
        assertThat(linea.total()).isEqualByComparingTo("1956.00");
        assertThat(linea.pesoTotalKg()).isEqualByComparingTo("25.80");
    }

    @Test
    void redondeaMitadHaciaArriba() {
        LineaCalculada linea = CalculoDocumento.linea(new BigDecimal("2.5"), new BigDecimal("10.333"), new BigDecimal("2.584"));

        assertThat(linea.precioUnit()).isEqualByComparingTo("10.33");
        assertThat(linea.total()).isEqualByComparingTo("25.83");      // 25.825
        assertThat(linea.pesoUnitKg()).isEqualByComparingTo("2.58");
        assertThat(linea.pesoTotalKg()).isEqualByComparingTo("6.45"); // 6.45
    }

    @Test
    void unaLineaSinPesoNoSumaKilos() {
        LineaCalculada servicio = CalculoDocumento.linea(BigDecimal.ONE, new BigDecimal("500"), null);
        LineaCalculada placa = CalculoDocumento.linea(new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("3.10"));

        CalculoDocumento.Totales totales = CalculoDocumento.totales(List.of(servicio, placa));

        assertThat(servicio.pesoTotalKg()).isNull();
        assertThat(totales.total()).isEqualByComparingTo("700.00");
        assertThat(totales.totalKg()).isEqualByComparingTo("6.20");
    }

    @Test
    void unDocumentoVacioSumaCero() {
        CalculoDocumento.Totales totales = CalculoDocumento.totales(List.of());

        assertThat(totales.total()).isEqualByComparingTo("0");
        assertThat(totales.totalKg()).isEqualByComparingTo("0");
    }

    @Test
    void enDolaresConvierteYRedondeaADosDecimales() {
        assertThat(CalculoDocumento.enDolares(new BigDecimal("36624.30"), new BigDecimal("36.6243")))
                .isEqualByComparingTo("1000.00");
    }
}
