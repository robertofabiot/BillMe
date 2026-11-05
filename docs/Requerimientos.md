# Documento de Requerimientos — BillMe

## 1. Descripción General del Sistema

BillMe es una plataforma web especializada en la gestión operativa y comercial para venta de materiales de construcción y metalmecánica. El sistema optimiza el ciclo de vida comercial completo: cotizaciones rápidas con autocompletado y fórmulas de cálculo de peso, facturación interna con control de entregas, cuenta corriente en vivo por cliente y por proyecto, compras a proveedores ligadas a facturas de venta, reporte de utilidades reales, y consolidados quincenales para cobro agrupados por fecha de entrega con exportación a imagen de alta resolución (WhatsApp), PDF y Excel.

Diseñado bajo la filosofía **KISS** (Keep It Simple, Stupid), prioriza la facilidad de uso diario en escritorio y celulares, navegación fluida, legibilidad con tipografía clara y controles táctiles de 44px, prescindiendo de burocracias innecesarias.

---

## 2. Requerimientos Funcionales

### 2.1. Módulo de Clientes y Cuenta Corriente
- **Gestión de clientes:** Registro, edición y búsqueda inmediata de clientes.
- **Condiciones comerciales individuales:**
  - Descuento comercial por defecto (`descuento_pct`) aplicado a precios de lista.
  - Tarifa personalizada en USD/kg (`tarifa_kg_usd`) para cálculo de placas y platinas.
- **Cuenta corriente en vivo:**
  - Definición de saldo inicial (`saldo_inicial`) y fecha de corte (`fecha_inicio_cuenta`).
  - Cálculo dinámico de saldo deudor o saldo a favor (`saldo_inicial + Σ facturas_activas − Σ abonos`).
  - Las facturas históricas importadas sirven exclusivamente para histórico de precios y no alteran la deuda corriente.
- **Registro de abonos:**
  - Abonos asignados directamente a la cuenta del cliente (con asignación opcional a proyecto).
  - Campos: fecha de pago, monto, proyecto, referencia bancaria y notas.
  - Soporte para pagos superiores a la deuda (saldo a favor).

---

### 2.2. Módulo de Proyectos
- Creación y edición de proyectos pertenecientes a un cliente.
- Estados del proyecto: `ACTIVO` y `FINALIZADO`.
- Creación rápida de proyectos directamente desde el editor de cotizaciones o facturas.
- Visualización de total facturado por proyecto calculado de forma eficiente en base de datos.
- Filtrado de documentos, abonos y estado de cuenta por proyecto.

---

### 2.3. Módulo de Catálogo y Precios de Lista
- **Catálogo de productos:**
  - Código interno único (ej. `PLACA-1/4`, `ANG-2X2X3/16`).
  - Nombre principal y descripción técnica.
  - Unidad de medida (`UND`, `M`, `KG`, etc.) y familia de material.
  - Productos genéricos (`GEN-PLACA`, `GEN-PLATINA`, `GEN-LIBRE`) para materiales cortados a medida o servicios libres.
  - Algoritmo de forma de cálculo geométrica: `NINGUNA`, `PLACA`, `PLATINA`, `ANGULAR`.
- **Búsqueda inteligente tolerante a fallos:**
  - Búsqueda mediante trigramas Postgres (`pg_trgm`) por código, nombre normalizado o alias alternativos.
- **Historial de precios de lista de proveedores:**
  - Registro de listas con vigencia por fecha y fuente (`LISTA_IMPORTADA`, `FACTURA_PROVEEDOR`, `PRECIO_CLIENTE`, `MANUAL`).
  - Consulta del precio de lista vigente y cálculo bidireccional (precio sin IVA, con descuento o deducido desde precio de venta).
- **Importador de listas de precios:** Carga recurrente de archivos Excel (`.xlsx`, `.xlsm`) de proveedores con mapeo automático y vista previa antes de guardar.

---

### 2.4. Módulo de Cotizaciones y Facturación
- **Series independientes:** Folios automáticos independientes para cotizaciones (`COT-0001`) y facturas (`FAC-0001`).
- **Conversión directa:** Conversión de cotizaciones a facturas activas con un solo clic preservando líneas, entregas y precios.
- **Fecha real de operación:** Selección de fecha real del documento independiente de la fecha de captura.
- **Líneas de documento avanzadas:**
  - Cantidad y precio unitario en córdobas con IVA incluido (15%).
  - Cálculo automático de peso unitario y peso total en kilogramos.
  - Métodos de fijación de precio: `ULTIMO_PRECIO`, `FORMULA_LISTA`, `POR_PESO`, `POR_METRO`, `MANUAL`.
  - Calculadora de pesos integrada para placas, platinas y angulares con densidad calibrada (0.129 kg/in³).
  - Alerta visual cuando un precio de referencia supera los 365 días de antigüedad.
  - Control opcional de entrega por línea: fecha de entrega y lugar de entrega.
- **Historial de revisiones:** Versionado inmutable (`documento_revisiones`) cada vez que se modifica un documento existente.
- **Anulación segura:** Posibilidad de anular facturas y cotizaciones con confirmación explícita sin eliminar registros históricos.
- **Paginación y búsqueda en servidor:** Búsqueda en tiempo real por texto libre (`q`) con debounce de 300 ms y paginación en servidor (`Pageable`).

---

