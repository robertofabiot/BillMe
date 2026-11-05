package com.example.billme.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        UUID proveedorId,
        String proveedorNombre,
        LocalDate fecha,
        String numeroFactura,
        String nota
) {}
