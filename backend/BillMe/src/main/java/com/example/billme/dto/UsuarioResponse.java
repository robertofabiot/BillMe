package com.example.billme.dto;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String username,
        String nombre,
        boolean activo
) {}
