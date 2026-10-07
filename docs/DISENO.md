# Guía de diseño de la interfaz

Esta guía resume cómo está organizado el estilo visual de erpadmin y qué
pasos seguir para que una pantalla nueva se vea igual que las demás.
Toda la interfaz es Thymeleaf + Bootstrap 5, con una capa propia de
variables y componentes encima. No hay preprocesadores ni dependencias
externas: los archivos CSS, las fuentes y los íconos están en
`src/main/resources/static/`.

## Paleta y tipografía

Los valores reales están en `static/css/jb-tokens.css`. Se usan siempre por
nombre de variable, nunca con el color escrito a mano.

| Uso | Variables | Valor |
|---|---|---|
| Fondo de la barra lateral y del login | `--jb-bg`, `--jb-bg-2` | `#0B1220`, `#121B2E` |
| Azul de acción (botones, foco, acentos) | `--jb-primary-h`, `--jb-primary-d` | `#1F6BE0`, `#1A5CC4` |
| Cian de acento (ítem activo, botón secundario) | `--jb-accent` | `#38BDF8` |
| Fondo del área de contenido | `--jb-page-bg` | `#F4F6FA` |
| Tarjetas y superficies | `--jb-surface`, `--jb-surface-2` | `#FFFFFF`, `#F8FAFC` |
| Texto principal / secundario / pistas | `--jb-ink`, `--jb-ink-2`, `--jb-ink-3` | `#0F172A`, `#5B6477`, `#64708A` |
| Bordes suaves / de campos | `--jb-line`, `--jb-line-strong` | `#E3E8F0`, `#858FA3` |
| Enlaces | `--jb-link` | `#1A5CC4` |
| Anillo de foco sobre claro | `--jb-focus-light` | `#0284C7` |

Los estados (éxito, aviso, error, información) tienen cuatro variantes:
`-solid` (fondo de botón), `-subtle` (fondo de alerta o badge), `-ink`
(texto sobre el fondo tenue) y `-line` (borde). Por ejemplo,
`--jb-success-subtle` + `--jb-success-ink` + `--jb-success-line`. Hay además
un violeta (`--jb-violet-*`) para distinguir categorías, y los tokens
`--jb-print-*` para los documentos impresos en blanco y negro.

Tipografía (archivos locales en `static/fonts/jb/`):

- **Space Grotesk** (`--jb-font-title`): títulos, cabeceras de tarjeta y cifras destacadas.
- **Inter** (`--jb-font-body`): todo lo demás. Tamaño base de 14 px.

Mínimos de accesibilidad que cumple toda combinación de tokens en uso:
contraste de texto de 4,5:1 o más, y de 3:1 o más para íconos, bordes de
campos y anillos de foco.

## Cómo se organizan las hojas de estilo

`layout.html` las carga en este orden. Cada una solo consume variables de
la anterior:

1. **`jb-tokens.css`**: la única fuente de colores, fuentes, radios y
   sombras. Si falta un color, se agrega aquí con un comentario de uso.
2. **`bootstrap-theme.css`**: redefine las variables de Bootstrap
   (`--bs-*`) con los tokens. Así `.btn-primary`, `.table`, `.badge`,
   `.form-control`, `.modal` y demás toman la paleta sin tocar el HTML.
3. **`common.css`**: componentes compartidos por varias pantallas (ver la
   lista abajo) y la base del `body`.
4. **`layout.css`**: el marco (barra lateral, cabecera, área de contenido y
   pie) y su comportamiento en celular, tablet y escritorio.
5. **CSS por pantalla** (`asistencia-registrar.css`, `usuarios-listar.css`,
   etc.): solo lo que es propio de una pantalla, como anchos de columna,
   disposición o tarjetas específicas. Se enlaza desde la plantilla, justo
   después del fragmento `head`, y **nunca redefine un componente
   compartido**; a lo sumo lo ajusta (por ejemplo, `.page-card { max-width }`).

`login.css` es independiente, porque el login no usa el marco. Los
documentos de impresión autónomos (`*-imprimir.html` y
`registro-departamento.html`) tienen estilos propios en blanco y negro y no
cargan el tema.

