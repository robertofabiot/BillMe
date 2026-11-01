package com.example.billme.dto;

import com.example.billme.model.Usuario;
import jakarta.validation.constraints.*;

/** Cambio de la propia contraseña: exige la actual. */
public record CambioPasswordRequest(
        @NotBlank String actual,
        @NotBlank @Size(min = Usuario.PASSWORD_MINIMO, max = 72) String nueva
) {}
