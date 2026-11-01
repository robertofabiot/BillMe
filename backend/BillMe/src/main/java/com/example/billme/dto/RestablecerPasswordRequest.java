package com.example.billme.dto;

import com.example.billme.model.Usuario;
import jakarta.validation.constraints.*;

/** Restablecer la contraseña de otro usuario (por ejemplo, si la olvidó). */
public record RestablecerPasswordRequest(
        @NotBlank @Size(min = Usuario.PASSWORD_MINIMO, max = 72) String nueva
) {}
