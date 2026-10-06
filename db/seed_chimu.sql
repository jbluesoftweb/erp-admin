-- ============================================================
-- seed_chimu.sql
--
-- Sucursal CHIMU de "Blue Security" (empresa de seguridad y
-- vigilancia) con su estructura y 20 trabajadores de ejemplo.
--
-- REQUISITO: el esquema ya existe (db/init-schema.sql).
-- Es idempotente: se puede correr más de una vez sin duplicar datos.
-- Alternativo a seed_admin_sys.sql (no hace falta correr ambos).
--
-- Estructura:
--   Sucursal (rrhh.empresa) ........ CHIMU
--   Departamentos .................. Dpto1, Dpto2, Dpto3
--   Tipos de personal (grupos) ..... VIGILANTE, SUPERVISOR
--   Niveles ........................ Tco (Técnico en Seguridad) -> VIGILANTE
--                                    ING (Ingeniero en Seguridad) -> SUPERVISOR
--   Especialidad ................... Sec (Seguridad), en ambos tipos
--   Códigos de trabajador .......... empiezan con 30 (9 dígitos)
--
-- Usuarios con acceso (contraseña inicial: Peru123, la app fuerza
-- el cambio en el primer login):
--   300000001  System         -> ADMIN_SYS
--   300000002  Administrador  -> ADMIN_APP
--   300000003  Supervisor Dpto1 -> ADMIN_DPTO (registra al personal de Dpto1)
--   300000004  Supervisor Dpto2 -> ADMIN_DPTO (registra al personal de Dpto2)
--   300000005  Supervisor Dpto3 -> ADMIN_DPTO (registra al personal de Dpto3)
-- Ocurrencias de asistencia: ASI (Asistió), PER (Permiso), HOS (Hospitalizado).
-- Etiquetas de pantalla (core.etiqueta): nivel, grupo1, grupo2, departamento, especialidad.
-- Los demás trabajadores (300000006 ... 300000020) no tienen usuario.
-- Los DNI y nombres son ficticios.
-- ============================================================

BEGIN;

-- 1. Sucursal
INSERT INTO rrhh.empresa (descripcion_corta, descripcion_larga)
SELECT 'CHIMU', 'Sucursal CHIMU - Blue Security'
WHERE NOT EXISTS (SELECT 1 FROM rrhh.empresa WHERE descripcion_corta = 'CHIMU');

-- 2. Departamentos de la sucursal
INSERT INTO rrhh.departamento (descripcion_corta, descripcion_larga, enabled, id_empresa)
SELECT v.corta, v.larga, true, (SELECT id_empresa FROM rrhh.empresa WHERE descripcion_corta = 'CHIMU')
FROM (VALUES ('Dpto1','Departamento 1'),('Dpto2','Departamento 2'),('Dpto3','Departamento 3')) AS v(corta, larga)
WHERE NOT EXISTS (SELECT 1 FROM rrhh.departamento d WHERE d.descripcion_corta = v.corta);

-- 3. Tipos de personal
INSERT INTO rrhh.tipo_personal (descripcion_corta, descripcion_larga)
SELECT v.corta, v.larga
FROM (VALUES ('VIGILANTE','Vigilante'),('SUPERVISOR','Supervisor')) AS v(corta, larga)
WHERE NOT EXISTS (SELECT 1 FROM rrhh.tipo_personal t WHERE t.descripcion_corta = v.corta);

-- 4. Niveles
INSERT INTO rrhh.nivel (id_tipo_personal, descripcion_corta, descripcion_larga, orden)
SELECT t.id_tipo_personal, v.corta, v.larga, v.orden
FROM (VALUES ('VIGILANTE','Tco','Técnico en Seguridad',1),
             ('SUPERVISOR','ING','Ingeniero en Seguridad',1)) AS v(tipo, corta, larga, orden)
JOIN rrhh.tipo_personal t ON t.descripcion_corta = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM rrhh.nivel n
                  WHERE n.id_tipo_personal = t.id_tipo_personal AND n.descripcion_corta = v.corta);

-- 5. Especialidad (Seguridad, en ambos tipos)
INSERT INTO rrhh.especialidad (id_tipo_personal, descripcion_corta, descripcion_larga)
SELECT t.id_tipo_personal, 'Sec', 'Seguridad'
FROM rrhh.tipo_personal t
WHERE t.descripcion_corta IN ('VIGILANTE','SUPERVISOR')
  AND NOT EXISTS (SELECT 1 FROM rrhh.especialidad e
                  WHERE e.id_tipo_personal = t.id_tipo_personal AND e.descripcion_corta = 'Sec');

-- 6. Personal (20 trabajadores; antiguedad en años)
INSERT INTO rrhh.personal (
    codigo, dni, ap_pat, ap_mat, nombres,
    id_nivel, id_especialidad, id_departamento,
    antiguedad, cargo, enabled, created_at, updated_at, created_by
)
SELECT v.codigo, v.dni, v.ap_pat, v.ap_mat, v.nombres,
       n.id_nivel, e.id_especialidad, d.id_departamento,
       v.antiguedad, v.cargo, true, now(), now(), '300000001'
