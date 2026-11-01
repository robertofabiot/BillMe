package com.example.billme.dto;

import java.util.List;

/**
 * Cuerpo de todas las respuestas de error de la API.
 * {@code errores} solo viene cuando falla la validación de campos.
 */
public record ErrorResponse(
        String mensaje,
        List<CampoError> errores
) {
    public record CampoError(String campo, String mensaje) {}

    public static ErrorResponse de(String mensaje) {
        return new ErrorResponse(mensaje, List.of());
    }
}
