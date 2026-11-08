# Manual de Uso de BillMe

Guía paso a paso para el uso diario del sistema. Redactada en lenguaje sencillo y directo para resolver las operaciones principales del negocio.

---

## 1. Hacer una cotización

1. En el menú de navegación, entra a **Cotizaciones**.
2. Presiona el botón **Nueva cotización**.
3. Selecciona el **Cliente** en la lista desplegable. Si la cotización pertenece a una obra específica, selecciona también el **Proyecto**.
4. La fecha se coloca automáticamente en el día de hoy, pero puedes cambiarla si lo necesitas.
5. En la sección de productos, escribe en el buscador el código, nombre o descripción del material.
6. Al elegir el producto de la lista, se agrega una nueva línea a la cotización:
   - Ingresa la **Cantidad** de piezas o metros.
   - Si el precio no se llena automáticamente o deseas cambiarlo, presiona el botón con la etiqueta de precio para ver el último precio vendido al cliente o la lista sugerida.
   - Si el material se calcula por peso, puedes presionar el botón de la calculadora para estimar los kilogramos.
   - Si necesitas indicar fecha o lugar de entrega, activa la casilla **Anotar fecha y lugar de entrega** y llena los campos correspondientes por línea. Para exportarlos en el documento final, marca la casilla **Mostrarlos también al exportar**.
7. Agrega todos los materiales necesarios.
8. En la barra inferior, presiona **Guardar**. El sistema le asignará un folio único de cotización (ej. `COT-0001`).

---

## 2. Convertir una cotización en factura

1. Entra a **Cotizaciones** y busca en la lista la cotización que fue aprobada por el cliente.
2. Haz clic sobre la cotización para abrirla en el editor.
3. En la esquina superior derecha, presiona el botón verde **Convertir en factura**.
4. El sistema te pedirá confirmación indicando el folio de la cotización. Presiona **Aceptar**.
5. De forma inmediata se genera una nueva **Factura activa** con su propio folio consecutivo (ej. `FAC-0001`), conservando todas las líneas, cantidades, precios y detalles de entrega.
6. La cotización original quedará registrada como aceptada y enlazada con la nueva factura.

---

## 3. Buscar un precio

Existen dos formas rápidas de buscar un precio:

### Opción A: Desde la pantalla de Precios
1. En el menú de navegación, entra a **Precios**.
2. Escribe en el buscador el nombre o código del producto.
3. Si deseas consultar los precios para un cliente en particular, selecciónalo en el filtro de cliente.
4. La tabla mostrará el histórico de ventas con sus columnas reales: **Fecha**, **Folio**, **Cliente**, **Descripción**, **Cantidad** y **Precio unit.** (con IVA incluido).
5. Ten en cuenta que el precio de lista vigente del proveedor no se busca aquí, sino que se consulta en el módulo de **Productos**.

### Opción B: Desde el editor de cotizaciones o facturas
1. Al agregar o editar una línea en una cotización o factura, presiona el botón con el ícono de **Etiqueta** junto al precio unitario.
2. Se abrirá la ventana de precios donde verás:
   - El último precio cobrado a ese cliente.
   - El precio calculado según la fórmula oficial con la tarifa por kg y el descuento del cliente.
   - El precio por peso o por metro lineal.
3. Si lo deseas, puedes modificar la tarifa por kg directamente en los parámetros de la línea; esta tarifa es editable solo para esa línea del documento sin alterar la ficha del cliente en el catálogo.
4. Presiona **Usar este precio** sobre la opción que desees aplicar a la línea.

---

## 4. Enviar un documento por WhatsApp

1. Abre la cotización, factura o consolidado que deseas enviar.
2. Presiona el botón **Exportar** en la parte superior.
3. Se abrirá la vista previa del documento con el formato oficial del negocio.
4. Puedes ajustar el encabezado si deseas agregar alguna referencia especial antes de generar el archivo.
5. Selecciona el formato de exportación:
   - **Imagen (PNG):** Genera una imagen nítida de alta resolución, ideal para verla directamente en el chat de WhatsApp desde el celular.
   - **PDF:** Genera un documento formal en PDF para clientes que requieren comprobante descargable.
   - **Excel:** Genera una hoja de cálculo estructurada con las líneas y montos del documento.
6. Una vez descargado el archivo (PNG, PDF o Excel) en tu computadora o celular, abre WhatsApp, selecciona la conversación del cliente y adjunta el archivo descargado como imagen o documento.

---

## 5. Registrar un abono y ver la cuenta de un cliente

1. En el menú de navegación, entra a **Clientes**.
2. Haz clic sobre el cliente para abrir su ficha completa y su **Estado de cuenta**.
3. En la parte superior verás en grande el saldo actual:
   - Si dice **Debe C$…**, es el monto total que el cliente debe a la fecha.
   - Si dice **Saldo a favor**, es dinero pagado por anticipado por el cliente.
4. Para registrar un pago, presiona el botón **Registrar abono**.
5. En la ventana emergente:
   - Ingresa el **Monto** en córdobas (C$).
   - Selecciona la **Fecha de pago**.
   - Si el abono corresponde a una obra en específico, selecciona el **Proyecto** (opcional).
   - Escribe una nota de referencia (ej. número de transferencia o cheque).
6. Presiona **Registrar abono**. El saldo del cliente y el historial cronológico de movimientos se actualizarán al instante.

