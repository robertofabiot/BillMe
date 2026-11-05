package com.example.billme.calculo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedidaTest {

    @Test
    void interpretaMedidasValidasEnPulgadas() {
        // 10 pulgadas
        Medida m10 = new Medida("10", UnidadLongitud.PULGADA);
        assertThat(m10.enPulgadas()).isEqualByComparingTo("10");

        // 3.05 metros a pulgadas -> 3.05 / 0.0254 = 120.07874...
        Medida mDec = new Medida("3.05", UnidadLongitud.METRO);
        assertThat(mDec.enPulgadas()).isGreaterThan(new BigDecimal("120"));

        // 1/4 pulgada
        Medida mFrac = new Medida("1/4", UnidadLongitud.PULGADA);
        assertThat(mFrac.enPulgadas()).isEqualByComparingTo("0.25");

        // 1 1/2 pulgada
        Medida mMix = new Medida("1 1/2", UnidadLongitud.PULGADA);
        assertThat(mMix.enPulgadas()).isEqualByComparingTo("1.5");

        // ½ pulgada
        Medida mUni = new Medida("½", UnidadLongitud.PULGADA);
        assertThat(mUni.enPulgadas()).isEqualByComparingTo("0.5");

        // ¼ y ¾
        Medida mUn4 = new Medida("¼", UnidadLongitud.PULGADA);
        assertThat(mUn4.enPulgadas()).isEqualByComparingTo("0.25");
        Medida mUn34 = new Medida("¾", UnidadLongitud.PULGADA);
        assertThat(mUn34.enPulgadas()).isEqualByComparingTo("0.75");
    }

    @Test
    void rechazaTextoInvalido() {
        assertThatThrownBy(() -> new Medida("abc", UnidadLongitud.PULGADA).enPulgadas())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se entiende la medida \"abc\". Escribe por ejemplo 10, 3.05, 1/4 o 1 1/2.");

        assertThatThrownBy(() -> new Medida("", UnidadLongitud.PULGADA).enPulgadas())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se entiende la medida \"\". Escribe por ejemplo 10, 3.05, 1/4 o 1 1/2.");
    }

    @Test
    void rechazaCeroYValoresNegativos() {
        assertThatThrownBy(() -> new Medida("0", UnidadLongitud.PULGADA).enPulgadas())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se entiende la medida \"0\". Escribe por ejemplo 10, 3.05, 1/4 o 1 1/2.");

        assertThatThrownBy(() -> new Medida("-5", UnidadLongitud.PULGADA).enPulgadas())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se entiende la medida \"-5\". Escribe por ejemplo 10, 3.05, 1/4 o 1 1/2.");
    }

    @Test
    void formateaParaDescripcionSegunUnidad() {
        assertThat(new Medida("10", UnidadLongitud.PULGADA).paraDescripcion()).isEqualTo("10\"");
        assertThat(new Medida("1/4", UnidadLongitud.PULGADA).paraDescripcion()).isEqualTo("1/4\"");
        assertThat(new Medida("10", UnidadLongitud.PIE).paraDescripcion()).isEqualTo("10'");
        assertThat(new Medida("3.05", UnidadLongitud.METRO).paraDescripcion()).isEqualTo("3.05 m");
        assertThat(new Medida("60", UnidadLongitud.CM).paraDescripcion()).isEqualTo("60 cm");
        assertThat(new Medida("6", UnidadLongitud.MM).paraDescripcion()).isEqualTo("6 mm");
    }
}
