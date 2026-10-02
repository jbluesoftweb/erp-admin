-- ============================================================
-- seed_admin_sys.sql
--
-- Datos mínimos para que un usuario ADMIN_SYS pueda iniciar
-- sesión por primera vez en una instalación nueva.
--
-- REQUISITO: el esquema (tablas de rrhh/iam/core) ya debe existir
-- en la base de datos antes de correr este script. spring.jpa.
-- hibernate.ddl-auto=none, así que la aplicación NUNCA crea tablas.
-- Este script asume rrhh.empresa, rrhh.departamento, rrhh.tipo_personal,
-- rrhh.nivel, rrhh.especialidad, rrhh.personal, iam.usuario, iam.rol,
-- iam.sistema e iam.registro_sistema ya existen y están vacías (o al
-- menos sin estos códigos específicos todavía).
--
-- Orden de ejecución:
--   1. Crear el esquema (db/init-schema.sql o una BD ya migrada)
--   2. Correr este script
--   3. Levantar la aplicación y loguearse
--
-- Login resultante:
--   Código de usuario: 000000001
--   Contraseña:        Peru123
--   (la app detecta esta contraseña como "por defecto" y fuerza el
--    cambio inmediato en el primer login — no hace falta hacer nada
--    extra para eso, ya está en tu código)
-- ============================================================

BEGIN;

-- 1. Empresa (la institución/organización raíz)
INSERT INTO rrhh.empresa (descripcion_corta, descripcion_larga)
VALUES ('EMPRESA', 'Empresa por defecto');

-- 2. Departamento (al menos uno, para poder asignar personal)
INSERT INTO rrhh.departamento (descripcion_corta, descripcion_larga, enabled, id_empresa)
VALUES (
    'SISTEMAS', 'Departamento de Sistemas', true,
    (SELECT id_empresa FROM rrhh.empresa WHERE descripcion_corta = 'EMPRESA')
);

-- 3. Tipo de personal (requerido por nivel y especialidad)
INSERT INTO rrhh.tipo_personal (descripcion_corta, descripcion_larga)
VALUES ('ADMIN', 'Administrador');

-- 4. Nivel (requerido por personal)
INSERT INTO rrhh.nivel (id_tipo_personal, descripcion_corta, descripcion_larga, orden)
VALUES (
    (SELECT id_tipo_personal FROM rrhh.tipo_personal WHERE descripcion_corta = 'ADMIN'),
    'ADMIN', 'Administrador del Sistema', 1
);

-- 5. Especialidad (requerido por personal)
INSERT INTO rrhh.especialidad (id_tipo_personal, descripcion_corta, descripcion_larga)
VALUES (
    (SELECT id_tipo_personal FROM rrhh.tipo_personal WHERE descripcion_corta = 'ADMIN'),
    'TI', 'Tecnología de la Información'
);

-- 6. Personal (la persona real detrás del usuario admin)
INSERT INTO rrhh.personal (
    codigo, dni, ap_pat, ap_mat, nombres,
    id_nivel, id_especialidad, id_departamento,
    antiguedad, cargo, enabled, created_at, updated_at
)
VALUES (
    '000000001', '00000000', 'Admin', 'Sistema', 'Administrador',
    (SELECT id_nivel FROM rrhh.nivel WHERE descripcion_corta = 'ADMIN'),
    (SELECT id_especialidad FROM rrhh.especialidad WHERE descripcion_corta = 'TI'),
    (SELECT id_departamento FROM rrhh.departamento WHERE descripcion_corta = 'SISTEMAS'),
    0, 'Administrador del Sistema', true, now(), now()
);

-- 7. Usuario — codigo debe coincidir EXACTO con personal.codigo
--    (lo valida el trigger iam.validar_codigo_usuario()).
--    Password: "Peru123" ya hasheado en BCrypt. Es el valor que
--    PasswordForceChangeInterceptor reconoce como "por defecto" y
--    fuerza el cambio en el primer login. No reemplaces este hash
--    por otra contraseña si quieres conservar ese comportamiento.
INSERT INTO iam.usuario (codigo, password_hash, email, enabled, id_personal, created_at, updated_at)
VALUES (
    '000000001',
    '$2b$10$rAdQcn1Mp/amW/pONHHuJuxmnHOJxU3mcIkAys/5.n589GI0b9E7S',
    'admin@tudominio.com',
    true,
    (SELECT id_personal FROM rrhh.personal WHERE codigo = '000000001'),
    now(), now()
);

-- 8. Rol ADMIN_SYS — omite si ya existe (por ejemplo, en una BD
--    migrada desde una instalación anterior).
INSERT INTO iam.rol (codigo, descripcion)
SELECT 'ADMIN_SYS', 'Administrador del Sistema'
WHERE NOT EXISTS (SELECT 1 FROM iam.rol WHERE codigo = 'ADMIN_SYS');

-- 9. Sistema ASISTENCIA — igual, omite si ya existe.
INSERT INTO iam.sistema (codigo, descripcion_corta, descripcion_larga, url)
SELECT 'ASISTENCIA', 'Asistencia', 'Sistema de Registro de Asistencia', '/asistencia'
WHERE NOT EXISTS (SELECT 1 FROM iam.sistema WHERE codigo = 'ASISTENCIA');

-- 10. Asignación usuario-rol-sistema — SIN esta fila el login
--     falla con "Usuario sin permisos para ASISTENCIA"
--     (CustomUserDetailsService lo exige explícitamente).
INSERT INTO iam.registro_sistema (id_usuario, id_rol, id_sistema, created_at, created_by)
VALUES (
    (SELECT id_usuario FROM iam.usuario WHERE codigo = '000000001'),
    (SELECT id_rol FROM iam.rol WHERE codigo = 'ADMIN_SYS'),
    (SELECT id_sistema FROM iam.sistema WHERE codigo = 'ASISTENCIA'),
    now(), '000000001'
);

COMMIT;
