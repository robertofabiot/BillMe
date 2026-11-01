package com.example.billme.controller;

import com.example.billme.dto.ImportacionListaResponse;
import com.example.billme.service.ImportacionListaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/importaciones")
@RequiredArgsConstructor
public class ImportacionController {

    private final ImportacionListaService importacionListaService;

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
}
