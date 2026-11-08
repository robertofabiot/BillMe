package com.example.billme.util;

import org.junit.jupiter.api.Test;

import static com.example.billme.util.NormalizadorTexto.normalizar;
import static org.assertj.core.api.Assertions.assertThat;

/** Casos con nombres de productos como se escriben a mano en las facturas y en la lista del proveedor. */
class NormalizadorTextoTest {

    @Test
    void igualaLasVariantesDeUnMismoTubo() {
        assertThat(normalizar("Tubo Estruct. Cua. 2\" x 1.50mm"))
                .isEqualTo(normalizar("Tubo Estruct. Cua. 2 x 1.50 mm"))
                .isEqualTo(normalizar("Tubo Estruct. Cua. 2 x 1.5mm "))
                .isEqualTo("tubo estruct cua 2 x 1.5mm");
    }

    @Test
    void quitaComillasTipograficasDePulgadas() {
        assertThat(normalizar("Placa 10” x 8” x 1/4”")).isEqualTo("placa 10 x 8 x 1/4");
    }

    @Test
    void convierteFraccionesUnicode() {
        assertThat(normalizar("Placa 12” x 22 ½” x ½”")).isEqualTo("placa 12 x 22 1/2 x 1/2");
    }

    @Test
    void igualaFraccionesConGuionYConEspacio() {
        assertThat(normalizar("Perlin Galvanizado 1-1/2 x 3 x 1.00 mm"))
                .isEqualTo(normalizar("Perlin Galvanizado 1 1/2 x 3 x 1 mm"))
                .isEqualTo("perlin galvanizado 1 1/2 x 3 x 1mm");
    }

    @Test
    void quitaAcentosYEspaciosDobles() {
        assertThat(normalizar("Lámina Lisa  Fría")).isEqualTo("lamina lisa fria");
        assertThat(normalizar("Perlin Negro 2 x  8 x 4.50mm")).isEqualTo("perlin negro 2 x 8 x 4.5mm");
    }

    @Test
    void separaLasMedidasPegadas() {
        assertThat(normalizar("Tubo 2x4")).isEqualTo("tubo 2 x 4");
    }

    @Test
    void conservaDecimalesYCodigos() {
        assertThat(normalizar("Lamina E105 Prep. Rojo 6.75mt x 0.55mm"))
                .isEqualTo("lamina e105 prep rojo 6.75mt x 0.55mm");
        assertThat(normalizar("Varilla 3/8\" x 6 mts")).isEqualTo("varilla 3/8 x 6mts");
    }

    @Test
    void textoVacioONulo() {
        assertThat(normalizar(null)).isEmpty();
        assertThat(normalizar("   ")).isEmpty();
    }

    @Test
    void sinAcentosQuitaDiacriticosYMinusculas() {
        assertThat(NormalizadorTexto.sinAcentos("PÉREZ")).isEqualTo("perez");
        assertThat(NormalizadorTexto.sinAcentos("Cotización ñandú")).isEqualTo("cotizacion nandu");
        assertThat(NormalizadorTexto.sinAcentos(null)).isEqualTo("");
    }
}
