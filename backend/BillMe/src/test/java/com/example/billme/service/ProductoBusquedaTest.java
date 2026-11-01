package com.example.billme.service;

import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoBusquedaTest extends IntegrationTest {

    @Autowired
    ProductoService productoService;

    @Test
    void encuentraElProductoAunqueEsteEscritoDistinto() {
        crear("009001045", "Tubo Estruct. Cua. 2\" x 1.50mm Ch16", List.of());
        crear("009001046", "Tubo Estruct. Cua. 2\" x 2.00mm Ch14", List.of());
        crear("001001116", "Perlin Negro 2 x  6 x 2.38mm", List.of());

        // Así se suelen escribir a mano en las facturas
        assertThat(primero("Tubo Estruct. Cua. 2 x 1.50 mm")).isEqualTo("009001045");
        assertThat(primero("tubo estruct cua 2 x 2")).isEqualTo("009001046");
        assertThat(primero("perlin negro 2x6")).isEqualTo("001001116");
    }

    @Test
    void buscaTambienPorAlias() {
        crear("009001271", "Tubo Estruct. Cua. 100 x 2.38 x 6000 mm", List.of("Caja de 4\" x 4\" x 3/32\""));

        assertThat(primero("caja de 4 x 4")).isEqualTo("009001271");
    }

    @Test
    void elCodigoTienePrioridad() {
        crear("024005084", "Lamina Lisa Prepintada 914 x 3050 x 0.55mm", List.of());
        crear("024005085", "Lamina Lisa Prepintada 914 x 3050 x 0.40mm", List.of());

        assertThat(primero("024005084")).isEqualTo("024005084");
        assertThat(productoService.buscar("0240050")).hasSize(2);
    }

    @Test
    void noDevuelveCosasQueNoSeParecen() {
        crear("018001001", "Varilla Lisa Importada 3/8\" x 6 mts", List.of());

        assertThat(productoService.buscar("disco de corte")).isEmpty();
    }

    @Test
    void respondeRapidoConUnCatalogoDelTamanoDeLaListaReal() {
        String[] tipos = {"Tubo Estruct. Cua.", "Tubo Estruct. Rec.", "Perlin Negro", "Perlin Galvanizado",
                "Lamina Lisa Fria", "Angular", "Platina", "Varilla Corrugada"};
        List<Object[]> filas = new ArrayList<>();
        for (int i = 0; i < 8000; i++) {
            String nombre = "%s %d x %d x %.2fmm".formatted(tipos[i % tipos.length], 1 + i % 8, 2 + i % 6, 1 + (i % 40) / 10.0);
            filas.add(new Object[]{"%09d".formatted(100000000 + i), nombre,
                    com.example.billme.util.NormalizadorTexto.normalizar(nombre)});
        }
        jdbc.batchUpdate("insert into productos (id, created_at, updated_at, codigo_interno, nombre_principal, nombre_norm) "
                + "values (gen_random_uuid(), now(), now(), ?, ?, ?)", filas);

        productoService.buscar("perlin negro 2 x 6"); // calentamiento
        long inicio = System.nanoTime();
        List<ProductoResponse> resultado = productoService.buscar("tubo estruct cua 2 x 4 x 1.5");
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(resultado).isNotEmpty().hasSizeLessThanOrEqualTo(20);
        assertThat(milisegundos).isLessThan(300);
    }

    private void crear(String codigo, String nombre, List<String> aliases) {
        productoService.crear(new ProductoRequest(codigo, nombre, null, null, null, aliases));
    }

    private String primero(String q) {
        List<ProductoResponse> resultado = productoService.buscar(q);
        assertThat(resultado).as("resultados para '%s'", q).isNotEmpty();
        return resultado.get(0).codigoInterno();
    }
}
