package com.example.billme;

import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillMeApplicationTests extends IntegrationTest {

    @Test
    void arrancaConElEsquemaDeFlyway() {
        // Si Hibernate (ddl-auto=validate) no coincidiera con las migraciones, el contexto no levantaría
        String version = jdbc.queryForObject(
                "select version from flyway_schema_history where success order by installed_rank desc limit 1",
                String.class);
        assertThat(version).isEqualTo("1");
    }
}
