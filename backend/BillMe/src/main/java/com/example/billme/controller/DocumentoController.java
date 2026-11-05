package com.example.billme.controller;

import com.example.billme.dto.CambioEstadoRequest;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.PaginaResponse;
import com.example.billme.dto.RevisionResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.service.DocumentoService;
import com.example.billme.exportacion.ArchivoExcel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cotizaciones y facturas. */
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @GetMapping
    public ResponseEntity<PaginaResponse<DocumentoResponse>> listar(
            @RequestParam(required = false) TipoDocumento tipo,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID proyectoId,
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") int pagina,
            @RequestParam(required = false, defaultValue = "50") int tamano) {
        int tamanoFinal = Math.max(1, Math.min(tamano, 200));
        int paginaFinal = Math.max(0, pagina);
        Pageable pageable = PageRequest.of(paginaFinal, tamanoFinal);
        return ResponseEntity.ok(documentoService.filtrar(tipo, estado, clienteId, proyectoId, productoId, desde, hasta, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(documentoService.obtener(id));
    }

    @GetMapping("/{id}/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @PathVariable UUID id,
            @RequestParam(required = false) String encabezado) {
        ArchivoExcel archivo = documentoService.exportarExcel(id, encabezado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo.contenido());
    }

    @PostMapping
    public ResponseEntity<DocumentoResponse> crear(@Valid @RequestBody DocumentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody DocumentoRequest request) {
        return ResponseEntity.ok(documentoService.actualizar(id, request));
    }

    @GetMapping("/{id}/revisiones")
    public ResponseEntity<List<RevisionResponse>> revisiones(@PathVariable UUID id) {
        return ResponseEntity.ok(documentoService.revisiones(id));
    }

    /** Crea la factura de una cotización; {@code fecha} es la de la factura (hoy si no se indica). */
    @PostMapping("/{id}/convertir-a-factura")
    public ResponseEntity<DocumentoResponse> convertirAFactura(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.convertirAFactura(id, fecha));
    }

    /** Rechazar o reabrir una cotización. */
    @PostMapping("/{id}/estado")
    public ResponseEntity<DocumentoResponse> cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(documentoService.cambiarEstadoCotizacion(id, request.estado()));
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<DocumentoResponse> anular(@PathVariable UUID id) {
        return ResponseEntity.ok(documentoService.anular(id));
    }
}
