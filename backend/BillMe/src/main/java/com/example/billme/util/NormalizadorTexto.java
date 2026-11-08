package com.example.billme.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lleva los nombres de productos a una forma canónica para compararlos y buscarlos:
 * sin acentos, en minúsculas, sin comillas de pulgadas ni puntos de abreviatura,
 * con medidas escritas siempre igual ("2\" x 1.50mm" y "2 x 1.5 mm" quedan iguales).
 */
public final class NormalizadorTexto {

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");
    private static final Pattern FRACCION_CON_GUION = Pattern.compile("(\\d)-(\\d+/\\d+)");
    private static final Pattern SIMBOLOS = Pattern.compile("[^a-z0-9./ ]");
    private static final Pattern PUNTO_SUELTO = Pattern.compile("(?<!\\d)\\.|\\.(?!\\d)");
    private static final Pattern CEROS_DECIMALES = Pattern.compile("(\\d+)\\.(\\d*?)0+(?!\\d)");
    private static final Pattern POR_ENTRE_NUMEROS = Pattern.compile("(\\d)\\s*x\\s*(?=\\d)");
    private static final Pattern NUMERO_Y_UNIDAD = Pattern.compile("(\\d)\\s+(mm|cm|mts|mt|m|kg)\\b");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private NormalizadorTexto() {
    }

    /** Minúsculas y sin acentos; conserva guiones y números, para buscar folios y nombres. */
    public static String sinAcentos(String texto) {
        if (texto == null) {
            return "";
        }
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return DIACRITICOS.matcher(s).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String s = Normalizer.normalize(texto, Normalizer.Form.NFKD);   // "½" pasa a "1⁄2"
        s = DIACRITICOS.matcher(s).replaceAll("").toLowerCase(Locale.ROOT);
        s = s.replace('⁄', '/').replace('×', 'x');
        s = FRACCION_CON_GUION.matcher(s).replaceAll("$1 $2");         // 1-1/2 → 1 1/2
        s = s.replaceAll("(\\d)(\\d/\\d)", "$1 $2");                     // 221/2 (de "22½") → 22 1/2
        s = SIMBOLOS.matcher(s).replaceAll(" ");                          // comillas, guiones, paréntesis
        s = PUNTO_SUELTO.matcher(s).replaceAll(" ");                      // "estruct." → "estruct"
        s = CEROS_DECIMALES.matcher(s).replaceAll(m ->
                m.group(2).isEmpty() ? m.group(1) : m.group(1) + "." + m.group(2)); // 1.50 → 1.5
        s = POR_ENTRE_NUMEROS.matcher(s).replaceAll("$1 x ");            // 2x4 → 2 x 4
        s = NUMERO_Y_UNIDAD.matcher(s).replaceAll("$1$2");               // 1.5 mm → 1.5mm
        return ESPACIOS.matcher(s).replaceAll(" ").trim();
    }
}
