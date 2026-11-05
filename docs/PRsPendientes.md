# PR pendientes (7 a 13)

Especificación de lo que falta para llegar a producción. Los PR 1 a 6 ya están en `main`. Este documento se implementa tal cual: cada decisión ya está tomada, así que no hace falta volver a preguntar ni elegir entre alternativas. La hoja de ruta general y las fórmulas oficiales están en [PlanDeDesarrollo.md](PlanDeDesarrollo.md).

**Cómo se mantiene:** el último commit de cada PR borra su sección de este documento, y el PR 13 borra el archivo. Así aquí solo queda lo que falta. La especificación de lo ya hecho queda en el historial de git.

| PR | Rama | Objetivo | Migración | Depende de |
|---|---|---|---|---|
| 7 | `feat/precio-sugerido-y-calculadora` | Último precio, fórmula, calculadora de peso, por peso y por metro, página Precios | V5 | PR 6 |
| 8 | `feat/exportacion-documentos` | PNG, PDF y Excel con el formato actual | — | PR 7 |
| 9 | `feat/importar-cotizaciones` | Importar `cotizaciones1` con conciliación | — | PR 8 |
| 10 | `feat/cuenta-corriente` | Abonos a la cuenta del cliente y saldo a favor | V6 | PR 9 |
| 11 | `feat/consolidados-por-entrega` | Consolidado en vivo por fecha de entrega | V7 | PR 10 |
| 12 | `feat/compras-y-ganancias` | Compra ligada a la venta y reporte de ganancias | V8 | PR 11 |
| 13 | `chore/pulido-movil-y-docs` | Celular, legibilidad, paginación, limpieza y manual | — | PR 12 |

Los PR se hacen en este orden y de uno en uno: cada rama sale de `main` cuando el PR anterior ya está mergeado.

---

## 1. Decisiones cerradas

Reemplazan a los supuestos S1 a S4 y resuelven todo lo que quedaba abierto.

| # | Tema | Decisión | Afecta |
|---|---|---|---|
| D1 | Formato de la cotización | Mismas columnas y diseño que la factura. Solo cambia el encabezado, que termina en ` / Cotización`. Hay una sola plantilla para los dos tipos. Las cotizaciones del libro actual ya son así. | PR 8 |
| D2 | Cliente al importar | La hoja admite una columna opcional `cliente`. Si la fila no la trae, se usa el cliente elegido al subir el archivo, que es obligatorio. Un nombre que no existe crea el cliente. | PR 9 |
| D3 | Filas que forman una factura | Las que comparten cliente, fecha, `proyecto`, `num_factura_dia` y `Entregado`. | PR 9 |
| D4 | Qué se crea al importar | Facturas `ACTIVA` con folio de la serie `FAC-`, en orden de fecha. Un producto sin conciliar nunca se pierde: entra como genérico con su descripción. | PR 9 |
| D5 | Descuento por cliente | Uno solo por cliente, en `clientes.descuento_pct`. Al calcular se puede cambiar para esa línea, sin tocar el del cliente. | PR 7 |
| D6 | Tarifa por kg sugerida | Primero la última usada con ese cliente, luego `clientes.tarifa_kg_usd`, y al final `tarifa_kg_usd` de Configuración. | PR 7 |
| D7 | Qué cuenta en la cuenta del cliente | Facturas `ACTIVA` sin `import_key` y con `fecha >= fecha_inicio_cuenta`, más abonos con `fecha_pago >= fecha_inicio_cuenta`. Lo importado es historial de precios y nunca genera deuda: esa deuda se carga como `saldo_inicial`. | PR 10, 11 |
| D8 | Abonos | Van a la cuenta del cliente, con proyecto opcional, y no se reparten por factura. Se puede pagar de más: el saldo queda negativo y se muestra "Saldo a favor". Las facturas dejan de tener estado de pago, y una factura se puede anular aunque el cliente tenga abonos. | PR 10 |
| D9 | Consolidado | Se arma en vivo con las mismas facturas y abonos de D7. Las facturas entran por su `fecha`, y las líneas se agrupan por `fecha_entrega` o, si no tienen, por la fecha de la factura. Solo se guardan los nombres editados. Si un ítem está mal, se corrige la factura. | PR 11 |
| D10 | Exportación | Lo único editable al exportar es el encabezado, que no se guarda. Todo lo demás sale del documento. `notas` nunca se imprime porque es interno. La fecha sale como `dd/mm/aaaa`. | PR 8, 11 |
| D11 | Compra | Se registra por factura. Reemplaza el costo de cada línea elegida y, si se indica, su peso. Nunca cambia el precio ni el total de la venta. Solo se corrige volviendo a registrar sobre las mismas líneas; no hay borrado de compras. | PR 12 |
| D12 | Ganancia | Por línea: `total − cantidad × costo_unit_con_iva`. Las líneas sin costo no cuentan como costo cero: quedan fuera de la ganancia y se avisa cuántas son. | PR 12 |
| D13 | Reportes | Una sola página: Facturado, Costo, Ganancia y una tabla por factura. Se eliminan las gráficas, `recharts` y todo el cálculo en el navegador. | PR 12 |
| D14 | Paginación | Solo en documentos. Productos ya está limitado a 100 recientes o 20 resultados de búsqueda. | PR 13 |
| D15 | Librerías | La única dependencia nueva es `jspdf` 4.2.1. No se agregan `sonner` ni librerías de formularios. | Todos |

---

## 2. Reglas para todos los PR

### 2.1 Git

```bash
git checkout main
git pull --ff-only origin main
git checkout -b <rama-del-pr>
```

- Commits atómicos en Conventional Commits y en español. Cada commit compila.
- El único autor es el dueño del repo, sin `Co-Authored-By`.
- Título del PR: `PR N · <objetivo>`. La descripción termina con la línea de atribución de Claude Code.
- Base del PR: `main`. El merge lo hace el dueño del repo.

### 2.2 Rutas abreviadas

| Abreviatura | Ruta |
|---|---|
| `BE/` | `backend/BillMe/src/main/java/com/example/billme/` |
| `BT/` | `backend/BillMe/src/test/java/com/example/billme/` |
| `MIG/` | `backend/BillMe/src/main/resources/db/migration/` |
| `FE/` | `frontend/src/` |

### 2.3 Verificación antes de abrir cada PR

```bash
cd backend/BillMe && LC_ALL=C.UTF-8 LANG=C.UTF-8 ./mvnw -q clean verify
```

```bash
cd frontend && npx tsc -b && npm run lint && npm run build
```

Para probar en el navegador, sin Docker:

1. Crear `backend/BillMe/src/test/java/tmpdev/DevServer.java`, que **nunca se sube**. Es un test JUnit que inicia `EmbeddedPostgres.builder().start()`, llama a `SpringApplication.run(BillMeApplication.class, ...)` con `--spring.datasource.*`, `--jwt.secret`, `--cors.allowed-origins=http://localhost:5179` y `--billme.usuario-inicial.*`, y luego duerme.
2. Arrancarlo: `./mvnw -q test -Dtest=DevServer -Dsurefire.failIfNoSpecifiedTests=false`.
3. Arrancar el frontend desde `frontend/`: `VITE_API_URL=http://localhost:8080/api ./node_modules/.bin/vite --port 5179 --strictPort`.
4. Al terminar, detener ambos y borrar `tmpdev/` antes de correr `verify`.

### 2.4 DoD común

Además del DoD de cada PR, todos cumplen esto:

- CI en verde: `./mvnw verify`, `npm run lint` y `npm run build`.
- Si hay migración, Flyway la aplica sobre el esquema anterior y Hibernate arranca con `ddl-auto=validate`.
- El flujo principal del PR se probó en el navegador contra el backend real, sin errores inesperados en consola.
- El diff, los mensajes de commit y la descripción del PR no tienen datos reales (sección 2.7).
- La sección del PR se borró de este documento.

### 2.5 Backend

- Clases de servicio con `@Service`, `@RequiredArgsConstructor` y `@Transactional(readOnly = true)`. Los métodos que escriben llevan `@Transactional`.
- Errores con `ResponseStatusException` y mensaje en español: 400 dato inválido, 404 no existe, 409 estado que no lo permite.
- Dinero, pesos y cantidades siempre en `BigDecimal`. El redondeo oficial es `CalculoDocumento.r2`.
- Los parámetros salen de `ConfiguracionService`, nunca de valores fijos en el código.
- Las fechas de "hoy" salen de `Fechas.hoy()` (America/Managua).
- DTOs como `record`. Los campos opcionales del JSON van en tipos con mayúscula (`Boolean`, `BigDecimal`), porque Jackson 3 rechaza `null` en primitivos.
- Las clases del paquete `calculo` son estáticas, sin Spring. Lanzan `IllegalArgumentException`, y el servicio que las llama la convierte en un 400.
- Los tests de integración extienden `support/IntegrationTest`. Toda tabla nueva se agrega a `vaciarDatos()`.

### 2.6 Frontend

- El backend calcula todo. El frontend solo muestra lo que recibe, salvo el total provisional del editor mientras se escribe.
- Colores: `#022F40`, `#91E5F6`, `#558564`, `#705D56`, `#80727B`, `#E5E7EB`, `#F9FAFB`.
- Componentes: `Input` y `Button` de `components/ui`, `Dialog` de `components/ui/dialog` para diálogos, `<select>` nativo e iconos de `lucide-react`.
- `Button` no admite `asChild`: para navegar se usa `onClick={() => navigate(...)}`.
- Las confirmaciones usan `window.confirm` nombrando el objeto. Los mensajes van en la misma pantalla, junto a la acción.
- Las fechas de hoy salen de `hoyIso()`, nunca de `toISOString()`.

### 2.7 Datos de prueba

El repositorio es público. Nunca se suben nombres reales de clientes, personas o proveedores, precios o descuentos reales, ni los Excel del negocio. Antes de cada commit se revisa el diff contra la lista privada de nombres y montos, que vive fuera del repo.

En tests y documentos se usa "Cliente de prueba", "María López", "Juan Pérez", "Obra centro", "Proveedor principal" y números redondos. Las descripciones de productos y los pesos físicos sí se permiten.

---

## PR 8 · `feat/exportacion-documentos`

**Objetivo:** PNG, PDF y Excel con el formato actual, sin logo. Una sola plantilla para factura y cotización (D1).

### Formato

