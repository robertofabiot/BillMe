package com.example.billme.importacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public final class ClaveImportacion {

    private ClaveImportacion() {
    }

    /**
     * Devuelve el digest SHA-256 en formato hexadecimal minúscula de 64 caracteres
     * de las partes unidas con '|' (las partes null se tratan como cadena vacía).
     */
    public static String de(String... partes) {
        String union = String.join("|", Arrays.stream(partes)
                .map(p -> p == null ? "" : p)
                .toList());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(union.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
