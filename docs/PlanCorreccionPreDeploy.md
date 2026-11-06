# Plan de corrección pre-deploy

Especificación cerrada, escrita el 2026-11-06 sobre el commit `137b8a1`. Todas las decisiones de negocio ya están tomadas y aparecen dentro del PR que las implementa.

## Reglas para quien implemente

1. Implementar los PR en el orden de la tabla. Cada uno sale de `main` actualizado, después de que el anterior esté fusionado.
2. Escribir exactamente lo que dice este documento: nombres, textos de pantalla, SQL y mensajes.
3. No agregar funciones, opciones ni refactors que no aparezcan aquí.
4. Si algo no coincide con el código, o falta un dato, detenerse y preguntar al dueño del proyecto. No suponer.
5. Si un test existente contradice este plan, cambiar el test al comportamiento descrito aquí. Si falla por cualquier otra causa, corregir la causa.
6. El repositorio es público. No escribir nombres reales de clientes, del proveedor ni precios reales en código, tests, commits o PR. Los archivos reales de Excel nunca entran al repositorio.
7. Commits en español con el formato ya usado (`fix(area): …`, `feat(area): …`, `docs: …`), con el usuario de git configurado y sin líneas `Co-Authored-By`.
8. Abrir un PR por cada fila de la tabla y dejar la fusión al dueño del proyecto.
9. Al terminar cada PR, marcarlo como `Hecho` en la tabla de esta página.

**Definición de terminado de cada PR:**

```bash
cd backend/BillMe && ./mvnw -B verify
```

```bash
cd frontend && npm run lint && npm run build
```

Ambos comandos deben pasar. `./mvnw` necesita `JAVA_HOME` apuntando a un JDK 21. Además, hay que hacer la verificación manual indicada en cada PR, con el backend y el frontend corriendo en local.

| PR | Rama | Título | Migración | Estado |
|---|---|---|---|---|
| C1 | `fix/bloqueantes-pre-deploy` | Fallos que bloquean el deploy | — | Hecho |
| C2 | `feat/historial-y-total-usd` | Historial inmutable y total en dólares | V9 | Hecho |
| C3 | `feat/historico-de-precios` | Histórico de precios, conversión y anulación | V10 | Pendiente |
| C4 | `feat/tarifa-por-cliente` | Tarifa por cliente y precio por peso | V11 | Pendiente |
| C5 | `fix/interfaz-pre-deploy` | Legibilidad, barra fija, consolidados y PDF | — | Pendiente |
| C6 | `fix/logica-menor` | Búsqueda, orden, cambio de cliente y mensajes | — | Pendiente |
| C7 | `docs/actualizacion-final` | Documentación final | — | Pendiente |

