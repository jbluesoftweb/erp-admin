--
-- init-schema.sql
--
-- Esquema completo de erpadmin para una instalación nueva.
-- Generado a partir de un pg_dump --schema-only de una base de desarrollo,
-- y luego limpiado:
--   - Se removió el esquema "imint" y todos sus objetos: pertenecen a
--     IMINT App, una aplicación distinta que convivía en la misma base
--     de datos del servidor original, no a erpadmin.
--   - Se removió la tabla asistencia.autorizacion_registro_legado y
--     todos sus objetos relacionados: era una tabla legado sin entidad
--     Java en el código de erpadmin (su eliminación ya estaba prevista
--     en el historial interno de migraciones).
--   - Se reescribieron dos comentarios de columna/tabla que tenían
--     texto específico de un ámbito militar ("Personal militar activo",
--     "Snapshot del arma...") por texto genérico, acorde al resto del
--     proyecto.
--
-- Uso:
--   psql -U tu_usuario -d tu_base -f db/init-schema.sql
--   psql -U tu_usuario -d tu_base -f db/seed_admin_sys.sql
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Esquemas
--

CREATE SCHEMA asistencia;
CREATE SCHEMA core;
CREATE SCHEMA iam;
CREATE SCHEMA rrhh;

--
-- Name: validar_codigo_usuario(); Type: FUNCTION; Schema: iam
--

CREATE FUNCTION iam.validar_codigo_usuario() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    codigo_personal CHAR(9);
BEGIN
    SELECT p.codigo INTO codigo_personal
    FROM rrhh.personal p
    WHERE p.id_personal = NEW.id_personal;

    IF codigo_personal IS NULL THEN
        RAISE EXCEPTION 'El id_personal % no existe en la tabla personal.', NEW.id_personal;
    END IF;

    IF NEW.codigo != codigo_personal THEN
        RAISE EXCEPTION 'El código % del usuario no coincide con el código % del personal asignado (id_personal: %).',
            NEW.codigo, codigo_personal, NEW.id_personal;
    END IF;

    RETURN NEW;
END;
$$;

SET default_tablespace = '';
SET default_table_access_method = heap;

--
-- Name: ocurrencia; Type: TABLE; Schema: asistencia
--

CREATE TABLE asistencia.ocurrencia (
    id_ocurrencia integer NOT NULL,
    codigo character varying(10) NOT NULL,
    descripcion character varying(50) NOT NULL,
    tipo character varying(20) NOT NULL,
    color character varying(7),
    orden integer,
    enabled boolean DEFAULT true,
    almuerzo boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_tipo CHECK (((tipo)::text = ANY (ARRAY[('DISPONIBLE'::character varying)::text, ('DESCUENTO'::character varying)::text])))
);

CREATE SEQUENCE asistencia.ocurrencia_id_ocurrencia_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE asistencia.ocurrencia_id_ocurrencia_seq OWNED BY asistencia.ocurrencia.id_ocurrencia;

--
-- Name: registro_diario; Type: TABLE; Schema: asistencia
--

CREATE TABLE asistencia.registro_diario (
    id_registro integer NOT NULL,
    fecha_registro date DEFAULT CURRENT_DATE NOT NULL,
    id_personal integer NOT NULL,
    codigo character(9) NOT NULL,
    antiguedad integer NOT NULL,
    id_departamento integer NOT NULL,
    id_nivel integer NOT NULL,
    nivel_snapshot character varying(50),
    id_especialidad integer NOT NULL,
    especialidad_snapshot character varying(50),
    full_name character varying(150),
    id_ocurrencia integer NOT NULL,
    detalle character varying(255),
    fecha_inicio date,
    fecha_termino date,
    created_at timestamp without time zone DEFAULT now(),
    created_by character(9)
);

COMMENT ON TABLE asistencia.registro_diario IS 'Registro diario de asistencia y ocurrencias';
COMMENT ON COLUMN asistencia.registro_diario.nivel_snapshot IS 'Snapshot del nivel al momento del registro';
COMMENT ON COLUMN asistencia.registro_diario.especialidad_snapshot IS 'Snapshot de la especialidad al momento del registro';

CREATE SEQUENCE asistencia.registro_diario_id_registro_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE asistencia.registro_diario_id_registro_seq OWNED BY asistencia.registro_diario.id_registro;

--
-- Name: departamento; Type: TABLE; Schema: rrhh
--

