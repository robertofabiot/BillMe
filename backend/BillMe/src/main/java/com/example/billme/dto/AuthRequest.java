package com.example.billme.dto;

import jakarta.validation.constraints.*;
public record AuthRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
