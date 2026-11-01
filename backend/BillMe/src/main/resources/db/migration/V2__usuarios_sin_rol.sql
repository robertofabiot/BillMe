-- Todos los usuarios tienen los mismos permisos: se elimina el rol.
alter table usuarios drop column rol;

-- Permite desactivar a un usuario sin borrarlo (sus facturas lo siguen referenciando).
alter table usuarios add column activo boolean not null default true;