```
                                                        dd/mm/aaaa
Cliente de prueba / Obra centro / Cotización

Cantidad | Descripción | Peso Unit (KG) | Peso Total (KG) | Precio Unitario C$ | Precio Total C$ | [Fecha entrega] | [Lugar entrega]
   2     | ...         |     30.00      |     60.00       |     C$1,500.00     |    C$3,000.00   |
                              *****FIN DE LÍNEA*****

                                           Total C$    C$3,150.00
                                           Total KG        60.00
```

- Encabezado por defecto: `clienteNombre`, luego ` / proyectoNombre` si hay proyecto, y ` / Cotización` si es cotización.
- `Fecha entrega` y `Lugar entrega` solo aparecen con `mostrarEntrega = true`.
- Una línea sin peso deja vacías las dos columnas de peso.
- `notas` nunca se imprime (D10).

### Backend

`BE/exportacion/NombreArchivo.java`:

```java
public static String de(String folio, String clienteNombre, LocalDate fecha, String extension)
```

Devuelve, por ejemplo, `FAC-0012_Cliente-de-prueba_2026-10-30.xlsx`: quita acentos y reemplaza por `-` todo lo que no sea letra o número.

`BE/exportacion/DocumentoExcel.java` (`@Component`), con `byte[] generar(Documento documento, String encabezado)`. Hoja con el nombre del folio:

| Celda | Contenido |
|---|---|
| `H1` (combinada `H1:I1`) | Fecha, con formato `dd/mm/yyyy`, en negrita |
| `B2` (combinada hasta la última columna) | Encabezado, en negrita, tamaño 14 |
| Fila 4 | `B` Cantidad, `C` Descripción (combinada `C:E`), `F` Peso Unit (KG), `G` Peso Total (KG), `H` Precio Unitario C$, `I` Precio Total C$, y con entrega `J` Fecha entrega, `K` Lugar entrega |
| Filas desde la 5 | `B` cantidad, `C:E` descripción, `F` peso, `G` `=ROUND(B5*F5,2)` si hay peso, `H` precio, `I` `=ROUND(B5*H5,2)`, `J` fecha, `K` lugar |
| Fila siguiente | `*****FIN DE LÍNEA*****`, combinada de `B` a la última columna, centrada, en negrita y cursiva |
| Dos filas después | `H` `Total C$`, `I` `=SUM(I5:In)` |
| Fila siguiente | `H` `Total KG`, `I` `=SUM(G5:Gn)` |

- Se usa `ROUND` para que el Excel dé lo mismo que `CalculoDocumento.r2`.
- Formatos: `0.###` en cantidad, `#,##0.00` en pesos, `"C$"#,##0.00` en precios.
- Anchos de columna, en caracteres: A 2, B 10, C a E 15 cada una, F y G 12, H e I 16, J 14, K 24.
- Bordes finos en la tabla y encabezados en negrita.
- Antes de escribir el archivo se llama a `getCreationHelper().createFormulaEvaluator().evaluateAll()`, para que las vistas previas del celular muestren los valores.

`BE/service/DocumentoService.java`: método `exportarExcel(UUID id, String encabezado)`, de solo lectura. Devuelve `record ArchivoExcel(String nombre, byte[] contenido)`, en `BE/exportacion/ArchivoExcel.java`. Si `encabezado` viene vacío, usa el encabezado por defecto.

`BE/controller/DocumentoController.java`: `GET /api/documentos/{id}/excel?encabezado=` con tipo `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` y `Content-Disposition: attachment`.

### Frontend

- `package.json`: `jspdf` 4.2.1.
- `FE/lib/format.ts`: `formatFechaCorta(fecha: string)` devuelve `dd/mm/aaaa`.
- `FE/services/api.ts`: separar el envío en `enviar(path, options): Promise<Response>` con el token y el manejo de 401, y agregar `api.descargar(path): Promise<Blob>`.
- `FE/services/documentoService.ts`: `descargarExcel(id, encabezado): Promise<Blob>`.
- `FE/lib/exportar.ts`:
  - `nombreArchivo(folio, cliente, fecha, extension)`: la misma regla que el backend.
  - `descargarArchivo(blob, nombre)`.
  - `descargarPng(elemento, nombre)`: `toPng` con `pixelRatio: 2` y fondo blanco.
  - `descargarPdf(elemento, nombre, horizontal)`: tamaño carta, márgenes de 10 mm y la imagen ajustada al ancho. Si la imagen es más alta que la página, se agrega la misma imagen en cada página nueva desplazada hacia arriba por el alto útil, hasta cubrirla.
- `FE/components/export/DocumentoPlantilla.tsx`: props `documento` y `encabezado`, sin inputs. Ancho de 700 px, o 960 px con entrega. Colores en negro sobre blanco, con bordes finos.
- `FE/components/export/ExportarDialog.tsx`, genérico para que el PR 11 lo reutilice. Props:

```ts
interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  encabezadoInicial: string
  nombreBase: (extension: string) => string
  horizontal: boolean
  plantilla: (encabezado: string) => ReactNode
  descargarExcel: (encabezado: string) => Promise<Blob>
}
```

  El diálogo muestra el campo `Encabezado`, la vista previa con desplazamiento y los botones `Imagen (PNG)`, `PDF` y `Excel`. El ancho del diálogo es `sm:max-w-[850px]`, o `sm:max-w-[1060px]` si es horizontal. Se deshabilitan los botones mientras se genera el archivo, y un error se muestra dentro del diálogo.
- `FE/pages/DocumentoEditorPage.tsx`: el botón pasa de `Exportar imagen` a `Exportar` y exporta lo guardado. Con cambios sin guardar, pregunta `Se exportará la última versión guardada. ¿Continuar?`.
- Se elimina `FE/components/documentos/DocumentoExportSheet.tsx`.

### Tests

`BT/exportacion/DocumentoExcelTest.java` abre el resultado con `XSSFWorkbook`. Datos: línea 1 de 2 unidades, PU 1,500.00 y peso 30.00; línea 2 de 1.5 unidades, PU 100.00, sin peso.

- `I5` tiene la fórmula `ROUND(B5*H5,2)`, y `G5` tiene `ROUND(B5*F5,2)`.
- Los valores evaluados son 3,000.00 y 150.00; `Total C$` es 3,150.00 y `Total KG` es 60.00.
- `G6` queda vacía.
- Con `mostrarEntrega`, existen `J` y `K` y la fila de fin de línea llega hasta `K`. Sin él, no existen.
- El encabezado de una cotización termina en ` / Cotización`.
- `NombreArchivo.de("FAC-0012", "María López", 2026-10-30, "pdf")` da `FAC-0012_Maria-Lopez_2026-10-30.pdf`.

### Commits

1. `feat(export): plantilla única de factura y cotización` — `FE/components/export/DocumentoPlantilla.tsx`, `FE/lib/format.ts`
2. `feat(export): exportar a PNG y PDF` — `frontend/package.json`, `frontend/package-lock.json`, `FE/lib/exportar.ts`, `FE/components/export/ExportarDialog.tsx`, `FE/pages/DocumentoEditorPage.tsx`; elimina `DocumentoExportSheet.tsx`
3. `feat(export): Excel con fórmulas` — `BE/exportacion/NombreArchivo.java`, `ArchivoExcel.java`, `DocumentoExcel.java`, `BE/service/DocumentoService.java`, `BE/controller/DocumentoController.java`, `BT/exportacion/DocumentoExcelTest.java`
4. `feat(ui): descargar el Excel desde el diálogo de exportar` — `FE/services/api.ts`, `FE/services/documentoService.ts`, `FE/components/export/ExportarDialog.tsx`, `FE/pages/DocumentoEditorPage.tsx`
5. `docs: quitar el PR 8 de los pendientes` — `docs/PRsPendientes.md`

### DoD

- Los tres formatos muestran los mismos datos y el mismo encabezado.
- Los totales del Excel coinciden con los de la app, y sus celdas tienen fórmulas.
- Los tres archivos se descargan de verdad y se abren bien. Se comprueba abriéndolos, no solo con la vista previa.
- El PNG mide al menos 1,000 px de ancho.
- Un documento largo ocupa varias páginas en el PDF sin cortar el contenido a la mitad de la página.
- Los archivos se llaman `FAC-0012_Cliente-de-prueba_2026-10-30.pdf`.

---

## PR 9 · `feat/importar-cotizaciones`

**Objetivo:** cargar el historial de precios desde `cotizaciones1`, y poder repetirlo cuando se agreguen filas nuevas. Con este PR se alcanza el MVP.

### Formato que se lee

Se lee la hoja llamada `cotizaciones`, sin distinguir mayúsculas. Si no existe, la primera hoja cuyos encabezados tengan todas las columnas obligatorias. El encabezado se busca en las primeras 20 filas, comparando con `trim().toLowerCase(Locale.ROOT)`. No se usa `NormalizadorTexto` aquí porque quita los guiones bajos.

| Columna | Uso | Obligatoria |
|---|---|---|
| `fecha` | Fecha de la factura. Admite número de serie de Excel y texto `dd/mm/aaaa` | Sí |
| `cantidad` | Cantidad | Sí |
| `nombre_cotizacion` | Descripción de la línea | Sí |
| `precio_unitario_cotizacion` | Precio unitario con IVA; se redondea con `r2` | Sí |
| `cod_producto` | Código del catálogo. Vacío o `0` significa sin código. Si es celda numérica, se completa con ceros hasta 9 dígitos | No |
| `peso_unitario` | Peso. Si no es un número positivo, se usa `peso_total / cantidad`; si tampoco, el peso del producto | No |
| `peso_total` | Ver `peso_unitario` | No |
| `precio_unitario_facturado` | Costo sin IVA. `costo_unit_con_iva = r2(valor × factorIva)` | No |
| `observaciones` | Se agrega a las notas de la factura | No |
| `Entregado` | `lugar_entrega` de la línea, recortado a 200 caracteres | No |
| `proyecto` | Proyecto del cliente; si no existe, se crea `ACTIVO` | No |
| `num_factura_dia` | Solo para agrupar | No |
| `cliente` | Ver D2 | No |

El resto de columnas son calculadas y se ignoran.

**Fila incompleta:** tiene datos, pero le falta fecha válida, cantidad positiva, descripción o precio positivo. No se importa y se informa con su número y el motivo. Una fila sin cantidad ni descripción se ignora en silencio, porque es una fila vacía.

