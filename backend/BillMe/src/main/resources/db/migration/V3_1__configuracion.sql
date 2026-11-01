-- Parámetros del negocio que se editan desde la pantalla de Configuración.
create table configuracion (
    clave       varchar(50)  not null primary key,
    valor       varchar(100) not null,
    descripcion varchar(200) not null
);

insert into configuracion (clave, valor, descripcion) values
    ('iva_pct', '15', 'IVA en porcentaje'),
    ('tipo_cambio', '36.6243', 'Córdobas por dólar'),
    ('tarifa_kg_usd', '1.80', 'Tarifa por kg por defecto para placas y platinas (USD, sin IVA)'),
    ('densidad_kg_in3', '0.129', 'Densidad del acero en kg por pulgada cúbica'),
    ('dias_precio_viejo', '365', 'Días a partir de los cuales un precio se marca como viejo');
