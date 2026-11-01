package com.example.billme.importacion;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListaPreciosLectorTest {

    private final ListaPreciosLector lector = new ListaPreciosLector();

    @Test
    void encuentraLaHojaDeLaListaYLeeSusFilas() {
        byte[] archivo = ExcelDePrueba.listaDelProveedor()
                .fila("001001169", "Perlin Negro 2 x  8 x 4.50mm Segunda", "UND", 65.95, 4100)
                .fila("024005084", "Lamina Lisa Prepintada 914 x 3050 x 0.55mm", "kg", 12.25, 850.00)
                .fila("009001999", "Tubo sin precio todavía", "Metro", null, null)
                .bytes();

        LecturaListaPrecios lectura = leer(archivo);

        assertThat(lectura.hoja()).isEqualTo("lista_precios");
        assertThat(lectura.filas()).hasSize(3);
        FilaListaPrecios perlin = lectura.filas().get(0);
        assertThat(perlin.codigo()).isEqualTo("001001169");
        assertThat(perlin.descripcion()).isEqualTo("Perlin Negro 2 x 8 x 4.50mm Segunda");
        assertThat(perlin.unidad()).isEqualTo("UND");
        assertThat(perlin.peso()).isEqualByComparingTo("65.95");
        assertThat(perlin.precio()).isEqualByComparingTo("4100");
        assertThat(lectura.filas().get(1).unidad()).isEqualTo("KG");
        assertThat(lectura.filas().get(2).unidad()).isEqualTo("METRO");
        assertThat(lectura.filas().get(2).precio()).isNull();
    }

    @Test
    void ignoraFilasSinCodigoOConCodigoRepetido() {
        byte[] archivo = ExcelDePrueba.listaDelProveedor()
                .fila("001001169", "Perlin Negro 2 x 8 x 4.50mm", "UND", 65.95, 4100)
                .fila(null, "Fila sin código", "UND", 1, 10)
                .fila("001001169", "Perlin repetido", "UND", 65.95, 4400)
                .fila()
                .bytes();

        LecturaListaPrecios lectura = leer(archivo);

        assertThat(lectura.filas()).extracting(FilaListaPrecios::descripcion)
                .containsExactly("Perlin Negro 2 x 8 x 4.50mm");
        assertThat(lectura.filasIgnoradas()).isEqualTo(2);
    }

    @Test
    void aceptaEncabezadosSencillosYCodigosNumericos() {
        byte[] archivo = new ExcelDePrueba().hoja("Precios")
                .fila("Lista de precios octubre")
                .fila()
                .fila("Código", "Descripción", "Precio")
                .fila(1001169, "Perlin Negro", 4100.5)
                .bytes();

        FilaListaPrecios fila = leer(archivo).filas().get(0);

        assertThat(fila.codigo()).isEqualTo("1001169");
        assertThat(fila.unidad()).isEqualTo("UND");
        assertThat(fila.precio()).isEqualByComparingTo("4100.5");
    }

    @Test
    void rechazaArchivosSinLista() {
        byte[] sinLista = new ExcelDePrueba().hoja("Hoja1").fila("Cantidad", "Descripción").bytes();

        assertThatThrownBy(() -> leer(sinLista))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se encontró una hoja");
        assertThatThrownBy(() -> leer("no es un excel".getBytes()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se pudo leer el archivo");
    }

    private LecturaListaPrecios leer(byte[] archivo) {
        return lector.leer(new ByteArrayInputStream(archivo));
    }
}
