-- Esquema inicial: refleja exactamente las entidades JPA existentes al introducir Flyway.
-- Los ids (UUID) los genera Hibernate.

create table usuarios (
    id         uuid                     not null primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    username   varchar(50)              not null unique,
    password   varchar(255)             not null,
    nombre     varchar(100)             not null,
    rol        varchar(20)              not null check (rol in ('ADMIN', 'VENDEDOR'))
);

create table clientes (
    id         uuid                     not null primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    nombre     varchar(150)             not null,
    telefono   varchar(30),
    detalles   text
);

create table proyectos (
    id         uuid                     not null primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    nombre     varchar(200)             not null,
    estado     varchar(20)              not null check (estado in ('ACTIVO', 'FINALIZADO')),
    cliente_id uuid                     not null constraint fk_proyectos_cliente references clientes
);

create table productos (
    id                     uuid                     not null primary key,
    created_at             timestamp(6) with time zone not null,
    updated_at             timestamp(6) with time zone not null,
    codigo_interno         varchar(50)              not null unique,
    nombre_principal       varchar(200)             not null,
    descripcion            text,
    precio_lista_proveedor numeric(12, 2),
    descuento_proveedor    numeric(5, 2),
    costo_neto             numeric(12, 2),
    peso                   numeric(10, 3)
);

create table alias_productos (
    id           uuid                     not null primary key,
    created_at   timestamp(6) with time zone not null,
    updated_at   timestamp(6) with time zone not null,
    nombre_alias varchar(200)             not null,
    producto_id  uuid                     not null constraint fk_alias_producto references productos
);

create table facturas (
    id                 uuid                     not null primary key,
    created_at         timestamp(6) with time zone not null,
    updated_at         timestamp(6) with time zone not null,
    folio_interno      varchar(20)              not null unique,
    estado             varchar(20)              not null
        check (estado in ('COTIZACION', 'PENDIENTE', 'PAGO_PARCIAL', 'PAGADO')),
    total_venta        numeric(14, 2)           not null,
    costo_real_empresa numeric(14, 2),
    cliente_id         uuid                     not null constraint fk_facturas_cliente references clientes,
    proyecto_id        uuid                              constraint fk_facturas_proyecto references proyectos,
    usuario_id         uuid                              constraint fk_facturas_usuario references usuarios
);

create table detalles_factura (
    id                      uuid                     not null primary key,
    created_at              timestamp(6) with time zone not null,
    updated_at              timestamp(6) with time zone not null,
    cantidad                numeric(10, 3)           not null,
    precio_unitario_venta   numeric(12, 2)           not null,
    costo_unitario_snapshot numeric(12, 2),
    factura_id              uuid                     not null constraint fk_detalles_factura references facturas,
    producto_id             uuid                     not null constraint fk_detalles_producto references productos
);

create table abonos (
    id         uuid                     not null primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    monto      numeric(12, 2)           not null,
    fecha_pago date                     not null,
    factura_id uuid                     not null constraint fk_abonos_factura references facturas
);

create table consolidados (
    id            uuid                     not null primary key,
    created_at    timestamp(6) with time zone not null,
    updated_at    timestamp(6) with time zone not null,
    folio_interno varchar(20)              not null unique,
    nombre        varchar(200)             not null,
    cliente_id    uuid                     not null constraint fk_consolidados_cliente references clientes
);

create table consolidado_groups (
    id             uuid                     not null primary key,
    created_at     timestamp(6) with time zone not null,
    updated_at     timestamp(6) with time zone not null,
    nombre         varchar(200)             not null,
    orden          integer                  not null,
    consolidado_id uuid                     not null constraint fk_groups_consolidado references consolidados
);

create table consolidado_items (
    id              uuid                     not null primary key,
    created_at      timestamp(6) with time zone not null,
    updated_at      timestamp(6) with time zone not null,
    producto_nombre varchar(200)             not null,
    cantidad        numeric(10, 3)           not null,
    precio_unitario numeric(12, 2)           not null,
    orden           integer                  not null,
    factura_id      uuid                              constraint fk_items_factura references facturas,
    grupo_id        uuid                     not null constraint fk_items_grupo references consolidado_groups
);

create index idx_alias_nombre on alias_productos (nombre_alias);

-- Postgres no indexa las llaves foráneas por sí solo
create index idx_proyectos_cliente on proyectos (cliente_id);
create index idx_alias_producto on alias_productos (producto_id);
create index idx_facturas_cliente on facturas (cliente_id);
create index idx_facturas_proyecto on facturas (proyecto_id);
create index idx_detalles_factura on detalles_factura (factura_id);
create index idx_detalles_producto on detalles_factura (producto_id);
create index idx_abonos_factura on abonos (factura_id);
create index idx_consolidados_cliente on consolidados (cliente_id);
create index idx_groups_consolidado on consolidado_groups (consolidado_id);
create index idx_items_grupo on consolidado_items (grupo_id);