FROM (VALUES
    ('300000001', '70000001', 'Sistema', 'Blue', 'System', 'SUPERVISOR', 'Dpto1', 8, 'Administrador del Sistema'),
    ('300000002', '70000002', 'Administrador', 'Blue', 'Administrador', 'SUPERVISOR', 'Dpto1', 10, 'Administrador'),
    ('300000003', '70000003', 'Ramos', 'Cruz', 'Ana María', 'SUPERVISOR', 'Dpto1', 6, 'Supervisor de Turno'),
    ('300000004', '70000004', 'Paredes', 'León', 'Pedro Pablo', 'SUPERVISOR', 'Dpto2', 3, 'Supervisor de Turno'),
    ('300000005', '70000005', 'Valdivia', 'Mendoza', 'Óscar Daniel', 'SUPERVISOR', 'Dpto3', 7, 'Supervisor de Turno'),
    ('300000006', '70000006', 'Ortiz', 'Ramos', 'Rosa Elena', 'VIGILANTE', 'Dpto1', 11, 'Vigilante'),
    ('300000007', '70000007', 'León', 'Espinoza', 'Juan Carlos', 'VIGILANTE', 'Dpto2', 1, 'Vigilante'),
    ('300000008', '70000008', 'Flores', 'Medina', 'Hugo Martín', 'VIGILANTE', 'Dpto3', 2, 'Vigilante'),
    ('300000009', '70000009', 'Vargas', 'Flores', 'Luz Marina', 'VIGILANTE', 'Dpto1', 9, 'Vigilante'),
    ('300000010', '70000010', 'Chávez', 'Torres', 'José Antonio', 'VIGILANTE', 'Dpto2', 2, 'Vigilante'),
    ('300000011', '70000011', 'Salazar', 'Paredes', 'Diego Armando', 'VIGILANTE', 'Dpto3', 6, 'Vigilante'),
    ('300000012', '70000012', 'Espinoza', 'Reyes', 'Jhon Alexander', 'VIGILANTE', 'Dpto1', 10, 'Vigilante'),
    ('300000013', '70000013', 'Reyes', 'Quispe', 'Miguel Ángel', 'VIGILANTE', 'Dpto2', 1, 'Vigilante'),
    ('300000014', '70000014', 'Silva', 'Vargas', 'César Augusto', 'VIGILANTE', 'Dpto3', 9, 'Vigilante'),
    ('300000015', '70000015', 'Huamán', 'Gutiérrez', 'Elmer Franco', 'VIGILANTE', 'Dpto1', 4, 'Vigilante'),
    ('300000016', '70000016', 'Mendoza', 'Valdivia', 'Jorge Luis', 'VIGILANTE', 'Dpto2', 1, 'Vigilante'),
    ('300000017', '70000017', 'Torres', 'Silva', 'Fernando Iván', 'VIGILANTE', 'Dpto3', 2, 'Vigilante'),
    ('300000018', '70000018', 'Gutiérrez', 'Rojas', 'Walter Jesús', 'VIGILANTE', 'Dpto1', 7, 'Vigilante'),
    ('300000019', '70000019', 'Cárdenas', 'Chávez', 'Carlos Enrique', 'VIGILANTE', 'Dpto2', 7, 'Vigilante'),
    ('300000020', '70000020', 'Cruz', 'Cárdenas', 'Ricardo André', 'VIGILANTE', 'Dpto3', 2, 'Vigilante')
) AS v(codigo, dni, ap_pat, ap_mat, nombres, tipo, dpto, antiguedad, cargo)
JOIN rrhh.tipo_personal t ON t.descripcion_corta = v.tipo
JOIN rrhh.nivel n ON n.id_tipo_personal = t.id_tipo_personal
                 AND n.descripcion_corta = CASE v.tipo WHEN 'VIGILANTE' THEN 'Tco' ELSE 'ING' END
JOIN rrhh.especialidad e ON e.id_tipo_personal = t.id_tipo_personal AND e.descripcion_corta = 'Sec'
JOIN rrhh.departamento d ON d.descripcion_corta = v.dpto
WHERE NOT EXISTS (SELECT 1 FROM rrhh.personal p WHERE p.codigo = v.codigo);

-- 7. Roles y sistema
INSERT INTO iam.rol (codigo, descripcion)
SELECT v.codigo, v.descripcion
FROM (VALUES ('ADMIN_SYS','Administrador del Sistema'),('ADMIN_APP','Administrador de la Aplicación'),('ADMIN_DPTO','Administrador de Departamento')) AS v(codigo, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM iam.rol r WHERE r.codigo = v.codigo);

INSERT INTO iam.sistema (codigo, descripcion_corta, descripcion_larga, url)
SELECT 'ASISTENCIA', 'Asistencia', 'Sistema de Registro de Asistencia', '/asistencia'
WHERE NOT EXISTS (SELECT 1 FROM iam.sistema WHERE codigo = 'ASISTENCIA');

