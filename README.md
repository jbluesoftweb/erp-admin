# erpadmin

ERP administrativo genérico: gestión de personal, asistencia y reportes
consolidados para cualquier institución u organización — pública o privada.

Todo texto visible en pantalla (nombres de niveles, especialidades,
departamentos, grupos de personal, etc.) es **personalizable por cada
institución** desde la propia aplicación, sin tocar código ni base de
datos. Ver [Personalización](#personalización) más abajo.

## Stack técnico

- Java 21 · Spring Boot 4.0.3
- Spring Data JPA / Hibernate
- Spring Security
- Thymeleaf + Bootstrap 5
- PostgreSQL 14
- Maven

## Requisitos

- JDK 21+
- PostgreSQL 14+
- Maven (o usar el wrapper incluido, `./mvnw`)

## Puesta en marcha (desarrollo local)

1. Crear la base de datos en PostgreSQL:

   ```bash
   createdb -U postgres tu_base
   ```

2. Crear el esquema completo (tablas, vistas, triggers) y los datos
   iniciales para poder iniciar sesión por primera vez:

   ```bash
   psql -U postgres -d tu_base -f db/init-schema.sql
   psql -U postgres -d tu_base -f db/seed_admin_sys.sql
   ```

   Ver [Datos iniciales](#datos-iniciales-primer-usuario-admin_sys) abajo
   para el detalle de qué crea el segundo script y con qué usuario
   podrás loguearte.

3. Configurar las variables de entorno de conexión antes de levantar la
   aplicación:

   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/tu_base
   export DB_USERNAME=tu_usuario
   export DB_PASSWORD=tu_password
   ./mvnw spring-boot:run
   ```

   (`DB_URL` y `DB_USERNAME` tienen un valor por defecto en
   `application.properties` para desarrollo local; `DB_PASSWORD` es
   obligatorio y nunca debe hardcodearse ni commitearse.)

4. La aplicación queda disponible en `http://localhost:8080`.

> **Importante:** `spring.jpa.hibernate.ddl-auto` debe quedar siempre en
> `none`. La aplicación nunca crea ni modifica el esquema por sí misma —
> eso es responsabilidad de `db/init-schema.sql` y, para instalaciones ya
> existentes, del historial de migraciones incrementales (mantenido
> internamente, fuera de este repositorio). Usar `create` o `update`,
> aunque sea para pruebas, borra y recrea las tablas mapeadas por JPA en
> cada arranque — incluyendo los datos que haya cargado
> `seed_admin_sys.sql`.

### Datos iniciales (primer usuario ADMIN_SYS)

Una vez creado el esquema (`db/init-schema.sql`), corre el script de
datos semilla para poder iniciar sesión por primera vez:

```bash
psql -U tu_usuario -d tu_base -f db/seed_admin_sys.sql
```

Este script asume que las tablas de `rrhh`/`iam` ya existen y están
vacías (o sin estos códigos todavía). Crea una empresa, departamento,
tipo de personal, nivel y especialidad genéricos, un registro de
personal, el usuario y el vínculo usuario-rol-sistema necesario para
poder loguearse.

Login resultante:

| Campo | Valor |
|---|---|
| Código de usuario | `000000001` |
| Contraseña | `Peru123` |

Esa contraseña es reconocida por la aplicación como "contraseña por
defecto" (ver `PasswordForceChangeInterceptor`), así que en el primer
login se forzará el cambio automáticamente — no hace falta configurar
nada adicional para eso.

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
| `ADMIN_APP` | Administración a nivel de aplicación. |
| `ADMIN_DPTO` / `JEFE_DPTO` | Administración/jefatura a nivel de departamento. |

## Licencia

MIT — ver [LICENSE](LICENSE).

## Contribuir

_Pendiente de definir el flujo de contribución para el repositorio
público._