### Backend: lectura

- `BE/importacion/LectorExcel.java`: se extrae de `ListaPreciosLector` el recorrido SAX, para no cargar el libro entero en memoria.

```java
public static void recorrer(InputStream hoja, ReadOnlySharedStringsTable textos, ManejadorFila manejador)

public interface ManejadorFila {
    void fila(int numero, Map<String, String> celdas, Set<String> numericas) throws SAXException;
}

/** Se lanza desde el manejador para dejar de leer una hoja. */
public static final class DetenerLectura extends SAXException {}
```

- `BE/importacion/ListaPreciosLector.java` pasa a usar `LectorExcel`. Su test debe seguir pasando sin cambios.
- `BE/importacion/FilaCotizacion.java`: `record` con `int numeroFila, String codigo, String numFacturaDia, String proyecto, LocalDate fecha, BigDecimal cantidad, String descripcion, BigDecimal pesoUnitKg, BigDecimal precioUnit, BigDecimal costoSinIva, String observaciones, String entregado, String cliente`.
- `BE/importacion/LecturaCotizaciones.java`: `record` con `String hoja, List<FilaCotizacion> filas, List<FilaProblema> incompletas`.
- `BE/importacion/CotizacionesLector.java` (`@Component`), con `LecturaCotizaciones leer(InputStream archivo)`. Hace una primera pasada para buscar la hoja por nombre y, si no la encuentra, una segunda pasada por encabezados. Sin hoja válida: 400 `No se encontró una hoja con fecha, cantidad, nombre_cotizacion y precio_unitario_cotizacion.`
- `BE/importacion/ClaveImportacion.java`: `static String de(String... partes)` devuelve el SHA-256 en hexadecimal (64 caracteres) de las partes unidas con `|`.

### Backend: claves y agrupación

```
norm(x)     = NormalizadorTexto.normalizar(x)
clave doc   = ClaveImportacion.de(clienteId, fecha, norm(proyecto), numFacturaDia, norm(entregado))
clave línea = ClaveImportacion.de(claveDoc, codigo o norm(descripcion), cantidad sin ceros a la derecha, r2(precio), ocurrencia)
```

`ocurrencia` es la posición de esa misma fila entre las filas idénticas del grupo (0, 1, 2…), para que dos filas iguales no se confundan. En la vista previa, las filas de un cliente que todavía no existe cuentan todas como nuevas.

Reglas al reimportar:

- Una línea cuya clave ya existe se salta.
- Una línea nueva de una factura que ya existe se agrega al final y se recalculan los totales con `CalculoDocumento`.
- Una fila corregida en Excel tiene otra clave, así que entra como línea nueva. La línea anterior se quita a mano en el editor.
- Una línea borrada en la app vuelve a entrar si sigue en el Excel.

### Backend: conciliación

Por cada fila, el producto se resuelve en este orden:

1. `cod_producto` existe en el catálogo.
2. `norm(descripcion)` coincide exactamente con el `nombre_norm` de un producto activo, o con un `alias_norm`.
3. Queda pendiente, agrupado por `norm(descripcion)`.

Propuesta para un pendiente: `GEN-PLACA` si la descripción normalizada empieza por `placa`, `GEN-PLATINA` si empieza por `platina`, y `GEN-LIBRE` en cualquier otro caso. Los candidatos son los 3 primeros de `ProductoService.buscar(descripcion)`.

Al elegir un producto real se crea un alias con la descripción original, salvo que tenga más de 200 caracteres. Al elegir un genérico no se crea alias.

Consultas nuevas:

- `ProductoRepository.findFirstByNombreNormAndActivoTrue(String)`
- `AliasProductoRepository.findFirstByAliasNorm(String)`
- `ClienteRepository.findFirstByNombreIgnoreCase(String)`
- `ProyectoRepository.findFirstByClienteIdAndNombreIgnoreCase(UUID, String)`
- `DocumentoRepository.findByImportKey(String)`
- `DocumentoLineaRepository.findImportKeysIn(Collection<String>)`, que devuelve solo las claves existentes

### Backend: servicio y API

`BE/service/ImportacionCotizacionesService.java`:

```java
public ImportacionCotizacionesResponse vistaPrevia(InputStream archivo, UUID clienteId)

@Transactional
public ImportacionCotizacionesResponse aplicar(InputStream archivo, UUID clienteId, List<DecisionConciliacion> decisiones)
```

Al aplicar, todo se guarda o nada se guarda. Los grupos se procesan por fecha ascendente, y cada factura nueva se crea así:

| Campo | Valor |
|---|---|
| `tipo`, `estado` | `FACTURA`, `ACTIVA` |
| `folio` | Serie `FAC-` |
| `fecha` | La del grupo |
| `import_key` | Clave del documento |
| `notas` | `Importado de {hoja}.` y, si hay, las observaciones distintas unidas con `; ` |
| `usuario` | El de la sesión |
| Línea: `metodo_precio`, `parametros` | `MANUAL`, `null` |
| Línea: `import_key` | Clave de la línea |

Para reutilizarlos, en `DocumentoService` pasan a visibilidad de paquete `siguienteFolio` y `usuarioActual`. Importar no crea versiones anteriores.

```java
public record DecisionConciliacion(@NotBlank String descripcion, @NotNull UUID productoId) {}
public record FilaProblema(int fila, String motivo) {}

public record ImportacionCotizacionesResponse(
        String hoja,
        int filasLeidas,
        int filasNuevas,
        int filasYaImportadas,
        int facturasNuevas,
        int facturasConLineasNuevas,
        List<String> clientesNuevos,
        List<FilaProblema> filasIncompletas,
        List<PorConciliar> porConciliar,
        boolean aplicada) {

    public record PorConciliar(String descripcion, int filas, UUID propuestaProductoId,
                               String propuestaNombre, List<Candidato> candidatos) {}
    public record Candidato(UUID id, String codigo, String nombre) {}
}
```

Decisiones que no se envían usan la propuesta. Un `productoId` que no existe da 404.

`BE/controller/ImportacionController.java`, en multipart, sin guardar nada entre pasos (el archivo se vuelve a subir):

| Ruta | Partes |
|---|---|
| `POST /api/importaciones/cotizaciones/vista-previa` | `archivo`, `clienteId` |
| `POST /api/importaciones/cotizaciones/aplicar` | `archivo`, `clienteId`, `decisiones` (texto JSON, opcional) |

### Frontend

- `FE/types/index.ts`: `ImportacionCotizaciones`.
- `FE/services/importacionService.ts`: `vistaPreviaCotizaciones(archivo, clienteId)` y `aplicarCotizaciones(archivo, clienteId, decisiones)`.
- `FE/pages/ImportarPage.tsx`: muestra `ImportarListaPanel` y debajo `ImportarCotizacionesPanel`.
- `FE/components/importacion/ImportarCotizacionesPanel.tsx`, con los mismos pasos y estilos que `ImportarListaPanel`:
  1. Elegir archivo y `Cliente de las filas sin columna cliente`.
  2. `Revisar archivo`: muestra Filas leídas, Filas nuevas, Ya importadas, Facturas nuevas y Filas incompletas (con su número y motivo).
  3. Si hay pendientes, `ConciliacionTabla`.
  4. `Importar`, deshabilitado si no hay filas nuevas, con el texto `No hay nada nuevo que importar`.
- `FE/components/importacion/ConciliacionTabla.tsx`: una fila por descripción con Descripción, Filas y un `<select>` con la propuesta seleccionada y los candidatos. Texto de ayuda: `Si no eliges nada, la línea se guarda con su descripción y su precio.`

### Tests

- `BT/importacion/ExcelDePrueba.java`: agrega `cotizaciones()`, con los encabezados del formato y sin datos reales.
- `BT/importacion/CotizacionesLectorTest.java`: elige la hoja `cotizaciones` entre varias; fecha de serie y de texto; código de texto con ceros; código numérico completado a 9 dígitos; código `0`; `peso_unitario` con texto usa `peso_total / cantidad`; fila vacía ignorada; fila incompleta con motivo; sin hoja válida da 400.
- `BT/importacion/ImportacionCotizacionesTest.java` (integración):
  - Dos filas iguales en fecha y entrega forman una factura; otra entrega forma otra.
  - Reimportar el mismo archivo da 0 nuevas y no crea nada.
  - Agregar una fila a un grupo la suma a la misma factura y actualiza el total.
  - Dos filas idénticas se conservan las dos.
  - Elegir un producto crea el alias, y al reimportar otra fila con esa descripción ya no queda pendiente.
  - Sin decisión, entra con `GEN-LIBRE`, o `GEN-PLACA` si dice placa, y conserva su descripción.
  - Costo 950.00 sin IVA queda en 1,092.50.
  - PU 100.555 queda en 100.56.
  - Una columna `cliente` con un nombre nuevo crea el cliente.
  - La vista previa no guarda nada.
  - Tras importar, `/precios/sugerido` devuelve ese precio.

### Commits

1. `refactor(import): lector de hojas de Excel compartido` — `BE/importacion/LectorExcel.java`, `ListaPreciosLector.java`
2. `feat(import): lector del formato cotizaciones1` — `BE/importacion/CotizacionesLector.java`, `FilaCotizacion.java`, `LecturaCotizaciones.java`, `BT/importacion/CotizacionesLectorTest.java`, `BT/importacion/ExcelDePrueba.java`
3. `feat(import): importar cotizaciones sin duplicar al repetir` — `BE/importacion/ClaveImportacion.java`, `BE/service/ImportacionCotizacionesService.java`, `BE/service/DocumentoService.java`, repositorios, DTOs, `BE/controller/ImportacionController.java`, `BT/importacion/ImportacionCotizacionesTest.java`
4. `feat(import): conciliación de productos con alias` — `BE/service/ImportacionCotizacionesService.java`, repositorios, tests
5. `feat(ui): importar cotizaciones y conciliar productos` — `FE/components/importacion/ImportarCotizacionesPanel.tsx`, `ConciliacionTabla.tsx`, `FE/pages/ImportarPage.tsx`, `FE/services/importacionService.ts`, `FE/types/index.ts`
6. `docs: quitar el PR 9 de los pendientes` — `docs/PRsPendientes.md`

### DoD

