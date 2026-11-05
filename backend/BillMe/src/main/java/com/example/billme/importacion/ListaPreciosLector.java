package com.example.billme.importacion;

import com.example.billme.util.NormalizadorTexto;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Lee la lista de precios del proveedor (.xlsx o .xlsm) en modo streaming, sin cargar el
 * libro entero en memoria. Busca en cada hoja una fila de encabezados con código,
 * descripción y precio (por ejemplo "Número de artículo", "Descripción del artículo",
 * "Nueva Lista Precios en Cordobas C$ antes de IVA") y lee las filas que siguen.
 */
@Component
public class ListaPreciosLector {

    /** Si en las primeras filas de una hoja no aparecen los encabezados, se pasa a la siguiente. */
    private static final int FILAS_PARA_ENCONTRAR_ENCABEZADO = 20;

    private static final Map<String, String> UNIDADES = Map.ofEntries(
            Map.entry("kg", "KG"), Map.entry("kgs", "KG"), Map.entry("kilogramos", "KG"),
            Map.entry("und", "UND"), Map.entry("unidad", "UND"), Map.entry("uni", "UND"), Map.entry("u", "UND"),
            Map.entry("metro", "METRO"), Map.entry("metros", "METRO"), Map.entry("m", "METRO"), Map.entry("mts", "METRO"),
            Map.entry("galon", "GALON"), Map.entry("gal", "GALON"),
            Map.entry("rol", "ROLLO"), Map.entry("rollo", "ROLLO"),
            Map.entry("libra", "LB"), Map.entry("lb", "LB"),
            Map.entry("quintal", "QQ"), Map.entry("qq", "QQ"),
            Map.entry("caja", "CAJA"), Map.entry("cubeta", "CUBETA"));

    public LecturaListaPrecios leer(InputStream archivo) {
        try (OPCPackage paquete = OPCPackage.open(archivo)) {
            XSSFReader lector = new XSSFReader(paquete);
            ReadOnlySharedStringsTable textos = new ReadOnlySharedStringsTable(paquete);
            XSSFReader.SheetIterator hojas = (XSSFReader.SheetIterator) lector.getSheetsData();
            while (hojas.hasNext()) {
                try (InputStream hoja = hojas.next()) {
                    ManejadorLista manejador = new ManejadorLista();
                    LectorExcel.recorrer(hoja, textos, manejador);
                    if (manejador.columnas != null) {
                        return new LecturaListaPrecios(hojas.getSheetName(), manejador.filas, manejador.ignoradas);
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
                "No se encontró una hoja con columnas de código, descripción y precio.");
    }

    static String normalizarUnidad(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return "UND";
        }
        String limpia = unidad.trim();
        return UNIDADES.getOrDefault(limpia.toLowerCase(Locale.ROOT), limpia.toUpperCase(Locale.ROOT));
    }

    /** Columnas (letras) de cada dato en la hoja. */
    private record Columnas(String codigo, String descripcion, String unidad, String peso, String precio) {

        static Columnas detectar(Map<String, String> encabezados) {
            String codigo = null, descripcion = null, unidad = null, peso = null, precio = null;
            for (Map.Entry<String, String> e : encabezados.entrySet()) {
                String texto = NormalizadorTexto.normalizar(e.getValue());
                if (texto.contains("descripcion") || texto.equals("nombre")) {
                    descripcion = descripcion == null ? e.getKey() : descripcion;
                } else if (texto.contains("codigo") || (texto.contains("numero") && texto.contains("articulo"))) {
                    codigo = codigo == null ? e.getKey() : codigo;
                } else if (texto.contains("unidad")) {
                    unidad = unidad == null ? e.getKey() : unidad;
                } else if (texto.contains("peso")) {
                    peso = peso == null ? e.getKey() : peso;
                } else if (texto.contains("precio") && (precio == null || texto.contains("lista"))) {
                    precio = e.getKey();
                }
            }
            return codigo != null && descripcion != null && precio != null
                    ? new Columnas(codigo, descripcion, unidad, peso, precio)
                    : null;
        }
    }

    /** Recorre las celdas de una hoja con LectorExcel y arma las filas. */
    private static final class ManejadorLista implements LectorExcel.ManejadorFila {

        private final Set<String> codigosVistos = new HashSet<>();
        private final List<FilaListaPrecios> filas = new ArrayList<>();

        private Columnas columnas;
        private int ignoradas;

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
            String codigo = codigo(celdas.get(columnas.codigo()), numericas.contains(columnas.codigo()));
            String descripcion = celdas.get(columnas.descripcion());
            if (codigo == null || descripcion == null || !codigosVistos.add(codigo)) {
                ignoradas++;
                return;
            }
            filas.add(new FilaListaPrecios(
                    numero,
                    codigo,
                    descripcion.replaceAll("\\s+", " "),
                    normalizarUnidad(columnas.unidad() != null ? celdas.get(columnas.unidad()) : null),
                    numero(columnas.peso() != null ? celdas.get(columnas.peso()) : null),
                    numero(celdas.get(columnas.precio()))));
        }

        /**
         * Los códigos de texto se respetan tal cual (con sus ceros a la izquierda);
         * uno guardado como número no debe quedar como "1001169.0".
         */
        private static String codigo(String texto, boolean esNumero) {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            if (esNumero) {
                BigDecimal numero = numero(texto);
                if (numero != null && numero.stripTrailingZeros().scale() <= 0) {
                    return numero.toBigInteger().toString();
                }
            }
            return texto.trim();
        }

        private static BigDecimal numero(String texto) {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            try {
                BigDecimal numero = new BigDecimal(texto.replace(",", "").replace("C$", "").trim());
                return numero.signum() > 0 ? numero : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
