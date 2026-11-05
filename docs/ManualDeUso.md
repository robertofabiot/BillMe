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
   - Si necesitas indicar fecha o lugar de entrega, activa la casilla **Mostrar entrega** y llena los campos correspondientes por línea.
7. Agrega todos los materiales necesarios.
8. En la barra inferior, presiona **Guardar cotización**. El sistema le asignará un folio único de cotización (ej. `COT-0001`).

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
3. Si deseas consultar el precio para un cliente en particular, selecciónalo en el filtro de cliente.
4. La tabla mostrará de inmediato:
   - El precio de lista vigente del proveedor (sin IVA).
   - La fecha en que rige esa lista.
   - El último precio al que se le vendió a ese cliente (con IVA).
   - La fecha de esa última venta.

### Opción B: Desde el editor de cotizaciones o facturas
1. Al agregar una línea en una cotización o factura, presiona el botón con el ícono de **Etiqueta** junto al precio unitario.
2. Se abrirá la ventana de precios donde verás:
   - El último precio cobrado a ese cliente.
   - El precio calculado según la fórmula oficial (tarifa por kg en dólares, descuento del cliente y tipo de cambio).
   - El precio por peso o por metro lineal.
3. Presiona **Usar este precio** sobre la opción que desees aplicar a la línea.

---

## 4. Enviar un documento por WhatsApp

1. Abre la cotización, factura o consolidado que deseas enviar.
2. Presiona el botón **Exportar** en la parte superior.
3. Se abrirá la vista previa del documento con el formato oficial del negocio.
4. Puedes ajustar el encabezado si deseas agregar alguna referencia especial antes de generar el archivo.
5. Selecciona el formato de exportación:
   - **Descargar PNG:** Genera una imagen nítida de alta resolución, ideal para verla directamente en el chat de WhatsApp desde el celular.
   - **Descargar PDF:** Genera un documento formal en PDF para clientes que requieren comprobante descargable.
6. Una vez descargada la imagen o el PDF en tu computadora o celular, abre WhatsApp, selecciona la conversación del cliente y adjunta el archivo descargado como imagen o documento.

---

## 5. Registrar un abono y ver la cuenta de un cliente

1. En el menú de navegación, entra a **Clientes**.
2. Haz clic sobre el cliente para abrir su ficha completa y su **Estado de cuenta**.
3. En la parte superior verás en grande el saldo actual:
   - Si dice **Por cobrar**, es el monto total que el cliente debe a la fecha.
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
2. Selecciona la pestaña **Lista de precios**.
3. Elige el archivo de Excel (`.xlsx` o `.xlsm`) suministrado por el proveedor.
4. Indica la fecha a partir de la cual rigen los nuevos precios (**Los precios rigen desde**).
5. El sistema leerá el archivo, detectará las columnas de código, descripción y precios, y mostrará un resumen previo de los productos encontrados.
6. Presiona **Importar lista**.
7. Los precios de lista quedarán guardados y se utilizarán de inmediato en la calculadora y en las sugerencias de cotizaciones y facturas.