- El archivo real se importa en local sin errores, sin subirlo al repo: `filasLeidas + filasIncompletas` da el total de filas con datos, y cada incompleta tiene motivo.
- Reimportar el mismo archivo da 0 nuevas. Al agregar filas, solo entran esas.
- Ninguna fila se pierde por no tener producto.
- Después de importar, el precio sugerido devuelve los precios del historial.
- Una conciliación elegida una vez no se vuelve a preguntar.

---

## PR 10 · `feat/cuenta-corriente`

**Objetivo:** los abonos pasan a la cuenta del cliente, con proyecto opcional y saldo a favor (D7 y D8).

```
saldo = saldo_inicial
      + Σ facturas ACTIVA sin import_key y con fecha >= fecha_inicio_cuenta
      − Σ abonos con fecha_pago >= fecha_inicio_cuenta

Sin fecha_inicio_cuenta no se filtra por fecha.
Por proyecto: lo mismo solo con ese proyecto y sin saldo_inicial.
"Sin proyecto": saldo_inicial + lo que no tiene proyecto.
La suma de todos los proyectos y "Sin proyecto" da el saldo total.
```

### Migración `MIG/V6__cuenta_cliente.sql`

```sql
-- Los abonos pasan de la factura a la cuenta del cliente, con proyecto opcional.
alter table clientes
    add column saldo_inicial       numeric(14, 2) not null default 0,
    add column fecha_inicio_cuenta date;

alter table abonos
    add column cliente_id  uuid constraint fk_abonos_cliente references clientes,
    add column proyecto_id uuid constraint fk_abonos_proyecto references proyectos,
    add column nota        text;

update abonos a
set cliente_id  = d.cliente_id,
    proyecto_id = d.proyecto_id
from documentos d
where d.id = a.factura_id;

alter table abonos alter column cliente_id set not null;
alter table abonos alter column monto type numeric(14, 2);
alter table abonos drop column factura_id;

create index idx_abonos_cliente_fecha on abonos (cliente_id, fecha_pago);
create index idx_abonos_proyecto on abonos (proyecto_id);
```

### Backend: modelo y documentos

- `BE/model/Abono.java`: quita `documento`; agrega `cliente` (obligatorio), `proyecto` y `nota`.
- `BE/model/Cliente.java`: agrega `saldoInicial` (`@Builder.Default BigDecimal.ZERO`) y `fechaInicioCuenta`.
- `BE/dto/ClienteRequest.java`: agrega `BigDecimal saldoInicial` (negativo significa saldo a favor; `null` se guarda como 0) y `LocalDate fechaInicioCuenta`.
- `BE/dto/ClienteResponse.java`: agrega esos dos campos y `BigDecimal saldo`.
- `BE/model/Documento.java`: quita `abonos`.
- `BE/dto/DocumentoResponse.java`: quita `abonos`, `totalAbonado` y `saldoPendiente`.
- `BE/service/DocumentoService.java`: `anular` ya no revisa abonos. Se elimina todo uso de `getAbonos()`.
- `BE/controller/DocumentoController.java`: quita `GET` y `POST /{id}/abonos`.
- `BE/service/ClienteService.java`: `eliminar` también da 409 si `abonoRepository.existsByClienteId(id)`, y el mensaje pasa a `No se puede eliminar un cliente que tiene cotizaciones, facturas, consolidados o abonos`. Los listados y `obtenerPorId` llenan `saldo` con `CuentaClienteService`.
- `BE/service/ReporteService.java`: `totalCobrado` sale de los abonos del cliente o proyecto en el rango, con `AbonoRepository`. `porCobrar` es `ingresos − cobrado`. Los abonos ya no dependen de la factura.

### Backend: consultas

```java
// BE/repository/AbonoRepository.java (reemplaza lo que usaba documentoId)
boolean existsByClienteId(UUID clienteId);
List<Abono> findByClienteIdOrderByFechaPagoAscCreatedAtAsc(UUID clienteId);

@Query("SELECT COALESCE(SUM(a.monto), 0) FROM Abono a WHERE a.cliente.id = :clienteId " +
       "AND (cast(:proyectoId as uuid) IS NULL OR a.proyecto.id = :proyectoId) " +
       "AND (cast(:desde as date) IS NULL OR a.fechaPago >= :desde) " +
       "AND (cast(:hasta as date) IS NULL OR a.fechaPago <= :hasta)")
BigDecimal sumar(@Param("clienteId") UUID clienteId, @Param("proyectoId") UUID proyectoId,
                 @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

// BE/repository/DocumentoRepository.java
@Query("SELECT d FROM Documento d WHERE d.cliente.id = :clienteId " +
       "AND d.tipo = com.example.billme.enums.TipoDocumento.FACTURA " +
       "AND d.estado = com.example.billme.enums.EstadoDocumento.ACTIVA AND d.importKey IS NULL " +
       "AND (cast(:proyectoId as uuid) IS NULL OR d.proyecto.id = :proyectoId) " +
       "AND (cast(:desde as date) IS NULL OR d.fecha >= :desde) " +
       "AND (cast(:hasta as date) IS NULL OR d.fecha <= :hasta) " +
       "ORDER BY d.fecha ASC, d.createdAt ASC")
List<Documento> facturasDeCuenta(@Param("clienteId") UUID clienteId, @Param("proyectoId") UUID proyectoId,
                                 @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
```

Para el saldo de la lista de clientes, sin una consulta por cliente, se agregan dos consultas nativas agrupadas por cliente: una en `documentos` y otra en `abonos`. Devuelven una proyección de interfaz `SumaPorCliente` con `getClienteId()` y `getSuma()`, así que los alias son `clienteid` y `suma`, sin guion bajo. Cada una une con `clientes` para aplicar `fecha_inicio_cuenta` con `(c.fecha_inicio_cuenta is null or fecha >= c.fecha_inicio_cuenta)`. La de documentos también filtra `tipo = 'FACTURA' and estado = 'ACTIVA' and import_key is null`.

### Backend: servicios y API

`BE/service/CuentaClienteService.java`:

```java
public CuentaClienteResponse cuenta(UUID clienteId)
public BigDecimal saldoAl(UUID clienteId, UUID proyectoId, LocalDate fecha)  // proyectoId null = toda la cuenta
public Map<UUID, BigDecimal> saldos()                                        // para la lista de clientes
```

- `saldoAl` suma los totales de `facturasDeCuenta(cliente, proyecto, fecha_inicio_cuenta, fecha)` y resta `AbonoRepository.sumar(cliente, proyecto, fecha_inicio_cuenta, fecha)`. Con proyecto no incluye `saldo_inicial`; sin proyecto, sí.
- Movimientos: solo lo que cuenta según D7. `SALDO_INICIAL` primero, solo si es distinto de 0, con la fecha `fecha_inicio_cuenta`, o sin fecha si no hay. Después, en orden ascendente por fecha; en la misma fecha, facturas antes que abonos, y luego por `createdAt`. El saldo corrido se calcula en ese orden.

```java
public record CuentaClienteResponse(
        UUID clienteId, String clienteNombre, BigDecimal saldoInicial, LocalDate fechaInicioCuenta,
        BigDecimal totalFacturado, BigDecimal totalAbonado, BigDecimal saldo,
        List<SaldoProyecto> proyectos, List<Movimiento> movimientos) {

    /** proyectoId null = "Sin proyecto". */
    public record SaldoProyecto(UUID proyectoId, String nombre, BigDecimal facturado, BigDecimal abonado, BigDecimal saldo) {}

    /** tipo: SALDO_INICIAL, FACTURA o ABONO. id: de la factura o del abono. concepto: folio, nota o "Saldo inicial". */
    public record Movimiento(LocalDate fecha, String tipo, UUID id, String concepto, String proyectoNombre,
                             BigDecimal cargo, BigDecimal abono, BigDecimal saldo) {}
}
```

`BE/service/AbonoService.java`, reescrito:

```java
public record AbonoRequest(@NotNull UUID clienteId, UUID proyectoId, @NotNull @Positive BigDecimal monto,
                           @NotNull LocalDate fechaPago, @Size(max = 500) String nota) {}
public record AbonoResponse(UUID id, UUID clienteId, UUID proyectoId, String proyectoNombre,
                            BigDecimal monto, LocalDate fechaPago, String nota) {}
```

- Un proyecto de otro cliente da 400 `El proyecto no pertenece a ese cliente`.
- No hay límite de monto: pagar de más deja saldo a favor.

| Método y ruta | Controlador |
|---|---|
| `GET /api/clientes/{id}/cuenta` | `BE/controller/CuentaClienteController.java` |
| `POST /api/abonos` | `BE/controller/AbonoController.java` |
| `PUT /api/abonos/{id}` | `BE/controller/AbonoController.java` |
| `DELETE /api/abonos/{id}` | `BE/controller/AbonoController.java` |

### Frontend

- `FE/types/index.ts`: quita `abonos`, `totalAbonado` y `saldoPendiente` de `Documento`. `Abono` queda con los campos de `AbonoResponse`. `Cliente` agrega `saldoInicial`, `fechaInicioCuenta` y `saldo`. Nuevo `CuentaCliente`.
- `FE/services/abonoService.ts`, nuevo: `crear`, `actualizar`, `eliminar`.
- `FE/services/clienteService.ts`: `cuenta(id)`.
- `FE/services/documentoService.ts`: quita `registrarAbono`.
- `FE/lib/format.ts`: `textoSaldo(saldo)` devuelve `Debe C$X` si es mayor que 0, `Saldo a favor C$X` si es menor, y `Al día` si es 0.
- `FE/components/documentos/EstadoDocumentoBadge.tsx`: acepta `Pick<Documento, 'estado'>`. `ACTIVA` se muestra como `Activa`, en verde. Desaparecen `Pagada`, `Abono parcial` y `Por cobrar`.
- `FE/pages/DocumentosPage.tsx`: quita `Total por cobrar` y la columna `Saldo`. Subtítulo de facturas: `Ventas confirmadas`.
- `FE/pages/DocumentoEditorPage.tsx`: quita `AbonosSeccion` y `recargarAbonos`. Se elimina `FE/components/documentos/AbonosSeccion.tsx`.
- `FE/components/clientes/ClienteModal.tsx`: campos `Saldo inicial (C$)`, con el texto de ayuda `Lo que debía al empezar a usar el sistema. Si tenía saldo a favor, escríbelo en negativo.`, y `La cuenta empieza el`.
- `FE/pages/ClientesPage.tsx`: deja de cargar documentos. Las tarjetas de resumen pasan a ser `Total clientes`, `Con saldo pendiente` (cuántos tienen saldo mayor que 0) y `Total por cobrar` (la suma de esos saldos). La tarjeta de cada cliente muestra `textoSaldo(cliente.saldo)` y al hacer clic abre `/clientes/:id`.
- `FE/App.tsx`: ruta `clientes/:id`.
- `FE/pages/ClienteDetallePage.tsx`, que reemplaza a `ClienteDetailSheet.tsx`, de arriba a abajo:
  1. Nombre, teléfono y notas, con los botones `Editar` y `Registrar abono` (acción principal).
  2. Tarjeta con `textoSaldo(saldo)` en grande y, debajo, Facturado y Abonado.
  3. `Por proyecto`, solo si tiene proyectos: Proyecto, Facturado, Abonado, Saldo.
  4. `Movimientos`, del más reciente al más antiguo: Fecha, Concepto (el folio enlaza a la factura), Proyecto, Cargo, Abono, Saldo. Las filas de abono tienen botones de editar y borrar; borrar pide confirmación con el monto y la fecha.
  5. `Cotizaciones y facturas`: Folio, Fecha, Total y Estado, con el mismo contenido del panel que se elimina.