CREATE TABLE rrhh.departamento (
    id_departamento integer NOT NULL,
    descripcion_corta character varying(50) NOT NULL,
    descripcion_larga character varying(100) NOT NULL,
    enabled boolean DEFAULT true,
    id_empresa integer
);

--
-- Name: nivel; Type: TABLE; Schema: rrhh
--

CREATE TABLE rrhh.nivel (
    id_nivel integer NOT NULL,
    id_tipo_personal integer NOT NULL,
    descripcion_corta character varying(50) NOT NULL,
    descripcion_larga character varying(100) NOT NULL,
    orden integer NOT NULL
);

--
-- Name: tipo_personal; Type: TABLE; Schema: rrhh
--

CREATE TABLE rrhh.tipo_personal (
    id_tipo_personal integer NOT NULL,
    descripcion_corta character varying(10) NOT NULL,
    descripcion_larga character varying(50) NOT NULL
);

--
-- Name: v_resumen_registro; Type: VIEW; Schema: asistencia
--

CREATE VIEW asistencia.v_resumen_registro AS
 SELECT rd.fecha_registro,
    rd.id_departamento,
    d.descripcion_corta AS departamento,
    tp.descripcion_corta AS tipo_persona,
    count(*) AS total,
    sum(
        CASE
            WHEN ((o.tipo)::text = 'DISPONIBLE'::text) THEN 1
            ELSE 0
        END) AS disponibles,
    sum(
        CASE
            WHEN ((o.tipo)::text = 'DESCUENTO'::text) THEN 1
            ELSE 0
        END) AS descuentos
   FROM ((((asistencia.registro_diario rd
     JOIN rrhh.departamento d ON ((rd.id_departamento = d.id_departamento)))
     JOIN rrhh.nivel n ON ((rd.id_nivel = n.id_nivel)))
     JOIN rrhh.tipo_personal tp ON ((n.id_tipo_personal = tp.id_tipo_personal)))
     JOIN asistencia.ocurrencia o ON ((rd.id_ocurrencia = o.id_ocurrencia)))
  GROUP BY rd.fecha_registro, rd.id_departamento, d.descripcion_corta, tp.descripcion_corta;

--
-- Name: etiqueta; Type: TABLE; Schema: core
--

CREATE TABLE core.etiqueta (
    clave character varying(100) NOT NULL,
    valor_defecto character varying(100) NOT NULL,
    valor_personalizado character varying(100),
    descripcion character varying(255),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(100)
);

--
-- Name: registro_sistema; Type: TABLE; Schema: iam
--

CREATE TABLE iam.registro_sistema (
    id_registro integer NOT NULL,
    id_usuario integer NOT NULL,
    id_rol integer NOT NULL,
    id_sistema integer NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    created_by character(9)
);

CREATE SEQUENCE iam.registro_sistema_id_registro_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE iam.registro_sistema_id_registro_seq OWNED BY iam.registro_sistema.id_registro;

--
-- Name: rol; Type: TABLE; Schema: iam
--

CREATE TABLE iam.rol (
    id_rol integer NOT NULL,
    codigo character varying(20) NOT NULL,
    descripcion character varying(255) NOT NULL
);

CREATE SEQUENCE iam.rol_id_rol_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE iam.rol_id_rol_seq OWNED BY iam.rol.id_rol;

--
-- Name: sistema; Type: TABLE; Schema: iam
--

CREATE TABLE iam.sistema (
    id_sistema integer NOT NULL,
    codigo character varying(50) NOT NULL,
    descripcion_corta character varying(50) NOT NULL,
    descripcion_larga character varying(100) NOT NULL,
    url character varying(255)
);

CREATE SEQUENCE iam.sistema_id_sistema_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE iam.sistema_id_sistema_seq OWNED BY iam.sistema.id_sistema;

--
-- Name: usuario; Type: TABLE; Schema: iam
--

CREATE TABLE iam.usuario (
    id_usuario integer NOT NULL,
    codigo character(9) NOT NULL,
    password_hash character varying(255) NOT NULL,
    email character varying(100),
    enabled boolean DEFAULT true,
    id_personal integer,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);

CREATE SEQUENCE iam.usuario_id_usuario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE iam.usuario_id_usuario_seq OWNED BY iam.usuario.id_usuario;

--
-- Name: departamento_id_departamento_seq; Type: SEQUENCE; Schema: rrhh
--

CREATE SEQUENCE rrhh.departamento_id_departamento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rrhh.departamento_id_departamento_seq OWNED BY rrhh.departamento.id_departamento;

