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