- `FE/components/clientes/AbonoDialog.tsx`: `Monto (C$)`, `Fecha` (hoy), `Proyecto` (`Sin proyecto` y los del cliente) y `Nota`. Sirve para crear y editar.
- `FE/components/proyectos/ProyectoDetailSheet.tsx`: Facturado, Cobrado y Por cobrar salen de la fila del proyecto en `clienteService.cuenta(clienteId)`. Se quita la columna de saldo por factura.
- `FE/pages/ReportesPage.tsx`: `Total Cobrado` y `Por Cobrar` salen de `reporteService` (`/reportes/general`) con los mismos filtros. Se quita la columna `Cobrado` de la pestaña de clientes. El PR 12 reescribe esta página.

### Tests

`BT/service/CuentaClienteServiceTest.java`, con `fecha_inicio_cuenta = 2026-02-01` y saldo inicial 1,000.00:

| Fecha | Movimiento | Proyecto | Saldo corrido |
|---|---|---|---|
| — | Saldo inicial 1,000.00 | — | 1,000.00 |
| 2026-02-10 | Factura 5,000.00 | Obra centro | 6,000.00 |
| 2026-02-15 | Factura 3,000.00 | — | 9,000.00 |
| 2026-02-20 | Abono 2,000.00 | Obra centro | 7,000.00 |
| 2026-02-25 | Abono 1,000.00 | — | 6,000.00 |

Comprobaciones:

- Saldo total 6,000.00; Obra centro 3,000.00; Sin proyecto 3,000.00.
- `saldoAl(2026-02-14)` es 6,000.00.
- Abono de 7,000.00 el 2026-03-01: saldo −1,000.00.
- No cuentan: una factura anulada, una cotización, una factura del 2026-01-15 ni una factura con `import_key`.
- Abono con proyecto de otro cliente: 400. Eliminar un cliente con abonos: 409.
- `saldos()` coincide con `cuenta()`.

`BT/service/DocumentoServiceTest.java`:

- Reemplazar el caso "no se anula con abonos": ahora se anula y deja de sumar a la deuda.
- Una versión anterior guardada con los campos `abonos`, `totalAbonado` y `saldoPendiente` se sigue leyendo.

`BT/controller/AbonoControllerTest.java`: crear, editar, borrar y validaciones.

`BT/support/IntegrationTest.java`: sin cambios de tablas.

### Commits

1. `feat(db): abonos por cliente y saldo inicial` — `MIG/V6__cuenta_cliente.sql`, `BE/model/Abono.java`, `Cliente.java`, `Documento.java`, `BE/dto/ClienteRequest.java`, `ClienteResponse.java` (sin `saldo` todavía), `DocumentoResponse.java`, `BE/service/DocumentoService.java`, `ClienteService.java`, `ReporteService.java`, `BE/repository/AbonoRepository.java`, `BE/controller/DocumentoController.java`, tests ajustados; elimina el `AbonoService.java`, `AbonoRequest.java` y `AbonoResponse.java` anteriores, que el commit 3 vuelve a crear
2. `feat(cuenta): estado de cuenta con saldo a favor y por proyecto` — `BE/service/CuentaClienteService.java`, `ClienteService.java`, `BE/dto/CuentaClienteResponse.java`, `ClienteResponse.java` (agrega `saldo`), `BE/controller/CuentaClienteController.java`, `BE/repository/DocumentoRepository.java`, `AbonoRepository.java`, `BT/service/CuentaClienteServiceTest.java`
3. `feat(abonos): registrar abonos en la cuenta del cliente` — `BE/service/AbonoService.java`, `BE/controller/AbonoController.java`, `BE/dto/AbonoRequest.java`, `AbonoResponse.java`, `BT/controller/AbonoControllerTest.java`
4. `refactor(ui): lo cobrado del reporte sale de los abonos de la cuenta` — `FE/pages/ReportesPage.tsx`
5. `feat(ui): pantalla del cliente con su cuenta y sus abonos` — `FE/pages/ClienteDetallePage.tsx`, `FE/components/clientes/AbonoDialog.tsx`, `ClienteModal.tsx`, `FE/pages/ClientesPage.tsx`, `FE/App.tsx`, `FE/services/abonoService.ts`, `clienteService.ts`, `FE/lib/format.ts`, `FE/types/index.ts` (agrega lo nuevo); elimina `ClienteDetailSheet.tsx`
6. `refactor(ui): las facturas dejan de mostrar saldo propio` — `FE/components/documentos/EstadoDocumentoBadge.tsx`, `FE/pages/DocumentosPage.tsx`, `FE/pages/DocumentoEditorPage.tsx`, `FE/components/proyectos/ProyectoDetailSheet.tsx`, `FE/services/documentoService.ts`, `FE/types/index.ts` (quita los campos de saldo); elimina `AbonosSeccion.tsx`
7. `docs: quitar el PR 10 de los pendientes` — `docs/PRsPendientes.md`

### DoD

- El ejemplo de la tabla da exactamente esos saldos.
- Pagar de más muestra `Saldo a favor C$1,000.00`.
- Un abono con proyecto rebaja ese proyecto y el total.
- Lo importado y lo anterior a `fecha_inicio_cuenta` no generan deuda.
- La migración conserva los abonos que ya existan, con su cliente y proyecto.
- Ninguna pantalla muestra un saldo por factura.

---

## PR 11 · `feat/consolidados-por-entrega`

**Objetivo:** estados de cuenta por fecha de entrega, siempre cuadrados con las facturas (D9).

```
Saldo anterior    = saldoAl(cliente, proyecto, desde − 1 día)
Total             = Σ subtotales
Abonos            = Σ abonos con fecha_pago entre desde y hasta
Total actualizado = Saldo anterior + Total − Abonos = saldoAl(cliente, proyecto, hasta)
```

### Migración `MIG/V7__consolidados_en_vivo.sql`

```sql
-- El consolidado se arma en vivo desde las facturas: solo se guardan el rango y los nombres editados.
drop table consolidado_items;
drop table consolidado_groups;

alter table consolidados rename column folio_interno to folio;
alter table consolidados
    add column proyecto_id uuid constraint fk_consolidados_proyecto references proyectos,
    add column desde       date,
    add column hasta       date;

update consolidados
set desde = (created_at at time zone 'America/Managua')::date,
    hasta = (created_at at time zone 'America/Managua')::date;

alter table consolidados
    alter column desde set not null,
    alter column hasta set not null,
    add constraint chk_consolidados_rango check (desde <= hasta);

create table consolidado_grupos (
    id             uuid                        not null primary key,
    created_at     timestamp(6) with time zone not null,
    updated_at     timestamp(6) with time zone not null,
    consolidado_id uuid                        not null constraint fk_grupos_consolidado references consolidados,
    fecha_entrega  date                        not null,
    nombre         varchar(200)                not null,
    constraint uq_grupos_consolidado_fecha unique (consolidado_id, fecha_entrega)
);

create index idx_consolidados_proyecto on consolidados (proyecto_id);

create sequence seq_folio_cons;
select setval('seq_folio_cons',
              coalesce((select max(substring(folio from 6)::int) from consolidados where folio ~ '^CONS-[0-9]+$'), 0) + 1,
              false);
```

Los consolidados que ya existan conservan folio, nombre y cliente, con un rango de un día. La app todavía no se ha desplegado, así que en producción no hay ninguno.

### Backend

Se eliminan `BE/model/ConsolidadoGroup.java`, `ConsolidadoItem.java`, `BE/repository/ConsolidadoGroupRepository.java`, `ConsolidadoItemRepository.java` y `BE/dto/ConsolidadoGroupRequest.java`, `ConsolidadoGroupResponse.java`, `ConsolidadoItemRequest.java`, `ConsolidadoItemResponse.java`.

- `BE/model/ConsolidadoGrupo.java`: `consolidado`, `fechaEntrega`, `nombre`.
- `BE/model/Consolidado.java`: `folio`, `nombre`, `cliente`, `proyecto`, `desde`, `hasta` y `grupos` (`cascade = ALL`, `orphanRemoval = true`).
- `BE/repository/ConsolidadoRepository.java`: agrega `@Query(value = "select nextval('seq_folio_cons')", nativeQuery = true) long siguienteFolio();`, y quita `existsByFolioInterno`.
- `BE/repository/AbonoRepository.java`: agrega `delPeriodo(clienteId, proyectoId, desde, hasta)`, una lista con los mismos filtros de `sumar`, ordenada por `fechaPago` y `createdAt`.
- `BE/util/Fechas.java`: `diaYMes(LocalDate)` devuelve `2 de octubre`, con `DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("es"))`.

