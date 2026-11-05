package com.example.billme.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Base de los tests de integración: levanta la app completa contra un Postgres 17
 * real (embebido, sin Docker) con el esquema creado por las migraciones de Flyway.
 * El Postgres se inicia una sola vez por ejecución y los datos se vacían antes de cada test.
 */
@SpringBootTest(properties = "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2Nzg=")
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    /** Vacía los datos de cada test; conserva lo que siembran las migraciones (genéricos, proveedores, configuración). */
    @BeforeEach
    void vaciarDatos() {
        jdbc.execute("truncate table consolidado_grupos, consolidados, abonos, "
                + "documento_revisiones, documento_lineas, documentos, precios_lista, alias_productos, proyectos, "
                + "clientes, usuarios cascade");
        jdbc.execute("delete from productos where not generico");
        jdbc.execute("delete from proveedores where nombre <> 'Proveedor principal'");
        jdbc.execute("alter sequence seq_folio_cot restart");
        jdbc.execute("alter sequence seq_folio_fac restart");
        jdbc.execute("alter sequence seq_folio_cons restart");
    }

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                    // la JVM ya está terminando
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo iniciar el Postgres embebido", e);
        }
    }
}
