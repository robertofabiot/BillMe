# Despliegue y respaldos

```
Vercel (frontend)  →  Render (backend, Docker)  →  Supabase (Postgres 17)
        GitHub Actions: CI en cada PR · respaldo semanal cifrado · ping en horario laboral
```

Todo corre en planes gratuitos. Antes de crear cada servicio, revisa los límites vigentes de cada plataforma.

## 1. Supabase (base de datos)

1. **Región:** usa la misma región que el backend. `render.yaml` crea el servicio en `virginia`, que equivale a *East US (North Virginia)* en Supabase. Si tu proyecto de Supabase ya existe en otra región, cambia `region` en `render.yaml` **antes** de crear el servicio en Render: después no se puede cambiar.
2. **Data API apagada:** BillMe no la usa. Al crear el proyecto, desmarca *Enable Data API*; si ya existe, desactívala en *Project Settings → Data API*. Comprueba en *Advisors → Security Advisor* que no queden avisos de tablas expuestas.
3. **Solo la primera vez:** borra las tablas que dejó la versión anterior (`ddl-auto=update`). Flyway no arranca sobre un esquema creado a mano. En *SQL Editor*:

   ```sql
   drop table if exists consolidado_items, consolidado_groups, consolidados, abonos,
       detalles_factura, facturas, alias_productos, productos, proyectos, clientes,
       usuarios cascade;
   ```

4. **Cadena de conexión:** en *Project Settings → Database → Connection string*, elige **Session pooler** (puerto 5432). La conexión directa solo funciona por IPv6, y ni Render ni GitHub la tienen.

   | Para | Formato |
   |---|---|
   | Render (`DATABASE_URL`) | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require` |
   | Render (`DATABASE_USERNAME`) | `postgres.<referencia-del-proyecto>` |
   | GitHub (`SUPABASE_DB_URL`) | `postgresql://postgres.<referencia>:<contraseña>@aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require` |

## 2. Render (backend)

1. *New → Blueprint* y elige este repositorio. Render lee `render.yaml`.
2. Completa las variables que pide:

   | Variable | Valor |
   |---|---|
   | `DATABASE_URL` | URL JDBC del *Session pooler* (ver arriba) |
   | `DATABASE_USERNAME` | `postgres.<referencia-del-proyecto>` |
   | `DATABASE_PASSWORD` | contraseña de la base de Supabase |
   | `CORS_ALLOWED_ORIGINS` | dominio de Vercel, sin barra final (ej. `https://billme.vercel.app`); si hay varios, separados por coma |
   | `BILLME_USUARIO_INICIAL` | usuario para entrar (ej. `juan`) |
   | `BILLME_PASSWORD_INICIAL` | contraseña de al menos 8 caracteres |
   | `BILLME_NOMBRE_INICIAL` | nombre visible (ej. `Juan Pérez`) |
   | `JWT_SECRET` | la genera Render; no tocar |

3. En el primer despliegue, los logs deben mostrar `Successfully applied 12 migrations` y `Usuario inicial '…' creado`.
4. Abre `https://<servicio>.onrender.com/actuator/health`: debe responder `{"status":"UP"}`.
5. Entra a la app, cambia la contraseña en *Configuración → Usuarios* y agrega al segundo usuario. Después puedes borrar `BILLME_PASSWORD_INICIAL` de Render: solo se usa cuando la base no tiene usuarios.

Render despliega solo cuando el CI de `main` pasa en verde (`autoDeployTrigger: checksPass`) y solo si cambió algo en `backend/`.

## 3. Vercel (frontend)

1. *Add New → Project*, importa el repositorio y pon **Root Directory** en `frontend`. Vite se detecta solo (`npm run build`, salida `dist`).
2. Variable de entorno `VITE_API_URL` = `https://<servicio>.onrender.com/api`.
3. Despliega y copia el dominio en `CORS_ALLOWED_ORIGINS` de Render. Luego reinicia el servicio en Render.

## 4. GitHub (respaldos y ping)

En *Settings → Secrets and variables → Actions*:

| Tipo | Nombre | Valor |
|---|---|---|
| Secret | `SUPABASE_DB_URL` | URI del *Session pooler* (formato `postgresql://…`) |
| Secret | `RESPALDO_CLAVE` | frase larga para cifrar los respaldos. **Guárdala en un lugar seguro: sin ella los respaldos no sirven.** |
| Variable | `BACKEND_URL` | `https://<servicio>.onrender.com` (sin `/api`) |

- **Respaldo semanal:** los domingos a las 03:00 (hora de Nicaragua) se respalda la base, cifrada con AES-256 porque el repositorio es público. Queda 90 días como artefacto del workflow *Respaldo semanal*. Pruébalo una vez con *Run workflow*.
- **Ping:** cada 10 minutos, de 7:00 a 22:00 (hora de Nicaragua), se consulta la salud del backend para que Render no lo duerma. También mantiene activo el proyecto de Supabase.
- GitHub desactiva los workflows programados de repositorios públicos tras 60 días sin commits. Si pasa, se reactivan desde la pestaña *Actions*.

## 5. Restaurar un respaldo

1. En *Actions → Respaldo semanal*, abre la ejecución y descarga el artefacto. Descomprime el `.zip` para obtener `billme-AAAA-MM-DD.dump.gpg`.
2. Descifra el archivo (pide `RESPALDO_CLAVE`):

   ```bash
   gpg --pinentry-mode loopback --decrypt billme-AAAA-MM-DD.dump.gpg > billme.dump
   ```

3. Para revisarlo, restaura en un Postgres 17 local:

   ```bash
   createdb billme_prueba
   psql -d billme_prueba -c 'create extension if not exists pg_trgm'
   pg_restore --no-owner --dbname=billme_prueba billme.dump
   ```

4. En una emergencia, restaura directo en Supabase. **Esto reemplaza los datos actuales:**

   ```bash
   pg_restore --no-owner --clean --if-exists --dbname="$SUPABASE_DB_URL" billme.dump
   ```

## 6. Desarrollo local

- **Backend:** define `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` y `JWT_SECRET` (se genera con `openssl rand -base64 32`) y luego corre `./mvnw spring-boot:run`.
- **Tests:** `./mvnw verify`. Usan un Postgres embebido, así que no hace falta Docker.
- **Frontend:** `cp .env.example .env.local` (apuntando a `http://localhost:8080/api`) y luego `npm run dev`.

## 7. Verificación después de desplegar

- [ ] `/actuator/health` responde `UP`.
- [ ] Se puede entrar con el usuario inicial y cambiar la contraseña.
- [ ] Recargar `https://<app>.vercel.app/clientes` no da 404.
- [ ] *Respaldo semanal* corre a mano y el archivo se descifra con `RESPALDO_CLAVE`.
- [ ] *Mantener despierto el backend* corre a mano sin errores.
- [ ] Restaurar un respaldo en una base vacía y confirmar que trae datos.
- [ ] Importar el histórico y confirmar que Facturas sigue vacía.
