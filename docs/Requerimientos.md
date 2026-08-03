# Documento de Requerimientos

## 1. Descripción General del Sistema

Plataforma web para la gestión de cotizaciones, ventas y pagos parciales de materiales de construcción. El sistema permite administrar clientes, proyectos, productos y facturas, realizar búsquedas de productos mediante múltiples identificadores (código o alias), mantener un historial de precios por cliente para ventas recurrentes y exportar comprobantes visuales para compartir por WhatsApp. La aplicación operará bajo una arquitectura en la nube, permitiendo el acceso desde cualquier dispositivo autorizado.

---

## 2. Requerimientos Funcionales

### 2.1. Módulo de Clientes

- Creación de perfiles de clientes.
- Edición y eliminación de clientes.
- Búsqueda rápida de clientes registrados.
- Visualización del historial de compras del cliente.
- Visualización de los proyectos asociados a cada cliente.

---

### 2.2. Módulo de Proyectos

- Creación de proyectos asociados a un cliente.
- Edición de proyectos.
- Cambio de estado del proyecto:
  - Activo
  - Finalizado
- Listado de proyectos activos al momento de crear una nueva factura.
- Posibilidad de crear un proyecto directamente desde la pantalla de facturación sin abandonar el flujo de trabajo.
- Agrupación de facturas por proyecto para facilitar el seguimiento de una obra o construcción, manteniendo el modelo estrictamente minimalista (solo nombre y estado, sin presupuestos ni fechas estimadas irrelevantes).

---

### 2.3. Módulo de Catálogo de Productos

- Registro de productos con:
  - UUID como llave primaria interna.
  - Código de producto como llave natural.
- Gestión avanzada de precios y costos:
  - Registro de **Precio de Lista del Proveedor**.
  - Registro de **Descuento** otorgado por el proveedor.
  - Cálculo automático del **Costo Neto** `(Precio Lista * (1 - Descuento) * (1 + IVA))`.
- Administración de alias paramétricos para vincular múltiples nombres o dimensiones a un mismo producto.
- Búsqueda de productos mediante:
  - Código interno.
  - Nombre principal.
  - Alias.

---

### 2.4. Módulo de Cotizaciones y Facturación

- Creación de Proformas/Cotizaciones sin afectar el historial de pagos.
- Conversión de una Proforma en una Factura Activa con un solo clic.
- Generación automática de un número de folio interno (por ejemplo, `FAC-0001`).
- Búsqueda de productos desde una única barra utilizando código o alias.
- Autocompletado inteligente del precio unitario basado en el historial de ventas del cliente.
- Posibilidad de modificar manualmente el precio sugerido.
- Asociación obligatoria de la factura a un cliente.
- Asociación opcional de la factura a un proyecto del cliente seleccionado.
- Si el cliente no posee proyectos, permitir crear uno sin abandonar el proceso de facturación.

---

### 2.5. Módulo de Estados y Abonos

- Gestión de estados de las facturas:
  - Cotización
  - Pendiente
  - Pago Parcial
  - Pagado
- Registro de múltiples abonos para una misma factura.
- Almacenamiento de:
  - Fecha del abono.
  - Monto abonado.
- Cálculo automático del saldo pendiente.

---

### 2.6. Módulo de Histórico y Márgenes

- Bandeja de facturas cerradas.
- Alertas o sugerencias para completar información faltante.
- Tablero de **Reportes Cruzados**: Motor analítico global para agrupar ingresos, costos y ganancias combinando Rango de Fechas, Clientes y Proyectos en tiempo real.
- Cálculo de diferencias entre Costo Neto y Precio de Venta al Cliente para mostrar ganancias exactas.
- Consulta del historial de precios por cliente y producto.

---

### 2.7. Módulo de Exportación

- Renderizado HTML de proformas y facturas.
- Exportación en formato imagen (`.jpg` o `.png`) para compartir mediante WhatsApp.
- Posibilidad de reexportar documentos históricos.

---

### 2.8. Módulo de Consolidados

- Agrupación de múltiples facturas de un mismo cliente en un solo estado de cuenta (Consolidado).
- Generación automática de subtotales por factura.
- Constructor de documentos completamente editable:
  - Reordenamiento visual de productos y facturas (bloques).
  - Personalización de los nombres de los subtotales.
  - Eliminación de ítems del reporte sin afectar las facturas originales en base de datos.
- Generación de un total general consolidado acumulando todos los grupos.

---

### 2.9. Módulo de Configuración

- Configuración del **IVA por defecto** utilizado para todos los cálculos internos y cálculos de costo neto.
- Selección de la **Moneda Principal** del sistema (limitada a Córdobas `C$` y Dólares `USD`).
- Modificación del prefijo y secuencia del siguiente folio de facturación.

---

## 3. Requerimientos No Funcionales

### Tecnologías

- **Frontend:** React
- **Backend:** Spring Boot
- **Base de Datos:** PostgreSQL

### Infraestructura

Despliegue en servicios gratuitos de nube:

- Vercel (Frontend)
- Render (Backend)
- Supabase (Base de datos PostgreSQL)

### Rendimiento

- Búsqueda de productos y clientes con respuesta inferior a un segundo bajo condiciones normales de operación.
- Soporte para cientos de clientes, proyectos y miles de facturas sin degradación significativa del rendimiento.

### Disponibilidad

- Acceso al sistema desde cualquier dispositivo con conexión a Internet.
- Almacenamiento centralizado de la información para evitar dependencia de un único equipo.

### Seguridad

- Autenticación mediante usuario y contraseña.
- Roles de usuario (`ADMIN` y `VENDEDOR`).
- Contraseñas almacenadas mediante hash.
- Restricción de funcionalidades según el rol del usuario.

### Mantenibilidad

- Arquitectura desacoplada entre frontend y backend.
- API REST para facilitar futuras integraciones.
- Modelo de datos preparado para admitir nuevas funcionalidades sin modificaciones estructurales importantes.
