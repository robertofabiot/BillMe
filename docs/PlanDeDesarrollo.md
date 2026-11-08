# Plan de Desarrollo (hoja de ruta a producción)

Hoja de ruta acordada el 2026-11-01 después de auditar los Excel del negocio (libro de facturas, histórico de precios y calculadora de pesos) y el código existente. Principio rector: **KISS**. El sistema lo usa a diario una persona mayor, así que cada pantalla tiene una acción principal clara y no se agregan funciones para casos excepcionales.

## 1. Decisiones de negocio

- **Precio sugerido:** el último precio dado a ese cliente. Las cotizaciones no aceptadas cuentan para el historial, nunca para ingresos. Se avisa si el precio tiene más de un año. Como alternativas se puede calcular con la fórmula de lista o escribir uno nuevo. El vendedor puede cambiar el precio libremente.
- **Moneda, IVA y redondeo:** siempre córdobas en contabilidad, IVA del 15 % incluido en el precio mostrado y TC fijo inicial de 36.6243 parametrizable. El precio unitario y el peso unitario se redondean a 2 decimales.
- **Placas y platinas:** se cobran por peso: tarifa de cada cliente (`tarifa_kg`) en USD o C$ (`tarifa_moneda`), con tarifa global de respaldo en Configuración: `tarifa * (moneda == NIO ? 1 : TC) * 1.15`.
- **Láminas por metro:** precio por metro de una lámina ya vendida × largo nuevo.
- **Proveedores:** la lista de precios es del proveedor principal; los pernos se cotizan con otros proveedores y todo se compra a pedido. Se guarda costo y precio final, y la compra al proveedor queda ligada a la venta.
- **Calculadora de pesos:** solo placas, platinas y angulares, con los errores corregidos. El peso de la factura del proveedor manda. La densidad se mantiene en 0.129 kg/in³ (configurable) porque coincide con los pesos del proveedor.
- **Abonos y consolidados:** los abonos van a la cuenta del cliente, con proyecto opcional; se rechazan pagos con fecha anterior al inicio de la cuenta. Los consolidados se agrupan por fecha de entrega, con opción de total en dólares y subtotales editables.
- **Documentos:** series independientes COT, FAC y HIS; son documentos internos, sin numeración fiscal. Incluyen opción de total en dólares fijando el tipo de cambio al crearse. Se exportan a PNG, PDF y Excel replicando el formato actual, sin logo.
- **Saldo y estado de cuenta:** el saldo deudor solo cuenta facturas activas (`saldo_inicial + Σ facturas activas − Σ abonos`). El histórico de precios (serie HIS) no suma al saldo ni a reportes de ganancias y puede convertirse individualmente a factura activa.
- **Usuarios y uso:** 2 usuarios sin roles. Se usa sobre todo en PC y el celular queda para emergencias. No se necesita trabajar sin internet.
- **Migración:** no se migran las hojas del libro de facturas. Se importa el histórico de precios bajo la serie HIS cuantas veces haga falta.

**Fuera de alcance:** roles, modo sin conexión, ajustes de precio excepcionales de un cliente, grupos de precio, área de pintado, calculadoras de perlín/viga/tubo/barra, inventario, USD contable, numeración DGI y aprobaciones de precio.

Todos los requerimientos operativos y decisiones de diseño fueron ejecutados e incorporados al sistema a través de los 13 Pull Requests iniciales más la serie de correcciones pre-deploy C1 a C7.

## 2. Fórmulas oficiales

```
IVA = 0.15 · TC = 36.6243 · ρ = 0.129 kg/in³ · precio viejo = 365 días   (editables)
r2(x) = redondeo a 2 decimales, mitad hacia arriba

Línea      PU = r2(PU) · total = r2(cant × PU) · peso_unit = r2(peso) · peso_total = cant × peso_unit
Sugerido   último PU dado a ese cliente → si no hay, último a cualquier cliente → alerta > 365 días
Fórmula    PU = r2(lista × 1.15 × (1 − d_cliente))
Lista      desde proveedor: lista = P_sin_IVA / (1 − d_prov)
           desde cliente:   lista = PU_cliente / 1.15 / (1 − d_cliente)
Por peso   PU = r2(peso_unit × tarifa × (moneda == NIO ? 1 : TC) × 1.15)
Por metro  PU = r2(PU_ref / largo_ref × largo_nuevo)
Placa/platina  peso = ancho × largo × espesor × ρ        (pulgadas)
Angular        peso = (ala1 + ala2 − espesor) × espesor × largo × ρ
Ganancia   total − cant × costo_sin_IVA × 1.15   (solo facturas activas con costo)
Saldo      saldo_inicial + Σ facturas activas (desde fecha_inicio) − Σ abonos (desde fecha_inicio)
           (el histórico de precios serie HIS no suma al saldo)
```

## 3. Modelo de datos objetivo

