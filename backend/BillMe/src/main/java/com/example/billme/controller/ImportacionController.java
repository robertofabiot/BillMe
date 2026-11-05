package com.example.billme.controller;

import com.example.billme.dto.DecisionConciliacion;
import com.example.billme.dto.ImportacionCotizacionesResponse;
import com.example.billme.dto.ImportacionListaResponse;
import com.example.billme.service.ImportacionCotizacionesService;
import com.example.billme.service.ImportacionListaService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/importaciones")
@RequiredArgsConstructor
public class ImportacionController {

    private final ImportacionListaService importacionListaService;
    private final ImportacionCotizacionesService importacionCotizacionesService;
    private final ObjectMapper objectMapper;

    /** Revisa el archivo sin guardar nada. */
    @PostMapping("/lista/vista-previa")
    public ResponseEntity<ImportacionListaResponse> vistaPrevia(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        try (InputStream contenido = archivo.getInputStream()) {
            return ResponseEntity.ok(importacionListaService.vistaPrevia(contenido));
        }
    }

    /** Importa la lista; {@code fecha} es desde cuándo rigen los precios (hoy si no se indica). */
    @PostMapping("/lista/aplicar")
    public ResponseEntity<ImportacionListaResponse> aplicar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha)
            throws IOException {
        try (InputStream contenido = archivo.getInputStream()) {
            return ResponseEntity.ok(importacionListaService.aplicar(contenido, fecha));
        }
    }

    /** Revisa el archivo de cotizaciones sin guardar nada. */
    @PostMapping("/cotizaciones/vista-previa")
    public ResponseEntity<ImportacionCotizacionesResponse> vistaPreviaCotizaciones(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("clienteId") UUID clienteId) throws IOException {
        try (InputStream contenido = archivo.getInputStream()) {
            return ResponseEntity.ok(importacionCotizacionesService.vistaPrevia(contenido, clienteId));
        }
    }

    /** Importa las cotizaciones agrupadas en facturas con conciliación. */
    @PostMapping("/cotizaciones/aplicar")
    public ResponseEntity<ImportacionCotizacionesResponse> aplicarCotizaciones(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("clienteId") UUID clienteId,
            @RequestParam(value = "decisiones", required = false) String decisionesJson) throws IOException {
        List<DecisionConciliacion> decisiones = null;
        if (decisionesJson != null && !decisionesJson.isBlank()) {
            decisiones = objectMapper.readValue(decisionesJson, new TypeReference<List<DecisionConciliacion>>() {});
        }
        try (InputStream contenido = archivo.getInputStream()) {
            return ResponseEntity.ok(importacionCotizacionesService.aplicar(contenido, clienteId, decisiones));
        }
    }
}
