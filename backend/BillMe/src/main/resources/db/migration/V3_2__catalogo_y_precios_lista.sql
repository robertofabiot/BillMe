-- Catálogo: proveedores, historial de precios de lista y productos genéricos.
-- El producto deja de guardar un precio fijo: el precio de lista vigente es el
-- último registro de precios_lista (importado, calculado o escrito a mano).

create extension if not exists pg_trgm;

create table proveedores (
    id         uuid                        not null primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    nombre     varchar(100)                not null unique,
    activo     boolean                     not null default true
);

insert into proveedores (id, created_at, updated_at, nombre)
values (gen_random_uuid(), now(), now(), 'Proveedor principal');

alter table productos
    drop column precio_lista_proveedor,
    drop column descuento_proveedor,
    drop column costo_neto,
    add column nombre_norm   varchar(200) not null default '',
    add column unidad        varchar(20)  not null default 'UND',
    add column familia       varchar(20),
    add column proveedor_id  uuid constraint fk_productos_proveedor references proveedores,
    add column generico      boolean      not null default false,
    add column forma_calculo varchar(20)  not null default 'NINGUNA'
        check (forma_calculo in ('NINGUNA', 'PLACA', 'PLATINA', 'ANGULAR')),
    add column activo        boolean      not null default true;

alter table alias_productos add column alias_norm varchar(200) not null default '';

-- Búsqueda tolerante a variantes de escritura (trigramas)
create index idx_productos_nombre_norm_trgm on productos using gin (nombre_norm gin_trgm_ops);
create index idx_alias_norm_trgm on alias_productos using gin (alias_norm gin_trgm_ops);
create index idx_productos_proveedor on productos (proveedor_id);

create table precios_lista (
    id             uuid                        not null primary key,
    created_at     timestamp(6) with time zone not null,
    updated_at     timestamp(6) with time zone not null,
    producto_id    uuid                        not null constraint fk_precios_lista_producto references productos,
    precio_sin_iva numeric(12, 4)              not null,
    fecha          date                        not null,
    fuente         varchar(30)                 not null
        check (fuente in ('LISTA_IMPORTADA', 'FACTURA_PROVEEDOR', 'PRECIO_CLIENTE', 'MANUAL')),
    descuento_pct  numeric(5, 2),
    precio_origen  numeric(12, 4),
    nota           text
);

create index idx_precios_lista_vigente on precios_lista (producto_id, fecha desc, created_at desc);

-- Productos genéricos: para lo que no tiene código de proveedor (se buscan por descripción)
insert into productos (id, created_at, updated_at, codigo_interno, nombre_principal, nombre_norm,
                       descripcion, unidad, generico, forma_calculo)
values (gen_random_uuid(), now(), now(), 'GEN-PLACA', 'Placa a medida', 'placa a medida',
        'Placa cortada a medida; el precio se calcula por peso', 'UND', true, 'PLACA'),
       (gen_random_uuid(), now(), now(), 'GEN-PLATINA', 'Platina a medida', 'platina a medida',
        'Platina cortada a medida; el precio se calcula por peso', 'UND', true, 'PLATINA'),
       (gen_random_uuid(), now(), now(), 'GEN-LIBRE', 'Producto o servicio libre', 'producto o servicio libre',
        'Para lo que no está en ninguna lista: descripción y precio se escriben a mano', 'UND', true, 'NINGUNA');