```java
public record ConsolidadoRangoRequest(@NotNull UUID clienteId, UUID proyectoId,
                                      @NotNull LocalDate desde, @NotNull LocalDate hasta) {}

public record ConsolidadoGuardarRequest(@NotNull UUID clienteId, UUID proyectoId,
        @NotBlank @Size(max = 200) String nombre, @NotNull LocalDate desde, @NotNull LocalDate hasta,
        List<@Valid NombreGrupo> grupos) {
    public record NombreGrupo(@NotNull LocalDate fechaEntrega, @NotBlank @Size(max = 200) String nombre) {}
}

public record ConsolidadoResponse(
        UUID id, String folio, String nombre, UUID clienteId, String clienteNombre,
        UUID proyectoId, String proyectoNombre, LocalDate desde, LocalDate hasta, Instant createdAt,
        List<Grupo> grupos, BigDecimal total, BigDecimal totalKg,
        BigDecimal saldoAnterior, List<AbonoDelPeriodo> abonos, BigDecimal totalAbonos, BigDecimal totalActualizado) {

    public record Grupo(LocalDate fechaEntrega, String nombre, List<Linea> lineas, BigDecimal subtotal, BigDecimal subtotalKg) {}
    public record Linea(UUID documentoId, String folio, BigDecimal cantidad, String descripcion, BigDecimal pesoUnitKg,
                        BigDecimal pesoTotalKg, BigDecimal precioUnit, BigDecimal total, String lugarEntrega) {}
    public record AbonoDelPeriodo(LocalDate fecha, BigDecimal monto, String nota) {}
}
```

`BE/service/ConsolidadoService.java`, reescrito. `armar(cliente, proyecto, desde, hasta, nombresGuardados)` hace exactamente esto:

1. Si `desde > hasta`: 400 `La fecha inicial no puede ser posterior a la final`. Un proyecto de otro cliente: 400.
2. `inicio = max(desde, fecha_inicio_cuenta)`.
3. `facturas = documentoRepository.facturasDeCuenta(cliente, proyecto, inicio, hasta)`.
4. Cada línea va al grupo `coalesce(fecha_entrega, documento.fecha)`. Grupos por fecha ascendente. Dentro de cada grupo, por fecha de la factura, folio y `orden`.
5. Nombre del grupo N (desde 1): el guardado para esa fecha o, si no hay, `Subtotal N (2 de octubre)`.
6. `abonos` del período con fecha entre `inicio` y `hasta`, del cliente o del proyecto.
7. Calcula los totales de arriba. `saldoAnterior` usa `CuentaClienteService.saldoAl`.

Solo se guardan en `consolidado_grupos` los nombres que difieren del nombre por defecto.

`BE/controller/ConsolidadoController.java`, reescrito:

| Método y ruta | Entrada | Salida |
|---|---|---|
| `GET /api/consolidados?clienteId=` | `clienteId` opcional | `List<ConsolidadoResponse>`, los más recientes primero |
| `GET /api/consolidados/{id}` | — | `ConsolidadoResponse` |
| `POST /api/consolidados/vista-previa` | `ConsolidadoRangoRequest` | `ConsolidadoResponse` con `id` y `folio` en `null` |
| `POST /api/consolidados` | `ConsolidadoGuardarRequest` | 201 `ConsolidadoResponse`, con folio `CONS-0001` |
| `PUT /api/consolidados/{id}` | `ConsolidadoGuardarRequest`; `clienteId` y `proyectoId` se ignoran | `ConsolidadoResponse` |
| `DELETE /api/consolidados/{id}` | — | 204 |
| `GET /api/consolidados/{id}/excel?encabezado=` | — | Archivo `.xlsx` |

`BE/exportacion/ConsolidadoExcel.java`: mismas columnas, estilos y encabezado que `DocumentoExcel`, con el encabezado por defecto `clienteNombre / proyectoNombre`.

| Filas | Contenido |
|---|---|
| Líneas de cada grupo | `G` y `I` con las fórmulas `ROUND` de `DocumentoExcel` |
| Subtotal de cada grupo | `C` nombre en negrita, `G` `=SUM(...)` y `I` `=SUM(...)` del grupo |
| Fin de línea | `*****FIN DE LÍNEA*****` |
| Totales | `Total C$` `=SUM(I8,I11,...)` y `Total KG` `=SUM(G8,G11,...)`, sumando solo los subtotales |
| Fila en blanco | |
| Solo si es distinto de 0 | `Saldo anterior`, en `H` e `I` |
| Solo si es distinto de 0 | `Menos abonos`, en `H` e `I` |
| Fila en blanco | |
| Solo si alguno de los dos anteriores aparece | `Total actualizado C$`, con la fórmula `=total + saldo − abonos` usando esas celdas |

### Frontend

Se eliminan `FE/components/consolidados/NuevoConsolidadoSheet.tsx` y `ConsolidadoDetailSheet.tsx`.

- `FE/types/index.ts`: reemplaza `ConsolidadoItem`, `ConsolidadoGroup` y `Consolidado` por los tipos de `ConsolidadoResponse`.
- `FE/services/consolidadoService.ts`: `listar`, `obtener`, `vistaPrevia`, `crear`, `actualizar`, `eliminar`, `descargarExcel`.
- `FE/pages/ConsolidadosPage.tsx`: tarjetas con Folio, Nombre, Cliente y proyecto, Rango y `Total actualizado`. El botón `Nuevo consolidado` va a `/consolidados/nuevo`, una tarjeta abre `/consolidados/:id`, y eliminar pide confirmación con el folio. Título: `Consolidados`.
- `FE/App.tsx`: rutas `consolidados/nuevo` y `consolidados/:id`.
- `FE/pages/ConsolidadoEditorPage.tsx`:
  1. Formulario: Cliente, Proyecto (`Todos`), Desde, Hasta y el botón `Ver consolidado`. Al editar uno guardado, Cliente y Proyecto no se pueden cambiar.
  2. Vista: un bloque por grupo, con sus líneas en tabla y un campo editable con el nombre del subtotal, junto al subtotal y sus kilos.
  3. Totales: `Total C$`, `Total KG`, `Saldo anterior`, `Menos abonos` y `Total actualizado C$`, con la misma regla de visibilidad del Excel. Los abonos del período se listan con fecha, monto y nota.
  4. Campo `Nombre del consolidado`, por defecto `Consolidado {cliente}`, y botón `Guardar`. Los nombres editados se recuerdan por fecha al volver a pedir la vista.
  5. Con el consolidado guardado aparece `Exportar`, que usa `ExportarDialog`, y al salir con cambios sin guardar se pregunta como en el editor de documentos.
- `FE/components/export/ConsolidadoPlantilla.tsx`: el mismo diseño que `DocumentoPlantilla`, con filas de subtotal y el bloque final de totales.

### Tests

`BT/support/IntegrationTest.java`: en el `truncate` reemplaza `consolidado_items, consolidado_groups, consolidados` por `consolidado_grupos, consolidados`, y agrega `alter sequence seq_folio_cons restart`.

`BT/service/ConsolidadoServiceTest.java`. Datos: rango del 2026-03-01 al 2026-03-31 y saldo al 28 de febrero de 4,000.00, que sale de un saldo inicial de 1,000.00, una factura de 5,000.00 y un abono de 2,000.00, todos de febrero.

| Dato | Valor |
|---|---|
| Factura del 2 de marzo, línea 1 | 1,000.00, sin fecha de entrega |
| Factura del 2 de marzo, línea 2 | 500.00, entrega el 5 de marzo |
| Factura del 5 de marzo | 2,000.00, sin fecha de entrega |
| Abono del 20 de marzo | 1,500.00 |

Resultado esperado:

- `Subtotal 1 (2 de marzo)` = 1,000.00 y `Subtotal 2 (5 de marzo)` = 2,500.00.
- Total 3,500.00; saldo anterior 4,000.00; abonos 1,500.00; total actualizado 6,000.00, igual a `saldoAl(31 de marzo)`.
- Editar la factura cambia el consolidado sin volver a guardarlo.
- Una factura anulada desaparece. Una importada no entra.
- Un nombre editado se conserva, y al mover una factura a otra fecha ese nombre sigue en su fecha.
- Filtrar por proyecto no incluye el saldo inicial.
- Los folios son `CONS-0001`, `CONS-0002`.

`BT/exportacion/ConsolidadoExcelTest.java`: subtotales con fórmula, `Total C$` igual a la suma de subtotales y `Total actualizado C$` evaluado igual al del servicio; sin saldo ni abonos, esas filas no existen.

### Commits

1. `feat(consolidados): armado en vivo por fecha de entrega` — `MIG/V7__consolidados_en_vivo.sql`, `BE/model/Consolidado.java`, `ConsolidadoGrupo.java`, `BE/repository/ConsolidadoRepository.java`, `AbonoRepository.java`, `BE/util/Fechas.java`, `BE/service/ConsolidadoService.java`, `BE/controller/ConsolidadoController.java`, DTOs, `BT/support/IntegrationTest.java`, `BT/service/ConsolidadoServiceTest.java`; elimina los modelos, repositorios y DTOs anteriores. Va en un solo commit porque el servicio anterior no compila sin esos modelos.
2. `feat(export): consolidado en Excel` — `BE/exportacion/ConsolidadoExcel.java`, `BE/service/ConsolidadoService.java`, `BE/controller/ConsolidadoController.java`, `BT/exportacion/ConsolidadoExcelTest.java`
3. `feat(ui): consolidado simple con vista previa` — `FE/pages/ConsolidadoEditorPage.tsx`, `FE/pages/ConsolidadosPage.tsx`, `FE/App.tsx`, `FE/services/consolidadoService.ts`, `FE/types/index.ts`; elimina los dos paneles anteriores
4. `feat(export): consolidado en PNG y PDF` — `FE/components/export/ConsolidadoPlantilla.tsx`, `FE/pages/ConsolidadoEditorPage.tsx`
5. `docs: quitar el PR 11 de los pendientes` — `docs/PRsPendientes.md`

### DoD

- Editar una factura se refleja en el consolidado.
- Los nombres de los subtotales se conservan.
- El ejemplo da exactamente esos números y `Total actualizado` coincide con el saldo de la cuenta.
- Los tres formatos de exportación muestran los mismos datos.
- Ya no existen ítems ni grupos editables a mano.

---

## PR 12 · `feat/compras-y-ganancias`

**Objetivo:** registrar la compra al proveedor ligada a la venta y ver la ganancia real (D11, D12 y D13).

### Migración `MIG/V8__compras.sql`