---

## 6. Armar un consolidado

1. En el menú de navegación, entra a **Consolidados**.
2. Presiona **Nuevo consolidado**.
3. Selecciona el **Cliente**, el **Proyecto** (opcional) y el rango de fechas (**Desde** y **Hasta**).
4. El sistema buscará en vivo todas las facturas activas y los abonos del cliente en ese período, agrupando las líneas por su fecha de entrega (o por la fecha de factura si no tienen entrega).
5. Asigna un nombre al consolidado para identificarlo (ej. `Entregas primera quincena`).
6. Si deseas renombrar algún subtotal o grupo de entrega, puedes editar el título del grupo directamente.
7. Revisa el balance matemático al final: Saldo anterior, Total del período, Abonos recibidos y Total actualizado.
8. Presiona **Guardar consolidado**.
9. Puedes presionar **Exportar** para descargarlo en **Excel**, **PNG** o **PDF**.

---

## 7. Subir una lista de precios nueva

1. En el menú de navegación, entra a **Importar**.
2. Ubica el panel **Lista de precios del proveedor**.
3. Elige el archivo de Excel (`.xlsx` o `.xlsm`) suministrado por el proveedor.
4. Indica la fecha a partir de la cual rigen los nuevos precios (**Los precios rigen desde**).
5. Presiona el botón **Revisar archivo**. El sistema leerá el archivo, detectará las columnas de código, descripción y precios, y mostrará un resumen previo de los productos encontrados.
6. Presiona **Importar**.
7. Los precios de lista quedarán guardados y se utilizarán de inmediato en la calculadora y en las sugerencias de cotizaciones y facturas.

---

## 8. Importar el histórico de precios y convertir un registro en factura activa

1. En el menú de navegación, entra a **Importar** y selecciona el panel para importar ventas anteriores.
2. Selecciona el archivo de Excel con el histórico y presiona **Revisar archivo**; el sistema validará los registros y mostrará una vista previa con las líneas leídas.
3. Presiona **Importar**. Todos los registros importados se guardan con el tipo especial **Histórico**, identificados con el prefijo **`HIS`** (ej. `HIS-0001`) y estado **Histórica**.
4. Estos registros entran al sistema únicamente para nutrir las sugerencias y consultas de precios pasados; **no computan en el saldo** deudor del cliente ni se consideran facturas activas.
5. Si requieres cobrar o reactivar formalmente una venta histórica, abre el registro `HIS` correspondiente y presiona el botón **Convertir en factura**. El sistema generará una factura activa con un folio consecutivo oficial de la serie **`FAC`** (ej. `FAC-0005`), pasará a estado **Activa** y comenzará a sumar en el saldo por cobrar del cliente de inmediato.

---

## 9. Ver el historial de cambios

1. Al consultar o editar cualquier cotización o factura existente, en la barra superior encontrarás el botón **Ver cambios**.
2. Al presionarlo se despliega el historial cronológico de revisiones inmutables del documento.
3. Cada vez que se modifica un documento guardado, el sistema solicita obligatoriamente ingresar un **Motivo del cambio**.
4. En este panel puedes auditar quién realizó la modificación, la fecha y hora exacta, el motivo ingresado y comparar las diferencias entre la versión actual y las versiones anteriores protegidas contra alteración o borrado en la base de datos.

---

## 10. Mostrar el total en dólares

1. En el editor de cotizaciones, facturas o consolidados, puedes marcar la casilla **Mostrar total en dólares**.
2. Al activarla, el documento mostrará en el encabezado y en el pie de página el resumen con: **Total C$**, **Total U$**, **Total KG** y el **Tipo de cambio**.
3. El tipo de cambio oficial queda fijado al momento de crear el documento (snapshot inmutable), garantizando que las fluctuaciones cambiarias posteriores no alteren los valores pactados.
4. Al exportar a Imagen (PNG), PDF o Excel, los totales en dólares se incluirán automáticamente en el formato final.

---

## 11. Configurar la tarifa por cliente

1. En el menú **Clientes**, selecciona un cliente y presiona **Editar cliente** (o regístralo como nuevo).
2. En la sección de tarifas comerciales, puedes asignarle una **Tarifa por kg** personalizada y seleccionar su moneda preferida (**USD** o **C$**).
3. Si el cliente no tiene una tarifa específica configurada, el sistema utilizará como respaldo (fallback) la tarifa global definida en la pantalla de **Configuración**.
4. Al elaborar una cotización o factura para este cliente, los precios por peso se calcularán con su tarifa y moneda por defecto, permitiendo además modificar la tarifa por kg de forma manual en una línea específica sin afectar la ficha permanente del cliente.

---

## 12. Anular y reactivar una factura

1. Para anular una factura emitida por error o rechazada por el cliente, abre la factura y presiona el botón **Anular factura**.
2. Al anularla, la factura pasa a estado **Anulada** y su monto deja de computar inmediatamente en el saldo por cobrar del cliente. Si la factura se originó a partir de una cotización, la cotización original regresa automáticamente a estado **Pendiente**, lista para ser corregida o reconvertida.
3. Si posteriormente necesitas recuperar una factura anulada, ábrela y presiona **Reactivar factura**. El sistema solicitará confirmación y verificará que no exista otra factura activa vinculada a la misma cotización; al confirmar, la factura volverá a estado **Activa** y computará de nuevo en el saldo del cliente.
