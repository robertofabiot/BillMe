package com.example.billme.importacion;

import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Recorre una hoja de cálculo Excel (.xlsx / .xlsm) en modo streaming SAX fila por fila,
 * resolviendo strings compartidas y clasificando celdas numéricas sin cargar el DOM completo en memoria.
 */
public final class LectorExcel {

    private LectorExcel() {}

    public interface ManejadorFila {
        void fila(int numero, Map<String, String> celdas, Set<String> numericas) throws SAXException;
    }

    /** Se lanza desde el manejador para dejar de leer una hoja. */
    public static final class DetenerLectura extends SAXException {
        private static final long serialVersionUID = 1L;
    }

    public static void recorrer(InputStream hoja, ReadOnlySharedStringsTable textos, ManejadorFila manejador) throws Exception {
        Objects.requireNonNull(hoja, "El stream de la hoja no puede ser nulo");
        Objects.requireNonNull(manejador, "El manejador de filas no puede ser nulo");

        XMLReader xml = XMLHelper.newXMLReader();
        xml.setContentHandler(new ManejadorHoja(textos, manejador));
        try {
            xml.parse(new InputSource(hoja));
        } catch (DetenerLectura e) {
            // Interrupción controlada de la lectura de la hoja
        } catch (SAXException e) {
            if (e.getCause() instanceof DetenerLectura) {
                return;
            }
            throw e;
        }
    }

    private static final class ManejadorHoja extends DefaultHandler {
        private final ReadOnlySharedStringsTable textos;
        private final ManejadorFila manejador;
        private final StringBuilder valor = new StringBuilder();
        private final Map<String, String> celdas = new HashMap<>();
        private final Set<String> numericas = new HashSet<>();

        private int fila;
        private String columna;
        private String tipo;
        private boolean leyendoValor;

        ManejadorHoja(ReadOnlySharedStringsTable textos, ManejadorFila manejador) {
            this.textos = textos;
            this.manejador = manejador;
        }

        @Override
        public void startElement(String uri, String local, String nombre, Attributes atributos) {
            switch (local) {
                case "row" -> {
                    celdas.clear();
                    numericas.clear();
                    String r = atributos.getValue("r");
                    fila = r != null ? Integer.parseInt(r) : fila + 1;
                }
                case "c" -> {
                    String r = atributos.getValue("r");
                    columna = r != null ? r.replaceAll("\\d", "") : "";
                    tipo = atributos.getValue("t");
                    valor.setLength(0);
                }
                case "v", "t" -> leyendoValor = true;
                default -> {}
            }
        }

        @Override
        public void characters(char[] ch, int inicio, int largo) {
            if (leyendoValor) {
                valor.append(ch, inicio, largo);
            }
        }

        @Override
        public void endElement(String uri, String local, String nombre) throws SAXException {
            switch (local) {
                case "v", "t" -> leyendoValor = false;
                case "c" -> {
                    String texto = valor.toString();
                    if ("s".equals(tipo) && !texto.isEmpty() && textos != null) {
                        try {
                            var item = textos.getItemAt(Integer.parseInt(texto));
                            texto = item != null && item.getString() != null ? item.getString() : "";
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (!texto.isBlank() && !"b".equals(tipo)) {
                        celdas.put(columna, texto.trim());
                        if (tipo == null || "n".equals(tipo)) {
                            numericas.add(columna);
                        }
                    }
                }
                case "row" -> manejador.fila(fila, celdas, numericas);
                default -> {}
            }
        }
    }
}