```sql
-- Compra al proveedor ligada a una factura: da el costo real de sus líneas.
create table compras (
    id             uuid                        not null primary key,
    created_at     timestamp(6) with time zone not null,
    updated_at     timestamp(6) with time zone not null,
    proveedor_id   uuid                        not null constraint fk_compras_proveedor references proveedores,
    documento_id   uuid                        not null constraint fk_compras_documento references documentos,
    fecha          date                        not null,
    numero_factura varchar(50),
    nota           text
);

create index idx_compras_documento on compras (documento_id);

alter table documento_lineas add column compra_id uuid constraint fk_lineas_compra references compras;
create index idx_lineas_compra on documento_lineas (compra_id);
```

### Backend: compras

- `BE/model/Compra.java`: `proveedor`, `documento`, `fecha`, `numeroFactura`, `nota`.
- `BE/model/DocumentoLinea.java`: agrega `compra`.
- `BE/dto/LineaResponse.java`: agrega `UUID compraId`.
- `BE/repository/CompraRepository.java` y `ProveedorRepository.findByActivoTrueOrderByNombre()`.
- `BE/service/DocumentoService.java`: en `aplicarLineas`, si la línea cambia de producto, además de recalcular el costo pone `compra = null`. Una línea que se conserva mantiene su compra y su costo. `convertirAFactura` no copia la compra.

```java
public record CompraRequest(@NotNull UUID proveedorId, @NotNull LocalDate fecha,
        @Size(max = 50) String numeroFactura, String nota, @NotEmpty List<@Valid Linea> lineas) {
    public record Linea(@NotNull UUID lineaId, @NotNull @Positive BigDecimal precioSinIva,
                        @DecimalMin("0") @DecimalMax("99.99") BigDecimal descuentoPct,
                        @Positive BigDecimal pesoUnitKg) {}
}
public record CompraResponse(UUID id, UUID proveedorId, String proveedorNombre, LocalDate fecha,
                             String numeroFactura, String nota) {}
public record ProveedorRequest(@NotBlank @Size(max = 100) String nombre) {}
public record ProveedorResponse(UUID id, String nombre) {}
```

`BE/service/CompraService.java`, `registrar(documentoId, request)`, en una transacción:

1. El documento debe ser `FACTURA` y `ACTIVA`. Si no: 409 `Solo se registran compras de facturas activas`.
2. Cada `lineaId` debe ser de ese documento. Si no: 400 `La línea no pertenece a esta factura`.
3. Por cada línea: `compra = la nueva` y `costoUnitConIva = r2(precioSinIva × factorIva)`.
4. Si trae `pesoUnitKg`: recalcula `pesoUnitKg` y `pesoTotalKg` de la línea con `CalculoDocumento.linea`. Luego recalcula `documento.totalKg`. El precio, el total de la línea y el total del documento no cambian.
5. Si el producto no es genérico y trae `descuentoPct`, llama a `precioListaService.registrar(productoId, new PrecioListaRequest(FACTURA_PROVEEDOR, precioSinIva, descuentoPct, fecha, "Compra " + numeroFactura))`. Sin número de factura, la nota es `Compra`. Sin descuento no se registra precio de lista.
6. No crea versión anterior del documento.

| Método y ruta | Controlador | Salida |
|---|---|---|
| `POST /api/documentos/{id}/compras` | `BE/controller/CompraController.java` | 201 `CompraResponse` |
| `GET /api/documentos/{id}/compras` | `BE/controller/CompraController.java` | `List<CompraResponse>` |
| `GET /api/proveedores` | `BE/controller/ProveedorController.java` | `List<ProveedorResponse>`, solo activos, por nombre |
| `POST /api/proveedores` | `BE/controller/ProveedorController.java` | 201. Nombre repetido: 409 `Ya existe ese proveedor` |

### Backend: ganancias

Se eliminan `GET /api/reportes/general` y `BE/dto/ReporteResponse.java`. `BE/service/ReporteService.java` y `BE/controller/ReporteController.java` quedan solo con esto:

```
GET /api/reportes/ganancias?desde=&hasta=&clienteId=&proyectoId=
```

```java
public record GananciasResponse(BigDecimal facturado, BigDecimal ventasConCosto, BigDecimal costo, BigDecimal ganancia,
                                int lineasSinCosto, BigDecimal ventasSinCosto, List<FilaGanancia> facturas) {
    public record FilaGanancia(UUID id, String folio, LocalDate fecha, String clienteNombre, BigDecimal total,
                               BigDecimal costo, BigDecimal ganancia, int lineasSinCosto) {}
}
```

Consulta nativa en `DocumentoRepository`, con la proyección de interfaz `GananciaPorFactura`. Sus getters son `getId`, `getFolio`, `getFecha`, `getCliente`, `getTotal`, `getCosto`, `getVentasConCosto` y `getLineasSinCosto`. Los alias van sin guion bajo porque Spring los compara con los getters sin distinguir mayúsculas:

```sql
select d.id as id, d.folio as folio, d.fecha as fecha, c.nombre as cliente, d.total as total,
       coalesce(sum(l.cantidad * l.costo_unit_con_iva), 0) as costo,
       coalesce(sum(l.total) filter (where l.costo_unit_con_iva is not null), 0) as ventasconcosto,
       count(*) filter (where l.costo_unit_con_iva is null) as lineassincosto
from documentos d
join clientes c on c.id = d.cliente_id
join documento_lineas l on l.documento_id = d.id
where d.tipo = 'FACTURA' and d.estado = 'ACTIVA'
  and (cast(:clienteId as uuid) is null or d.cliente_id = cast(:clienteId as uuid))
  and (cast(:proyectoId as uuid) is null or d.proyecto_id = cast(:proyectoId as uuid))
  and (cast(:desde as date) is null or d.fecha >= cast(:desde as date))
  and (cast(:hasta as date) is null or d.fecha <= cast(:hasta as date))
group by d.id, d.folio, d.fecha, c.nombre, d.total
order by d.fecha desc, d.folio desc
```

Por factura: `ganancia = ventasConCosto − r2(costo)`. Los totales son la suma de las filas, y `ventasSinCosto = facturado − ventasConCosto`. Lo importado sí cuenta en este reporte, porque es venta real con costo conocido.

### Frontend

- `package.json`: se quita `recharts`.
- `FE/types/index.ts`: quita `ReporteData`. Nuevos `Ganancias`, `Compra`, `Proveedor`. `LineaDocumento` agrega `compraId`.
- `FE/services/reporteService.ts`: solo `ganancias(filtros)`.
- `FE/services/compraService.ts` y `FE/services/proveedorService.ts`, nuevos.
- `FE/pages/ReportesPage.tsx`, reescrita. El título es `Reportes`:
  1. Filtros: Desde, Hasta, Cliente, Proyecto.
  2. Tarjetas: Facturado, Costo, Ganancia.
  3. Si `lineasSinCosto > 0`, aviso en ámbar: `N líneas sin costo (C$X vendidos) no entran en la ganancia.`
  4. Tabla: Folio (enlace), Fecha, Cliente, Total, Costo, Ganancia y `Sin costo`.
- `FE/pages/DocumentoEditorPage.tsx`: botón `Registrar compra` en facturas `ACTIVA` guardadas. Con cambios sin guardar, el botón se deshabilita con el texto de ayuda `Guarda primero los cambios`.
- `FE/components/documentos/RegistrarCompraDialog.tsx`:
  - Proveedor (`<select>` y botón para crear uno en el momento), Fecha (hoy) y Número de factura.
  - Una fila por línea: casilla de incluir (marcada si no tiene compra), Descripción, Cantidad, `Precio sin IVA (C$)`, `Descuento (%)` y `Peso unit. real (kg)`.
  - El descuento se llena con `descuentoProveedorSugerido` de `GET /api/productos/{id}/precios-lista`, y se oculta en productos genéricos.
  - Al guardar, recarga el documento y muestra `Compra registrada.`

### Tests

`BT/support/IntegrationTest.java`: agrega `compras` al `truncate`.

`BT/service/CompraServiceTest.java`:

- Precio 950.00 sin IVA: costo 1,092.50.
- Precio 840.00 con 16 %: costo 966.00, y se crea un precio de lista 1,000.0000 con fuente `FACTURA_PROVEEDOR`.
- Sin descuento: no crea precio de lista.
- Genérico con descuento: no crea precio de lista.
- Peso real 30.00 con 2 unidades: 60.00 kg en la línea, `totalKg` actualizado y total sin cambios.
- Línea de otra factura: 400. Factura anulada o cotización: 409.
- Editar la factura conservando la línea mantiene compra y costo. Cambiar el producto los recalcula.
- Registrar otra vez sobre la misma línea reemplaza el costo.

`BT/service/ReporteServiceTest.java`. Datos: línea de 2 unidades a PU 1,300.00 con costo 1,092.50, y línea de 1 unidad a PU 500.00 sin costo.

- Facturado 3,100.00; ventas con costo 2,600.00; costo 2,185.00; ganancia 415.00, que son 207.50 por unidad.
- `lineasSinCosto` = 1 y `ventasSinCosto` = 500.00.
- Cotizaciones y anuladas no cuentan. Los filtros de fecha, cliente y proyecto funcionan.

`BT/controller/ProveedorControllerTest.java`: listar, crear y nombre repetido.

### Commits

1. `feat(db): compras ligadas a la factura` — `MIG/V8__compras.sql`, `BE/model/Compra.java`, `DocumentoLinea.java`, `BE/repository/CompraRepository.java`, `BT/support/IntegrationTest.java`
2. `feat(compras): registrar la compra de una factura` — `BE/service/CompraService.java`, `BE/controller/CompraController.java`, `BE/service/DocumentoService.java`, `BE/dto/CompraRequest.java`, `CompraResponse.java`, `LineaResponse.java`, `BT/service/CompraServiceTest.java`
3. `feat(proveedores): listar y crear proveedores` — `BE/controller/ProveedorController.java`, `BE/repository/ProveedorRepository.java`, DTOs, `BT/controller/ProveedorControllerTest.java`
4. `feat(ganancias): reporte por factura sin contar costos en cero` — `BE/service/ReporteService.java`, `BE/controller/ReporteController.java`, `BE/repository/DocumentoRepository.java`, `BE/dto/GananciasResponse.java`, `BT/service/ReporteServiceTest.java`; elimina `ReporteResponse.java`
5. `feat(ui): registrar la compra desde la factura` — `FE/components/documentos/RegistrarCompraDialog.tsx`, `FE/pages/DocumentoEditorPage.tsx`, `FE/services/compraService.ts`, `proveedorService.ts`, `FE/types/index.ts`
6. `feat(ui): reporte de ganancias simple` — `FE/pages/ReportesPage.tsx`, `FE/services/reporteService.ts`, `FE/types/index.ts`, `frontend/package.json`, `frontend/package-lock.json`
7. `docs: quitar el PR 12 de los pendientes` — `docs/PRsPendientes.md`

