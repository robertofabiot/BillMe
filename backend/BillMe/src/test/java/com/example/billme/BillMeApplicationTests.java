package com.example.billme;

import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

class BillMeApplicationTests extends IntegrationTest {

    @Test
    void arrancaConTodasLasMigracionesAplicadas() throws Exception {
        // Si Hibernate (ddl-auto=validate) no coincidiera con las migraciones, el contexto no levantaría
        int migraciones = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql").length;
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success and version is not null", Integer.class);

        assertThat(aplicadas).isEqualTo(migraciones);
    }
}
