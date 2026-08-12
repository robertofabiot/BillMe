package com.example.billme.dto;

import com.example.billme.enums.Rol;

public record AuthResponse(
    String token,
    String username,
    String nombre,
    Rol rol
) {}
