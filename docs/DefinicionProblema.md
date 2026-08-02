# Documento Base: Definición del Problema (Borrador v1)

## Situación Actual

La gestión de ventas de materiales se realiza mediante hojas de cálculo de Excel. El flujo de trabajo requiere duplicar hojas para generar nuevas facturas, introducir datos manualmente y buscar transacciones previas para mantener la consistencia en los precios por cliente.

## Puntos de Dolor

- Alta probabilidad de error humano al copiar y pegar datos.
- Tiempo excesivo invertido en buscar el historial de precios de un cliente específico.
- Dificultad para escalar: manejar múltiples clientes y facturas recurrentes se vuelve insostenible en un entorno no relacional.
- Dependencia de un equipo local específico, lo que limita la accesibilidad.

## Objetivo del Sistema

Desarrollar una aplicación que automatice la generación de facturas, centralice el catálogo de clientes y productos, y aplique la lógica de precios (históricos o calculados) de forma instantánea.