--
-- Name: empresa; Type: TABLE; Schema: rrhh
--

CREATE TABLE rrhh.empresa (
    id_empresa integer NOT NULL,
    descripcion_corta character varying(50),
    descripcion_larga character varying(200)
);

CREATE SEQUENCE rrhh.empresa_id_empresa_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rrhh.empresa_id_empresa_seq OWNED BY rrhh.empresa.id_empresa;

--
-- Name: especialidad; Type: TABLE; Schema: rrhh
--

CREATE TABLE rrhh.especialidad (
    id_especialidad integer NOT NULL,
    id_tipo_personal integer NOT NULL,
    descripcion_corta character varying(50) NOT NULL,
    descripcion_larga character varying(100) NOT NULL
);

CREATE SEQUENCE rrhh.especialidad_id_especialidad_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rrhh.especialidad_id_especialidad_seq OWNED BY rrhh.especialidad.id_especialidad;

CREATE SEQUENCE rrhh.nivel_id_nivel_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rrhh.nivel_id_nivel_seq OWNED BY rrhh.nivel.id_nivel;

--
-- Name: personal; Type: TABLE; Schema: rrhh
--

CREATE TABLE rrhh.personal (
    id_personal integer NOT NULL,
    codigo character(9) NOT NULL,
    dni character(8) NOT NULL,
    ap_pat character varying(50) NOT NULL,
    ap_mat character varying(50) NOT NULL,
    nombres character varying(100) NOT NULL,
    id_nivel integer NOT NULL,
    id_especialidad integer NOT NULL,
    id_departamento integer NOT NULL,
    antiguedad integer NOT NULL,
    cargo character varying(50),
    enabled boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by character(9)
);

COMMENT ON TABLE rrhh.personal IS 'Personal activo de la organización';

CREATE SEQUENCE rrhh.personal_id_personal_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rrhh.personal_id_personal_seq OWNED BY rrhh.personal.id_personal;

CREATE SEQUENCE rrhh.tipo_personal_id_tipo_personal_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rrhh.tipo_personal_id_tipo_personal_seq OWNED BY rrhh.tipo_personal.id_tipo_personal;

--
-- Name: v_personal_completo; Type: VIEW; Schema: rrhh
--

CREATE VIEW rrhh.v_personal_completo AS
 SELECT p.id_personal,
    p.codigo,
    p.dni,
    (((((p.ap_pat)::text || ' '::text) || (p.ap_mat)::text) || ' '::text) || (p.nombres)::text) AS full_name,
    p.ap_pat,
    p.ap_mat,
    p.nombres,
    n.descripcion_corta AS nivel,
    n.descripcion_larga AS nivel_largo,
    e.descripcion_corta AS especialidad,
    e.descripcion_larga AS especialidad_larga,
    d.descripcion_corta AS departamento,
    tp.descripcion_corta AS tipo_persona,
    p.antiguedad,
    p.cargo,
    p.enabled
   FROM ((((rrhh.personal p
     JOIN rrhh.nivel n ON ((p.id_nivel = n.id_nivel)))
     JOIN rrhh.especialidad e ON ((p.id_especialidad = e.id_especialidad)))
     JOIN rrhh.departamento d ON ((p.id_departamento = d.id_departamento)))
     JOIN rrhh.tipo_personal tp ON ((n.id_tipo_personal = tp.id_tipo_personal)));

--
-- Defaults (nextval)
--

