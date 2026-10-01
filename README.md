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

1. Crear la base de datos y el usuario de aplicación en PostgreSQL.
2. Correr el script de instalación de base de datos (schema completo +
   datos iniciales genéricos) — *ver nota abajo, este script todavía no
   está publicado en este repo.*
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

> **Nota:** el script de instalación limpia para un entorno nuevo
> (`db/init-schema.sql` o similar) está pendiente de publicarse. El
> historial de migraciones incrementales usado para evolucionar bases de
> datos ya existentes se mantiene internamente, fuera de este repositorio.

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
