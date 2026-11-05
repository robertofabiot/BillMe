-- Compra al proveedor ligada a una factura: da el costo real de sus líneas.
create table compras (
    id             uuid                        not null primary key,
    created_at     timestamp(6) with time zone not null,
    updated_at     timestamp(6) with time zone not null,
    proveedor_id   uuid                        not null constraint fk_compras_proveedor references proveedores,
    documento_id   uuid                        not null constraint fk_compras_documento references documentos,
    fecha          date                        not null,
    numero_factura varchar(50),
    nota           text
);

create index idx_compras_documento on compras (documento_id);

alter table documento_lineas add column compra_id uuid constraint fk_lineas_compra references compras;
create index idx_lineas_compra on documento_lineas (compra_id);
