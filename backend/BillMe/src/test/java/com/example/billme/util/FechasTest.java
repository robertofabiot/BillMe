package com.example.billme.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FechasTest {

    @Test
    void cortaFormateaFechaCorrectamente() {
        LocalDate fecha = LocalDate.of(2026, 11, 6);
        assertThat(Fechas.corta(fecha)).isEqualTo("06/11/2026");
    }

    @Test
    void cortaConNullRetornaCadenaVacia() {
        assertThat(Fechas.corta(null)).isEqualTo("");
    }
}