Después de C7 sigue la sección [Deploy](#deploy). El backend vive en `backend/BillMe/src/main/java/com/example/billme/` (abreviado `BE/`) y el frontend en `frontend/src/` (abreviado `FE/`).

---

## C1 · Fallos que bloquean el deploy

### 1. Volver a guardar un consolidado con subtotales renombrados

Hoy responde 409: Hibernate inserta los grupos nuevos antes de borrar los viejos y choca con `uq_grupos_consolidado_fecha`.

- `BE/service/ConsolidadoService.java`, método `actualizar`: agregar `consolidadoRepository.flush();` en la línea siguiente a `consolidado.getGrupos().clear();`.
- `FE/pages/ConsolidadoEditorPage.tsx`, `handleGuardar`: enviar solo nombres con texto:

```ts
const gruposPayload = Object.entries(nombresGrupos)
  .filter(([, nombre]) => nombre.trim() !== '')
  .map(([fechaEntrega, nombre]) => ({ fechaEntrega, nombre: nombre.trim() }))
```

- Test nuevo en `ConsolidadoServiceTest`: crear un consolidado con un grupo renombrado, llamar `actualizar` con el mismo nombre, llamar `actualizar` con otro nombre y comprobar con `obtener` que el grupo tiene el último nombre.

### 2. El diálogo de precio aplica un método distinto del que se ve

Escribir en "Por metro" o en "Descuento" no marca esa opción y se aplica el último precio. En `FE/components/documentos/PrecioLineaDialog.tsx`:

- En los tres `Input` de "Por metro", agregar `onFocus={() => setMetodo('POR_METRO')}`.
- En el `Input` de descuento, agregar `onFocus={() => setMetodo('FORMULA_LISTA')}`.
- Validar el descuento antes de aplicarlo:

```ts
const descuento = parseFloat(descuentoPct)
const descuentoValido = !isNaN(descuento) && descuento >= 0 && descuento < 100
const formulaAlDia = formulaRes != null && Number(formulaRes.descuentoPct) === descuento
```

- `puedeAplicar`: la condición de fórmula pasa a `metodo === 'FORMULA_LISTA' && descuentoValido && formulaAlDia`.
- `handleAplicar`: en fórmula, usar solo `formulaRes!.precioUnit`.
- Si `descuentoValido` es falso, mostrar `Descuento no válido` en vez del precio de esa opción.

**Verificación manual:** con un último precio existente, escribir 2700, 6.75 y 5.40 en "Por metro" y presionar "Usar este precio". La línea debe quedar en 2,160.00.

### 3. Un proyecto con movimientos no cambia de cliente

- Agregar `boolean existsByProyectoId(UUID proyectoId);` a `DocumentoRepository`, `AbonoRepository` y `ConsolidadoRepository`.
- `BE/service/ProyectoService.java`: inyectar `AbonoRepository` y `ConsolidadoRepository`. En `actualizar`, antes de asignar el cliente nuevo:

```java
if (documentoRepository.existsByProyectoId(id) || abonoRepository.existsByProyectoId(id)
        || consolidadoRepository.existsByProyectoId(id)) {
    throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Este proyecto tiene movimientos: no se puede pasar a otro cliente");
}
```

- `FE/components/proyectos/ProyectoModal.tsx`: al editar, el selector de cliente queda `disabled={isEdit}` y debajo se muestra `El cliente de un proyecto no se cambia.`
- Test nuevo en `ProyectoServiceTest`: con una factura en el proyecto, `actualizar` con otro cliente da 409 y la factura sigue siendo editable.

### 4. Movimientos anteriores al inicio de la cuenta

Hoy se guardan pero no aparecen ni afectan el saldo.

- `BE/util/Fechas.java`: agregar `public static String corta(LocalDate fecha)` con el patrón `dd/MM/yyyy`.
- `BE/service/AbonoService.java`: en `registrarAbono` y `actualizarAbono`, justo después de obtener el cliente:

```java
LocalDate inicio = cliente.getFechaInicioCuenta();
if (inicio != null && request.fechaPago().isBefore(inicio)) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "La fecha del abono es anterior al inicio de la cuenta (" + Fechas.corta(inicio) + ")");
}
```

- `AbonoRepository`: agregar `boolean existsByClienteIdAndFechaPagoBefore(UUID clienteId, LocalDate fecha);`
- `BE/service/ClienteService.java`, método `actualizar`, antes de asignar los campos:

```java
if (request.fechaInicioCuenta() != null
        && abonoRepository.existsByClienteIdAndFechaPagoBefore(id, request.fechaInicioCuenta())) {
    throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Hay abonos anteriores a esa fecha: cámbialos antes de mover el inicio de la cuenta");
}
```

- `FE/pages/DocumentoEditorPage.tsx`: una factura con fecha anterior sí se guarda, pero avisa. Definir `const clienteActual = clientes.find(c => c.id === form.clienteId)` y debajo del campo Fecha mostrar en `text-sm text-amber-700`, solo si `tipo === 'FACTURA'` y `clienteActual?.fechaInicioCuenta` existe y `form.fecha < clienteActual.fechaInicioCuenta`:

  `Esta fecha es anterior al inicio de la cuenta (dd/mm/aaaa): la factura no afectará el saldo.` (usar `formatFechaCorta`)

- Tests nuevos: abono anterior rechazado al crear y al editar; mover el inicio después de un abono da 409.

### 5. Formularios que no caben en pantalla

En `FE/components/clientes/ClienteModal.tsx`, `FE/components/proyectos/ProyectoModal.tsx` y `FE/components/productos/ProductoModal.tsx`:

- Contenedor `fixed inset-0`: agregar `p-4`.
- Tarjeta `relative z-10`: agregar `max-h-full`. Debe conservar `flex flex-col overflow-hidden`.
- Encabezado y pie: deben tener `shrink-0`.
- Cuerpo: clases `flex-1 min-h-0 overflow-y-auto` junto con las que ya tiene.

En `FE/components/clientes/AbonoDialog.tsx`, agregar `max-h-[90vh] overflow-y-auto` a `DialogContent`.

**Verificación manual:** a 1366×650 y a 375×667, el botón de guardar de cada formulario queda visible y el cuerpo hace scroll.

### 6. Errores de guardado que se pierden

Cliente, proyecto y producto cierran el formulario aunque falle el guardado.

- En los tres modales, cambiar la prop a `onSave: (...) => Promise<void>` y agregar los estados `error` (string o null) y `guardando` (boolean).
- `handleSave` queda así, con los datos que ya arma cada modal:

```ts
const handleSave = async () => {
  setGuardando(true)
  setError(null)
  try {
    await onSave(datos)
    onOpenChange(false)
  } catch (e) {
    setError(e instanceof Error ? e.message : 'No se pudo guardar')
  } finally {
    setGuardando(false)
  }
}
```

- Reiniciar `error` al abrir. Mostrarlo al inicio del cuerpo con las clases `p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm`. El botón de guardar queda deshabilitado mientras `guardando`.
- Quitar el `try/catch` de los `handleSave` de `FE/pages/ClientesPage.tsx`, `FE/pages/ProyectosPage.tsx` y `FE/pages/ProductosPage.tsx`, y de `handleSaveCliente` en `FE/pages/ClienteDetallePage.tsx`, para que el error llegue al modal.

**Verificación manual:** crear un producto con un código repetido. El formulario sigue abierto, muestra el error y conserva lo escrito.

---

## C2 · Historial inmutable y total en dólares

Decisiones que implementa:

- **Campos editables en factura:** los visibles al exportar (nombre del producto en la línea, precio unitario, peso, cantidad, cliente y proyecto), además de fecha, notas y entrega. Esto ya funciona y se conserva. Cambiar el nombre en una línea no cambia el catálogo.
- **No editables:** el folio; la cotización aceptada (se corrige la factura); la factura anulada mientras no se reactive (C3). El costo cambia solo con "Registrar compra". El encabezado del diálogo de exportar vale solo para ese archivo y no se guarda.
- **Historial:** cada "Guardar" con cambios crea una versión. Nadie puede editar, borrar, reordenar ni restaurar versiones.
- **Total en dólares:** opción por documento, apagada por defecto, en cotizaciones, facturas y consolidados. Al activarla se ven Total C$, Total U$ y Total KG. El tipo de cambio se guarda al crear el documento, así que los totales viejos no cambian.

### Migración `V9__historial_inmutable_y_total_usd.sql`

```sql
-- Historial que no se puede alterar, tipo de cambio por documento y opción de total en dólares.
alter table documento_revisiones add column motivo varchar(200);
update documento_revisiones set motivo = 'Edición';
alter table documento_revisiones alter column motivo set not null;

create function impedir_cambio_revision() returns trigger language plpgsql as $$
begin
    raise exception 'El historial de un documento no se puede modificar ni borrar';
end;
$$;

create trigger revisiones_inmutables
    before update or delete on documento_revisiones
    for each row execute function impedir_cambio_revision();

alter table documentos
    add column mostrar_total_usd boolean not null default false,
    add column tipo_cambio numeric(12, 6);
update documentos
set tipo_cambio = (select valor::numeric from configuracion where clave = 'tipo_cambio');
alter table documentos alter column tipo_cambio set not null;

alter table consolidados
    add column mostrar_total_usd boolean not null default false,
    add column tipo_cambio numeric(12, 6);
update consolidados
set tipo_cambio = (select valor::numeric from configuracion where clave = 'tipo_cambio');
alter table consolidados alter column tipo_cambio set not null;
```

### Backend: historial

- `BE/model/DocumentoRevision.java`: agregar `@org.hibernate.annotations.Immutable` a la clase, quitar `@Setter` y agregar el campo `motivo` (`@Column(name = "motivo", nullable = false, length = 200)`).
- `BE/dto/RevisionResponse.java`: agregar `String motivo` después de `editadoPor`, y llenarlo en `DocumentoService.revisiones`.
- `BE/service/DocumentoService.java`: agregar estos métodos con visibilidad de paquete. No crear un servicio aparte.

```java
String copia(Documento documento) {
    return jsonMapper.writeValueAsString(toResponse(documento, null));
}

void registrarRevision(Documento documento, String copiaAnterior, String motivo) {
    revisionRepository.save(DocumentoRevision.builder()
            .documento(documento)
            .numero(revisionRepository.countByDocumentoId(documento.getId()) + 1)
            .snapshot(copiaAnterior)
            .motivo(motivo)
            .usuario(usuarioActual())
            .build());
}
```

Sacar la copia antes de cambiar el documento y registrar la versión así:

| Lugar | Motivo exacto | Cuándo |
|---|---|---|
| `actualizar` | `Edición` | Solo si `huella` cambió, como hoy |
| `convertirAFactura`, sobre la cotización | `Convertida en factura FAC-0000` (folio real) | Siempre |
| `cambiarEstadoCotizacion` | `Cotización rechazada` o `Cotización reabierta` | Siempre |
| `anular` | `Factura anulada` | Siempre |
| `CompraService.registrar` | `Compra registrada` | Siempre |

- `BE/service/CompraService.java`: inyectar `DocumentoService`. Sacar `String copiaAnterior = documentoService.copia(documento);` después de validar la factura y antes de cambiar líneas. Registrar la versión justo antes del `return`.
- No crear rutas de edición ni borrado de versiones. La API sigue siendo solo `GET /api/documentos/{id}/revisiones`.
- Tests nuevos en `DocumentoServiceTest`:
  - Editar cliente, proyecto, fecha, notas, descripción, cantidad, peso, precio y entrega deja una versión con los valores anteriores.
  - Registrar una compra, anular y cambiar estado dejan versión con su motivo.
  - `jdbc.update("update documento_revisiones set snapshot = 'x'")` lanza excepción.
  - `jdbc.update("delete from documento_revisiones")` lanza excepción.
  - Una copia JSON sin `mostrarTotalUsd`, `tipoCambio` ni `totalUsd` se sigue leyendo con `revisiones`.

### Backend: dólares en documentos

- `BE/calculo/CalculoDocumento.java`:

```java
public static BigDecimal enDolares(BigDecimal cordobas, BigDecimal tipoCambio) {
    return r2(cordobas.divide(tipoCambio, MathContext.DECIMAL64));
}
```

- El valor que se guarda en cada documento o consolidado nuevo siempre es `configuracionService.tipoCambio().setScale(6, RoundingMode.HALF_UP)`.
- `BE/model/Documento.java`: agregar `boolean mostrarTotalUsd` (`@Builder.Default` en `false`, columna `mostrar_total_usd`) y `BigDecimal tipoCambio` (columna `tipo_cambio`, `nullable = false`, `precision = 12`, `scale = 6`).
- `BE/dto/DocumentoRequest.java`: agregar `Boolean mostrarTotalUsd` después de `mostrarEntrega`, y conservar este constructor para los tests existentes:

```java
public DocumentoRequest(TipoDocumento tipo, UUID clienteId, UUID proyectoId, LocalDate fecha,
                        String notas, Boolean mostrarEntrega, List<LineaRequest> lineas) {
    this(tipo, clienteId, proyectoId, fecha, notas, mostrarEntrega, null, lineas);
}
```

- `BE/dto/DocumentoResponse.java`: agregar `Boolean mostrarTotalUsd, BigDecimal tipoCambio, BigDecimal totalUsd` después de `totalKg`. Se usa `Boolean` y no `boolean` para poder leer copias antiguas del historial.
- `BE/service/DocumentoService.java`:
  - `crear`: el `builder` incluye `.tipoCambio(...)` con el valor de Configuración.
  - `aplicar`: `documento.setMostrarTotalUsd(Boolean.TRUE.equals(request.mostrarTotalUsd()));`
  - `convertirAFactura`: copiar `mostrarTotalUsd` y `tipoCambio` de la cotización.
  - `huella`: agregar `isMostrarTotalUsd()`.
  - `toResponse`: `totalUsd = CalculoDocumento.enDolares(d.getTotal(), d.getTipoCambio())`.
- `BE/service/ImportacionCotizacionesService.java`: el `builder` de `Documento` incluye `.tipoCambio(...)` de Configuración. Inyectar el valor con el `configuracionService` que ya tiene.
- `BE/exportacion/DocumentoExcel.java`: agregar el formato `"U$"#,##0.00` con un estilo `styleTotalUsd` igual a `styleTotalPrecio`. Si `documento.isMostrarTotalUsd()`, insertar entre "Total C$" y "Total KG" la fila `Total U$`, con la fórmula `ROUND(I{filaTotalC$}/{tipoCambio},2)`, donde `{tipoCambio}` es `documento.getTipoCambio().toPlainString()`. El orden es Total C$, Total U$, Total KG.
- En `DocumentoControllerTest`, a cada `Documento.builder()` que se guarde en la base agregarle `.tipoCambio(new BigDecimal("36.6243"))`.

### Backend: dólares en consolidados

- `BE/model/Consolidado.java`: los mismos dos campos que en `Documento`.
- `BE/dto/ConsolidadoGuardarRequest.java`: agregar `Boolean mostrarTotalUsd` al final y conservar el constructor de 6 argumentos, que pasa `null`.
- `BE/dto/ConsolidadoResponse.java`: agregar al final `Boolean mostrarTotalUsd, BigDecimal tipoCambio, BigDecimal totalUsd, BigDecimal totalActualizadoUsd`. Conservar el constructor con los campos actuales, que pasa `false, null, null, null`, para `ConsolidadoExcelTest`.
- `BE/service/ConsolidadoService.java`: inyectar `ConfiguracionService` y agregar a `armar` los parámetros `boolean mostrarTotalUsd, BigDecimal tipoCambio`.
  - `vistaPrevia` y `guardarGruposEditados` pasan `false` y el TC de Configuración.
  - `toResponse` pasa los valores guardados.
  - `crear` guarda el flag del request y el TC de Configuración con escala 6.
  - `actualizar` guarda el flag y **no** cambia el TC.
  - `armar` calcula `totalUsd = enDolares(total, tipoCambio)` y `totalActualizadoUsd = enDolares(totalActualizado, tipoCambio)`.
- `BE/exportacion/ConsolidadoExcel.java`: si `Boolean.TRUE.equals(consolidado.mostrarTotalUsd())`, insertar `Total U$` entre Total C$ y Total KG. Si además existe el bloque de saldo o abonos, agregar `Total actualizado U$` después de `Total actualizado C$`, con la fórmula `ROUND(I{filaActualizado}/{tipoCambio},2)`.

### Frontend

- `FE/types/index.ts`: agregar `mostrarTotalUsd?: boolean | null`, `tipoCambio?: number | null`, `totalUsd?: number | null` a `Documento`; lo mismo más `totalActualizadoUsd?: number | null` a `Consolidado`; y `motivo: string` a `RevisionDocumento`.
- `FE/services/documentoService.ts` y `FE/services/consolidadoService.ts`: agregar `mostrarTotalUsd?: boolean` a los payloads.
- `FE/lib/format.ts`:

```ts
export function formatUsd(monto: number): string {
  return `U$${monto.toLocaleString('es-NI', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
```

- `FE/pages/DocumentoEditorPage.tsx`:
  - Agregar `mostrarTotalUsd: boolean` a `Formulario`, con `false` en `formularioVacio`, `Boolean(d.mostrarTotalUsd)` en `formularioDe`, e incluirlo en `payloadDe`.
  - Agregar el estado `config` con `configuracionService.obtener()` al montar.
  - Agregar un checkbox siempre visible, debajo de los de entrega, con el texto `Mostrar total en dólares`.
  - Si está activo, mostrar en el bloque de totales la fila `Total U$` entre Total y Total KG. Definir `const tipoCambio = documento?.tipoCambio ?? config?.tipoCambio`. El valor es `r2(totales.total / tipoCambio)`. Exportar `r2` desde `lineaForm.ts` y usarlo aquí. Si `tipoCambio` no existe, mostrar `—`.
- `FE/components/export/DocumentoPlantilla.tsx`: si `documento.mostrarTotalUsd`, agregar la fila `Total U$` con `formatUsd(documento.totalUsd ?? 0)` entre Total C$ y Total KG.
- `FE/pages/ConsolidadoEditorPage.tsx`:
  - Agregar el estado `mostrarTotalUsd` (falso por defecto), cargarlo en `cargarConsolidado`, reiniciarlo al crear uno nuevo y enviarlo en el payload.
  - Agregar en la sección 4, antes del nombre, un checkbox `Mostrar total en dólares` que también marca `cambiosSinGuardar`.
  - Si está activo, el cuadro de totales muestra `Total U$:` y, dentro del bloque de saldo, `Total actualizado U$:`.
- `FE/components/export/ConsolidadoPlantilla.tsx`: si `consolidado.mostrarTotalUsd`, mostrar `Total U$` entre Total C$ y Total KG, y `Total actualizado U$` después de `Total actualizado C$`.

### Frontend: pantalla de historial

Reescribir `FE/components/documentos/RevisionesDialog.tsx` con estas reglas. No debe tener botones de editar, restaurar ni borrar.

- Nueva prop `documentoActual: Documento`. Título: `Historial de cambios`. Clase del diálogo: `sm:max-w-5xl max-h-[85vh] overflow-y-auto`.
- La API devuelve las versiones de la más reciente a la más antigua. Para comparar la posición `i`: `siguiente = i === 0 ? documentoActual : revisiones[i - 1].documento`.
- Encabezado de cada versión: `Versión {numero}` y `Así estaba antes de: {motivo} · {fecha y hora}{ por usuario}`.
- Datos del documento, en este orden: Folio, Estado, Cliente, Proyecto, Fecha, Notas, Mostrar entrega al exportar (Sí/No), Mostrar total en dólares (Sí/No), Tipo de cambio, Total C$, Total U$, Total KG.
- Columnas de líneas: Cantidad, Descripción, Peso unit., Peso total, Precio unit., Total, Método, Fecha de entrega, Lugar de entrega, Costo unit. con IVA.
- Etiquetas de método: `ULTIMO_PRECIO` = Último precio, `FORMULA_LISTA` = Fórmula de lista, `POR_PESO` = Por peso, `POR_METRO` = Por metro, `MANUAL` = Manual.
- Un dato o celda con valor distinto en `siguiente` lleva `bg-amber-100`. Las líneas se emparejan por `id`.
- Una línea que no existe en `siguiente` lleva `bg-red-50` y la nota `Se quitó en este cambio`.
- Si `siguiente` tiene líneas que esta versión no tiene, debajo de la tabla se muestra `Se agregó en este cambio: {descripciones separadas por coma}`.
- Sin versiones: `Este documento no se ha editado desde que se creó.`
- Pasar `documentoActual={documento}` desde `DocumentoEditorPage`.

### Tests y verificación

- `CalculoDocumentoTest`: `enDolares(36624.30, 36.6243)` da `1000.00`.
- `DocumentoServiceTest`: el documento guarda el TC de Configuración; cambiar Configuración después no cambia su `totalUsd`; `convertirAFactura` copia flag y TC.
- `DocumentoExcelTest` y `ConsolidadoExcelTest`: con el flag activo aparece la fila `Total U$` con el valor correcto; con el flag apagado la estructura sigue igual.
- `ConsolidadoServiceTest`: `actualizar` conserva el TC original aunque cambie Configuración.
- **Verificación manual:** activar la opción en una factura y en un consolidado, y comprobar los totales en pantalla, PNG, PDF y Excel. Editar una factura dos veces y revisar los resaltados del historial.

---

## C3 · Histórico de precios, conversión y anulación

Decisiones que implementa:

- `cotizaciones1` se importa solo como **histórico de precios**, no como facturas.
- Los registros usan el tipo `HISTORICO`, el estado `HISTORICA` y la serie `HIS-0001`. La primera factura real es `FAC-0001`.
- No aparecen en Facturas, Cotizaciones ni en la lista de documentos del cliente o proyecto.
- No cuentan en saldo, consolidados, ganancias ni totales por proyecto.
- Sí alimentan el precio sugerido y la pantalla Precios.
- El importador crea los clientes que falten, pero no crea proyectos.
- Un registro se convierte en factura activa **de uno en uno y completo**, nunca por líneas ni por lote. Antes de convertirlo es de solo lectura.
- Al anular una factura, su cotización vuelve a pendiente. Su precio se sigue sugiriendo, porque las cotizaciones pendientes cuentan como referencia.
- Una factura anulada se vuelve editable solo después de presionar "Reactivar".

### Migración `V10__historico_de_precios.sql`

Producción está vacía, así que no se migran datos.

```sql
-- Registros del histórico de precios: tipo, estado y serie propios.
alter table documentos drop constraint documentos_tipo_check;
alter table documentos add constraint documentos_tipo_check
    check (tipo in ('COTIZACION', 'FACTURA', 'HISTORICO'));

alter table documentos drop constraint documentos_estado_check;
alter table documentos add constraint documentos_estado_check
    check (estado in ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'ACTIVA', 'ANULADA', 'HISTORICA'));

create sequence seq_folio_his;
```

### Backend

- `BE/enums/TipoDocumento.java`: agregar `HISTORICO`.
- `BE/enums/EstadoDocumento.java`: agregar `HISTORICA` y documentarlo en el comentario de la clase.
- `BE/repository/DocumentoRepository.java`:
  - Agregar `@Query(value = "select nextval('seq_folio_his')", nativeQuery = true) long siguienteFolioHistorico();`
  - En `filtrar`, cambiar la condición de tipo por `"((:tipo IS NULL AND d.tipo <> com.example.billme.enums.TipoDocumento.HISTORICO) OR d.tipo = :tipo) AND " +`
  - En `facturasDeCuenta`, quitar `AND d.importKey IS NULL`.
  - En `sumaFacturasPorCliente`, quitar `AND d.import_key IS NULL`.
  - Sustituir `findFirstByCotizacionOrigenId` por `Optional<Documento> findFirstByCotizacionOrigenIdAndEstadoNot(UUID cotizacionId, EstadoDocumento estado);` y usarlo en `obtener` con `EstadoDocumento.ANULADA`.
- `BE/service/DocumentoService.java`:

```java
String siguienteFolio(TipoDocumento tipo) {
    return switch (tipo) {
        case COTIZACION -> "COT-%04d".formatted(documentoRepository.siguienteFolioCotizacion());
        case FACTURA -> "FAC-%04d".formatted(documentoRepository.siguienteFolioFactura());
        case HISTORICO -> "HIS-%04d".formatted(documentoRepository.siguienteFolioHistorico());
    };
}
```

- `actualizar`: los rechazos quedan así, en este orden.

| Estado | Código | Mensaje exacto |
|---|---|---|
| `ANULADA` | 409 | `Reactiva la factura para poder editarla` |
| `ACEPTADA` | 409 | El actual |
| `HISTORICA` | 409 | `Este registro del histórico no se edita: conviértelo primero en factura activa` |

- `anular`: justo después de cambiar el estado de la factura y registrar su versión:

```java
Documento origen = documento.getCotizacionOrigen();
if (origen != null && origen.getEstado() == EstadoDocumento.ACEPTADA) {
    String copiaOrigen = copia(origen);
    origen.setEstado(EstadoDocumento.PENDIENTE);
    registrarRevision(origen, copiaOrigen,
            "Factura " + documento.getFolio() + " anulada: la cotización vuelve a pendiente");
}
```

- Métodos nuevos:

```java
/** Un registro del histórico pasa a ser una factura activa con folio propio. */
@Transactional
public DocumentoResponse convertirHistorico(UUID id) {
    Documento documento = buscar(id);
    if (documento.getTipo() != TipoDocumento.HISTORICO || documento.getEstado() != EstadoDocumento.HISTORICA) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se convierte un registro del histórico");
    }
    String copiaAnterior = copia(documento);
    documento.setTipo(TipoDocumento.FACTURA);
    documento.setEstado(EstadoDocumento.ACTIVA);
    documento.setFolio(siguienteFolio(TipoDocumento.FACTURA));
    registrarRevision(documento, copiaAnterior, "Convertido en factura activa");
    return toResponse(documento, null);
}

/** Una factura anulada vuelve a estar activa, salvo que su cotización ya tenga otra factura. */
@Transactional
public DocumentoResponse reactivar(UUID id) {
    Documento documento = buscar(id);
    if (documento.getTipo() != TipoDocumento.FACTURA || documento.getEstado() != EstadoDocumento.ANULADA) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se reactiva una factura anulada");
    }
    Documento origen = documento.getCotizacionOrigen();
    if (origen != null) {
        if (documentoRepository.findFirstByCotizacionOrigenIdAndEstadoNot(origen.getId(), EstadoDocumento.ANULADA).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cotización ya tiene otra factura: no se puede reactivar esta");
        }
        String copiaOrigen = copia(origen);
        origen.setEstado(EstadoDocumento.ACEPTADA);
        registrarRevision(origen, copiaOrigen, "Factura " + documento.getFolio() + " reactivada");
    }
    String copiaAnterior = copia(documento);
    documento.setEstado(EstadoDocumento.ACTIVA);
    registrarRevision(documento, copiaAnterior, "Factura reactivada");
    return toResponse(documento, null);
}
```

- `BE/controller/DocumentoController.java`: agregar `POST /{id}/convertir-historico` y `POST /{id}/reactivar`. Ambos responden 200 con `DocumentoResponse`.
- `BE/dto/ImportacionCotizacionesResponse.java`: renombrar `facturasNuevas` a `registrosNuevos` y `facturasConLineasNuevas` a `registrosConLineasNuevas`.
- `BE/service/ImportacionCotizacionesService.java`:
  - Quitar `ProyectoRepository`, `EstadoProyecto`, el mapa `proyectosCache` y el bloque que crea proyectos. `proyecto` queda `null`.
  - Si `grupo.proyectoRaw()` tiene texto, agregar a `notas`: ` Proyecto en el Excel: {nombre}.`
  - El `builder` usa `.tipo(TipoDocumento.HISTORICO)`, `.estado(EstadoDocumento.HISTORICA)` y `.folio(documentoService.siguienteFolio(TipoDocumento.HISTORICO))`.
  - Un registro ya convertido no recibe líneas nuevas, ni siquiera si se editó. En `vistaPrevia` y `aplicar`, después de calcular `clavesExistentes`:

```java
Set<String> gruposConvertidos = new HashSet<>();
for (String clave : grupos.keySet()) {
    documentoRepository.findByImportKey(clave)
            .filter(d -> d.getTipo() != TipoDocumento.HISTORICO)
            .ifPresent(d -> gruposConvertidos.add(clave));
}
```

  Sustituir cada `clavesExistentes.contains(fp.claveLinea())` por `clavesExistentes.contains(fp.claveLinea()) || gruposConvertidos.contains(fp.claveDoc())`.

### Frontend

- `FE/types/index.ts`: `TipoDocumento` agrega `'HISTORICO'`; `EstadoDocumento` agrega `'HISTORICA'`; renombrar los dos campos de `ImportacionCotizaciones`.
- `FE/pages/DocumentosPage.tsx`: la prop pasa a `tipo: Exclude<TipoDocumento, 'HISTORICO'>`, igual que las claves de `ESTADOS`.
- `FE/components/documentos/EstadoDocumentoBadge.tsx`: `case 'HISTORICA': return { texto: 'Histórico', ...GRIS }`.
- `FE/services/documentoService.ts`: agregar `convertirHistorico: (id) => api.post<Documento>('/documentos/' + id + '/convertir-historico', {})` y `reactivar` con la ruta correspondiente.
- `FE/pages/DocumentoEditorPage.tsx`:
  - `TITULOS` agrega `HISTORICO: { nuevo: 'Histórico', lista: 'Precios', ruta: '/precios' }`.
  - `soloLectura` también es verdadero con `estado === 'HISTORICA'`.
  - El botón Exportar no aparece con `HISTORICA`. "Ver cambios" sigue visible.
  - Con `HISTORICA`, mostrar el aviso `Este registro viene del histórico de precios. No cuenta en saldos ni reportes. Para usarlo como venta, conviértelo en factura activa.` y el botón verde `Convertir en factura activa`.
  - Confirmación de ese botón: `¿Convertir {folio} en factura activa? Recibirá un folio de factura nuevo y sumará {total} al saldo de {cliente}.` Si la fecha es anterior a `clienteActual.fechaInicioCuenta`, agregar: ` Su fecha es anterior al inicio de la cuenta: no cambiará el saldo.` Al aceptar: `ejecutar(() => documentoService.convertirHistorico(documento.id), 'Factura activa creada.')`.
  - Con `ANULADA`, el aviso pasa a `Esta factura está anulada: no cuenta en ingresos. Para editarla, reactívala.` y aparece el botón `Reactivar`, con la confirmación `¿Reactivar {folio}? Volverá a contar en ingresos y en el saldo.`
  - Confirmación de `anular`: `¿Anular {folio}? Dejará de contar en ingresos.` Si existe `cotizacionOrigenFolio`, agregar ` La cotización {folio} volverá a pendiente.`
- `FE/components/importacion/ImportarCotizacionesPanel.tsx`:
  - Título: `Histórico de precios`.
  - Texto: `Sube el Excel con el historial de precios. Primero se muestra qué se va a importar; nada se guarda hasta que presiones "Importar". Puedes subir el mismo archivo otra vez sin que se duplique nada. Estos registros no aparecen en Facturas ni cuentan en saldos o reportes: solo sirven de referencia de precios. Para usar uno como venta, ábrelo desde Precios y conviértelo en factura activa.`
  - Cifra: `Registros nuevos` en vez de `Facturas nuevas`.
  - Aviso de líneas nuevas: `1 registro existente recibirá líneas nuevas.` y `{n} registros existentes recibirán líneas nuevas.`
  - Éxito: `Histórico importado.`

### Tests y verificación

- `IntegrationTest.vaciarDatos`: agregar `jdbc.execute("alter sequence seq_folio_his restart");`.
- En `CuentaClienteServiceTest` y `ConsolidadoServiceTest`, donde hoy se llama `setImportKey`, usar `setTipo(TipoDocumento.HISTORICO)` y `setEstado(EstadoDocumento.HISTORICA)`.
- `ImportacionCotizacionesTest`:
  - Lo importado queda con tipo `HISTORICO`, estado `HISTORICA`, folio `HIS-0001` y sin proyecto.
  - La siguiente factura normal es `FAC-0001`.
  - El precio sugerido usa el histórico.
  - Reimportar no duplica.
  - Tras convertir un registro y agregar una fila al Excel, la factura no cambia y esa fila cuenta como ya importada.
- `DocumentoServiceTest`:
  - Un histórico no aparece con `tipo` null en `filtrar`.
  - `convertirHistorico` asigna folio FAC, deja versión y falla si se repite.
  - `actualizar` sobre un histórico da 409.
  - Anular devuelve la cotización a pendiente y permite reconvertirla.
  - `reactivar` funciona sin segunda factura y da 409 con segunda factura.
- `CuentaClienteServiceTest`, `ReporteServiceTest`, `ConsolidadoServiceTest` y `ProyectoServiceTest`: antes de convertir, el histórico no suma; después de convertir, sí.
- **Verificación manual:** importar un Excel de prueba. Facturas sigue vacía y Precios lo muestra. Abrir un registro desde Precios, convertirlo y comprobar el folio FAC, el saldo y el historial.

---

## C4 · Tarifa por cliente y precio por peso

Decisiones que implementa:

- Cada cliente tiene una tarifa por kg antes de IVA, en USD o C$. Si está vacía se usa la de Configuración, que sigue en USD.
- Se elimina la prioridad "última tarifa usada".
- Fórmulas: USD `PU = r2(r2(peso) × tarifa × TC × (1 + IVA))`; C$ `PU = r2(r2(peso) × tarifa × (1 + IVA))`.
- En cada línea se puede recalcular con otra tarifa o moneda. Eso se guarda en la línea y nunca cambia al cliente.
- El precio siempre sigue editable. Escribirlo a mano pasa la línea a `MANUAL`.
- El peso cambia el precio **solo** en líneas `POR_PESO`, tanto en el editor como en "Registrar compra". Los demás métodos no cambian.
- "Por peso" está disponible en cualquier línea con peso mayor a 0, sin cambiar el precio sugerido inicial.

Producción está vacía: no hay compatibilidad con el formato anterior de `parametros`.

### Migración `V11__tarifa_por_cliente.sql`

```sql
-- La tarifa por kg de cada cliente puede estar en dólares o en córdobas.
alter table clientes rename column tarifa_kg_usd to tarifa_kg;
alter table clientes add column tarifa_moneda varchar(3) not null default 'USD'
    check (tarifa_moneda in ('USD', 'NIO'));
