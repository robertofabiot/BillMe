package com.example.billme.util;

import java.time.LocalDate;
import java.time.ZoneId;

/** El negocio está en Nicaragua y el servidor corre en UTC: "hoy" siempre es la fecha de allá. */
public final class Fechas {

    private static final ZoneId NICARAGUA = ZoneId.of("America/Managua");

    private Fechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(NICARAGUA);
    }
}
