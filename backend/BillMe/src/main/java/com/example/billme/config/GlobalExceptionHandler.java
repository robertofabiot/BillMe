package com.example.billme.config;

import com.example.billme.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas JSON con la forma {@link ErrorResponse},
 * para que el frontend siempre pueda mostrar un mensaje legible.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("nombre", "Nombre"), Map.entry("telefono", "Teléfono"),
            Map.entry("descuentoPct", "Descuento"), Map.entry("tarifaKg", "Tarifa por kg"),
            Map.entry("monto", "Monto"), Map.entry("fechaPago", "Fecha"), Map.entry("nota", "Nota"),
            Map.entry("clienteId", "Cliente"), Map.entry("fecha", "Fecha"), Map.entry("lineas", "Líneas"),
            Map.entry("cantidad", "Cantidad"), Map.entry("precioUnit", "Precio unitario"),
            Map.entry("pesoUnitKg", "Peso"), Map.entry("descripcion", "Descripción"),
            Map.entry("lugarEntrega", "Lugar de entrega"), Map.entry("codigoInterno", "Código"),
            Map.entry("nombrePrincipal", "Nombre"), Map.entry("username", "Usuario"),
            Map.entry("password", "Contraseña"), Map.entry("nueva", "Contraseña nueva"),
            Map.entry("actual", "Contraseña actual"), Map.entry("desde", "Desde"), Map.entry("hasta", "Hasta"),
            Map.entry("proveedorId", "Proveedor"), Map.entry("precioSinIva", "Precio sin IVA"));

    private static String mensajeDeCampo(FieldError e) {
        String campo = e.getField().substring(e.getField().lastIndexOf('.') + 1);
        String etiqueta = ETIQUETAS.getOrDefault(campo, campo);
        String problema = switch (String.valueOf(e.getCode())) {
            case "NotNull", "NotBlank", "NotEmpty" -> "es obligatorio";
            case "Size" -> "es demasiado largo";
            case "Positive" -> "debe ser mayor a 0";
            case "PositiveOrZero", "DecimalMin", "Min" -> "tiene un valor muy bajo";
            case "DecimalMax", "Max" -> "tiene un valor muy alto";
            default -> "no es válido";
        };
        return etiqueta + ": " + problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        List<ErrorResponse.CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.CampoError(e.getField(), mensajeDeCampo(e)))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Hay datos inválidos en el formulario", errores));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> conEstado(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : mensajePorDefecto(ex.getStatusCode());
        return ResponseEntity.status(ex.getStatusCode()).body(ErrorResponse.de(mensaje));
    }

    /** Cubre las excepciones estándar de Spring MVC (404 de ruta, 405, parámetro faltante, etc.). */
    @ExceptionHandler({ErrorResponseException.class, ServletException.class})
    public ResponseEntity<ErrorResponse> errorMvc(Exception ex) {
        if (!(ex instanceof org.springframework.web.ErrorResponse errorMvc)) {
            return inesperado(ex);
        }
        HttpStatusCode status = errorMvc.getStatusCode();
        return ResponseEntity.status(status).body(ErrorResponse.de(mensajePorDefecto(status)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de("Los datos enviados no tienen un formato válido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de("El parámetro '" + ex.getName() + "' no tiene un formato válido"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ErrorResponse.de("El archivo es demasiado grande (máximo 20 MB)"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de("No se pudo guardar: hay un dato repetido o fuera de rango"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.de("Usuario o contraseña incorrectos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(mensajePorDefecto(HttpStatus.FORBIDDEN)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.de("Ocurrió un error inesperado. Intenta de nuevo."));
    }

    private static String mensajePorDefecto(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "La petición no es válida";
            case 401 -> "Tu sesión no es válida, vuelve a iniciar sesión";
            case 403 -> "No tienes permiso para hacer esto";
            case 404 -> "No se encontró lo que buscabas";
            case 405 -> "Operación no permitida";
            case 409 -> "La operación choca con datos existentes";
            default -> "Ocurrió un error (" + status.value() + ")";
        };
    }
}
