package com.example.billme.calculo;

import com.example.billme.enums.FormaCalculo;

import java.math.BigDecimal;
import java.math.MathContext;

public final class CalculadoraPeso {

    private CalculadoraPeso() {
    }

    public static BigDecimal pesoUnitKg(FormaCalculo forma, Medida ancho, Medida largo, Medida espesor,
                                        Medida ala2, BigDecimal densidad) {
        if (forma == null || forma == FormaCalculo.NINGUNA) {
            throw new IllegalArgumentException("Forma no soportada para cálculo de peso: " + forma);
        }
        if (ancho == null || largo == null || espesor == null) {
            throw new IllegalArgumentException("Las medidas ancho, largo y espesor son obligatorias");
        }
        if (densidad == null || densidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La densidad debe ser mayor a cero");
        }

        BigDecimal peso;
        if (forma == FormaCalculo.PLACA || forma == FormaCalculo.PLATINA) {
            peso = ancho.enPulgadas()
                    .multiply(largo.enPulgadas(), MathContext.DECIMAL64)
                    .multiply(espesor.enPulgadas(), MathContext.DECIMAL64)
                    .multiply(densidad, MathContext.DECIMAL64);
        } else if (forma == FormaCalculo.ANGULAR) {
            if (ala2 == null) {
                throw new IllegalArgumentException("El angular requiere ala2");
            }
            BigDecimal ala1In = ancho.enPulgadas();
            BigDecimal ala2In = ala2.enPulgadas();
            BigDecimal espIn = espesor.enPulgadas();
            BigDecimal largoIn = largo.enPulgadas();
            BigDecimal sumaAlasMenosEsp = ala1In.add(ala2In).subtract(espIn);
            peso = sumaAlasMenosEsp
                    .multiply(espIn, MathContext.DECIMAL64)
                    .multiply(largoIn, MathContext.DECIMAL64)
                    .multiply(densidad, MathContext.DECIMAL64);
        } else {
            throw new IllegalArgumentException("Forma no soportada para cálculo de peso: " + forma);
        }

        return CalculoDocumento.r2(peso);
    }
}