### 2.5. Módulo de Consolidados por Entrega
- Generación dinámica de estados de cuenta quincenales o mensuales filtrando por cliente, proyecto opcional y rango de fechas.
- Agrupación automática de líneas facturadas según su fecha de entrega (o fecha de factura si no indica entrega).
- Subtotales editables en vivo: personalización del nombre del bloque/entrega con persistencia en `consolidado_grupos`.
- Balance financiero completo:
  - Saldo anterior al período.
  - Total facturado en el período.
  - Abonos recibidos en el período.
  - Saldo total actualizado.
- Exportación en formatos listos para entrega: Excel estructurado, imagen PNG nítida para mensajería instantánea y documento PDF.

---

### 2.6. Módulo de Compras y Rentabilidad
- **Compras ligadas a factura:**
  - Registro de compras a proveedores asociadas a una factura de venta específica (`compras`).
  - Actualización directa del costo unitario con IVA (`costo_unit_con_iva`) y peso real en las líneas seleccionadas.
- **Reporte de ganancias en servidor:**
  - Tablero centralizado con tres métricas clave: Total facturado, Total costo y Ganancia neta (`Total − Cantidad × Costo`).
  - Tabla de desglose por factura.
  - Aviso explícito de líneas facturadas que no tienen costo asignado (evitando sesgos al no computar costo cero).

---

### 2.7. Módulo de Exportación y Compartición
- Motor de exportación a tres formatos clave:
  - **PNG:** Renderizado en alta resolución a escala retina para lectura perfecta en WhatsApp desde pantallas móviles.
  - **PDF:** Documento formal generado vía `jspdf` para archivo y descarga.
  - **Excel:** Hojas de cálculo con fórmulas y estilos estructurados.
- Encabezado editable al vuelo para notas o condiciones especiales sin alterar el registro en base de datos.
- Formato de fecha uniforme `dd/mm/aaaa`.

---

### 2.8. Módulo de Configuración y Parámetros
- Tabla centralizada de parámetros de negocio en base de datos (`configuracion`):
  - Porcentaje de IVA (15%).
  - Tipo de cambio oficial (36.6243 C$/USD).
  - Tarifa base por kg en dólares para placas y platinas (1.80 USD/kg).
  - Densidad del acero (0.129 kg/in³).
  - Umbral de días para considerar un precio antiguo (365 días).

---

### 2.9. Usabilidad, Ergonomía y Responsive Móvil
- **Diseño adaptativo (< 768px):**
  - Barra superior compacta con botón de menú hamburguesa y panel lateral en drawer (`Sheet`).
  - Contenedores con espaciado ergonómico (`p-4` en móviles, `md:p-8` en escritorio).
  - Vista dual en tablas densas: tabla expandida en escritorio y tarjetas legibles con etiquetas visibles en celular (`LineasTabla`).
- **Legibilidad:** Tipografía base mínima de 14px (`text-sm`), inputs a 16px (`text-base`) para prevenir zoom forzado en dispositivos iOS, botones táctiles con altura mínima de 44px (`h-11`).
- **Lenguaje claro y confirmaciones seguras:** Títulos y botones en redacción natural (ej. "Convertir en factura", "Guardar cambios", "Registrar abono"). Diálogos de confirmación nativos que identifican por nombre/código el objeto que será eliminado o modificado.
- **Pantallas vacías amigables:** Mensajes claros y botones de acción directa cuando no existen registros en ninguna de las pantallas principales.

---

## 3. Requerimientos No Funcionales

### Tecnologías
- **Frontend:** React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons, html2canvas, jspdf, xlsx.
- **Backend:** Java 21 LTS, Spring Boot 3.x, Spring Data JPA, Hibernate, Flyway.
- **Base de Datos:** PostgreSQL 17 con extensiones `pg_trgm` y `uuid-ossp`.

### Infraestructura y Despliegue
- Despliegue continuo en nube:
  - Frontend: Vercel.
  - Backend: Render (contenedor Docker con health check `/api/health` y ping keep-alive en horario de oficina).
  - Base de datos: Supabase / PostgreSQL administrado con respaldos automáticos.

### Rendimiento y Escalabilidad
- Búsqueda de clientes, productos y documentos en tiempo real con tiempos de respuesta < 200 ms.
- Paginación server-side para tablas con miles de registros de documentos.
- Agregaciones numéricas (totales por proyecto, ganancias) ejecutadas nativamente en motor SQL.

### Seguridad y Control de Acceso
- Autenticación mediante tokens JWT firmados con expiración y respuesta HTTP 401 unificada.
- Dos usuarios de acceso sin roles jerárquicos artificiales.
- Contraseñas protegidas mediante algoritmos de hash seguros (BCrypt).
- Funcionalidad de cambio seguro de contraseña.

---

## 4. Alcance y Límites del Sistema (Fuera de Alcance)

Para mantener la robustez, simplicidad y apego estricto a las operaciones reales del negocio, los siguientes aspectos quedan explícitamente fuera del alcance:
1. **Roles y permisos complejos:** Se mantiene un esquema simplificado sin niveles burocráticos.
2. **Facturación fiscal electrónica (DGI):** El sistema emite comprobantes de control interno comercial, no facturación fiscal electrónica.
3. **Múltiples monedas en simultáneo:** Toda la contabilidad y saldos operan estrictamente en Córdobas (C$); el dólar se usa solo como variable de conversión paramétrica.
4. **Control de inventario perpetuo / Kardex:** Las compras se adquieren contra pedido y se ligan directamente a las facturas vendidas.
5. **Modo fuera de línea (offline):** La aplicación requiere conectividad a internet para garantizar sincronización inmediata.
