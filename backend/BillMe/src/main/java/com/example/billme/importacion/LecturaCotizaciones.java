package com.example.billme.importacion;

import com.example.billme.dto.FilaProblema;
import java.util.List;

public record LecturaCotizaciones(
        String hoja,
        List<FilaCotizacion> filas,
        List<FilaProblema> incompletas
) {}