```

### Backend

- Crear `BE/enums/MonedaTarifa.java` con los valores `USD` y `NIO`.
- `BE/model/Cliente.java`: `tarifaKgUsd` pasa a `tarifaKg` (columna `tarifa_kg`). Agregar `MonedaTarifa tarifaMoneda` con `@Enumerated(EnumType.STRING)`, columna `tarifa_moneda` y `@Builder.Default` en `USD`.
- `BE/dto/ClienteRequest.java`: `tarifaKgUsd` pasa a `@Positive @DecimalMax("9999.9999") BigDecimal tarifaKg`, seguido de `MonedaTarifa tarifaMoneda`. Los dos constructores cortos pasan `null` en `tarifaMoneda`. Agregar un tercer constructor con los 7 argumentos actuales, también con `null` en `tarifaMoneda`, para los tests existentes.
- `BE/dto/ClienteResponse.java`: `tarifaKg` y `tarifaMoneda` (`String`).
- `BE/service/ClienteService.java`: guardar la moneda con `request.tarifaMoneda() != null ? request.tarifaMoneda() : MonedaTarifa.USD`.
- Crear `BE/service/PrecioPorPesoService.java`:

```java
package com.example.billme.service;

import com.example.billme.calculo.CalculadoraPrecio;
import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.enums.MonedaTarifa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

