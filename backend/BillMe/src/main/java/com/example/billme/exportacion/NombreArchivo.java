package com.example.billme.exportacion;

import java.text.Normalizer;
import java.time.LocalDate;

public final class NombreArchivo {

    private NombreArchivo() {}

    public static String de(String folio, String clienteNombre, LocalDate fecha, String extension) {
        String extLimpia = (extension != null && extension.startsWith(".")) ? extension.substring(1) : (extension != null ? extension : "");
        String cliente = clienteNombre != null ? clienteNombre : "";
        String clienteSinAcentos = Normalizer.normalize(cliente, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
        String clienteLimpio = clienteSinAcentos.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-+|-+$", "");
        String fechaStr = fecha != null ? fecha.toString() : "";
        return folio + "_" + clienteLimpio + "_" + fechaStr + "." + extLimpia;
    }
}
