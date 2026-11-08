package com.example.billme.service;

import com.example.billme.calculo.CalculadoraPeso;
import com.example.billme.calculo.CalculadoraPrecio;
import com.example.billme.calculo.DescripcionGenerica;
import com.example.billme.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculoService {

    private final ConfiguracionService configuracionService;
    private final PrecioPorPesoService precioPorPesoService;

    public CalculoPesoResponse calcularPeso(CalculoPesoRequest request) {
        try {
            BigDecimal densidad = configuracionService.densidadKgIn3();
            BigDecimal peso = CalculadoraPeso.pesoUnitKg(
                    request.forma(),
                    request.ancho(),
                    request.largo(),
                    request.espesor(),
                    request.ala2(),
                    densidad
            );
            String descripcion = DescripcionGenerica.describir(
                    request.forma(),
                    request.ancho(),
                    request.largo(),
                    request.espesor(),
                    request.ala2()
            );
            return new CalculoPesoResponse(peso, descripcion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public PrecioCalculadoResponse calcularPrecioPorPeso(PrecioPorPesoRequest request) {
        try {
            BigDecimal tipoCambio = request.tipoCambio() != null ? request.tipoCambio() : configuracionService.tipoCambio();
            BigDecimal pu = precioPorPesoService.calcular(
                    request.pesoUnitKg(),
                    request.tarifa(),
                    request.moneda(),
                    tipoCambio
            );
            return new PrecioCalculadoResponse(pu);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public PrecioCalculadoResponse calcularPrecioPorMetro(PrecioPorMetroRequest request) {
        try {
            BigDecimal pu = CalculadoraPrecio.porMetro(
                    request.precioReferencia(),
                    request.largoReferencia(),
                    request.largoNuevo()
            );
            return new PrecioCalculadoResponse(pu);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
