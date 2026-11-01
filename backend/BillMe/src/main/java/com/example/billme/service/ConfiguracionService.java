package com.example.billme.service;

import com.example.billme.dto.ConfiguracionRequest;
import com.example.billme.dto.ConfiguracionResponse;
import com.example.billme.model.Configuracion;
import com.example.billme.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Parámetros del negocio editables. Todos los cálculos de precios deben leerlos de aquí,
 * nunca usar valores fijos en el código.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracionService {

    private static final String IVA_PCT = "iva_pct";
    private static final String TIPO_CAMBIO = "tipo_cambio";
    private static final String TARIFA_KG_USD = "tarifa_kg_usd";
    private static final String DENSIDAD_KG_IN3 = "densidad_kg_in3";
    private static final String DIAS_PRECIO_VIEJO = "dias_precio_viejo";

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionResponse obtener() {
        return new ConfiguracionResponse(ivaPct(), tipoCambio(), tarifaKgUsd(), densidadKgIn3(), diasPrecioViejo());
    }

    @Transactional
    public ConfiguracionResponse actualizar(ConfiguracionRequest request) {
        guardar(IVA_PCT, request.ivaPct());
        guardar(TIPO_CAMBIO, request.tipoCambio());
        guardar(TARIFA_KG_USD, request.tarifaKgUsd());
        guardar(DENSIDAD_KG_IN3, request.densidadKgIn3());
        guardar(DIAS_PRECIO_VIEJO, BigDecimal.valueOf(request.diasPrecioViejo()));
        return obtener();
    }

    public BigDecimal ivaPct() {
        return decimal(IVA_PCT);
    }

    /** 1 + IVA, por ejemplo 1.15. */
    public BigDecimal factorIva() {
        return BigDecimal.ONE.add(ivaPct().divide(BigDecimal.valueOf(100), MathContext.DECIMAL64));
    }

    public BigDecimal tipoCambio() {
        return decimal(TIPO_CAMBIO);
    }

    public BigDecimal tarifaKgUsd() {
        return decimal(TARIFA_KG_USD);
    }

    public BigDecimal densidadKgIn3() {
        return decimal(DENSIDAD_KG_IN3);
    }

    public int diasPrecioViejo() {
        return decimal(DIAS_PRECIO_VIEJO).intValueExact();
    }

    private BigDecimal decimal(String clave) {
        return configuracionRepository.findById(clave)
                .map(c -> new BigDecimal(c.getValor()))
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro de configuración " + clave));
    }

    private void guardar(String clave, BigDecimal valor) {
        Configuracion configuracion = configuracionRepository.findById(clave)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro de configuración " + clave));
        configuracion.setValor(valor.stripTrailingZeros().toPlainString());
    }
}