### DoD

- PU 1,300.00 con costo 950.00 sin IVA da 207.50 de ganancia por unidad.
- Las líneas sin costo se avisan aparte y no inflan la ganancia.
- Registrar una compra no cambia el total de la factura.
- El peso del proveedor reemplaza al calculado en esa línea.
- Cotizaciones y anuladas no aparecen en el reporte.
- `grep -r recharts frontend/src frontend/package.json` no devuelve nada.

---

## PR 13 · `chore/pulido-movil-y-docs`

**Objetivo:** que sirva en el celular para una emergencia, que sea cómoda para una persona mayor y que quede documentada.

### Celular

- `FE/components/layout/AppLayout.tsx`: debajo de 768 px, una barra superior con el logo y un botón de menú (`aria-label="Abrir menú"`). Desde 768 px, el menú lateral actual.
- `FE/components/layout/AppSidebar.tsx`: el contenido del menú se saca a `MenuNavegacion`. En celular se muestra dentro de `Sheet` (`components/ui/sheet`) y se cierra al elegir una opción.
- Se usan solo clases `md:` de Tailwind, sin `useIsMobile`.
- Todas las páginas: `p-8` pasa a `p-4 md:p-8`.
- `FE/components/documentos/LineasTabla.tsx`: desde 768 px, la tabla actual. Debajo, una tarjeta por línea, con los mismos campos, etiquetas visibles, `Total` y `Quitar`.
- Las demás tablas conservan `overflow-x-auto`. La barra de `Guardar` sigue fija abajo.

### Legibilidad

En `FE/pages`, `FE/components/**`, excepto `FE/components/ui` y `FE/components/export`:

| Cambio | Detalle |
|---|---|
| Texto mínimo de 14 px | `text-[10px]`, `text-[11px]` y `text-xs` pasan a `text-sm` |
| Campos de 16 px | `FE/components/ui/input.tsx`: `text-base`, para que el celular no haga zoom al escribir |
| Botones de 44 px | `FE/components/ui/button.tsx`: `default` con `h-11`. Se quitan los `h-8` y `h-9` puestos a mano en botones de acción. Los de ícono miden 44 × 44 px |

Títulos, etiquetas y botones llevan mayúscula solo en la primera palabra: `Nuevo Cliente` pasa a `Nuevo cliente`, `Guardar Cambios` a `Guardar cambios`, `Cerrar Sesión` a `Cerrar sesión`, y así en todos. Además:

| Pantalla | Texto actual | Texto nuevo |
|---|---|---|
| Productos | `Catálogo de materiales y precios` | `Los materiales que vendes y sus precios de lista` |
| Proyectos | `Facturado en Obras` | `Total facturado` |
| Proyectos | `Total Proyectos` | `Total de proyectos` |

Confirmación con `window.confirm`, nombrando el objeto, antes de: eliminar cliente, producto, abono o consolidado, y anular o rechazar un documento.

Pantallas vacías, con mensaje y una acción:

| Pantalla | Mensaje | Acción |
|---|---|---|
| Facturas | `Todavía no hay facturas.` | `Nueva factura` |
| Cotizaciones | `Todavía no hay cotizaciones.` | `Nueva cotización` |
| Clientes | `Todavía no hay clientes.` | `Nuevo cliente` |
| Productos | `Todavía no hay productos.` | `Importar lista` |
| Proyectos | `Todavía no hay proyectos.` | `Nuevo proyecto` |
| Consolidados | `Todavía no hay consolidados.` | `Nuevo consolidado` |

### Paginación de documentos (D14)

```java
public record PaginaResponse<T>(List<T> contenido, int pagina, int totalPaginas, long totalElementos) {}
```

- `BE/repository/DocumentoRepository.java`: `filtrar` pasa a recibir `Pageable` y devolver `Page<Documento>`, y agrega el parámetro `q`. Usa `LEFT JOIN d.proyecto p` y la condición `(:q = '' OR lower(d.folio) LIKE :contiene OR lower(d.cliente.nombre) LIKE :contiene OR lower(p.nombre) LIKE :contiene)`, donde `q` llega en minúsculas y `contiene = "%" + q + "%"`.
- `BE/controller/DocumentoController.java`: `GET /api/documentos` agrega `q`, `pagina` (por defecto 0) y `tamano` (por defecto 50, máximo 200), y devuelve `PaginaResponse<DocumentoResponse>`.
- `FE/pages/DocumentosPage.tsx`: la búsqueda (espera de 300 ms) y el filtro de estado van al backend. Abajo, `Anterior`, `Página X de Y` y `Siguiente`. Cambiar la búsqueda o el filtro vuelve a la primera página.
- `FE/pages/ClienteDetallePage.tsx`, `FE/components/proyectos/ProyectoDetailSheet.tsx` y `FE/components/productos/ProductoDetailSheet.tsx`: piden `tamano=200` y leen `contenido`. Con más de 200, muestran `Se muestran los 200 más recientes.`
- `FE/pages/ProyectosPage.tsx`: deja de cargar documentos. `BE/dto/ProyectoResponse.java` agrega `BigDecimal totalFacturado`, y `BE/service/ProyectoService.java` lo llena con una consulta nativa agrupada por `proyecto_id` sobre las facturas `ACTIVA`, con la proyección `SumaPorProyecto` (`getProyectoId()` y `getSuma()`, alias `proyectoid` y `suma`). La tarjeta `Total facturado` suma lo de los proyectos.

### Limpieza

1. Se eliminan estos archivos, que nada usa fuera de `sidebar.tsx`:
   - `FE/components/ui/`: `avatar.tsx`, `badge.tsx`, `card.tsx`, `dropdown-menu.tsx`, `label.tsx`, `select.tsx`, `separator.tsx`, `sidebar.tsx`, `skeleton.tsx`, `table.tsx` y `tooltip.tsx`.
   - `FE/hooks/use-mobile.ts`.
2. `FE/App.tsx`: se quita `TooltipProvider`.
3. Advertencias de `oxlint`:
   - `FE/context/AuthContext.tsx:29`: `catch {`.
   - `FE/context/AuthContext.tsx:59`: mover `useAuth` a `FE/hooks/useAuth.ts` y el contexto a `FE/context/auth-context.ts`, y actualizar los imports.
   - `FE/components/ui/button.tsx:58`: dejar de exportar `buttonVariants`, que ningún otro archivo usa.
   - `badge.tsx` y `sidebar.tsx`: desaparecen con los archivos.
4. Las ramas viejas del remoto no se tocan.

### Documentación

- `docs/ManualDeUso.md`, nuevo, en lenguaje llano y con pasos numerados, sin capturas para no publicar datos:
  1. Hacer una cotización.
  2. Convertirla en factura.
  3. Buscar un precio.
  4. Enviar un documento por WhatsApp.
  5. Registrar un abono y ver la cuenta de un cliente.
  6. Armar un consolidado.
  7. Subir una lista de precios nueva.
- `docs/Requerimientos.md`: refleja lo implementado y lo que quedó fuera de alcance.
- `docs/DiagramaER.md`: esquema final, hasta V8.
- `docs/PlanDeDesarrollo.md`: todos los PR marcados como hechos.
- Se elimina `docs/PRsPendientes.md`.

### Tests

`BT/controller/DocumentoControllerTest.java`, nuevo:

- `tamano=2` con 5 documentos da 3 páginas y 5 elementos.
- `tamano=500` se limita a 200.
- `q` encuentra por folio, cliente y proyecto, y funciona con documentos sin proyecto.
- Se mantiene el orden por fecha descendente.

`BT/service/ProyectoServiceTest.java`, nuevo: `totalFacturado` suma las facturas activas del proyecto, y no cuenta cotizaciones ni anuladas.

### Commits

1. `feat(ui): menú y líneas adaptados al celular` — `FE/components/layout/AppLayout.tsx`, `AppSidebar.tsx`, `FE/components/documentos/LineasTabla.tsx`, `FE/pages/*.tsx`
2. `feat(ui): texto y botones más grandes` — `FE/components/ui/input.tsx`, `button.tsx`, `FE/pages/*.tsx`, `FE/components/**`
3. `feat(ui): lenguaje llano, confirmaciones y pantallas vacías` — `FE/pages/*.tsx`, `FE/components/**`
4. `perf(api): paginación y búsqueda de documentos en el servidor` — `BE/dto/PaginaResponse.java`, `ProyectoResponse.java`, `BE/repository/DocumentoRepository.java`, `BE/service/DocumentoService.java`, `ProyectoService.java`, `BE/controller/DocumentoController.java`, tests, `FE/services/documentoService.ts`, `FE/types/index.ts`, `FE/pages/DocumentosPage.tsx`, `ClienteDetallePage.tsx`, `ProyectosPage.tsx`, `FE/components/proyectos/ProyectoDetailSheet.tsx`, `FE/components/productos/ProductoDetailSheet.tsx`
5. `refactor: eliminar componentes sin uso y advertencias de lint` — los archivos de la sección Limpieza, `FE/App.tsx`, `FE/context/`, `FE/hooks/`, `FE/components/ui/button.tsx`
6. `docs: manual de uso y documentación final` — `docs/ManualDeUso.md`, `Requerimientos.md`, `DiagramaER.md`, `PlanDeDesarrollo.md`; elimina `docs/PRsPendientes.md`

### DoD

- A 375 px se completa este recorrido sin desplazamiento horizontal de la página: buscar un cliente, crear una factura con el último precio, exportar el PNG y registrar un abono.
- Lighthouse en DevTools, en modo móvil, da 90 o más en accesibilidad en `/`, `/documentos/nuevo` y `/clientes`.
- No queda texto de menos de 14 px fuera de las plantillas de exportación.
- Las listas de documentos responden igual con miles de registros: cada página pide como máximo 50.
- `npx tsc -b` y `npm run lint` terminan sin errores ni advertencias.
- El manual cubre las 7 tareas.
- `docs/PRsPendientes.md` ya no existe.
