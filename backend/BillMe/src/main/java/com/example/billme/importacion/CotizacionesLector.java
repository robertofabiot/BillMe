package com.example.billme.importacion;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.dto.FilaProblema;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lee el libro de cotizaciones (cotizaciones1) en modo streaming SAX fila por fila.
 * Busca la hoja "cotizaciones" (ignora mayúsculas/minúsculas) o, en su defecto,
 * la primera hoja que contenga los 4 encabezados obligatorios en sus primeras 20 filas:
 * fecha, cantidad, nombre_cotizacion y precio_unitario_cotizacion.
 */
@Component
public class CotizacionesLector {

    private static final int FILAS_PARA_ENCONTRAR_ENCABEZADO = 20;
    private static final DateTimeFormatter FORMATO_FECHA_TEXTO = DateTimeFormatter.ofPattern("d/M/yyyy");

    public LecturaCotizaciones leer(InputStream archivo) {
        try (OPCPackage paquete = OPCPackage.open(archivo)) {
            XSSFReader lector = new XSSFReader(paquete);
            ReadOnlySharedStringsTable textos = new ReadOnlySharedStringsTable(paquete);

            // 1ª pasada: buscar hoja llamada "cotizaciones" (ignora mayúsculas/minúsculas)
            var hojas = (XSSFReader.SheetIterator) lector.getSheetsData();
            while (hojas.hasNext()) {
                try (InputStream hoja = hojas.next()) {
                    if (hojas.getSheetName().equalsIgnoreCase("cotizaciones")) {
                        ManejadorCotizaciones manejador = new ManejadorCotizaciones();
                        LectorExcel.recorrer(hoja, textos, manejador);
                        if (manejador.columnas != null) {
                            return new LecturaCotizaciones(hojas.getSheetName(), manejador.filas, manejador.incompletas);
                        }
                    }
                }
            }

            // 2ª pasada: si no hay hoja "cotizaciones" válida, buscar primera hoja con los 4 encabezados obligatorios
            hojas = (XSSFReader.SheetIterator) lector.getSheetsData();
            while (hojas.hasNext()) {
                try (InputStream hoja = hojas.next()) {
                    ManejadorCotizaciones manejador = new ManejadorCotizaciones();
                    LectorExcel.recorrer(hoja, textos, manejador);
                    if (manejador.columnas != null) {
                        return new LecturaCotizaciones(hojas.getSheetName(), manejador.filas, manejador.incompletas);
                    }
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo leer el archivo. Debe ser un Excel (.xlsx o .xlsm).", e);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No se encontró una hoja con fecha, cantidad, nombre_cotizacion y precio_unitario_cotizacion.");
    }

    private static LocalDate parsearFecha(String texto, boolean esNumerica) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpia = texto.trim();
        if (esNumerica) {
            try {
                double serial = Double.parseDouble(limpia);
                var ldt = DateUtil.getLocalDateTime(serial);
                if (ldt != null) {
                    return ldt.toLocalDate();
                }
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDate.parse(limpia, FORMATO_FECHA_TEXTO);
        } catch (DateTimeParseException ignored) {
        }
        try {
            double serial = Double.parseDouble(limpia);
            var ldt = DateUtil.getLocalDateTime(serial);
            if (ldt != null) {
                return ldt.toLocalDate();
            }
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(limpia);
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    static String parsearCodigo(String texto, boolean esNumerica) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if ("0".equals(limpio)) {
            return null;
        }
        if (esNumerica) {
            try {
                BigDecimal bd = new BigDecimal(limpio);
                long val = bd.longValue();
                if (val == 0) {
                    return null;
                }
                return String.format(Locale.ROOT, "%09d", val);
            } catch (Exception ignored) {
            }
        }
        return limpio;
    }

    private static BigDecimal parsearNumero(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.replace(",", "").replace("C$", "").replace("$", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parsearNumeroPositivo(String texto) {
        BigDecimal n = parsearNumero(texto);
        return n != null && n.compareTo(BigDecimal.ZERO) > 0 ? n : null;
    }

    private static BigDecimal parsearPeso(String rawPesoUnit, String rawPesoTotal, BigDecimal cantidad) {
        BigDecimal pesoUnit = parsearNumeroPositivo(rawPesoUnit);
        if (pesoUnit != null) {
            return pesoUnit;
        }
        BigDecimal pesoTotal = parsearNumeroPositivo(rawPesoTotal);
        if (pesoTotal != null && cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0) {
            return pesoTotal.divide(cantidad, MathContext.DECIMAL64);
        }
        return null;
    }

    private static String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        if (t.endsWith(".0") && t.substring(0, t.length() - 2).chars().allMatch(Character::isDigit)) {
            return t.substring(0, t.length() - 2);
        }
        return t;
    }

    private static String recortar(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }

    /** Columnas (letras) detectadas en la fila de encabezados. */
    private record Columnas(
            String fecha,
            String cantidad,
            String nombreCotizacion,
            String precioUnitarioCotizacion,
            String codProducto,
            String pesoUnitario,
            String pesoTotal,
            String precioUnitarioFacturado,
            String observaciones,
            String entregado,
            String proyecto,
            String numFacturaDia,
            String cliente
    ) {
        static Columnas detectar(Map<String, String> celdas) {
            String colFecha = null;
            String colCantidad = null;
            String colNombreCotizacion = null;
            String colPrecioUnitarioCotizacion = null;
            String colCodProducto = null;
            String colPesoUnitario = null;
            String colPesoTotal = null;
            String colPrecioUnitarioFacturado = null;
            String colObservaciones = null;
            String colEntregado = null;
            String colProyecto = null;
            String colNumFacturaDia = null;
            String colCliente = null;

            for (Map.Entry<String, String> e : celdas.entrySet()) {
                String encabezado = e.getValue().trim().toLowerCase(Locale.ROOT);
                switch (encabezado) {
                    case "fecha" -> { if (colFecha == null) colFecha = e.getKey(); }
                    case "cantidad" -> { if (colCantidad == null) colCantidad = e.getKey(); }
                    case "nombre_cotizacion" -> { if (colNombreCotizacion == null) colNombreCotizacion = e.getKey(); }
                    case "precio_unitario_cotizacion" -> { if (colPrecioUnitarioCotizacion == null) colPrecioUnitarioCotizacion = e.getKey(); }
                    case "cod_producto" -> { if (colCodProducto == null) colCodProducto = e.getKey(); }
                    case "peso_unitario" -> { if (colPesoUnitario == null) colPesoUnitario = e.getKey(); }
                    case "peso_total" -> { if (colPesoTotal == null) colPesoTotal = e.getKey(); }
                    case "precio_unitario_facturado" -> { if (colPrecioUnitarioFacturado == null) colPrecioUnitarioFacturado = e.getKey(); }
                    case "observaciones" -> { if (colObservaciones == null) colObservaciones = e.getKey(); }
                    case "entregado" -> { if (colEntregado == null) colEntregado = e.getKey(); }
                    case "proyecto" -> { if (colProyecto == null) colProyecto = e.getKey(); }
                    case "num_factura_dia" -> { if (colNumFacturaDia == null) colNumFacturaDia = e.getKey(); }
                    case "cliente" -> { if (colCliente == null) colCliente = e.getKey(); }
                    default -> {}
                }
            }

            if (colFecha != null && colCantidad != null && colNombreCotizacion != null && colPrecioUnitarioCotizacion != null) {
                return new Columnas(colFecha, colCantidad, colNombreCotizacion, colPrecioUnitarioCotizacion,
                        colCodProducto, colPesoUnitario, colPesoTotal, colPrecioUnitarioFacturado,
                        colObservaciones, colEntregado, colProyecto, colNumFacturaDia, colCliente);
            }
            return null;
        }
    }

    private static final class ManejadorCotizaciones implements LectorExcel.ManejadorFila {
        private final List<FilaCotizacion> filas = new ArrayList<>();
        private final List<FilaProblema> incompletas = new ArrayList<>();
        private Columnas columnas;

        @Override
        public void fila(int numero, Map<String, String> celdas, Set<String> numericas) throws SAXException {
            if (columnas == null) {
                columnas = Columnas.detectar(celdas);
                if (columnas == null && numero >= FILAS_PARA_ENCONTRAR_ENCABEZADO) {
                    throw new LectorExcel.DetenerLectura();
                }
                return;
            }

            if (celdas.isEmpty()) {
                return;
            }

            String rawCantidad = celdas.get(columnas.cantidad());
            String rawDescripcion = celdas.get(columnas.nombreCotizacion());

            boolean tieneCantidad = rawCantidad != null && !rawCantidad.isBlank();
            boolean tieneDescripcion = rawDescripcion != null && !rawDescripcion.isBlank();

            // Fila vacía (sin cantidad ni nombre_cotizacion): ignorar en silencio
            if (!tieneCantidad && !tieneDescripcion) {
                return;
            }

            // Validaciones para fila incompleta
            String rawFecha = celdas.get(columnas.fecha());
            boolean fechaNumerica = numericas.contains(columnas.fecha());
            LocalDate fecha = parsearFecha(rawFecha, fechaNumerica);

            BigDecimal cantidad = parsearNumero(rawCantidad);
            boolean cantidadValida = cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0;

            String rawPrecio = celdas.get(columnas.precioUnitarioCotizacion());
            BigDecimal precio = parsearNumero(rawPrecio);
            boolean precioValido = precio != null && precio.compareTo(BigDecimal.ZERO) > 0;

            String descripcion = tieneDescripcion ? rawDescripcion.trim().replaceAll("\\s+", " ") : null;
            boolean descripcionValida = descripcion != null && !descripcion.isBlank();

            List<String> motivos = new ArrayList<>();
            if (fecha == null) {
                motivos.add("fecha inválida o ausente");
            }
            if (!cantidadValida) {
                motivos.add("cantidad inválida o no positiva");
            }
            if (!descripcionValida) {
                motivos.add("descripción vacía");
            }
            if (!precioValido) {
                motivos.add("precio unitario inválido o no positivo");
            }

            if (!motivos.isEmpty()) {
                incompletas.add(new FilaProblema(numero, String.join(", ", motivos)));
                return;
            }

            // Campos opcionales
            String rawCodigo = columnas.codProducto() != null ? celdas.get(columnas.codProducto()) : null;
            boolean codigoNumerico = columnas.codProducto() != null && numericas.contains(columnas.codProducto());
            String codigo = parsearCodigo(rawCodigo, codigoNumerico);

            String rawPesoUnit = columnas.pesoUnitario() != null ? celdas.get(columnas.pesoUnitario()) : null;
            String rawPesoTotal = columnas.pesoTotal() != null ? celdas.get(columnas.pesoTotal()) : null;
            BigDecimal pesoUnitKg = parsearPeso(rawPesoUnit, rawPesoTotal, cantidad);

            BigDecimal precioUnit = CalculoDocumento.r2(precio);

            String rawPrecioFacturado = columnas.precioUnitarioFacturado() != null
                    ? celdas.get(columnas.precioUnitarioFacturado())
                    : null;
            BigDecimal costoSinIva = parsearNumero(rawPrecioFacturado);

            String observaciones = columnas.observaciones() != null
                    ? limpiarTexto(celdas.get(columnas.observaciones()))
                    : null;

            String entregado = columnas.entregado() != null
                    ? recortar(celdas.get(columnas.entregado()), 200)
                    : null;

            String proyecto = columnas.proyecto() != null
                    ? limpiarTexto(celdas.get(columnas.proyecto()))
                    : null;

            String numFacturaDia = columnas.numFacturaDia() != null
                    ? limpiarTexto(celdas.get(columnas.numFacturaDia()))
                    : null;

            String cliente = columnas.cliente() != null
                    ? limpiarTexto(celdas.get(columnas.cliente()))
                    : null;

            filas.add(new FilaCotizacion(
                    numero,
                    codigo,
                    numFacturaDia,
                    proyecto,
                    fecha,
                    cantidad,
                    descripcion,
                    pesoUnitKg,
                    precioUnit,
                    costoSinIva,
                    observaciones,
                    entregado,
                    cliente
            ));
        }
    }
}
