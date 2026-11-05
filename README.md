<p align="center">
  <img src="docs/logo-rounded.svg" alt="Logo de BillMe" width="180">
</p>

<h1 align="center">BillMe</h1>

<p align="center">
  Aplicación web de cotización, facturación y cuenta corriente con sugerencia de precios por cliente.
</p>

<p align="center">
  <a href="https://github.com/robertofabiot/BillMe/actions/workflows/ci.yml"><img src="https://github.com/robertofabiot/BillMe/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black" alt="React">
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL">
</p>

---

## Descripción

BillMe es un sistema de gestión comercial de arquitectura cliente-servidor: una SPA en React que consume una API REST sin estado sobre Spring Boot, con persistencia en PostgreSQL. Cubre el ciclo completo de una venta: catálogo con precios de lista por proveedor, cotizaciones, facturas, cuenta corriente por cliente y proyecto, consolidados de cobro, compras y reporte de utilidad.

Su objetivo es sustituir el flujo basado en hojas de cálculo, donde cada documento se duplica a mano y el historial de precios por cliente se consulta de forma manual, por un modelo relacional con folios consecutivos, trazabilidad de cambios y precios sugeridos de forma determinista.

## Arquitectura

```
React (Vite, Vercel)  ──HTTPS/JSON──►  Spring Boot (Docker, Render)  ──JDBC──►  PostgreSQL 17 (Supabase)
                                        └─ Flyway · JPA · Spring Security (JWT)
```

El backend se organiza por capas en `com.example.billme`:

| Paquete | Responsabilidad |
| --- | --- |
| `controller` | Endpoints REST bajo `/api` (documentos, clientes, productos, precios, abonos, consolidados, compras, reportes, importaciones). |
| `service` | Lógica de negocio y transacciones. |
| `calculo` | Cálculo de precios, impuestos y peso, aislado de la capa web para probarlo por separado. |
| `repository`, `model` | Acceso a datos con Spring Data JPA; entidades con `ddl-auto=validate`. |
| `importacion`, `exportacion` | Lectura de listas de precios en `.xlsx` y generación de documentos en Excel con Apache POI. |
| `security`, `config` | Filtro JWT, `BCryptPasswordEncoder`, CORS y usuario inicial. |

El esquema lo gestiona Flyway (`V1` a `V8`); Hibernate solo lo valida. La estructura completa está en el [diagrama entidad-relación](docs/DiagramaER.md).

## Características técnicas

- **Documentos versionados:** folios independientes por serie (`COT-0001`, `FAC-0001`), revisiones inmutables en cada modificación y anulación lógica en lugar de borrado.
- **Precio sugerido:** resuelve el último precio vendido al cliente y, si no existe, aplica el precio de lista vigente con el descuento del cliente. Incluye cálculo por peso y por metro.
- **Cuenta corriente derivada:** el saldo se calcula como `saldo_inicial + Σ facturas activas − Σ abonos`, con soporte para saldo a favor. No se almacena, se deriva en la consulta.
- **Búsqueda tolerante a errores:** coincidencia aproximada con la extensión `pg_trgm` de PostgreSQL.
- **Paginación y filtrado en servidor:** `Pageable` en los listados de documentos, con búsqueda por texto libre y *debounce* de 300 ms en el cliente.
- **Importación de listas de precios:** lectura de `.xlsx`/`.xlsm` con mapeo automático de columnas y vista previa antes de confirmar.
- **Consolidados por fecha de entrega:** vistas agregadas calculadas en vivo, sin tablas duplicadas.
- **Exportación:** Excel desde el backend; PDF e imagen desde el cliente.
- **Interfaz adaptable:** diseño responsivo bajo 768 px, controles táctiles de 44 px y campos a 16 px para evitar el zoom automático en iOS.

## Seguridad

- Autenticación sin estado con JWT firmado; `SessionCreationPolicy.STATELESS`.
- Contraseñas almacenadas con hash BCrypt.
- Secretos y credenciales solo mediante variables de entorno.
- CORS restringido a los orígenes configurados en `CORS_ALLOWED_ORIGINS`.
- Respaldo semanal de la base con `pg_dump`, cifrado con AES-256 antes de almacenarse, ya que el repositorio es público.

## Integración y despliegue continuos

| Flujo | Función |
| --- | --- |
| `ci.yml` | En cada PR: `./mvnw verify` (backend) y `npm run lint` + `npm run build` (frontend). |
| `backup.yml` | Respaldo cifrado semanal de la base. |
| `keep-warm.yml` | Mantiene activo el servicio gratuito en horario laboral. |

Render despliega el backend solo cuando el CI de `main` pasa (`autoDeployTrigger: checksPass`) y únicamente si cambió algo en `backend/`. Las pruebas de integración usan un PostgreSQL embebido (`embedded-postgres`), por lo que no requieren servicios externos. El procedimiento completo está en [Despliegue y respaldos](docs/Despliegue.md).

## Ejecución local

Requisitos: Java 21, Node 22 y una instancia de PostgreSQL 17.

**Backend**

```bash
cd backend/BillMe
export DATABASE_URL="jdbc:postgresql://localhost:5432/billme"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="tu-contraseña"
export JWT_SECRET="$(openssl rand -base64 32)"
export BILLME_USUARIO_INICIAL="admin"
export BILLME_PASSWORD_INICIAL="una-contraseña-de-8-o-mas"
./mvnw spring-boot:run
```

El primer arranque aplica las migraciones y crea el usuario inicial solo si la tabla de usuarios está vacía. El *health check* queda en `/actuator/health`.

**Frontend**

```bash
cd frontend
npm ci
echo "VITE_API_URL=http://localhost:8080/api" > .env.local
npm run dev
```

**Variables del backend**

| Variable | Descripción |
| --- | --- |
| `DATABASE_URL` | URL JDBC de PostgreSQL. |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Credenciales de la base de datos. |
| `JWT_SECRET` | Clave de 256 bits en base64 para firmar los tokens. |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos, separados por coma (por defecto `localhost:5173` y `localhost:3000`). |
| `BILLME_USUARIO_INICIAL` / `BILLME_PASSWORD_INICIAL` | Usuario de arranque; se ignora si ya existen usuarios. |
| `BILLME_NOMBRE_INICIAL` | Nombre visible del usuario inicial (opcional). |
| `PORT` | Puerto HTTP (opcional, 8080 por defecto). |

## Pruebas

```bash
cd backend/BillMe && ./mvnw verify
cd frontend && npm run lint && npm run build
```

## Documentación

| Documento | Contenido |
| --- | --- |
| [Requerimientos](docs/Requerimientos.md) | Requerimientos funcionales y no funcionales, alcance y límites. |
| [Plan de desarrollo](docs/PlanDeDesarrollo.md) | Hoja de ruta y fórmulas de cálculo. |
| [Diagrama entidad-relación](docs/DiagramaER.md) | Modelo de datos. |
| [Despliegue y respaldos](docs/Despliegue.md) | Puesta en producción. |
| [Manual de uso](docs/ManualDeUso.md) | Guía operativa para usuarios finales. |
| [Manual de marca](docs/ManualDeMarca.md) | Sistema de diseño de la interfaz. |
