package com.example.billme.dto;

import com.example.billme.calculo.Medida;
import com.example.billme.enums.FormaCalculo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CalculoPesoRequest(
        @NotNull FormaCalculo forma,
        @NotNull @Valid Medida ancho,
        @NotNull @Valid Medida largo,
        @NotNull @Valid Medida espesor,
        @Valid Medida ala2
) {}
