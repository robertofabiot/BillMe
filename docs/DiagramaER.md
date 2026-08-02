# Modelo de Datos (Esquema Lógico)

> **Nota:** Todas las tablas (excepto la entidad base) heredan automáticamente los atributos de `BaseEntity`.

---

## 1. Entidad Base (Abstracta)

> No corresponde a una tabla física en la base de datos. Sus atributos son heredados por todas las entidades.

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `id` | UUID | Llave primaria. |
| `created_at` | Timestamp | Fecha de creación del registro. |
| `updated_at` | Timestamp | Fecha de la última modificación del registro. |

---

## 2. Entidades de Seguridad y Directorio

### Tabla: `Usuario`

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `username` | String | Nombre de usuario único. |
| `password` | String | Contraseña almacenada como hash. |
| `rol` | Enum (`ADMIN`, `VENDEDOR`) | Rol del usuario en el sistema. |

### Tabla: `Cliente`

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `nombre` | String | Nombre del cliente. |
| `telefono` | String | Opcional. |
| `detalles` | String | Opcional. Notas adicionales sobre el cliente. |

---

## 3. Entidades de Catálogo

### Tabla: `Producto`

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `codigo_interno` | String | Código único del producto (llave natural). |
| `nombre_principal` | String | Nombre principal del producto. |
| `descripcion` | String | Opcional. |
| `peso` | Decimal | Peso del producto en una unidad estandarizada (por ejemplo, kilogramos). |

### Tabla: `AliasProducto`

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `nombre_alias` | String | Alias del producto (ejemplo: `"Tubo 1/2"`). |
| `producto_id` | UUID | Llave foránea hacia `Producto`. |

---

## 4. Entidades de Transacción

### Tabla: `Factura`

> Puede representar tanto una **Proforma/Cotización** como una **Venta Real**.

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `folio_interno` | String | Identificador único (ejemplo: `FAC-0001`). |
| `estado` | Enum (`COTIZACION`, `PENDIENTE`, `PAGO_PARCIAL`, `PAGADO`) | Estado actual de la factura. |
| `costo_real_empresa` | Decimal | Se registra posteriormente para calcular la ganancia real. |
| `total_venta` | Decimal | Total de la venta calculado a partir de sus detalles. |
| `cliente_id` | UUID | Llave foránea hacia `Cliente`. |
| `usuario_id` | UUID | Llave foránea hacia `Usuario`. |

### Tabla: `DetalleFactura`

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `cantidad` | Decimal | Permite valores decimales (por ejemplo, metros o yardas). |
| `precio_unitario_venta` | Decimal | Precio acordado para el cliente. |
| `factura_id` | UUID | Llave foránea hacia `Factura`. |
| `producto_id` | UUID | Llave foránea hacia `Producto`. |

### Tabla: `Abono`

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `monto` | Decimal | Monto abonado. |
| `fecha_pago` | Timestamp | Fecha en que el cliente realizó el pago. |
| `factura_id` | UUID | Llave foránea hacia `Factura`. |

---

# Resumen de Relaciones Principales

- **Producto** → **AliasProducto**: **1:N**
  - Un producto puede tener múltiples alias.

- **Cliente** → **Factura**: **1:N**
  - Un cliente puede tener múltiples facturas.

- **Factura** → **DetalleFactura**: **1:N**
  - Una factura puede contener múltiples detalles.

- **Factura** → **Abono**: **1:N**
  - Una factura puede registrar múltiples abonos.

- **Producto** → **DetalleFactura**: **1:N**
  - Un producto puede aparecer en múltiples detalles de factura.
