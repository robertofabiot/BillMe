package com.example.billme.service;

import com.example.billme.calculo.CalculadoraPrecio;
import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.enums.MonedaTarifa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

/** Precio con IVA calculado desde el peso, una tarifa sin IVA y su moneda. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecioPorPesoService {

    private final ConfiguracionService configuracionService;
    private final ObjectMapper objectMapper;

    public BigDecimal calcular(BigDecimal peso, BigDecimal tarifa, MonedaTarifa moneda, BigDecimal tipoCambio) {
        BigDecimal cambio = moneda == MonedaTarifa.USD ? tipoCambio : BigDecimal.ONE;
        return CalculadoraPrecio.porPeso(CalculoDocumento.r2(peso), tarifa, cambio, configuracionService.factorIva());
    }

    /** Vacío si los parámetros de la línea o el peso no permiten calcular. */
    public Optional<BigDecimal> recalcular(String parametros, BigDecimal peso, BigDecimal tipoCambio) {
        if (parametros == null || parametros.isBlank() || peso == null || peso.signum() <= 0) {
            return Optional.empty();
        }
        try {
            JsonNode nodo = objectMapper.readTree(parametros);
            BigDecimal tarifa = new BigDecimal(nodo.get("tarifa").asText());
            MonedaTarifa moneda = MonedaTarifa.valueOf(nodo.get("moneda").asText());
            return tarifa.signum() > 0
                    ? Optional.of(calcular(peso, tarifa, moneda, tipoCambio))
                    : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