ALTER TABLE ONLY asistencia.ocurrencia ALTER COLUMN id_ocurrencia SET DEFAULT nextval('asistencia.ocurrencia_id_ocurrencia_seq'::regclass);
ALTER TABLE ONLY asistencia.registro_diario ALTER COLUMN id_registro SET DEFAULT nextval('asistencia.registro_diario_id_registro_seq'::regclass);
ALTER TABLE ONLY iam.registro_sistema ALTER COLUMN id_registro SET DEFAULT nextval('iam.registro_sistema_id_registro_seq'::regclass);
ALTER TABLE ONLY iam.rol ALTER COLUMN id_rol SET DEFAULT nextval('iam.rol_id_rol_seq'::regclass);
ALTER TABLE ONLY iam.sistema ALTER COLUMN id_sistema SET DEFAULT nextval('iam.sistema_id_sistema_seq'::regclass);
ALTER TABLE ONLY iam.usuario ALTER COLUMN id_usuario SET DEFAULT nextval('iam.usuario_id_usuario_seq'::regclass);
ALTER TABLE ONLY rrhh.departamento ALTER COLUMN id_departamento SET DEFAULT nextval('rrhh.departamento_id_departamento_seq'::regclass);
ALTER TABLE ONLY rrhh.empresa ALTER COLUMN id_empresa SET DEFAULT nextval('rrhh.empresa_id_empresa_seq'::regclass);
ALTER TABLE ONLY rrhh.especialidad ALTER COLUMN id_especialidad SET DEFAULT nextval('rrhh.especialidad_id_especialidad_seq'::regclass);
ALTER TABLE ONLY rrhh.nivel ALTER COLUMN id_nivel SET DEFAULT nextval('rrhh.nivel_id_nivel_seq'::regclass);
ALTER TABLE ONLY rrhh.personal ALTER COLUMN id_personal SET DEFAULT nextval('rrhh.personal_id_personal_seq'::regclass);
ALTER TABLE ONLY rrhh.tipo_personal ALTER COLUMN id_tipo_personal SET DEFAULT nextval('rrhh.tipo_personal_id_tipo_personal_seq'::regclass);

--
-- Primary keys / unique constraints
--

ALTER TABLE ONLY asistencia.ocurrencia ADD CONSTRAINT ocurrencia_codigo_key UNIQUE (codigo);
ALTER TABLE ONLY asistencia.ocurrencia ADD CONSTRAINT ocurrencia_pkey PRIMARY KEY (id_ocurrencia);
ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_fecha_registro_codigo_key UNIQUE (fecha_registro, codigo);
ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_pkey PRIMARY KEY (id_registro);

ALTER TABLE ONLY core.etiqueta ADD CONSTRAINT etiqueta_pkey PRIMARY KEY (clave);

ALTER TABLE ONLY iam.registro_sistema ADD CONSTRAINT registro_sistema_id_usuario_id_rol_id_sistema_key UNIQUE (id_usuario, id_rol, id_sistema);
ALTER TABLE ONLY iam.registro_sistema ADD CONSTRAINT registro_sistema_pkey PRIMARY KEY (id_registro);
ALTER TABLE ONLY iam.rol ADD CONSTRAINT rol_codigo_key UNIQUE (codigo);
ALTER TABLE ONLY iam.rol ADD CONSTRAINT rol_pkey PRIMARY KEY (id_rol);
ALTER TABLE ONLY iam.sistema ADD CONSTRAINT sistema_codigo_key UNIQUE (codigo);
ALTER TABLE ONLY iam.sistema ADD CONSTRAINT sistema_pkey PRIMARY KEY (id_sistema);
ALTER TABLE ONLY iam.usuario ADD CONSTRAINT usuario_codigo_key UNIQUE (codigo);
ALTER TABLE ONLY iam.usuario ADD CONSTRAINT usuario_email_key UNIQUE (email);
ALTER TABLE ONLY iam.usuario ADD CONSTRAINT usuario_id_personal_key UNIQUE (id_personal);
ALTER TABLE ONLY iam.usuario ADD CONSTRAINT usuario_pkey PRIMARY KEY (id_usuario);

ALTER TABLE ONLY rrhh.departamento ADD CONSTRAINT departamento_descripcion_corta_key UNIQUE (descripcion_corta);
ALTER TABLE ONLY rrhh.departamento ADD CONSTRAINT departamento_pkey PRIMARY KEY (id_departamento);
ALTER TABLE ONLY rrhh.empresa ADD CONSTRAINT empresa_pkey PRIMARY KEY (id_empresa);
ALTER TABLE ONLY rrhh.especialidad ADD CONSTRAINT especialidad_id_tipo_personal_descripcion_corta_key UNIQUE (id_tipo_personal, descripcion_corta);
ALTER TABLE ONLY rrhh.especialidad ADD CONSTRAINT especialidad_pkey PRIMARY KEY (id_especialidad);
ALTER TABLE ONLY rrhh.nivel ADD CONSTRAINT nivel_id_tipo_personal_descripcion_corta_key UNIQUE (id_tipo_personal, descripcion_corta);
ALTER TABLE ONLY rrhh.nivel ADD CONSTRAINT nivel_pkey PRIMARY KEY (id_nivel);
ALTER TABLE ONLY rrhh.personal ADD CONSTRAINT personal_codigo_key UNIQUE (codigo);
ALTER TABLE ONLY rrhh.personal ADD CONSTRAINT personal_dni_key UNIQUE (dni);
ALTER TABLE ONLY rrhh.personal ADD CONSTRAINT personal_pkey PRIMARY KEY (id_personal);
ALTER TABLE ONLY rrhh.tipo_personal ADD CONSTRAINT tipo_personal_descripcion_corta_key UNIQUE (descripcion_corta);
ALTER TABLE ONLY rrhh.tipo_personal ADD CONSTRAINT tipo_personal_pkey PRIMARY KEY (id_tipo_personal);

