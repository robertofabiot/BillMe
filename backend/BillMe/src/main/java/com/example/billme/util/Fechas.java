package com.example.billme.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** El negocio está en Nicaragua y el servidor corre en UTC: "hoy" siempre es la fecha de allá. */
public final class Fechas {

    private static final ZoneId NICARAGUA = ZoneId.of("America/Managua");
    private static final DateTimeFormatter DIA_Y_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("es"));

    private Fechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(NICARAGUA);
    }

    public static String diaYMes(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.format(DIA_Y_MES);
    }
}
