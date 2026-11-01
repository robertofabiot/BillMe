package com.example.billme.dto;

public record AuthResponse(
    String token,
    String username,
    String nombre
) {}
