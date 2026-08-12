# Plan de Desarrollo y Siguientes Pasos (Next Steps)

Para convertir el prototipo actual en una aplicación 100% funcional, se seguirá el siguiente plan de acción estructurado en 4 fases de desarrollo:

## Fase 1: Desarrollo de la API (Backend)
1. **Crear la capa de Servicios (`@Service`):** Escribir la lógica de negocio en Spring Boot (ej. calcular totales de facturas, aplicar abonos, gestionar el stock o los historiales de precios).
2. **Crear la capa de Controladores (`@RestController`):** Exponer los endpoints REST (ej. `GET /api/clientes`, `POST /api/facturas`) para que el frontend pueda consumir los datos de forma dinámica.
3. **Configurar la Base de Datos:** Actualizar el archivo de configuración (`application.properties` / `application.yml`) para conectar el backend con la base de datos PostgreSQL (Supabase, según los requerimientos).
4. **Configurar CORS:** Permitir que el frontend pueda realizar peticiones HTTP al backend sin ser bloqueado por las políticas de seguridad del navegador.

## Fase 2: Integración (Frontend + Backend)
1. **Reemplazar los Mock Data:** Modificar el código del frontend (React) para que deje de importar la información de los archivos locales estáticos (como `mock.ts`) y comience a hacer peticiones reales a la nueva API de Spring Boot.
2. **Gestión de Estados Asíncronos:** Agregar manejo de estados de red, como indicadores de carga (spinners) y notificaciones de error al consultar, crear o modificar datos en el frontend.

## Fase 3: Seguridad y Autenticación
1. **Backend:** Implementar **Spring Security** junto con **JWT** (JSON Web Tokens) para manejar el inicio de sesión y proteger los endpoints según los roles establecidos (`ADMIN` y `VENDEDOR`).
2. **Frontend:** Crear la pantalla de Login y configurar la protección de las rutas en React para garantizar que solo los usuarios autenticados y autorizados puedan acceder al sistema.

## Fase 4: Refinamiento y Despliegue
1. **Exportación de Documentos:** Implementar la lógica necesaria (conversión de HTML a Imagen) para renderizar y descargar las facturas y proformas para que puedan ser compartidas mediante WhatsApp.
2. **Despliegue (Deploy):** Poner el sistema en producción desplegando:
   - La base de datos en **Supabase**.
   - El backend en **Render**.
   - El frontend en **Vercel**.
