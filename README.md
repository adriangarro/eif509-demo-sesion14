# EIF509 · Demo Sesión 14 — Una actualización perdida y su corrección con @Version

Repositorio de demostración del curso **EIF509 «Desarrollo de Aplicaciones
Basadas en Web»** (Universidad Nacional, Costa Rica).

Esta demo continúa el proyecto de la
[Sesión 13 (pipeline CI/CD)](https://github.com/adriangarro/eif509-demo-sesion13).
Reproduce con una prueba y con dos peticiones simultáneas el problema de la
**actualización perdida**, lo corrige con **bloqueo optimista** (`@Version`)
y traduce el conflicto a una respuesta **409** con Problem Details. Además,
corrige dos casos de **control de acceso roto** (OWASP A01) que el proyecto
arrastraba desde la Sesión 11. Las demos anteriores de la serie son:
[Sesión 3](https://github.com/adriangarro/eif509-demo-sesion3) ·
[Sesión 4](https://github.com/adriangarro/eif509-demo-sesion4) ·
[Sesión 5](https://github.com/adriangarro/eif509-demo-sesion5) ·
[Sesión 6](https://github.com/adriangarro/eif509-demo-sesion6) ·
[Sesión 7](https://github.com/adriangarro/eif509-demo-sesion7) ·
[Sesión 8](https://github.com/adriangarro/eif509-demo-sesion8) ·
[Sesión 9](https://github.com/adriangarro/eif509-demo-sesion9) ·
[Sesión 10](https://github.com/adriangarro/eif509-demo-sesion10) ·
[Sesión 11](https://github.com/adriangarro/eif509-demo-sesion11) ·
[Sesión 12](https://github.com/adriangarro/eif509-demo-sesion12) ·
[Sesión 13](https://github.com/adriangarro/eif509-demo-sesion13).

## ¿Qué van a construir?

**La demostración (20 minutos, láminas 19 a 23)**

1. Una prueba con dos copias del mismo producto: dos operadores venden 8
   unidades de un inventario de 10. La prueba espera un inventario final de
   2… y lo es, pero se vendieron 16 unidades.
2. La misma falla desde la API: dos ventas simultáneas responden `200` y el
   inventario queda en 2.
3. La prueba reescrita con la regla real (la segunda venta se rechaza): falla.
4. `@Version` en la entidad y su migración Flyway: la prueba pasa.
5. La sentencia `UPDATE ... where id=? and version=?` que genera Hibernate.
6. El manejador del conflicto: la segunda venta recibe `409` en lugar de `500`.

**La seguridad del proyecto (láminas 6 a 8 y la actividad)**

- Un CLIENTE ya no puede confirmar el pedido de otro cambiando el
  identificador de la URL, ni crear un pedido a nombre de otro con el
  `clienteId` del cuerpo (A01). Los accesos denegados y los conflictos de
  concurrencia quedan registrados en el log (A09).

## Requisitos previos

| Herramienta | Versión mínima | Descarga oficial | Cómo verificar |
|---|---|---|---|
| Docker Desktop | 4.x | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |
| JDK (Java) | 21 | [adoptium.net](https://adoptium.net/) | `java -version` |
| Git | 2.30 | [git-scm.com/downloads](https://git-scm.com/downloads) | `git --version` |
| curl | cualquiera | Incluido en macOS, Linux y Windows 10 o superior | `curl --version` |

En macOS con Homebrew, en cada terminal que usen:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

## Las dos ramas

| Rama | Contenido | Uso |
|---|---|---|
| `punto-de-partida` | El proyecto con la entidad `Inventario` **sin** `@Version` y la prueba de la lámina 22 en su versión inicial | La rama en la que se trabaja en vivo durante la demostración |
| `main` | La solución completa: la prueba final, `@Version`, la migración V9 y el manejador del 409 | La rama de respaldo y la referencia para el Laboratorio 7 |

Cada paso de la demostración se puede escribir en vivo o traer desde `main`
con un solo comando (`git restore --source=main <archivo>`); ambas formas
dejan el mismo código.

## Usuarios de demostración

| Correo | Clave | Rol |
|---|---|---|
| `admin@demo.cr` | `admin123` | `ADMIN` (el operador que vende) |
| `cliente@demo.cr` | `cliente123` | `CLIENTE` (la clienta Ana Rojas) |

## Estructura del proyecto

Lo nuevo respecto a la Sesión 13 está marcado con `←`:

```text
├── demo/V9__version_en_inventario.sql             ← la migración preparada para el paso 3 (solo en punto-de-partida)
├── peticiones/concurrencia.http                   ← la venta desde el cliente HTTP
└── src/
    ├── main/java/cr/una/eif509/demo/
    │   ├── model/Inventario.java                  ← el producto: aquí va @Version (lámina 21)
    │   ├── service/ProductoService.java           ← vender(): leer, descontar y guardar
    │   ├── service/PausaEntreLeerYGuardar.java    ← solo para la demo: 3 s entre leer y guardar
    │   ├── service/PedidoService.java             ← crear y confirmar verifican al dueño (A01)
    │   ├── api/ProductoApi.java                   ← POST /api/v1/productos/{id}/ventas
    │   └── api/ManejadorErrores.java              ← el 409 del conflicto (lámina 21)
    ├── main/resources/db/migration/
    │   ├── V8__inventario_sin_version.sql         ← el punto de partida: sin columna version
    │   └── V9__version_en_inventario.sql          ← la columna version (solo en main)
    └── test/java/cr/una/eif509/demo/
        └── repository/ConcurrenciaProductoIT.java ← la prueba de la lámina 22
```

La columna `version` existía desde la V4 (Sesión 7). La V8 la quita para que
la demostración parta de un proyecto que nunca usó bloqueo optimista, como
el de la mayoría de las parejas.

## Las demostraciones paso a paso

Sigan estos pasos en orden; cada uno tiene el comando listo para copiar. La
explicación de cada paso, con la salida esperada, está en las secciones
detalladas más abajo (el número indica dónde).

### Antes de empezar

Abran Docker Desktop y esperen a que termine de iniciar. Se usan tres
terminales en la carpeta del repositorio:

| Terminal | Para qué |
|---|---|
| 1 | Pruebas, Git y la base de datos |
| 2 | La API (`./gradlew bootRun`) |
| 3 | Las peticiones con curl |

Clonen el repositorio y cámbiense a la rama de la demostración:

```bash
git clone https://github.com/adriangarro/eif509-demo-sesion14.git
```

```bash
cd eif509-demo-sesion14 && git switch punto-de-partida
```

Ejecuten la prueba una vez antes de la clase, para que Testcontainers
descargue la imagen de PostgreSQL:

```bash
./gradlew test --tests '*ConcurrenciaProductoIT'
```

### Ejemplo: comandos en la computadora del profesor (macOS)

El guion completo de la clase, en orden, con el repositorio ya clonado en
`~/Documents/Claude/eif509-demo-sesion14` (no hace falta el `git clone`).
Cada bloque se copia y se pega tal cual.

**Antes de la clase** (terminal 1). Deja la rama `punto-de-partida` al día,
una base de datos nueva (borra la de un ensayo anterior) y la imagen de
PostgreSQL descargada:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && cd ~/Documents/Claude/eif509-demo-sesion14 && git switch punto-de-partida && git pull && docker compose down -v && docker compose up -d && ./gradlew test --tests '*ConcurrenciaProductoIT'
```

Termina con `BUILD SUCCESSFUL`. Abran en Visual Studio Code la prueba y la
entidad que se van a mostrar:

```bash
code -g src/test/java/cr/una/eif509/demo/repository/ConcurrenciaProductoIT.java:23 src/main/java/cr/una/eif509/demo/model/Inventario.java:31
```

En la terminal 2, la API (el registro de SQL ya está activo en el perfil
dev):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && export JWT_SECRETO=$(openssl rand -base64 48) && cd ~/Documents/Claude/eif509-demo-sesion14 && ./gradlew bootRun
```

En la terminal 3, el token del operador:

```bash
cd ~/Documents/Claude/eif509-demo-sesion14 && TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"correo": "admin@demo.cr", "clave": "admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/') && echo "Token obtenido: ${TOKEN:0:20}..."
```

**Paso 1 · La prueba pasa y el sistema está mal** (terminal 1):

```bash
./gradlew test --tests '*ConcurrenciaProductoIT'
```

**Opcional · La misma falla desde la API** (terminal 3). Dos ventas de 8
unidades al mismo tiempo; ambas responden `200`:

```bash
(for operador in A B; do curl -s -w "  <- operador $operador: %{http_code}\n" -X POST http://localhost:8080/api/v1/productos/1/ventas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cantidad": 8}' & done; wait)
```

El inventario quedó en 2, aunque se vendieron 16 unidades:

```bash
curl -s http://localhost:8080/api/v1/productos/1 -H "Authorization: Bearer $TOKEN"; echo
```

**Paso 2 · La prueba expresa la regla real** (terminal 1). Trae la versión
final de la prueba, muestra el cambio y la ejecuta (falla):

```bash
git restore --source=main src/test/java/cr/una/eif509/demo/repository/ConcurrenciaProductoIT.java && git --no-pager diff && ./gradlew test --tests '*ConcurrenciaProductoIT'
```

**Paso 3 · `@Version` y la migración** (terminal 1). Trae la entidad con
`@Version`, copia la migración preparada, muestra el cambio y ejecuta la
prueba (pasa):

```bash
git restore --source=main src/main/java/cr/una/eif509/demo/model/Inventario.java && cp demo/V9__version_en_inventario.sql src/main/resources/db/migration/ && git --no-pager diff src/main/java && ./gradlew test --tests '*ConcurrenciaProductoIT'
```

**Paso 4 · La sentencia UPDATE con la versión** (terminal 1):

```bash
grep -A 10 "update" build/test-results/test/TEST-cr.una.eif509.demo.repository.ConcurrenciaProductoIT.xml
```

**Paso 5 · Del 500 al 409.** En la terminal 2, detengan la API con `Ctrl+C`
y vuelvan a iniciarla (Flyway aplica la V9 al arrancar):

```bash
./gradlew bootRun
```

En la terminal 3, devuelvan el inventario a 10 y repitan las dos ventas: la
segunda responde `500`:

```bash
docker compose exec db psql -U dev -d eif509 -c "UPDATE inventario SET disponible = 10 WHERE id = 1;"
```

```bash
(for operador in A B; do curl -s -w "  <- operador $operador: %{http_code}\n" -X POST http://localhost:8080/api/v1/productos/1/ventas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cantidad": 8}' & done; wait)
```

En la terminal 1, traigan el manejador del conflicto y muestren el cambio:

```bash
git restore --source=main src/main/java/cr/una/eif509/demo/api/ManejadorErrores.java && git --no-pager diff src/main/java/cr/una/eif509/demo/api/ManejadorErrores.java
```

En la terminal 2, `Ctrl+C` y de nuevo `./gradlew bootRun`. En la terminal 3,
el inventario a 10 y las dos ventas: la segunda responde `409`:

```bash
docker compose exec db psql -U dev -d eif509 -c "UPDATE inventario SET disponible = 10 WHERE id = 1;"
```

```bash
(for operador in A B; do curl -s -w "  <- operador $operador: %{http_code}\n" -X POST http://localhost:8080/api/v1/productos/1/ventas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cantidad": 8}' & done; wait)
```

**Paso 6 · Cierre con el Laboratorio 7.** Sin comandos (ver el paso 6
detallado).

**Al terminar.** En la terminal 2, detengan la API con `Ctrl+C`. En la
terminal 1, guarden los cambios de la demostración (quedan en `git stash
list` por si los necesitan), vuelvan a `main` y borren la base de datos de
la demo:

```bash
git stash push --include-untracked -m "demo sesión 14" && git switch main && docker compose down -v
```

**Si algo falla en vivo.** En la terminal 1, cámbiense a la solución
completa, ejecuten sus pruebas y expliquen sobre el resultado final:

```bash
git stash push --include-untracked -m "respaldo sesión 14" && git switch main && ./gradlew test --tests '*ConcurrenciaProductoIT' --tests '*ProductoApiTest'
```

## 1. La prueba pasa y el sistema está mal

[`ConcurrenciaProductoIT`](src/test/java/cr/una/eif509/demo/repository/ConcurrenciaProductoIT.java)
es la prueba de la lámina 22 en su versión inicial. En este proyecto, el
producto es la entidad `Inventario` y vender es `reservar()`:

```java
var copiaA = productos.findById(id).orElseThrow();   // operador A lee 10
var copiaB = productos.findById(id).orElseThrow();   // operador B lee 10

copiaA.reservar(8);
productos.saveAndFlush(copiaA);   // A guarda: quedan 2

copiaB.reservar(8);               // B todavía cree que hay 10
productos.saveAndFlush(copiaB);   // B guarda: quedan 2

assertThat(productos.findById(id).orElseThrow().getDisponible())
        .isEqualTo(2);
```

```bash
./gradlew test --tests '*ConcurrenciaProductoIT'
```

```text
  ConcurrenciaProductoIT       segundaActualizacionSimultaneaSeRechaza()     SUCCESS    355 ms
BUILD SUCCESSFUL
```

**Qué observar**: la prueba pasa y el sistema está mal: se vendieron 16
unidades de un inventario de 10. La clase **no** tiene `@Transactional`: si
toda la prueba corriera en una sola transacción, las dos lecturas
devolverían el mismo objeto y no habría conflicto que observar.

### La misma falla desde la API (opcional)

[`ProductoService.vender`](src/main/java/cr/una/eif509/demo/service/ProductoService.java)
lee el producto, descuenta y guarda, todo en un método `@Transactional`. En
el perfil dev espera 3 segundos entre leer y guardar
([`PausaEntreLeerYGuardar`](src/main/java/cr/una/eif509/demo/service/PausaEntreLeerYGuardar.java)),
que representan el tiempo que el usuario pasa frente a la pantalla; así dos
peticiones simultáneas leen el mismo inventario.

Inicien la API en la terminal 2:

```bash
docker compose up -d
```

```bash
export JWT_SECRETO=$(openssl rand -base64 48) && ./gradlew bootRun
```

En la terminal 3, obtengan el token del operador:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"correo": "admin@demo.cr", "clave": "admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/') && echo "Token obtenido: ${TOKEN:0:20}..."
```

Y envíen dos ventas de 8 unidades del producto 1 (Teclado mecánico, 10
unidades) al mismo tiempo. Los paréntesis ejecutan el bucle en una subshell,
para que la terminal no muestre los mensajes de los procesos en segundo
plano:

```bash
(for operador in A B; do curl -s -w "  <- operador $operador: %{http_code}\n" -X POST http://localhost:8080/api/v1/productos/1/ventas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cantidad": 8}' & done; wait)
```

```text
{"id":1,"nombre":"Teclado mecánico","precio":15000.00,"disponible":2}  <- operador A: 200
{"id":1,"nombre":"Teclado mecánico","precio":15000.00,"disponible":2}  <- operador B: 200
```

En la terminal 2, las dos ventas leyeron 10 y ninguna de las dos sentencias
`UPDATE` compara nada más que el `id`:

```text
INFO ... ProductoService  : Venta de 8 unidades de «Teclado mecánico»: disponible leído = 10
INFO ... ProductoService  : Venta de 8 unidades de «Teclado mecánico»: disponible leído = 10
Hibernate:
    update
        inventario
    set
        disponible=?,
        precio=?,
        producto=?
    where
        id=?
```

**Qué observar**: cada venta corrió en su propia transacción y cada una es
correcta por separado; la segunda escritura reemplazó a la primera. Una
transacción no evita la actualización perdida (autoevaluación, pregunta 3).
El mismo pedido con el cliente HTTP está en
[`peticiones/concurrencia.http`](peticiones/concurrencia.http), aunque ahí
las peticiones se envían una tras otra.

## 2. La prueba expresa la regla real

Reemplacen el método de la prueba por la versión de la lámina 22: la primera
escritura funciona y la segunda se rechaza.

```java
copiaA.reservar(8);
productos.saveAndFlush(copiaA);   // versión 0 -> 1

copiaB.reservar(8);               // aún tiene versión 0
assertThatThrownBy(() -> productos.saveAndFlush(copiaB))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);

assertThat(productos.findById(id).orElseThrow().getDisponible())
        .isEqualTo(2);            // no se vendió de más
```

Con las importaciones
`org.springframework.orm.ObjectOptimisticLockingFailureException` y
`static org.assertj.core.api.Assertions.assertThatThrownBy`. O tráiganla de
`main`:

```bash
git restore --source=main src/test/java/cr/una/eif509/demo/repository/ConcurrenciaProductoIT.java
```

```bash
./gradlew test --tests '*ConcurrenciaProductoIT'
```

```text
  ConcurrenciaProductoIT       segundaActualizacionSimultaneaSeRechaza()     FAILURE    359 ms

ConcurrenciaProductoIT > segundaActualizacionSimultaneaSeRechaza() FAILED
    java.lang.AssertionError at ConcurrenciaProductoIT.java:40
```

El detalle, en `build/reports/tests/test/index.html`, es
`Expecting code to raise a throwable.`

**Qué observar**: ahora la prueba dice lo que el negocio necesita, y falla
porque el sistema no detecta el conflicto.

## 3. `@Version` y la migración Flyway

En [`Inventario`](src/main/java/cr/una/eif509/demo/model/Inventario.java),
el campo de la lámina 21 (con `import jakarta.persistence.Version;`):

```java
@Version
private Long version;
```

Y la migración preparada en
[`demo/V9__version_en_inventario.sql`](demo/V9__version_en_inventario.sql),
copiada a la carpeta de migraciones:

```sql
ALTER TABLE inventario ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

```bash
cp demo/V9__version_en_inventario.sql src/main/resources/db/migration/
```

(O la entidad desde `main`:
`git restore --source=main src/main/java/cr/una/eif509/demo/model/Inventario.java`.)

```bash
./gradlew test --tests '*ConcurrenciaProductoIT'
```

```text
  ConcurrenciaProductoIT       segundaActualizacionSimultaneaSeRechaza()     SUCCESS    401 ms
BUILD SUCCESSFUL
```

**Qué observar**: con una anotación y una columna, la primera venta deja la
versión en 1; la segunda intenta guardar con la versión 0 y Hibernate lanza
la excepción. `DEFAULT 0` es necesario porque la columna es `NOT NULL` y la
tabla ya tiene filas. Si ejecutan la prueba con `@Version` pero sin copiar la
migración, Hibernate no arranca:

```text
Schema-validation: missing column [version] in table [inventario]
```

## 4. La sentencia UPDATE con la versión

El perfil dev registra cada SQL (`show-sql: true`, Sesión 8), también en las
pruebas. La salida de la última ejecución queda en el reporte de la prueba:

```bash
grep -A 10 "update" build/test-results/test/TEST-cr.una.eif509.demo.repository.ConcurrenciaProductoIT.xml
```

```text
    update
        inventario
    set
        disponible=?,
        precio=?,
        producto=?,
        version=?
    where
        id=?
        and version=?
```

(También en `build/reports/tests/test/index.html`, pestaña **Standard
output** de la clase.)

**Qué observar**: aquí está todo el mecanismo. La base de datos solo acepta
la escritura si nadie cambió el registro desde que se leyó.

## 5. Del 500 al 409

Reinicien la API (`Ctrl+C` y `./gradlew bootRun`); al arrancar, Flyway
aplica la V9:

```text
Migrating schema "public" to version "9 - version en inventario"
```

Devuelvan el inventario a 10 y repitan las dos ventas:

```bash
docker compose exec db psql -U dev -d eif509 -c "UPDATE inventario SET disponible = 10 WHERE id = 1;"
```

```bash
(for operador in A B; do curl -s -w "  <- operador $operador: %{http_code}\n" -X POST http://localhost:8080/api/v1/productos/1/ventas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cantidad": 8}' & done; wait)
```

```text
{"id":1,"nombre":"Teclado mecánico","precio":15000.00,"disponible":2}  <- operador A: 200
{"timestamp":"2026-10-22T00:58:59.669+00:00","status":500,"error":"Internal Server Error","path":"/api/v1/productos/1/ventas"}  <- operador B: 500
```

El conflicto ya se detecta (en la terminal 2, las dos sentencias terminan en
`and version=?` y luego aparece `ObjectOptimisticLockingFailureException`),
pero llega al cliente como un error del servidor. Agreguen el manejador de
la lámina 21 en
[`ManejadorErrores`](src/main/java/cr/una/eif509/demo/api/ManejadorErrores.java)
(o tráiganlo de `main` con
`git restore --source=main src/main/java/cr/una/eif509/demo/api/ManejadorErrores.java`):

```java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
ProblemDetail conflicto(ObjectOptimisticLockingFailureException e) {
    log.warn("Conflicto de concurrencia en {} con id {}", e.getPersistentClassName(), e.getIdentifier());
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "Otra persona modificó este registro. Recargue los datos e intente de nuevo.");
    pd.setTitle("Conflicto de concurrencia");
    pd.setType(URI.create(TIPOS + "conflicto-de-concurrencia"));
    return pd;
}
```

Reinicien la API, devuelvan el inventario a 10 y repitan las dos ventas:

```text
{"id":1,"nombre":"Teclado mecánico","precio":15000.00,"disponible":2}  <- operador A: 200
{"type":"https://api.ejemplo.cr/errores/conflicto-de-concurrencia","title":"Conflicto de concurrencia","status":409,"detail":"Otra persona modificó este registro. Recargue los datos e intente de nuevo.","instance":"/api/v1/productos/1/ventas"}  <- operador B: 409
```

Y en la terminal 2, el conflicto queda registrado (A09):

```text
WARN ... ManejadorErrores   : Conflicto de concurrencia en cr.una.eif509.demo.model.Inventario con id 1
```

**Qué observar**: para el usuario, un conflicto no es un error del servidor:
es la indicación de que debe recargar los datos. Por eso la respuesta es
`409` con un mensaje claro y no `500`. En `main`, la prueba
`ProductoApiTest.ventaSimultaneaRechazadaDevuelve409` lo verifica con
MockMvc.

## 6. Cierre: el Laboratorio 7

Esto es lo que pide el criterio «Concurrencia controlada» del Laboratorio 7:
identificar la operación de su dominio con contención real, protegerla con
`@Version` y su migración, traducir el conflicto a 409 en el manejador global
y demostrarlo con una prueba que provoque el conflicto. En este proyecto, la
oración que lo justifica en el README sería: *la venta tiene contención real,
porque varios operadores pueden vender el mismo producto al mismo tiempo y
cada venta lee y modifica el mismo inventario.*

## La seguridad del proyecto: control de acceso roto (A01)

El caso 1 de la actividad ocurría en este proyecto, aunque no en la consulta:
desde la Sesión 11, `GET /api/v1/pedidos/{id}` ya comparaba al dueño del
pedido con el usuario del token, pero otras dos acciones no lo hacían.

| Acción | Antes | Ahora |
|---|---|---|
| `POST /api/v1/pedidos/{id}/confirmacion` | Ana podía confirmar el pedido 3, de otro cliente: reservaba su inventario y creaba su factura | `403 Acceso denegado` |
| `POST /api/v1/pedidos` con `clienteId` de otro | Ana podía crear pedidos a nombre de otro cliente | `403 Acceso denegado` |
| `POST /api/v1/pedidos` con un `clienteId` inexistente | `422` para todos (revelaba qué identificadores existen) | `403` para un CLIENTE; `422` solo para ADMIN |

La verificación vive en el servicio
([`PedidoService.verificarPropiedad`](src/main/java/cr/una/eif509/demo/service/PedidoService.java)),
porque depende de los datos, y registra cada acceso denegado sin contraseñas
ni tokens:

```text
WARN ... PedidoService  : Acceso denegado: cliente@demo.cr intentó confirmar el pedido 3
```

La prueba de integración
`AutenticacionIT.unClienteNoPuedeConfirmarNiCrearPedidosDeOtro` lo verifica
con tokens reales, y `PedidoServiceTest` cubre cada regla sin base de datos.

### La actividad: ¿qué vulnerabilidad tiene este código?

| Caso | Categoría | Cómo se defiende este proyecto |
|---|---|---|
| 1 · El pedido de otro usuario | A01 Control de acceso roto | `PedidoService` compara al dueño con el usuario del token al consultar, crear y confirmar |
| 2 · La entidad con el hash de la clave | Exposición de información | Los controladores devuelven DTO (`PedidoResumen`, `ProductoResumen`); ningún endpoint devuelve `Usuario` |
| 3 · La búsqueda que concatena texto | A05 Inyección | Todas las consultas son métodos de Spring Data con parámetros; no hay JPQL construido con `+` |
| 4 · La traza en el error 500 | A10 Manejo de condiciones excepcionales | `ManejadorErrores` responde Problem Details; un nombre duplicado es `409 Producto duplicado` |

## Comandos útiles

| Acción | Comando |
|---|---|
| Solo la prueba de concurrencia | `./gradlew test --tests '*ConcurrenciaProductoIT'` |
| Todas las verificaciones (lo que ejecuta el pipeline) | `./gradlew check` |
| Iniciar la base de datos | `docker compose up -d` |
| Borrar la base de datos de la demo | `docker compose down -v` |
| Devolver el inventario del producto 1 a 10 | `docker compose exec db psql -U dev -d eif509 -c "UPDATE inventario SET disponible = 10 WHERE id = 1;"` |
| Ver el inventario y la versión | `docker compose exec db psql -U dev -d eif509 -c "SELECT id, producto, disponible, version FROM inventario WHERE id = 1;"` |
| Ver lo que cambió respecto a la solución | `git --no-pager diff main --stat` |
| Recuperar los cambios guardados de la demo | `git stash list` y `git stash pop` |

## Solución de problemas

| Problema | Causa | Solución |
|---|---|---|
| La prueba no detecta el conflicto aunque hay `@Version` | La prueba corre dentro de una transacción (`@Transactional` o `@DataJpaTest`): las dos lecturas devuelven el mismo objeto | Quitar `@Transactional` y usar `@SpringBootTest`, como `ConcurrenciaProductoIT` |
| `Schema-validation: missing column [version] in table [inventario]` | Se agregó `@Version` sin la migración | Copiar `demo/V9__version_en_inventario.sql` a `src/main/resources/db/migration/` (paso 3) |
| La migración falla con `column "version" of relation "inventario" contains null values` | La columna se agregó como `NOT NULL` sin `DEFAULT 0` y la tabla ya tiene filas | Usar `NOT NULL DEFAULT 0`, como la V9 |
| El conflicto llega como `500` | Falta el manejador, o captura una excepción distinta | Capturar `ObjectOptimisticLockingFailureException` de Spring (paso 5) |
| Las dos ventas responden `200` con `@Version` | La API no se reinició después del cambio, o las peticiones no fueron simultáneas | Reiniciar `bootRun`; enviar las dos ventas con el mismo comando (el bucle con `&`) |
| La venta responde `422 Inventario insuficiente` | Una ejecución anterior dejó el inventario en 2 | Devolver el inventario a 10 (ver Comandos útiles) |
| Las ventas responden `401` | El token se obtuvo con otra API: cada `export JWT_SECRETO=$(openssl rand ...)` genera un secreto nuevo | Volver a obtener el token en la terminal 3 |
| Al iniciar la API aparece `Schema "public" has a version (9) that is newer than the latest available migration (8)` | La base de datos tiene la V9 de un ensayo anterior y la rama `punto-de-partida` todavía no | La demo funciona igual, pero para partir de cero: `docker compose down -v && docker compose up -d` antes de iniciar la API |
| Testcontainers no inicia (`Could not find a valid Docker environment`) | Docker Desktop no está en ejecución | Abrirlo y esperar a que inicie; si no, mostrar el resultado desde `main` |
| `Port 8080 was already in use` | Otra API sigue en ejecución | Detenerla con `Ctrl+C` en su terminal |
| `Unable to locate a Java Runtime` | Falta el JDK o `JAVA_HOME` | Ver Requisitos previos |
| Todo falla | — | Cambiar a `main` (bloque «Si algo falla en vivo») y explicar sobre el resultado final |

## Relación con los laboratorios

**Laboratorio 7** (se asigna hoy; entrega el jueves 5 de noviembre a las
6:00 p. m., junto con el proyecto final). Este repositorio cubre dos de sus
criterios:

- **Concurrencia controlada**: `@Version`, la migración, el 409 con Problem
  Details y la prueba que provoca el conflicto (pasos 1 a 6).
- **Auditoría de seguridad**: un ejemplo de hallazgo y corrección (A01) con
  su prueba. Su `AUDITORIA-SEGURIDAD.md` revisa los ocho puntos de la lista
  de verificación de la lámina 28 sobre su propio sistema.

El pipeline es el de la Sesión 13; aquí `desplegar` termina sin error si el
repositorio no tiene el secreto `DEPLOY_HOOK`, porque la demostración se
ejecuta en local. La observabilidad (Actuator y registros estructurados) no
está incluida: los registros estructurados requieren Spring Boot 3.4 o
posterior, y este proyecto usa 3.3.2. En `main` hay 87 pruebas en verde: 27
unitarias, 34 de la API y las vistas, y 26 de integración.

**Proyecto final**: avance obligatorio el 29 de octubre; entrega y defensa
el 5 de noviembre.

---

> **Material de referencia del curso.** La concurrencia, la auditoría y las
> pruebas de su laboratorio deben corresponder a su propio dominio; no copien
> este ejemplo.
