package com.example.billme.dto;

import com.example.billme.model.Usuario;
import jakarta.validation.constraints.*;

public record UsuarioRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(min = Usuario.PASSWORD_MINIMO, max = 72) String password
) {}
