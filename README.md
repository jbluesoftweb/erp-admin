# erpadmin

ERP administrativo genérico: gestión de personal, asistencia y reportes
consolidados para cualquier institución u organización — pública o privada.

Todo texto visible en pantalla (nombres de niveles, especialidades,
departamentos, grupos de personal, etc.) es **personalizable por cada
institución** desde la propia aplicación, sin tocar código ni base de
datos. Ver [Personalización](#personalización) más abajo.

## Stack técnico

- Java 25 · Spring Boot 4.0.3
- Spring Data JPA / Hibernate
- Spring Security
- Thymeleaf + Bootstrap 5
- PostgreSQL 18
- Maven

## Requisitos

- JDK 25
- PostgreSQL 18
- Maven (o usar el wrapper incluido, `./mvnw`)

## Puesta en marcha (desarrollo local)

1. Crear un usuario para la aplicación y una base de datos a su nombre
   (desde la consola de administración de PostgreSQL, por ejemplo
   `sudo -u postgres psql`):

   ```sql
   CREATE ROLE tu_usuario LOGIN;
   \password tu_usuario
   CREATE DATABASE tu_base OWNER tu_usuario;
   ```

2. Crear el esquema completo (tablas, vistas, triggers) y cargar los
   datos de ejemplo para poder iniciar sesión por primera vez,
   ejecutando los scripts con ese mismo usuario y **en este orden**:

   ```bash
   psql -h localhost -U tu_usuario -d tu_base -f db/init-schema.sql
   psql -h localhost -U tu_usuario -d tu_base -f db/seed_chimu.sql
   ```

   Usa una base de datos nueva y vacía: la aplicación asume que la
   ocurrencia "Asistió" es la número 1. Ver
   [Datos de ejemplo](#datos-de-ejemplo-sucursal-chimu) abajo para el
   detalle de qué crea el segundo script y con qué usuarios podrás
   loguearte.

3. Configurar las variables de entorno de conexión antes de levantar la
   aplicación:

   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/tu_base
   export DB_USERNAME=tu_usuario
   export DB_PASSWORD=tu_password
   ./mvnw spring-boot:run
   ```

   (Las tres variables son obligatorias: no hay valores por defecto. La
   contraseña nunca debe escribirse en `application.properties` ni
   commitearse. En IntelliJ IDEA se definen en *Run → Edit
   Configurations → Environment variables*.)

4. La aplicación queda disponible en `http://localhost:8080`.

> **Importante:** `spring.jpa.hibernate.ddl-auto` debe quedar siempre en
> `none`. La aplicación nunca crea ni modifica el esquema por sí misma —
> eso es responsabilidad de `db/init-schema.sql` y, para instalaciones ya
> existentes, del historial de migraciones incrementales (mantenido
> internamente, fuera de este repositorio). Usar `create` o `update`,
> aunque sea para pruebas, borra y recrea las tablas mapeadas por JPA en
> cada arranque — incluyendo los datos que haya cargado
> `seed_chimu.sql`.

### Datos de ejemplo (sucursal CHIMU)

`db/seed_chimu.sql` carga un caso de ejemplo completo: la sucursal CHIMU
de una empresa ficticia de seguridad y vigilancia ("Blue Security"), con
sus departamentos, personal y usuarios. Es idempotente: puede ejecutarse
más de una vez sin duplicar datos. Todos los nombres y DNI son ficticios.
Para tu propia organización, parte de este archivo y cámbiale los datos.

| Elemento | Contenido |
|---|---|
| Sucursal (`rrhh.empresa`) | `CHIMU` |
| Departamentos | `Dpto1`, `Dpto2`, `Dpto3` |
| Tipos de personal (grupos) | `VIGILANTE`, `SUPERVISOR` |
| Niveles | `Tco` (Técnico en Seguridad) para vigilantes; `ING` (Ingeniero en Seguridad) para supervisores |
| Especialidad | `Sec` (Seguridad), en ambos tipos |
| Personal | 20 trabajadores, códigos `300000001` a `300000020`, repartidos entre los 3 departamentos |
| Ocurrencias de asistencia | `ASI` Asistió, `PER` Permiso, `HOS` Hospitalizado |
| Etiquetas de pantalla | nivel, grupo 1 (Vigilantes), grupo 2 (Supervisores), departamento, especialidad |

Usuarios con acceso (contraseña inicial **`Peru123`**; la aplicación
fuerza el cambio en el primer login, ver `PasswordForceChangeInterceptor`):

| Código | Trabajador | Rol |
|---|---|---|
| `300000001` | System | `ADMIN_SYS` |
| `300000002` | Administrador | `ADMIN_APP` |
| `300000003` | Supervisor de Dpto1 | `ADMIN_DPTO` |
| `300000004` | Supervisor de Dpto2 | `ADMIN_DPTO` |
| `300000005` | Supervisor de Dpto3 | `ADMIN_DPTO` |

El resto del personal no tiene usuario: lo registra el `ADMIN_DPTO` de su
departamento.

> **Notas:** (1) La aplicación asume que la ocurrencia "Asistió" tiene
> `id_ocurrencia = 1` y código `ASI`, y que el grupo 1 es el tipo de
> personal con id 1 y el grupo 2 el de id 2. El script ya lo respeta si
> se ejecuta sobre una base nueva. (2) Las etiquetas se cargan en memoria
> al arrancar: si las modificas directamente en la base, reinicia la
> aplicación.

## Personalización

El sistema no tiene textos hardcodeados de un rubro específico. Cada
institución configura sus propias etiquetas (nombres de campos que se
muestran en pantalla) desde:

**Menú → SISTEMA → Administrar Etiquetas** (requiere rol `ADMIN_SYS`)

Ahí se puede cambiar, por ejemplo, cómo se llama el "nivel jerárquico" del
personal, la "especialidad", el "departamento" o los dos grupos en que se
divide el personal — sin necesidad de reiniciar la aplicación ni tocar la
base de datos directamente.

## Roles del sistema

| Rol | Alcance |
|---|---|
| `ADMIN_SYS` | Administración completa: usuarios, roles, etiquetas del sistema. |
| `ADMIN_APP` | Administración de la sucursal: crea y gestiona usuarios `ADMIN_DPTO`. |
| `ADMIN_DPTO` | Registra y consulta al personal y la asistencia de su propio departamento. |

## Licencia

MIT — ver [LICENSE](LICENSE).

## Contribuir

_Pendiente de definir el flujo de contribución para el repositorio
público._
