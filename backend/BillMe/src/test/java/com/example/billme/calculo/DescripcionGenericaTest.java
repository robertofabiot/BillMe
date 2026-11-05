package com.example.billme.calculo;

import com.example.billme.enums.FormaCalculo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescripcionGenericaTest {

    @Test
    void generaDescripcionPlaca() {
        Medida ancho = new Medida("10", UnidadLongitud.PULGADA);
        Medida largo = new Medida("8", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("1/4", UnidadLongitud.PULGADA);

        String desc = DescripcionGenerica.describir(FormaCalculo.PLACA, ancho, largo, espesor, null);
        assertThat(desc).isEqualTo("Placa 10\" x 8\" x 1/4\"");
    }

    @Test
    void generaDescripcionPlatina() {
        Medida ancho = new Medida("4", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("3/16", UnidadLongitud.PULGADA);
        Medida largo = new Medida("3.05", UnidadLongitud.METRO);

        String desc = DescripcionGenerica.describir(FormaCalculo.PLATINA, ancho, largo, espesor, null);
        assertThat(desc).isEqualTo("Platina 4\" x 3/16\" x 3.05 m");
    }

    @Test
    void generaDescripcionAngular() {
        Medida ala1 = new Medida("2", UnidadLongitud.PULGADA);
        Medida ala2 = new Medida("2", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("3/16", UnidadLongitud.PULGADA);
        Medida largo = new Medida("6", UnidadLongitud.METRO);

        String desc = DescripcionGenerica.describir(FormaCalculo.ANGULAR, ala1, largo, espesor, ala2);
        assertThat(desc).isEqualTo("Angular 2\" x 2\" x 3/16\" x 6 m");
    }

    @Test
    void angularSinAla2LanzaExcepcion() {
        Medida ala1 = new Medida("2", UnidadLongitud.PULGADA);
        Medida espesor = new Medida("3/16", UnidadLongitud.PULGADA);
        Medida largo = new Medida("6", UnidadLongitud.METRO);

        assertThatThrownBy(() -> DescripcionGenerica.describir(FormaCalculo.ANGULAR, ala1, largo, espesor, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El angular requiere ala2");
    }
}