## Componentes compartidos (`common.css`)

| Componente | Clases | Cuándo usarlo |
|---|---|---|
| Título de página | `.page-title` | Un `h2` al inicio del contenido, cuando la cabecera no basta. |
| Tarjeta | `.page-card`, `.page-card-header`, `.page-card-body` | Contenedor de un formulario o una tabla. La cabecera lleva un ícono `bi` y un `<span>` con el título; un `<small>` opcional va a la derecha. |
| Título de sección | `.section-title` | Encabezado de una tabla suelta; se une a la `.data-table` que le sigue. |
| Barra de controles | `.controls-bar`, `.controls-left`, `.filter-group` | Filtros y acciones sobre una lista. Es una barra de herramientas: separa sus hijos a los extremos. No sirve para avisos. |
| Selector de fecha | `.date-selector` (con `label` + `input`) | Elegir la fecha de un registro o reporte. |
| Barra de información | `.info-bar`, `.info-item`, `.date-badge`, `.efectivo-badge` | Resumen de un registro diario (fecha, efectivo). |
| Alertas | `.alert-custom` + `.alert-success`, `-danger`, `-warning` o `-info` | Mensajes de resultado y avisos. Estructura: `<i class="bi …">` + `<span>texto</span>`. |
| Tabla | `.data-table` (contenedor) + `<table>` | Toda tabla de datos. Se desplaza dentro de su contenedor si no cabe. Ayudas: `.td-num`/`.row-num`, `.td-datos`, `.td-persona`, `.td-acciones`, `.td-empty` y anchos mínimos `.col-*`. |
| Formulario | `.form-section-title`, `.form-row` (`.single`, `.triple`), `.form-group`, `.field-hint`, `.required` | Formularios de alta y edición. |
| Datos fijos | `.datos-fijos`, `.dato-item`, `.dato-label`, `.dato-valor` | Datos de solo lectura al inicio de un formulario de edición. |
| Botones | `.btn-institucional` (principal), `.btn-institucional-gold` (secundario cian), `.btn-cancelar` (enlace discreto, también para "Volver"), `.btn-save`/`.btn-update` (acción grande al pie de un registro) | Acciones de formularios y barras. En tablas se usan los botones de Bootstrap con el tema (`btn btn-sm btn-primary`, `btn-outline-primary`, `btn-outline-danger`, `btn-success`, `btn-secondary`). |
| Badges | `.badge-total`, `.badge-rol` + `.rol-*`, `.admin-badge` | Totales de una lista, rol de un usuario, acciones exclusivas de un rol. |
| Estado vacío | `.empty-state` | Lista o búsqueda sin resultados. |

Todos los controles miden 44 px de alto o más en pantallas menores de 992 px, y
respetan `prefers-reduced-motion`.

## Agregar una pantalla nueva

1. Parte de la estructura de cualquier pantalla existente: `layout :: head`,
   `layout :: sidebar`, el `div.sidebar-overlay`, `layout :: header`, un
   `div.content-area` dentro de `div.main-wrapper`, y luego
   `layout :: footer` y `layout :: scripts`.
2. Arma el contenido con los componentes de la tabla anterior antes de
   escribir CSS nuevo.
3. Si necesitas estilos propios, crea `static/css/<modulo>-<pantalla>.css`
   y enlázalo con `<link th:href="@{/css/...}" rel="stylesheet">` después
   del fragmento `head`. Usa solo variables `var(--jb-…)`. Si un color no
   existe, agrégalo a `jb-tokens.css`.
4. Sin atributos `style=""` en el HTML. Sin colores escritos a mano. No
   redefinas un componente compartido: si hace falta una variante útil para
   varias pantallas, agrégala a `common.css`.
5. Asocia cada `label` con su campo (`for` / `id`), y marca los íconos
   decorativos con `aria-hidden="true"`.
6. Revisa la pantalla de 320 px a 1920 px. No debe haber desplazamiento
   horizontal de la página (las tablas sí pueden desplazarse dentro de su
   contenedor), los objetivos táctiles deben medir 44 px o más en celular y
   el foco con teclado debe verse.
