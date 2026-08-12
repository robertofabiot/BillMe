package com.example.billme.dto;

public record ClienteRequest(
        String nombre,
        String telefono,
        String detalles
) {}