--
-- Indexes
--

CREATE INDEX idx_registro_codigo ON asistencia.registro_diario USING btree (codigo);
CREATE INDEX idx_registro_departamento ON asistencia.registro_diario USING btree (id_departamento);
CREATE INDEX idx_registro_fecha ON asistencia.registro_diario USING btree (fecha_registro);
CREATE INDEX idx_registro_fecha_departamento ON asistencia.registro_diario USING btree (fecha_registro, id_departamento);
CREATE INDEX idx_registro_ocurrencia ON asistencia.registro_diario USING btree (id_ocurrencia);

CREATE INDEX idx_personal_departamento ON rrhh.personal USING btree (id_departamento);
CREATE INDEX idx_personal_enabled ON rrhh.personal USING btree (enabled) WHERE (enabled = true);
CREATE INDEX idx_personal_nivel ON rrhh.personal USING btree (id_nivel);

--
-- Triggers
--

CREATE TRIGGER trg_validar_codigo_usuario BEFORE INSERT OR UPDATE ON iam.usuario FOR EACH ROW EXECUTE FUNCTION iam.validar_codigo_usuario();

--
-- Foreign keys
--

ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_id_departamento_fkey FOREIGN KEY (id_departamento) REFERENCES rrhh.departamento(id_departamento);
ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_id_especialidad_fkey FOREIGN KEY (id_especialidad) REFERENCES rrhh.especialidad(id_especialidad);
ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_id_nivel_fkey FOREIGN KEY (id_nivel) REFERENCES rrhh.nivel(id_nivel);
ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_id_ocurrencia_fkey FOREIGN KEY (id_ocurrencia) REFERENCES asistencia.ocurrencia(id_ocurrencia);
ALTER TABLE ONLY asistencia.registro_diario ADD CONSTRAINT registro_diario_id_personal_fkey FOREIGN KEY (id_personal) REFERENCES rrhh.personal(id_personal);

ALTER TABLE ONLY iam.registro_sistema ADD CONSTRAINT registro_sistema_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES iam.rol(id_rol);
ALTER TABLE ONLY iam.registro_sistema ADD CONSTRAINT registro_sistema_id_sistema_fkey FOREIGN KEY (id_sistema) REFERENCES iam.sistema(id_sistema);
ALTER TABLE ONLY iam.registro_sistema ADD CONSTRAINT registro_sistema_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES iam.usuario(id_usuario);
ALTER TABLE ONLY iam.usuario ADD CONSTRAINT usuario_id_personal_fkey FOREIGN KEY (id_personal) REFERENCES rrhh.personal(id_personal);

ALTER TABLE ONLY rrhh.especialidad ADD CONSTRAINT especialidad_id_tipo_personal_fkey FOREIGN KEY (id_tipo_personal) REFERENCES rrhh.tipo_personal(id_tipo_personal);
ALTER TABLE ONLY rrhh.departamento ADD CONSTRAINT fk_departamento_empresa FOREIGN KEY (id_empresa) REFERENCES rrhh.empresa(id_empresa);
ALTER TABLE ONLY rrhh.nivel ADD CONSTRAINT nivel_id_tipo_personal_fkey FOREIGN KEY (id_tipo_personal) REFERENCES rrhh.tipo_personal(id_tipo_personal);
ALTER TABLE ONLY rrhh.personal ADD CONSTRAINT personal_id_departamento_fkey FOREIGN KEY (id_departamento) REFERENCES rrhh.departamento(id_departamento);
ALTER TABLE ONLY rrhh.personal ADD CONSTRAINT personal_id_especialidad_fkey FOREIGN KEY (id_especialidad) REFERENCES rrhh.especialidad(id_especialidad);
ALTER TABLE ONLY rrhh.personal ADD CONSTRAINT personal_id_nivel_fkey FOREIGN KEY (id_nivel) REFERENCES rrhh.nivel(id_nivel);

--
-- Fin
--
