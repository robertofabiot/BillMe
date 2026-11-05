package com.example.billme.calculo;

import com.example.billme.enums.FormaCalculo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraPesoTest {

    private static final BigDecimal DENSIDAD_ACERO = new BigDecimal("0.129");

    @Test
    void calculaPesoPlaca() {
        // Placa 10" x 8" x 1/4" -> 2.58 kg
        Medida ancho = new Medida("10", UnidadLongitud.PULGADA);
        Medida largo = new Medida("8", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("1/4", UnidadLongitud.PULGADA);

        BigDecimal peso = CalculadoraPeso.pesoUnitKg(FormaCalculo.PLACA, ancho, largo, espesor, null, DENSIDAD_ACERO);
        assertThat(peso).isEqualByComparingTo("2.58");
    }

    @Test
    void calculaPesoPlatinaEnLasCincoUnidades() {
        Medida ancho = new Medida("4", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("3/16", UnidadLongitud.PULGADA);

        // Metro: 3.05 m -> 11.62 kg
        Medida largoMetro = new Medida("3.05", UnidadLongitud.METRO);
        assertThat(CalculadoraPeso.pesoUnitKg(FormaCalculo.PLATINA, ancho, largoMetro, espesor, null, DENSIDAD_ACERO))
                .isEqualByComparingTo("11.62");

        // Centímetro: 305 cm -> 11.62 kg
        Medida largoCm = new Medida("305", UnidadLongitud.CM);
        assertThat(CalculadoraPeso.pesoUnitKg(FormaCalculo.PLATINA, ancho, largoCm, espesor, null, DENSIDAD_ACERO))
                .isEqualByComparingTo("11.62");

        // Milímetro: 3050 mm -> 11.62 kg
        Medida largoMm = new Medida("3050", UnidadLongitud.MM);
        assertThat(CalculadoraPeso.pesoUnitKg(FormaCalculo.PLATINA, ancho, largoMm, espesor, null, DENSIDAD_ACERO))
                .isEqualByComparingTo("11.62");

        // Pulgada: 120.0787 pulg -> 11.62 kg
        Medida largoPulg = new Medida("120.0787", UnidadLongitud.PULGADA);
        assertThat(CalculadoraPeso.pesoUnitKg(FormaCalculo.PLATINA, ancho, largoPulg, espesor, null, DENSIDAD_ACERO))
                .isEqualByComparingTo("11.62");

        // Pie: 10.0066 pie -> 11.62 kg
        Medida largoPie = new Medida("10.0066", UnidadLongitud.PIE);
        assertThat(CalculadoraPeso.pesoUnitKg(FormaCalculo.PLATINA, ancho, largoPie, espesor, null, DENSIDAD_ACERO))
                .isEqualByComparingTo("11.62");
    }

    @Test
    void calculaPesoAngular() {
        // Angular 2" x 2" x 3/16" x 6 m -> 21.78 kg
        Medida ala1 = new Medida("2", UnidadLongitud.PULGADA);
        Medida ala2 = new Medida("2", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("3/16", UnidadLongitud.PULGADA);
        Medida largo = new Medida("6", UnidadLongitud.METRO);

        BigDecimal peso = CalculadoraPeso.pesoUnitKg(FormaCalculo.ANGULAR, ala1, largo, espesor, ala2, DENSIDAD_ACERO);
        assertThat(peso).isEqualByComparingTo("21.78");
    }

    @Test
    void angularSinAla2LanzaExcepcion() {
        Medida ala1 = new Medida("2", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("3/16", UnidadLongitud.PULGADA);
        Medida largo = new Medida("6", UnidadLongitud.METRO);

        assertThatThrownBy(() -> CalculadoraPeso.pesoUnitKg(FormaCalculo.ANGULAR, ala1, largo, espesor, null, DENSIDAD_ACERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El angular requiere ala2");
    }
}