/** Precio con IVA calculado desde el peso, una tarifa sin IVA y su moneda. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecioPorPesoService {

    private final ConfiguracionService configuracionService;
    private final ObjectMapper objectMapper;

    public BigDecimal calcular(BigDecimal peso, BigDecimal tarifa, MonedaTarifa moneda, BigDecimal tipoCambio) {
        BigDecimal cambio = moneda == MonedaTarifa.USD ? tipoCambio : BigDecimal.ONE;
        return CalculadoraPrecio.porPeso(CalculoDocumento.r2(peso), tarifa, cambio, configuracionService.factorIva());
    }

    /** Vacío si los parámetros de la línea o el peso no permiten calcular. */
    public Optional<BigDecimal> recalcular(String parametros, BigDecimal peso, BigDecimal tipoCambio) {
        if (parametros == null || parametros.isBlank() || peso == null || peso.signum() <= 0) {
            return Optional.empty();
        }
        try {
            JsonNode nodo = objectMapper.readTree(parametros);
            BigDecimal tarifa = new BigDecimal(nodo.get("tarifa").asText());
            MonedaTarifa moneda = MonedaTarifa.valueOf(nodo.get("moneda").asText());
            return tarifa.signum() > 0
                    ? Optional.of(calcular(peso, tarifa, moneda, tipoCambio))
                    : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
```

- `BE/dto/PrecioPorPesoRequest.java`:

```java
public record PrecioPorPesoRequest(
        @NotNull @Positive BigDecimal pesoUnitKg,
        @NotNull @Positive BigDecimal tarifa,
        @NotNull MonedaTarifa moneda,
        @Positive BigDecimal tipoCambio
) {}
```

- `BE/service/CalculoService.java`, `calcularPrecioPorPeso`: usar `PrecioPorPesoService.calcular`. El TC es `request.tipoCambio()` y, si es null, el de Configuración. Conservar el `catch (IllegalArgumentException)`.
- `BE/dto/PrecioSugeridoResponse.java`: `tarifaKgUsd` pasa a `BigDecimal tarifaKg`, seguido de `String monedaTarifa` y `String origenTarifa`.
- `BE/service/PrecioSugeridoService.java`:
  - Quitar el bloque condicionado por forma, el método `extraerTarifaUsd`, el campo `objectMapper` y sus imports.
  - La tarifa se calcula para todos los productos:

```java
boolean propia = cliente.getTarifaKg() != null;
BigDecimal tarifaKg = propia ? cliente.getTarifaKg() : configuracionService.tarifaKgUsd();
String monedaTarifa = propia ? cliente.getTarifaMoneda().name() : MonedaTarifa.USD.name();
String origenTarifa = propia ? "CLIENTE" : "CONFIGURACION";
```

- `BE/repository/DocumentoLineaRepository.java`: borrar `ultimasPorPeso`.
- `BE/service/DocumentoService.java`, `aplicarLineas`: inyectar `PrecioPorPesoService` y, antes de llamar a `CalculoDocumento.linea`:

```java
BigDecimal pesoPedido = pedida.pesoUnitKg() != null ? pedida.pesoUnitKg() : producto.getPeso();
BigDecimal precioPedido = pedida.precioUnit();
if (pedida.metodoPrecio() == MetodoPrecio.POR_PESO) {
    boolean esNueva = linea.getId() == null;
    boolean cambioPeso = linea.getPesoUnitKg() == null || pesoPedido == null
            || CalculoDocumento.r2(pesoPedido).compareTo(linea.getPesoUnitKg()) != 0;
    boolean cambioParametros = !Objects.equals(pedida.parametros(), linea.getParametros());
    if (esNueva || cambioPeso || cambioParametros) {
        precioPedido = precioPorPesoService
                .recalcular(pedida.parametros(), pesoPedido, documento.getTipoCambio())
                .orElse(pedida.precioUnit());
    }
}
LineaCalculada calculada = CalculoDocumento.linea(cantidad, precioPedido, pesoPedido);
```

- `BE/service/CompraService.java`: inyectar `PrecioPorPesoService`. Dentro de `if (reqLine.pesoUnitKg() != null)`:

```java
BigDecimal precio = linea.getPrecioUnit();
if (linea.getMetodoPrecio() == MetodoPrecio.POR_PESO) {
    precio = precioPorPesoService
            .recalcular(linea.getParametros(), reqLine.pesoUnitKg(), documento.getTipoCambio())
            .orElse(precio);
}
CalculoDocumento.LineaCalculada calc = CalculoDocumento.linea(linea.getCantidad(), precio, reqLine.pesoUnitKg());
linea.setPrecioUnit(calc.precioUnit());
linea.setTotal(calc.total());
linea.setPesoUnitKg(calc.pesoUnitKg());
linea.setPesoTotalKg(calc.pesoTotalKg());
```

  Después del ciclo, agregar junto a `totalKg`: `documento.setTotal(suma de los total de todas las líneas)`, con `CalculoDocumento.r2`.

### Formato de `parametros` en líneas por peso

Siempre es un JSON con `tarifa` (número) y `moneda` (`"USD"` o `"NIO"`). Si viene de la calculadora, también lleva `forma`, `ancho`, `largo` y `espesor` en el formato actual.

### Frontend

- `FE/types/index.ts`: en `Cliente`, `tarifaKg?: number | null` y `tarifaMoneda?: MonedaTarifa`; en `PrecioSugerido`, `tarifaKg`, `monedaTarifa?: MonedaTarifa | null` y `origenTarifa?: 'CLIENTE' | 'CONFIGURACION' | null`. Definir `export type MonedaTarifa = 'USD' | 'NIO'`. `ConfiguracionSistema` no cambia.
- `FE/services/clienteService.ts`: mismos nombres en `ClientePayload`.
- `FE/services/calculoService.ts`: `precioPorPeso: (data: { pesoUnitKg: number; tarifa: number; moneda: MonedaTarifa; tipoCambio?: number }) => …`.
- `FE/components/clientes/ClienteModal.tsx`:
  - Etiqueta: `Tarifa por kg para placas y platinas (sin IVA)`.
  - Junto al campo, un selector con las opciones `USD` y `C$` (valor `NIO`). Por defecto USD.
  - Ayuda: `Si lo dejas vacío se usa la de Configuración. En USD se convierte con el tipo de cambio configurado. Al facturar puedes usar otra tarifa sin cambiar esta.`
  - Enviar `tarifaKg` y `tarifaMoneda`.
- `FE/components/documentos/lineaForm.ts`:

```ts
export function recalcularPrecioPorPeso(
  metodoPrecio: MetodoPrecio, parametros: string | null | undefined,
  pesoTexto: string, tipoCambio: number, ivaPct: number,
): string | null {
  if (metodoPrecio !== 'POR_PESO' || !parametros) return null
  try {
    const p = JSON.parse(parametros)
    const peso = Number(pesoTexto)
    const tarifa = Number(p.tarifa)
    if (!(peso > 0) || !(tarifa > 0)) return null
    const cambio = p.moneda === 'NIO' ? 1 : tipoCambio
    return String(r2(r2(peso) * tarifa * cambio * (1 + ivaPct / 100)))
  } catch {
    return null
  }
}
```

  En `formatearTagPrecio`, para `POR_PESO`: `` `${Number(p.tarifa).toFixed(2)} ${p.moneda === 'NIO' ? 'C$' : 'USD'} por kg` ``.

- `FE/components/documentos/LineasTabla.tsx`: nueva prop `onCambiarPeso: (clave: string, peso: string) => void`. Los dos campos de peso, de escritorio y celular, la usan en vez de `onCambiar`.
- `FE/pages/DocumentoEditorPage.tsx`:

```ts
// Reutiliza la constante tipoCambio creada en C2
const cambiarPeso = (clave: string, pesoUnitKg: string) => {
  const linea = form.lineas.find(l => l.clave === clave)
  const precio = linea && tipoCambio && config
    ? recalcularPrecioPorPeso(linea.metodoPrecio, linea.parametros, pesoUnitKg, tipoCambio, config.ivaPct)
    : null
  cambiarLinea(clave, precio ? { pesoUnitKg, precioUnit: precio } : { pesoUnitKg })
}
```

  - En `onAplicar` de `CalculadoraPesoDialog`: si `datos.precioUnit` no viene, llamar a `cambiarPeso(clave, datos.pesoUnitKg)` antes de aplicar los demás cambios.
  - Pasar `tipoCambio` a `PrecioLineaDialog` y `CalculadoraPesoDialog`; pasar `tipoCambio` e `ivaPct` a `RegistrarCompraDialog`.
- `FE/components/documentos/CalculadoraPesoDialog.tsx`:
  - Cambiar `tarifaUsd` por los estados `tarifa` y `moneda` (`'USD'` por defecto).
  - Precargar con `p.tarifa` y `p.moneda` de la línea. Si no existen, con `res.tarifaKg` y `res.monedaTarifa`.
  - Debajo del campo, mostrar el selector USD/C$ y el texto `Solo para esta línea: la tarifa del cliente no cambia.`
  - Etiqueta del campo: `Tarifa por kg (sin IVA)`.
  - `precioPorPeso` envía `tipoCambio`. Los `parametros` guardan `tarifa` y `moneda`.
- `FE/components/documentos/PrecioLineaDialog.tsx`:
  - Sustituir la tarjeta "Por peso" por esta regla:
    - Si `Number(linea.pesoUnitKg) > 0`, mostrar la opción de radio `POR_PESO`, con campo de tarifa y selector USD/C$. Cada campo hace `onFocus={() => setMetodo('POR_PESO')}`.
    - Precargar con los `parametros` de la línea si es `POR_PESO`; si no, con la tarifa de `sugerido`.
    - El precio se calcula con `calculoService.precioPorPeso`, con espera de 300 ms.
    - El mismo texto: `Solo para esta línea: la tarifa del cliente no cambia.`
    - Si la forma es `PLACA` o `PLATINA`, conservar además el botón `Abrir calculadora`, tenga o no peso.
  - Selección inicial: si `linea.metodoPrecio === 'POR_PESO'` y tiene peso, `POR_PESO`. Si no, el orden actual.
  - `handleAplicar`: `onAplicar(String(puPeso), 'POR_PESO', JSON.stringify({ ...parametrosDeLaLinea, tarifa, moneda }))`. `parametrosDeLaLinea` es el JSON actual si la línea ya es `POR_PESO`; si no, `{}`.
- `FE/components/documentos/RegistrarCompraDialog.tsx`:
  - Nuevas props `tipoCambio: number` e `ivaPct: number`.
  - Agregar `metodoPrecio`, `parametros` y `precioUnit` a `LineaCompraItem`, copiados de cada línea.
  - Si una línea incluida es `POR_PESO` y tiene "Peso unit. real", calcular el valor nuevo con `recalcularPrecioPorPeso` y mostrar debajo del campo `El precio unitario cambiará de {actual} a {nuevo}.`, con `formatCurrency`. Si devuelve null, no mostrar nada.

### Tests y verificación

- `PrecioPorPesoService`, test nuevo con TC 36.6243 e IVA 15:
  - 2.58 kg a 1.80 USD da `195.60`.
  - 2.58 kg a C$70 da `207.69`.
  - Parámetros vacíos o peso 0 dan `Optional.empty()`.
- `PrecioSugeridoServiceTest`: sustituir `tresPrioridadesDeTarifa` por `dosPrioridadesDeTarifa`. Una línea `POR_PESO` previa ya no influye; primero va la ficha del cliente y luego Configuración. Actualizar `crearDocPorPeso` al nuevo formato de `parametros`.
- `CalculoControllerTest` y `ClienteControllerTest`: nuevos nombres de campos y un caso en C$.
- `DocumentoServiceTest`:
  - Línea `POR_PESO` con otra tarifa en `parametros`: la tarifa del cliente no cambia.
  - Cambiar el peso recalcula.
  - La misma edición en `MANUAL` no recalcula.
  - Guardar otros datos después de cambiar Configuración no altera el precio de la línea.
- `CompraServiceTest`: factura `POR_PESO` de 10 × 2.58 kg a 1.80 USD, total `1956.00`. Con peso real 3 queda PU `227.44`, total `2274.40` y una versión nueva. Una línea `MANUAL` conserva el precio.
- **Verificación manual:**
  - Crear un cliente con tarifa en C$, abrir la calculadora, usar otra tarifa y comprobar que la ficha no cambia.
  - Editar el peso en una línea por peso y en una manual.
  - Usar "Por peso" en un producto de catálogo con peso.

---

## C5 · Legibilidad, barra fija, consolidados y PDF

### 1. Letra y controles de tamaño real

- `FE/index.css`: borrar `font-size: 14px;` de `html, body`.
- `FE/components/ui/input.tsx`: `h-9` pasa a `h-11`.
- Constantes `SELECT` de `FE/pages/DocumentoEditorPage.tsx`, `FE/pages/ReportesPage.tsx` y `FE/components/documentos/RegistrarCompraDialog.tsx`: `h-9` pasa a `h-11` y `text-sm` a `text-base`.
- Los demás `<select>` llevan `h-11 text-base`: `ConciliacionTabla`, `ImportarCotizacionesPanel`, `ProyectoModal`, `CalculadoraPesoDialog`, `AbonoDialog`, `ConsolidadoEditorPage`, `DocumentosPage` y `PreciosPage`.
- `FE/components/documentos/LineasTabla.tsx`, vista de celular: `h-10 text-sm` pasa a `h-11 text-base` y `w-10 h-10` a `w-11 h-11`. Vista de escritorio: `CELDA` no cambia; los botones `w-7 h-7` y `w-8 h-8` pasan a `w-9 h-9`.
- En todo `FE/components` y `FE/pages`, salvo lo anterior, quitar `h-7`, `h-8`, `h-9`, `h-10` y `size="sm"` de `Button` e `Input`, para que queden en 44 px. Para encontrarlos:

```bash
grep -rnE 'h-(7|8|9|10)\b|size="sm"' frontend/src/components frontend/src/pages
```

  No tocar `FE/components/ui/` (salvo `input.tsx`, ya indicado) ni los cuadros decorativos de logo o ícono (`w-7 h-7 rounded bg-…`, `w-10 h-10 rounded bg-…`).

- El campo de descuento de `PrecioLineaDialog.tsx` pasa de `w-16` a `w-24`.
- Botones de ícono de `FE/pages/ConsolidadosPage.tsx` y `FE/pages/ClienteDetallePage.tsx` (`p-1`) y de cierre de modales (`w-7 h-7`): pasan a `w-11 h-11 flex items-center justify-center`.
- Cualquier otro `button` que señale el script de abajo, como "volver" en los editores: agregar `min-h-11`.

**Verificación manual:** a 375×812, en Facturas, Clientes, Proyectos, Consolidados, Reportes, Importar, Precios, un documento abierto y un consolidado abierto, ejecutar esto en la consola. Debe devolver una lista vacía.

```js
[...document.querySelectorAll('input:not([type=checkbox]):not([type=radio]),select,button')]
  .filter(e => e.offsetParent)
  .filter(e => e.offsetHeight < 44 || (e.tagName !== 'BUTTON' && parseFloat(getComputedStyle(e).fontSize) < 16))
  .map(e => e.outerHTML.slice(0, 80))
```

Revisar además a 1366×768 que ninguna pantalla se desborde.

### 2. Barra de Guardar fija

- `FE/components/layout/AppLayout.tsx`:
  - Contenedor raíz: `flex flex-col md:flex-row h-dvh bg-[#F9FAFB]`.
  - Envoltura del menú: `hidden md:flex shrink-0 h-full`.
  - `main`: `flex-1 min-h-0 overflow-auto` (sin `flex flex-col`).
  - Volver arriba al navegar:

```tsx
const { pathname } = useLocation()
const contenido = useRef<HTMLElement>(null)
useEffect(() => { contenido.current?.scrollTo(0, 0) }, [pathname])
```

- `FE/components/layout/AppSidebar.tsx`: `aside` pasa de `min-h-screen` a `h-full overflow-y-auto`.

**Verificación manual:** en un documento de 45 líneas a 1366×650, la barra de Guardar y "Cerrar sesión" están visibles sin bajar.

### 3. Editor de consolidados

- `BE/dto/ConsolidadoResponse.java`: `Grupo` agrega `boolean editado` y conserva el constructor de 5 argumentos, que pasa `false`.
- `BE/service/ConsolidadoService.java`, `armar`: `editado` es verdadero si `nombresGuardados.containsKey(fecha)`.
- `FE/types/index.ts`: `ConsolidadoGrupo` agrega `editado: boolean`.
- `FE/pages/ConsolidadoEditorPage.tsx`:
  - `cargarConsolidado` y el resultado de `actualizar` llenan `nombresGrupos` solo con los grupos `editado`.
  - Al cambiar cliente o proyecto: `setConsolidado(null)`, `setNombreConsolidado('')` y `setNombresGrupos({})`.
  - Al cambiar una fecha sin estar guardado: `setConsolidado(null)`.
  - El botón Exportar queda `disabled={cambiosSinGuardar}`, dentro de un `<span title={cambiosSinGuardar ? 'Guarda primero los cambios' : undefined}>`.
- Crear `FE/hooks/useAvisoSalir.ts` moviendo tal cual el `useEffect` de `DocumentoEditorPage` que usa `alClic` y `alCerrar`:

```ts
export const SALIR_SIN_GUARDAR = 'Hay cambios sin guardar. ¿Salir sin guardar?'
export function useAvisoSalir(cambiosSinGuardar: boolean) { /* efecto actual */ }
```

  Usarlo en `DocumentoEditorPage` y `ConsolidadoEditorPage`. `volver` usa el mismo texto en ambos.

### 4. PDF sin filas cortadas

En `FE/lib/exportar.ts`, sustituir `descargarPdf`. El nombre del archivo, el formato carta y los márgenes de 10 mm no cambian.

```ts
export async function descargarPdf(elemento: HTMLElement, nombre: string, horizontal: boolean): Promise<void> {
  const dataUrl = await toPng(elemento, { pixelRatio: 2, backgroundColor: '#FFFFFF' })
  const img = new Image()
  img.src = dataUrl
  await img.decode()

  const pdf = new jsPDF({ orientation: horizontal ? 'l' : 'p', unit: 'mm', format: 'letter' })
  const margen = 10
  const anchoUtil = pdf.internal.pageSize.getWidth() - margen * 2
  const altoUtil = pdf.internal.pageSize.getHeight() - margen * 2

  const anchoCss = elemento.offsetWidth
  const escala = img.naturalWidth / anchoCss
  const altoTotalCss = img.naturalHeight / escala
  const altoPaginaCss = (altoUtil / anchoUtil) * anchoCss
  const origen = elemento.getBoundingClientRect().top
  const finesDeFila = [...elemento.querySelectorAll('tr')]
    .map(fila => fila.getBoundingClientRect().bottom - origen)

  let inicio = 0
  let primera = true
  while (inicio < altoTotalCss - 1) {
    const limite = inicio + altoPaginaCss
    let fin = altoTotalCss
    if (limite < altoTotalCss) {
      const candidatos = finesDeFila.filter(y => y > inicio && y <= limite)
      fin = candidatos.length > 0 ? Math.max(...candidatos) : limite
    }

    const recorte = document.createElement('canvas')
    recorte.width = img.naturalWidth
    recorte.height = Math.ceil((fin - inicio) * escala)
    const contexto = recorte.getContext('2d')
    if (!contexto) throw new Error('No se pudo preparar el PDF')
    contexto.fillStyle = '#FFFFFF'
    contexto.fillRect(0, 0, recorte.width, recorte.height)
    contexto.drawImage(img, 0, -Math.floor(inicio * escala))

    if (!primera) pdf.addPage()
    pdf.addImage(recorte.toDataURL('image/png'), 'PNG', margen, margen, anchoUtil,
      ((fin - inicio) / anchoCss) * anchoUtil, undefined, 'FAST')
    primera = false
    inicio = fin
  }
  descargarArchivo(pdf.output('blob'), nombre)
}
```

**Verificación manual:** exportar el documento de 45 líneas, abrir el PDF y comprobar que ninguna fila queda partida.

### 5. Detalles

- `frontend/index.html`: `<html lang="es">`; agregar `crossorigin="anonymous"` al `<link>` de la hoja de estilos de Google Fonts.
- `FE/components/export/ExportarDialog.tsx`: el título pasa a `Exportar`.
- `FE/pages/ProyectosPage.tsx`: la cuadrícula de tarjetas pasa de `grid-cols-3` a `grid-cols-1 sm:grid-cols-3`.
- `FE/pages/DocumentosPage.tsx`: agregar `whitespace-nowrap` a la celda del folio.
- `BE/service/ConsolidadoService.java`, `armar`: una línea sin peso conserva `null`. Quitar los valores por defecto, usar `l.getPesoUnitKg()` y `l.getPesoTotalKg()`, y en `subtotalKg` filtrar con `.filter(Objects::nonNull)` antes de sumar.

---

## C6 · Búsqueda, orden, cambio de cliente y mensajes

### 1. Buscar sin acentos

- `BE/util/NormalizadorTexto.java`:

```java
/** Minúsculas y sin acentos; conserva guiones y números, para buscar folios y nombres. */
public static String sinAcentos(String texto) {
    if (texto == null) {
        return "";
    }
    String s = Normalizer.normalize(texto, Normalizer.Form.NFD);
    return DIACRITICOS.matcher(s).replaceAll("").toLowerCase(Locale.ROOT).trim();
}
```

- `BE/repository/DocumentoRepository.java`, `filtrar`: sustituir la condición de texto por:

```java
"(:q = '' OR cast(function('translate', lower(d.folio), 'áéíóúüñ', 'aeiouun') as string) LIKE :contiene " +
"OR cast(function('translate', lower(d.cliente.nombre), 'áéíóúüñ', 'aeiouun') as string) LIKE :contiene " +
"OR cast(function('translate', lower(p.nombre), 'áéíóúüñ', 'aeiouun') as string) LIKE :contiene) " +
```

- `BE/service/DocumentoService.java`, `filtrar`: `String qNormalizada = NormalizadorTexto.sinAcentos(q);` y `contiene = "%" + qNormalizada + "%"`.
- `BE/repository/ClienteRepository.java`, `buscar`:

```java
@Query("SELECT c FROM Cliente c WHERE " +
       "cast(function('translate', lower(c.nombre), 'áéíóúüñ', 'aeiouun') as string) LIKE CONCAT('%', :q, '%') OR " +
       "c.telefono LIKE CONCAT('%', :q, '%') ORDER BY lower(c.nombre)")
```

- `BE/service/ClienteService.java`, `buscar`: pasar `NormalizadorTexto.sinAcentos(q)`.
- `FE/lib/format.ts`:

```ts
export function sinAcentos(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}
```

- Usar `sinAcentos` en los filtros locales de `FE/pages/ClientesPage.tsx`, `FE/pages/ConsolidadosPage.tsx` y `FE/pages/ProyectosPage.tsx`, en el texto buscado y en los valores comparados.
- Tests: `perez` y `PÉREZ` encuentran a `Cliente Pérez`, en documentos y en clientes.

### 2. Listas en orden alfabético

- `ClienteRepository`: agregar `@Query("SELECT c FROM Cliente c ORDER BY lower(c.nombre)") List<Cliente> ordenados();` y usarlo en `ClienteService.listarTodos`.
- `ProyectoRepository`: cambiar a `findAllByOrderByNombreAsc()`, `findByClienteIdOrderByNombreAsc(UUID)` y `findByClienteIdAndEstadoOrderByNombreAsc(UUID, EstadoProyecto)`. Actualizar `ProyectoService`.
- Test: los clientes salen en orden sin importar el orden de creación.

### 3. Cambio de cliente con líneas escritas

En `FE/pages/DocumentoEditorPage.tsx`, sustituir el `onChange` del selector de cliente por `alCambiarCliente(e.target.value)`. Extraer de `agregarProducto` una función `precioDeSugerido(res)` que devuelva `{ precioUnit, metodoPrecio, parametros } | null`, con los mismos `parametros` que hoy.

- Sin líneas: comportamiento actual.
- Con líneas, preguntar:

```text
¿Recalcular los precios para el nuevo cliente?

Aceptar: recalcula los precios que salieron del historial, la lista o el peso.
Cancelar: conserva los precios escritos.
```

- **Cancelar:** `ULTIMO_PRECIO` y `FORMULA_LISTA` pasan a `MANUAL` con `parametros: null` y el mismo precio. Las demás no cambian.
- **Aceptar:** para cada línea, llamar a `precioService.sugerido(clienteNuevo, productoId, generico ? descripcion : undefined)`.
  - `ULTIMO_PRECIO` y `FORMULA_LISTA`: aplicar `precioDeSugerido`. Si devuelve null, conservar el precio y pasar a `MANUAL`.
  - `POR_PESO`: conservar los datos de `parametros` pero con `tarifa = res.tarifaKg` y `moneda = res.monedaTarifa`, y recalcular con `recalcularPrecioPorPeso`, usando el peso de la línea. Si devuelve null, conservar el precio.
  - `MANUAL` y `POR_METRO`: no cambian.
- En ambos casos: `proyectoId` queda vacío y `nuevoProyecto` en null.

### 4. Mensajes de validación en español

- `BE/config/GlobalExceptionHandler.java`, `validacion`: el mensaje de cada campo se arma así:

```java
private static final Map<String, String> ETIQUETAS = Map.ofEntries(
        Map.entry("nombre", "Nombre"), Map.entry("telefono", "Teléfono"),
        Map.entry("descuentoPct", "Descuento"), Map.entry("tarifaKg", "Tarifa por kg"),
        Map.entry("monto", "Monto"), Map.entry("fechaPago", "Fecha"), Map.entry("nota", "Nota"),
        Map.entry("clienteId", "Cliente"), Map.entry("fecha", "Fecha"), Map.entry("lineas", "Líneas"),
        Map.entry("cantidad", "Cantidad"), Map.entry("precioUnit", "Precio unitario"),
        Map.entry("pesoUnitKg", "Peso"), Map.entry("descripcion", "Descripción"),
        Map.entry("lugarEntrega", "Lugar de entrega"), Map.entry("codigoInterno", "Código"),
        Map.entry("nombrePrincipal", "Nombre"), Map.entry("username", "Usuario"),
        Map.entry("password", "Contraseña"), Map.entry("nueva", "Contraseña nueva"),
        Map.entry("actual", "Contraseña actual"), Map.entry("desde", "Desde"), Map.entry("hasta", "Hasta"),
        Map.entry("proveedorId", "Proveedor"), Map.entry("precioSinIva", "Precio sin IVA"));

private static String mensajeDeCampo(FieldError e) {
    String campo = e.getField().substring(e.getField().lastIndexOf('.') + 1);
    String etiqueta = ETIQUETAS.getOrDefault(campo, campo);
    String problema = switch (String.valueOf(e.getCode())) {
        case "NotNull", "NotBlank", "NotEmpty" -> "es obligatorio";
        case "Size" -> "es demasiado largo";
        case "Positive" -> "debe ser mayor a 0";
        case "PositiveOrZero", "DecimalMin", "Min" -> "tiene un valor muy bajo";
        case "DecimalMax", "Max" -> "tiene un valor muy alto";
        default -> "no es válido";
    };
    return etiqueta + ": " + problema;
}
```

  `CampoError` queda con `e.getField()` y `mensajeDeCampo(e)`. Si un test espera el texto anterior de `@Size` o similar, actualizarlo.

- `FE/services/api.ts`, `toApiError`: si `body.errores` tiene elementos, el mensaje es `body.errores.map(e => e.mensaje).join('. ')`.
- `BE/dto/LineaRequest.java`: agregar `@DecimalMax("9999999.999")` a `cantidad`, `@DecimalMax("99999999.99")` a `pesoUnitKg` y `@DecimalMax("9999999999.99")` a `precioUnit`.
- `BE/service/DocumentoService.java`, `aplicarLineas`, después de `CalculoDocumento.linea`:

```java
if (calculada.total().compareTo(new BigDecimal("999999999999.99")) > 0
        || (calculada.pesoTotalKg() != null && calculada.pesoTotalKg().compareTo(new BigDecimal("9999999999.99")) > 0)) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "La línea " + (lineas.size() + 1) + " tiene un total demasiado grande");
}
```

- `BE/config/GlobalExceptionHandler.java`, `integridad`: el mensaje pasa a `No se pudo guardar: hay un dato repetido o fuera de rango`.
- Tests: nombre de 200 caracteres responde `Nombre: es demasiado largo`; cantidad `12345678` responde 400, no 409.

### 5. Buscador de productos

En `FE/components/documentos/ProductoBuscador.tsx`, agregar el estado `resultadosDe`, que se asigna a `q` junto con `setResultados` al recibir la respuesta. Con Enter, solo se agrega el primer resultado si `resultadosDe === texto.trim()`.

### 6. Dependencias

```bash
cd frontend && npm uninstall shadcn && npm install -D shadcn && npm audit fix
```

No usar `--force`. Después, `npm run lint` y `npm run build` deben pasar. Ejecutar `npm audit --omit=dev` y anotar el resultado en la descripción del PR.

---

## C7 · Documentación final

### `docs/ManualDeUso.md`

Reemplazar en el texto:

| Dice hoy | Debe decir |
|---|---|
| `Guardar cotización` | `Guardar` |
| casilla `Mostrar entrega` | casilla `Anotar fecha y lugar de entrega` y, para exportarlos, `Mostrarlos también al exportar` |
| `Descargar PNG` / `Descargar PDF` | `Imagen (PNG)` / `PDF`, y agregar `Excel` |
| `Por cobrar` | `Debe C$…` |
| pestaña `Lista de precios` | panel `Lista de precios del proveedor` |
| botón `Importar lista` | botón `Revisar archivo` y luego `Importar` |

Reescribir también:

- La sección "Opción A: Desde la pantalla de Precios", con las columnas reales: Fecha, Folio, Cliente, Descripción, Cantidad y Precio unit. Aclarar que el precio de lista se consulta en Productos.
- En la opción B, describir la tarifa por kg editable solo para esa línea.

Agregar secciones breves para: importar el histórico de precios y convertir un registro en factura activa; ver el historial de cambios; mostrar el total en dólares; configurar la tarifa por cliente; y anular y reactivar una factura.

### `docs/Despliegue.md`

- Paso de logs: `Successfully applied 12 migrations`.
- Sección 5, antes del `pg_restore` local, agregar `psql -d billme_prueba -c 'create extension if not exists pg_trgm'`.
- Sección 7, agregar a la lista: restaurar un respaldo en una base vacía y confirmar que trae datos; importar el histórico y confirmar que Facturas sigue vacía.

### `docs/Requerimientos.md`

- Actualizar: React 19, Spring Boot 4, `html-to-image`, `jspdf`, Apache POI y health check `/actuator/health`.
- Sección 2.1: tarifa por kg antes de IVA en USD o C$. Abonos anteriores al inicio de la cuenta rechazados.
- Sección 2.4: historial inmutable, total en dólares, tipo de cambio por documento y reactivar factura.
- Importación: solo histórico de precios, con serie `HIS` y conversión de uno en uno.
- Sección 2.5: total en dólares en consolidados.

### `docs/PlanDeDesarrollo.md`

Actualizar las decisiones, fórmulas y modelo de datos para que coincidan con C2, C3 y C4. Agregar a la tabla de PR las filas C1 a C7 como `Hecho`.

### `README.md` y `docs/DiagramaER.md`

- README: `V1` a `V11`; el saldo ya no excluye "no importadas", sino que solo cuenta facturas activas; mencionar el histórico de precios y el historial inmutable.
- Diagrama: `documentos.mostrar_total_usd`, `documentos.tipo_cambio`, `consolidados.mostrar_total_usd`, `consolidados.tipo_cambio`, `documento_revisiones.motivo`, `clientes.tarifa_kg` y `clientes.tarifa_moneda`.

---

## Deploy

Hacerlo cuando C1 a C7 estén en `main` con el CI en verde. Las cuentas y secretos los maneja el dueño del proyecto; nunca se escriben en el repositorio.

1. **Supabase.** Crear el proyecto en *East US (North Virginia)*. Como la base es nueva, no hay que borrar tablas. Copiar de *Connect → Session pooler* el host real, porque puede no ser igual al ejemplo de `docs/Despliegue.md`.
2. **Render.** *New → Blueprint* con este repositorio y las variables de `docs/Despliegue.md`, sección 2. Comprobar en los logs que la imagen Docker construye, que aparece `Successfully applied 12 migrations` y `Usuario inicial '…' creado`, y que `/actuator/health` responde `UP`.
3. **Vercel.** Root Directory `frontend` y `VITE_API_URL=https://<servicio>.onrender.com/api`. Copiar el dominio en `CORS_ALLOWED_ORIGINS` de Render, sin barra final, y reiniciar.
4. **GitHub.** Crear los secretos `SUPABASE_DB_URL` y `RESPALDO_CLAVE`, y la variable `BACKEND_URL`. Correr a mano *Respaldo semanal* y *Mantener despierto el backend*.
5. **Prueba con datos de ensayo** en la app desplegada, con el usuario inicial:
   - Cotización → factura.
   - Abono.
   - Registrar compra.
   - Consolidado con subtotal renombrado, guardado dos veces.
   - Exportar PNG, PDF y Excel, con y sin total en dólares.
   - Recargar `/clientes` sin obtener 404.
6. **Respaldo.** Correr otra vez *Respaldo semanal*, descifrarlo y restaurarlo en un Postgres 17 local vacío, siguiendo `docs/Despliegue.md`, sección 5. Comprobar que trae las facturas de ensayo. Si falla, no continuar.
7. **Vaciar la base**, para que la primera factura real sea `FAC-0001`. En el *SQL Editor* de Supabase:

   ```sql
   drop schema public cascade;
   create schema public;
   grant all on schema public to postgres, anon, authenticated, service_role;
   ```

   Reiniciar el servicio en Render. Los logs deben mostrar de nuevo `Successfully applied 12 migrations` y `Usuario inicial '…' creado`.
8. **Usuarios.** Entrar, cambiar la contraseña inicial, crear el segundo usuario y borrar `BILLME_PASSWORD_INICIAL` de Render.
9. **Datos reales.** Importar la lista del proveedor y `cotizaciones1`. Comprobar:
   - Facturas sigue vacía.
   - Ganancias y saldos quedan en cero.
   - El precio sugerido ya funciona.
10. **Entrega.** El dueño del proyecto hace la primera factura real junto con quien la usará a diario.