| Tabla | Cambio |
|---|---|
| `usuarios` | Sin `rol` |
| `configuracion` | Nueva: IVA, TC, tarifa por kg, densidad, días de precio viejo |
| `clientes` | + descuento_pct, tarifa_kg, tarifa_moneda, saldo_inicial, fecha_inicio_cuenta; sin borrados en cascada |
| `proveedores`, `precios_lista`, `compras` | Nuevas |
| `productos` | Sin precio/costo (pasan a `precios_lista`); + unidad, familia, genéricos y búsqueda por similitud |
| `documentos` / `documento_lineas` | Reemplazan `facturas` / `detalles_factura`: tipo (COTIZACION, FACTURA, HISTORICO), folio por serie (COT, FAC, HIS), fecha real, descripción libre, pesos, método de precio, entrega, costo, mostrar_total_usd, tipo_cambio |
| `documento_revisiones` | Nueva: versiones anteriores al editar con motivo inmutable protegido por base de datos |
| `abonos` | Pasan a la cuenta del cliente, con proyecto opcional y validación de fecha de corte |
| `consolidados` / `consolidado_grupos` | Consolidado en vivo por rango; mostrar_total_usd, tipo_cambio; solo se guardan los nombres editados |

## 4. Pull Requests

Todos los PR comparten este DoD: CI en verde (`./mvnw verify`, `npm run lint`, `npm run build`), la migración aplica sobre el esquema anterior y cada commit compila. Los tests de integración usan un Postgres 17 embebido, así que no hace falta Docker.

| # | Rama | Objetivo | Migración | Estado |
|---|---|---|---|---|
| 1 | `chore/fundaciones` | Flyway, validación y errores, transacciones, sin cascadas peligrosas, tests de integración y CI | V1 | Hecho |
| 2 | `refactor/usuarios-sin-roles` | Dos usuarios sin roles, sin `admin123`, 401 con token vencido, cambio de contraseña | V2 | Hecho |
| 3 | `chore/despliegue` | Docker, Render, Vercel, health check, respaldo semanal, ping en horario laboral | — | Hecho |
| 4 | `feat/catalogo-precios-lista` | Configuración real, proveedores, búsqueda por similitud, historial de lista y su cálculo | V3 | Hecho |
| 5 | `feat/importar-lista-proveedor` | Importar la lista del proveedor cuantas veces haga falta | — | Hecho |
| 6 | `feat/documentos` | Cotizaciones y facturas: series, fecha real, líneas libres, editor, revisiones, conversión y anulación | V4 | Hecho |
| 7 | `feat/precio-sugerido-y-calculadora` | Último precio, fórmula, calculadora de peso, precio por peso y por metro, página Precios | V5 | Hecho |
| 8 | `feat/exportacion-documentos` | PNG, PDF y Excel con el formato actual | — | Hecho |
| 9 | `feat/importar-cotizaciones` | Importar `cotizaciones1` con conciliación asistida (hito MVP) | — | Hecho |
| 10 | `feat/cuenta-corriente` | Abonos a la cuenta, saldo a favor, estado de cuenta por proyecto | V6 | Hecho |
| 11 | `feat/consolidados-por-entrega` | Consolidado en vivo por fecha de entrega con saldo anterior y abonos | V7 | Hecho |
| 12 | `feat/compras-y-ganancias` | Compra al proveedor ligada a la venta y reporte de ganancias | V8 | Hecho |
| 13 | `chore/pulido-movil-y-docs` | Celular, legibilidad, paginación, limpieza, manual de uso | — | Hecho |
| C1 | `fix/bloqueantes-pre-deploy` | Fallos que bloquean el deploy | — | Hecho |
| C2 | `feat/historial-y-total-usd` | Historial inmutable y total en dólares | V9 | Hecho |
| C3 | `feat/historico-de-precios` | Histórico de precios, conversión y anulación | V10 | Hecho |
| C4 | `feat/tarifa-por-cliente` | Tarifa por cliente y precio por peso | V11 | Hecho |
| C5 | `fix/interfaz-pre-deploy` | Legibilidad, barra fija, consolidados y PDF | — | Hecho |
| C6 | `fix/logica-menor` | Búsqueda, orden, cambio de cliente y mensajes | — | Hecho |
| C7 | `docs/actualizacion-final` | Documentación final | — | Hecho |

### Casos de prueba (números de ejemplo)

- Placa 10"×8"×¼", 10 unidades, tarifa 1.80 → 2.58 kg, PU 195.60, total 1,956.00 (el PU se redondea a 2 decimales antes de multiplicar).
- Platina 4"×3/16"×3.05 m → 11.62 kg en cualquier unidad.
- Angular 2"×2"×3/16"×6 m → 21.78 kg.
- Lista: 840.00 sin IVA con 16 % → 1,000.00. Precio de cliente con 4 % → 1,104.00.
- Lista desde precio de cliente: 1,104.00 con 4 % → 1,000.00; con 6 % para otro cliente → 1,081.00.
- Por metro: 2,700.00 / 6.75 m × 5.40 m → 2,160.00.
- Ganancia: PU 1,300.00 con costo 950.00 sin IVA → 207.50 por unidad.
