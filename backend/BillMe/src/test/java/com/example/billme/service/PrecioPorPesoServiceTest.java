package com.example.billme.service;

import com.example.billme.enums.MonedaTarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrecioPorPesoServiceTest {

    @Mock
    private ConfiguracionService configuracionService;

    private PrecioPorPesoService service;

    private static final BigDecimal TIPO_CAMBIO = new BigDecimal("36.6243");
    private static final BigDecimal FACTOR_IVA = new BigDecimal("1.15");

    @BeforeEach
    void setUp() {
        service = new PrecioPorPesoService(configuracionService, new ObjectMapper());
    }

    @Test
    void calcularEnUsd() {
        when(configuracionService.factorIva()).thenReturn(FACTOR_IVA);
        BigDecimal pu = service.calcular(new BigDecimal("2.58"), new BigDecimal("1.80"), MonedaTarifa.USD, TIPO_CAMBIO);
        assertThat(pu).isEqualByComparingTo("195.60");
    }

    @Test
    void calcularEnNio() {
        when(configuracionService.factorIva()).thenReturn(FACTOR_IVA);
        BigDecimal pu = service.calcular(new BigDecimal("2.58"), new BigDecimal("70"), MonedaTarifa.NIO, TIPO_CAMBIO);
        assertThat(pu).isEqualByComparingTo("207.69");
    }

    @Test
    void recalcularConParametrosValidosUsd() {
        when(configuracionService.factorIva()).thenReturn(FACTOR_IVA);
        String params = "{\"tarifa\": 1.80, \"moneda\": \"USD\"}";
        Optional<BigDecimal> pu = service.recalcular(params, new BigDecimal("2.58"), TIPO_CAMBIO);
        assertThat(pu).contains(new BigDecimal("195.60"));
    }

    @Test
    void recalcularConParametrosValidosNio() {
        when(configuracionService.factorIva()).thenReturn(FACTOR_IVA);
        String params = "{\"tarifa\": 70, \"moneda\": \"NIO\"}";
        Optional<BigDecimal> pu = service.recalcular(params, new BigDecimal("2.58"), TIPO_CAMBIO);
        assertThat(pu).contains(new BigDecimal("207.69"));
    }

    @Test
    void recalcularParametrosVaciosOPesoInvalidoDanEmpty() {
        assertThat(service.recalcular(null, new BigDecimal("2.58"), TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("", new BigDecimal("2.58"), TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("   ", new BigDecimal("2.58"), TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("{\"tarifa\": 1.80, \"moneda\": \"USD\"}", null, TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("{\"tarifa\": 1.80, \"moneda\": \"USD\"}", BigDecimal.ZERO, TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("{\"tarifa\": 1.80, \"moneda\": \"USD\"}", new BigDecimal("-1"), TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("json-invalido", new BigDecimal("2.58"), TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("{\"tarifa\": 0, \"moneda\": \"USD\"}", new BigDecimal("2.58"), TIPO_CAMBIO)).isEmpty();
        assertThat(service.recalcular("{\"tarifa\": -5, \"moneda\": \"USD\"}", new BigDecimal("2.58"), TIPO_CAMBIO)).isEmpty();
    }
}
