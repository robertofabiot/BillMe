package com.example.billme.calculo;

import com.example.billme.enums.FormaCalculo;

public final class DescripcionGenerica {

    private DescripcionGenerica() {
    }

    public static String describir(FormaCalculo forma, Medida ancho, Medida largo, Medida espesor, Medida ala2) {
        if (forma == null) {
            throw new IllegalArgumentException("La forma no puede ser nula");
        }
        if (ancho == null || largo == null || espesor == null) {
            throw new IllegalArgumentException("Las medidas ancho, largo y espesor son obligatorias");
        }
        return switch (forma) {
            case PLACA -> "Placa " + ancho.paraDescripcion() + " x " + largo.paraDescripcion() + " x " + espesor.paraDescripcion();
            case PLATINA -> "Platina " + ancho.paraDescripcion() + " x " + espesor.paraDescripcion() + " x " + largo.paraDescripcion();
            case ANGULAR -> {
                if (ala2 == null) {
                    throw new IllegalArgumentException("El angular requiere ala2");
                }
                yield "Angular " + ancho.paraDescripcion() + " x " + ala2.paraDescripcion() + " x " + espesor.paraDescripcion() + " x " + largo.paraDescripcion();
            }
            case NINGUNA -> throw new IllegalArgumentException("No se genera descripción genérica para forma NINGUNA");
        };
    }
}