-- 8. Usuarios (codigo = personal.codigo; lo exige el trigger).
--    Password "Peru123" en BCrypt: la app fuerza el cambio al primer login.
INSERT INTO iam.usuario (codigo, password_hash, email, enabled, id_personal, created_at, updated_at)
SELECT v.codigo, '$2b$10$rAdQcn1Mp/amW/pONHHuJuxmnHOJxU3mcIkAys/5.n589GI0b9E7S',
       v.email, true, p.id_personal, now(), now()
FROM (VALUES ('300000001','system@tudominio.com'),('300000002','admin@tudominio.com'),
             ('300000003','dpto1@tudominio.com'),('300000004','dpto2@tudominio.com'),('300000005','dpto3@tudominio.com')) AS v(codigo, email)
JOIN rrhh.personal p ON p.codigo = v.codigo
WHERE NOT EXISTS (SELECT 1 FROM iam.usuario u WHERE u.codigo = v.codigo);

-- 9. Usuario - rol - sistema (sin esto el login falla)
INSERT INTO iam.registro_sistema (id_usuario, id_rol, id_sistema, created_at, created_by)
SELECT u.id_usuario, r.id_rol, s.id_sistema, now(), '300000001'
FROM (VALUES ('300000001','ADMIN_SYS'),('300000002','ADMIN_APP'),
             ('300000003','ADMIN_DPTO'),('300000004','ADMIN_DPTO'),('300000005','ADMIN_DPTO')) AS v(codigo, rol)
JOIN iam.usuario u ON u.codigo = v.codigo
JOIN iam.rol r ON r.codigo = v.rol
JOIN iam.sistema s ON s.codigo = 'ASISTENCIA'
WHERE NOT EXISTS (SELECT 1 FROM iam.registro_sistema x
                  WHERE x.id_usuario = u.id_usuario AND x.id_rol = r.id_rol AND x.id_sistema = s.id_sistema);

-- 10. Ocurrencias de asistencia.
--     IMPORTANTE: la app asume que "Asistió" es el id_ocurrencia = 1 y
--     el código 'ASI' (valor por defecto al registrar; solo ASI tiene
--     almuerzo = true). Por eso se inserta PRIMERO, y el bloque final
--     aborta todo (ROLLBACK) si no quedó con id 1.
--     tipo: DISPONIBLE (cuenta como disponible) / DESCUENTO (se descuenta).
INSERT INTO asistencia.ocurrencia (codigo, descripcion, tipo, color, orden, enabled, almuerzo)
SELECT 'ASI', 'Asistió', 'DISPONIBLE', '#28a745', 1, true, true
WHERE NOT EXISTS (SELECT 1 FROM asistencia.ocurrencia WHERE codigo = 'ASI');

INSERT INTO asistencia.ocurrencia (codigo, descripcion, tipo, color, orden, enabled, almuerzo)
SELECT v.codigo, v.descripcion, 'DESCUENTO', v.color, v.orden, true, false
FROM (VALUES ('PER','Permiso','#ffc107',2),
             ('HOS','Hospitalizado','#dc3545',3)) AS v(codigo, descripcion, color, orden)
WHERE NOT EXISTS (SELECT 1 FROM asistencia.ocurrencia o WHERE o.codigo = v.codigo);

DO $$
BEGIN
    IF (SELECT id_ocurrencia FROM asistencia.ocurrencia WHERE codigo = 'ASI') <> 1 THEN
        RAISE EXCEPTION 'La ocurrencia ASI (Asistió) debe tener id_ocurrencia = 1 (la app lo asume). Usa una base de datos nueva.';
    END IF;
END $$;

-- 11. Etiquetas de pantalla (core.etiqueta). La app lee estas 5 claves;
--     sin ellas los títulos y columnas salen vacíos. La app las carga en
--     memoria al arrancar: reinícela después de correr este script.
--     grupo1 = tipo_personal id 1 (VIGILANTE), grupo2 = id 2 (SUPERVISOR):
--     la app agrupa por id, por eso VIGILANTE se inserta primero.
INSERT INTO core.etiqueta (clave, valor_defecto, valor_personalizado, descripcion)
VALUES
    ('rrhh.personal.nivel',        'Nivel',        'Nivel',        'Nivel del personal'),
    ('rrhh.personal.grupo1',       'Grupo 1',      'Vigilantes',   'Grupo 1 de personal (tipo de personal id 1)'),
    ('rrhh.personal.grupo2',       'Grupo 2',      'Supervisores', 'Grupo 2 de personal (tipo de personal id 2)'),
    ('rrhh.personal.departamento', 'Departamento', 'Departamento', 'Departamento del personal'),
    ('rrhh.personal.especialidad', 'Especialidad', 'Especialidad', 'Especialidad del personal')
ON CONFLICT (clave) DO NOTHING;

COMMIT;
