# EIF509 · Demo Sesión 13 — Del push al despliegue automático

Repositorio de demostración del curso **EIF509 «Desarrollo de Aplicaciones
Basadas en Web»** (Universidad Nacional, Costa Rica).

Esta demo continúa el proyecto de la
[Sesión 12 (despliegue en la nube)](https://github.com/adriangarro/eif509-demo-sesion12).
Organiza las pruebas del proyecto en una **estrategia**, agrega a la
construcción con Gradle un **análisis estático** (Checkstyle) y un **umbral
de cobertura** que la hace fallar, y automatiza el camino completo con un
**pipeline de GitHub Actions**: cada cambio se verifica y, si está en `main`
y pasó todas las verificaciones, se despliega solo en la plataforma. Las
demos anteriores de la serie son:
[Sesión 3](https://github.com/adriangarro/eif509-demo-sesion3) ·
[Sesión 4](https://github.com/adriangarro/eif509-demo-sesion4) ·
[Sesión 5](https://github.com/adriangarro/eif509-demo-sesion5) ·
[Sesión 6](https://github.com/adriangarro/eif509-demo-sesion6) ·
[Sesión 7](https://github.com/adriangarro/eif509-demo-sesion7) ·
[Sesión 8](https://github.com/adriangarro/eif509-demo-sesion8) ·
[Sesión 9](https://github.com/adriangarro/eif509-demo-sesion9) ·
[Sesión 10](https://github.com/adriangarro/eif509-demo-sesion10) ·
[Sesión 11](https://github.com/adriangarro/eif509-demo-sesion11) ·
[Sesión 12](https://github.com/adriangarro/eif509-demo-sesion12).

## ¿Qué van a construir?

**Parte 1 · Las verificaciones, en su computadora**

1. Las pruebas del proyecto ordenadas por nivel: unitarias, de la API con
   MockMvc y de integración con Testcontainers.
2. Una regla de negocio nueva, el **13 % de IVA**, con una prueba unitaria
   que la protege (la versión corregida de la prueba 1 de la actividad).
3. Checkstyle: el análisis estático que detecta errores comunes.
4. El umbral de JaCoCo: si la capa de negocio baja del 70 % de cobertura, la
   construcción falla.
5. `./gradlew check`: pruebas, análisis estático y cobertura en un solo
   comando, el mismo que ejecuta el pipeline.

**Parte 2 · El pipeline completo en GitHub Actions**

1. El workflow `ci-cd.yml` con dos trabajos: `verificar` y `desplegar`.
2. El deploy hook de la plataforma guardado como secreto del repositorio.
3. Un cambio visible (rama `cambio-visible`): el pipeline pasa en verde y la
   aplicación publicada se actualiza sin pasos manuales.
4. Un cambio que rompe la regla del IVA (rama `cambio-con-error`): la prueba
   falla, el pipeline se detiene y el despliegue no se ejecuta.

### El pipeline

```text
                    ┌──────────────────────────────────────────┐
 push a main   ───► │ verificar  (en cada push y pull request) │
 pull request  ───► │   ./gradlew check                        │
                    │     compilación · pruebas (76)           │
                    │     Checkstyle · umbral de cobertura     │
                    └──────────────────┬───────────────────────┘
                                       │ needs: verificar   (solo si terminó en verde)
                                       │ if: refs/heads/main (solo en main)
                                       ▼
                    ┌──────────────────────────────────────────┐
                    │ desplegar                                │
                    │   curl -X POST $DEPLOY_HOOK  ───────────►│──► Render construye la imagen
                    └──────────────────────────────────────────┘    y publica la versión nueva
```

En la plataforma, el despliegue automático por push está **desactivado**:
solo se despliega lo que pasó por el pipeline.

### La estrategia de pruebas del proyecto

Cada comportamiento se prueba en el nivel más económico que permita
verificarlo:

| Nivel | Qué verifica | Clases | Pruebas | Tiempo típico |
|---|---|---|---|---|
| Unitarias (JUnit y Mockito, sin Spring) | Reglas de negocio | `CalculadoraIvaTest`, `PedidoServiceTest`, `ProductoServiceTest`, `FacturaServiceTest`, `PedidoMapperTest` | 22 | Milisegundos |
| API y vistas (MockMvc, sin base de datos) | Contrato HTTP, validación y seguridad | `PedidoControllerTest`, `ProductoApiTest`, `SeguridadApiTest`, `ProductoWebTest` | 30 | Decenas de milisegundos |
| Integración (Testcontainers con PostgreSQL) | Mapeo JPA, restricciones, transacciones y el recorrido completo | `PedidoRepositoryIT`, `PedidoServiceIT`, `PedidoApiIT`, `AutenticacionIT`, `AdminProductosIT` | 24 | Segundos |
| **Total** | | | **76** | |

El Laboratorio 7 pedirá al menos 40 pruebas en verde en la integración
continua.

## Requisitos previos

| Herramienta | Versión mínima | Descarga oficial | Cómo verificar |
|---|---|---|---|
| Docker Desktop | 4.x | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |
| JDK (Java) | 21 | [adoptium.net](https://adoptium.net/) | `java -version` |
| Git | 2.30 | [git-scm.com/downloads](https://git-scm.com/downloads) | `git --version` |
| GitHub CLI (opcional) | 2.x | [cli.github.com](https://cli.github.com/) | `gh --version` |
| Cuenta en GitHub | — | [github.com](https://github.com) | Con permiso de escritura en el repositorio |
| Cuenta en la plataforma | — | [render.com](https://render.com) (iniciar sesión con GitHub) | Para la Parte 2 |

Con la GitHub CLI, los pasos de la Parte 2 se hacen desde la terminal; sin
ella, cada paso indica también dónde hacerlo en la página de GitHub. En
macOS con Homebrew, en cada terminal que usen:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

## Las tres ramas

| Rama | Contenido | Uso |
|---|---|---|
| `main` | El proyecto con el pipeline, la regla del IVA, Checkstyle y el umbral de cobertura | Lo que está desplegado |
| `cambio-visible` | `main` más un texto nuevo en la descripción de la API (Swagger UI) | Paso 13: se integra a `main` y se despliega solo |
| `cambio-con-error` | `main` con el IVA cambiado de 13 % a 12 % | Paso 14: el pipeline lo detiene |

## Usuarios de demostración

| Correo | Clave | Rol |
|---|---|---|
| `admin@demo.cr` | `admin123` | `ADMIN` |
| `cliente@demo.cr` | `cliente123` | `CLIENTE` |

## Estructura del proyecto

Lo nuevo respecto a la Sesión 12 está marcado con `←`:

```text
├── .github/workflows/ci-cd.yml                    ← el pipeline: verificar y desplegar (lámina 17)
├── build.gradle                                   ← Checkstyle y el umbral de cobertura en check (lámina 16)
├── config/checkstyle/checkstyle.xml               ← las reglas del análisis estático
├── Dockerfile                                        la imagen que construye la plataforma (Sesión 12)
└── src/
    ├── main/java/cr/una/eif509/demo/
    │   ├── service/CalculadoraIva.java            ← la regla del 13 % de IVA
    │   └── dto/PedidoResumen.java                 ← el pedido ahora incluye iva y totalConIva
    └── test/java/cr/una/eif509/demo/
        ├── service/CalculadoraIvaTest.java        ← una buena prueba: AAA, nombre de la regla, aserción
        └── service/FacturaServiceTest.java        ← una prueba revisada con los criterios de la actividad
```

## Las demostraciones paso a paso

Sigan estos pasos en orden para ejecutar las dos partes; cada uno tiene el
comando listo para copiar. La explicación de cada paso está en las
secciones detalladas más abajo: el número entre paréntesis indica dónde.

### Antes de empezar

Abran Docker Desktop y esperen a que termine de iniciar. Abran en el
navegador la pestaña **Actions** del repositorio
(`https://github.com/adriangarro/eif509-demo-sesion13/actions`) y la URL
pública de la API. Abran dos terminales en la carpeta del repositorio
clonado:

```bash
git clone https://github.com/adriangarro/eif509-demo-sesion13.git
```

```bash
cd eif509-demo-sesion13
```

En macOS con Homebrew, ejecuten en ambas terminales:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

**Despertar el servicio en la nube.** El plan gratuito suspende el servicio
tras 15 minutos sin tráfico y tarda unos dos minutos en volver a arrancar.
Para que responda al instante durante la demostración, abran una **tercera
terminal** y dejen este bucle en ejecución durante toda la clase (una
petición cada 5 minutos):

```bash
while true; do curl -s -o /dev/null -w "$(date +%H:%M:%S) -> %{http_code}\n" https://eif509-demo-sesion13.onrender.com/api/v1/productos; sleep 300; done
```

La primera línea puede tardar hasta dos minutos (el servicio está
despertando); a partir de ahí cada línea muestra `401` de inmediato, que es
la respuesta correcta sin token. Al terminar la clase, deténganlo con
`Ctrl+C`.

### Ejemplo: comandos en la computadora del profesor (macOS)

El guion completo de la clase, en orden, con el repositorio ya clonado en
`~/Documents/Claude/eif509-demo-sesion13` (no hace falta el `git clone`).
Cada bloque se copia y se pega tal cual. Se usan tres terminales:

| Terminal | Para qué |
|---|---|
| 1 | Gradle, Git y GitHub CLI (queda en la carpeta del repositorio) |
| 2 | Consultas a la URL pública (no necesita `JAVA_HOME`) |
| 3 | El bucle que mantiene despierto el servicio |

**Antes de la clase.** Abran Docker Desktop y esperen a que termine de
iniciar. En la terminal 3, dejen el bucle en ejecución (la primera línea
puede tardar dos minutos; luego cada línea muestra `401`):

```bash
while true; do curl -s -o /dev/null -w "$(date +%H:%M:%S) -> %{http_code}\n" https://eif509-demo-sesion13.onrender.com/api/v1/productos; sleep 300; done
```

En la terminal 1, preparen la carpeta, Java y la rama `main`:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && cd ~/Documents/Claude/eif509-demo-sesion13 && git switch main && git pull
```

Abran en el navegador las tres pestañas que se van a mostrar (Actions, Swagger
UI y el pull request de ensayo como respaldo):

```bash
open https://github.com/adriangarro/eif509-demo-sesion13/actions https://eif509-demo-sesion13.onrender.com/swagger-ui.html https://github.com/adriangarro/eif509-demo-sesion13/pull/1
```

**Paso 1 · El workflow** (terminal 1). Se abre en Visual Studio Code:

```bash
code .github/workflows/ci-cd.yml
```

**Paso 2 · El umbral de cobertura y `./gradlew check`** (terminal 1). Abre
`build.gradle` en el bloque `jacocoTestCoverageVerification`:

```bash
code -g build.gradle:123
```

```bash
./gradlew clean check
```

Termina con `BUILD SUCCESSFUL` (unos dos minutos). El reporte de cobertura:

```bash
open build/reports/jacoco/test/html/index.html
```

**Opcional · Checkstyle detecta una importación sin usar** (terminal 1).
Agrega `import java.util.List;` a `CalculadoraIva.java`, ejecuta Checkstyle
(falla con `CalculadoraIva.java:5:8: Importación no usada`) y deja el archivo
como estaba:

```bash
perl -pi -e 's/^(import java\.math\.RoundingMode;)$/$1\nimport java.util.List;/' src/main/java/cr/una/eif509/demo/service/CalculadoraIva.java; ./gradlew checkstyleMain; git checkout -- src/main/java/cr/una/eif509/demo/service/CalculadoraIva.java
```

**Opcional · Prueba de mutación: el IVA al 12 %** (terminal 1). Ejecuta las
pruebas unitarias en `cambio-con-error` (fallan 3 de 52) y vuelve a `main`:

```bash
git switch cambio-con-error && ./gradlew unitTest; git switch main
```

**Paso 3 · El secreto DEPLOY_HOOK** (terminal 1). Muestra el nombre, nunca el
valor:

```bash
gh secret list
```

**Paso 4 · Integrar el cambio visible a main** (terminal 1):

```bash
cd ~/Documents/Claude/eif509-demo-sesion13 && git switch main && git pull && git merge --no-edit origin/cambio-visible && git push
```

Seguir la ejecución del pipeline en la terminal (también se ve en la pestaña
**Actions**). Espera diez segundos a que GitHub registre la ejecución y
termina cuando `desplegar` concluye:

```bash
sleep 10 && gh run watch "$(gh run list --branch main --limit 1 --json databaseId --jq '.[0].databaseId')"
```

**Paso 5 · Ver el cambio en producción** (terminal 2). Revisa cada 20
segundos y avisa cuando la versión nueva está en línea (unos cinco minutos
después de `desplegar`):

```bash
until curl -s https://eif509-demo-sesion13.onrender.com/v3/api-docs | grep -q "pipeline CI/CD"; do echo "$(date +%H:%M:%S) todavía la versión anterior"; sleep 20; done; echo "$(date +%H:%M:%S) versión nueva en línea"
```

Cuando diga «versión nueva en línea», recarguen la pestaña de Swagger UI
(`Cmd+Shift+R`): la descripción de la API termina con «Versión publicada por
el pipeline CI/CD de la Sesión 13…».

**Paso 6 · Un pull request que rompe una prueba** (terminal 1):

```bash
cd ~/Documents/Claude/eif509-demo-sesion13 && gh pr create --base main --head cambio-con-error --title "IVA al 12 %" --body "Cambio que rompe la regla del IVA (demostración de la Sesión 13)."
```

Abrir el pull request en el navegador (pestaña **Checks**):

```bash
gh pr view cambio-con-error --web
```

Seguir las verificaciones en la terminal hasta que `verificar` termine en
rojo (unos dos minutos; espera diez segundos a que GitHub registre la
ejecución):

```bash
sleep 10 && gh pr checks cambio-con-error --watch
```

**Al terminar.** Cierren el pull request sin integrarlo (terminal 1):

```bash
gh pr close cambio-con-error
```

En la terminal 3, detengan el bucle con `Ctrl+C`.

**Si algo falla en vivo.** La pestaña **Actions** conserva una ejecución en
verde en `main` (con `desplegar` en verde) y la del pull request de ensayo
#1 en rojo (con `desplegar` omitido). Para listarlas desde la terminal 1:

```bash
gh run list --limit 5
```

### Parte 1 · Las verificaciones en su computadora

**1. Recorrer el workflow (paso 10).** Abran
[`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml). Qué observar:
los dos trabajos, `needs: verificar` y la condición
`if: github.ref == 'refs/heads/main'`.

**2. El umbral de cobertura y `./gradlew check` (pasos 7 y 8).** Muestren el
bloque `jacocoTestCoverageVerification` en
[`build.gradle`](build.gradle) y ejecuten en la terminal 1:

```bash
./gradlew clean check
```

Termina con `BUILD SUCCESSFUL`. Los reportes quedan en
`build/reports/jacoco/test/html/index.html` y
`build/reports/checkstyle/main.html`:

```bash
open build/reports/jacoco/test/html/index.html
```

(En Linux, `xdg-open`; en Windows, `start`.)

### Parte 2 · El pipeline en GitHub Actions

**3. El secreto DEPLOY_HOOK (paso 12).** En GitHub: **Settings → Secrets and
variables → Actions**. Se ve el nombre `DEPLOY_HOOK`, nunca su valor. Desde
la terminal:

```bash
gh secret list
```

**4. Integrar el cambio visible a main (paso 13).** En la terminal 1:

```bash
git switch main && git pull && git merge --no-edit origin/cambio-visible && git push
```

Sigan la ejecución en la pestaña **Actions**: primero `verificar` (unos dos
minutos) y, cuando termina en verde, `desplegar`. Desde la terminal:

```bash
gh run watch
```

**5. Ver el cambio en producción (paso 13).** El trabajo `desplegar` solo
inicia el despliegue; la plataforma tarda unos cinco minutos en construir y
arrancar la versión nueva (pestaña **Events** de Render). Mientras tanto, la
versión anterior sigue atendiendo. Comprueben desde la terminal 2:

```bash
curl -s https://eif509-demo-sesion13.onrender.com/v3/api-docs | grep -c "pipeline CI/CD"
```

Responde `0` mientras la versión anterior sigue publicada y `1` cuando la
nueva está en línea. Entonces recarguen Swagger UI
(`https://eif509-demo-sesion13.onrender.com/swagger-ui.html`): la
descripción de la API tiene un párrafo nuevo.

**6. Un pull request que rompe una prueba (paso 14).** En la terminal 1:

```bash
gh pr create --base main --head cambio-con-error --title "IVA al 12 %" --body "Cambio que rompe la regla del IVA (demostración de la Sesión 13)."
```

(Sin la GitHub CLI: en la página del repositorio, **Pull requests → New pull
request**, base `main`, compare `cambio-con-error`.) En la pestaña
**Actions**, `verificar` termina en rojo con tres pruebas fallidas y
`desplegar` aparece como omitido (*skipped*). Producción sigue igual.

### Al terminar

Detengan con `Ctrl+C` el bucle de la terminal 3. Cierren el pull request sin
integrarlo (**Close pull request** en GitHub, o desde la terminal):

```bash
gh pr close cambio-con-error
```

## Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/adriangarro/eif509-demo-sesion13.git
cd eif509-demo-sesion13
```

### 2. Verificar que Docker Desktop está en ejecución

```bash
docker info
```

Las pruebas de integración levantan PostgreSQL con Testcontainers: sin
Docker, `./gradlew check` falla. Si ven `Cannot connect to the Docker
daemon`, abran Docker Desktop y esperen a que termine de iniciar.

### 3. Verificar Java

```bash
java -version
```

Debe reportar 21 o superior.

### 4. (Opcional) Ejecutar la API en local

No hace falta para esta demostración, pero funciona igual que en la Sesión
12:

```bash
docker compose up -d
export JWT_SECRETO=$(openssl rand -base64 48)
./gradlew bootRun
```

`GET /api/v1/pedidos/1` ahora incluye `iva` y `totalConIva`.

## Parte 1 · Las verificaciones, en su computadora

### 5. La regla del IVA y una buena prueba

[`CalculadoraIva`](src/main/java/cr/una/eif509/demo/service/CalculadoraIva.java)
calcula el 13 % de IVA, redondeado a dos decimales:

```java
public static final BigDecimal TARIFA = new BigDecimal("0.13");

public static BigDecimal iva(BigDecimal subtotal) {
    return subtotal.multiply(TARIFA).setScale(2, RoundingMode.HALF_UP);
}

public static BigDecimal totalConIva(BigDecimal subtotal) {
    return subtotal.add(iva(subtotal)).setScale(2, RoundingMode.HALF_UP);
}
```

[`CalculadoraIvaTest`](src/test/java/cr/una/eif509/demo/service/CalculadoraIvaTest.java)
la protege con la versión corregida de la prueba 1 de la actividad:

```java
@Test
void totalConIva_sumaElTrecePorCientoAlSubtotal() {
    // Arrange: dos unidades de 1000 colones
    var subtotal = new BigDecimal("2000.00");

    // Act
    var total = CalculadoraIva.totalConIva(subtotal);

    // Assert
    assertThat(total).isEqualByComparingTo("2260.00");
}
```

**Qué observar**: la estructura AAA (preparar, ejecutar una sola acción,
verificar), un nombre que describe la regla y una aserción sobre el
resultado. Es una regla pura, sin dependencias, así que se prueba en el
nivel más económico: la prueba tarda menos de un milisegundo.

La API expone el resultado en cada pedido (`PedidoResumen`): `total` es el
subtotal (precio × cantidad), y `iva` y `totalConIva` los calcula
`CalculadoraIva`.

### 6. Una prueba del proyecto revisada con los criterios de la actividad

En
[`FacturaServiceTest`](src/test/java/cr/una/eif509/demo/service/FacturaServiceTest.java),
la prueba del camino feliz solo verificaba que se llamara al repositorio:

```java
service.crear(pedidoPor("15000.00"));

verify(facturas).saveAndFlush(any(Factura.class));   // antes: la implementación
```

Ahora verifica el resultado, el monto de la factura:

```java
var factura = service.crear(pedidoPor("15000.00"));

assertThat(factura.getMonto()).isEqualByComparingTo("15000.00");   // ahora: el comportamiento
```

**Qué observar**: la versión anterior pasaba aunque la factura se creara con
un monto incorrecto. Es el defecto de la prueba 4 de la actividad: verificar
llamadas internas en lugar del resultado.

### 7. El análisis estático con Checkstyle

[`build.gradle`](build.gradle) aplica el plugin `checkstyle` con las reglas de
[`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml):
importaciones sin usar o con comodín, bloques `catch` vacíos, comparaciones
de cadenas con `==`, `equals` sin `hashCode`, `if` sin llaves, entre otras.

```groovy
checkstyle {
    toolVersion = '10.18.1'
    maxWarnings = 0
}
```

Para ver que funciona, agreguen una importación sin usar a cualquier clase
(por ejemplo, `import java.util.List;` en `CalculadoraIva.java`) y ejecuten:

```bash
./gradlew checkstyleMain
```

```text
> Task :checkstyleMain FAILED
[ant:checkstyle] [ERROR] .../service/CalculadoraIva.java:5:8: Importación no usada - java.util.List. [UnusedImports]
```

Quiten la línea antes de continuar.

### 8. El umbral de cobertura y `./gradlew check`

El umbral de la lámina 16 está en [`build.gradle`](build.gradle):

```groovy
jacocoTestCoverageVerification {
    dependsOn test
    violationRules {
        rule {
            element = 'PACKAGE'
            includes = ['cr.una.eif509.demo.service']
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.70
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

**Qué observar**: si la capa de negocio (el paquete `service`) baja del 70 %
de líneas cubiertas, la tarea falla, y con ella `check` y el pipeline. La
regla de la rúbrica queda verificada de forma automática.

```bash
./gradlew clean check
```

Salida esperada (resumida):

```text
> Task :checkstyleMain
> Task :checkstyleTest
> Task :test
  CalculadoraIvaTest           totalConIva_sumaElTrecePorCientoAlSubtotal()  SUCCESS      0 ms
  ...
> Task :jacocoTestCoverageVerification
> Task :jacocoTestReport
BUILD SUCCESSFUL
```

Un solo comando ejecuta la compilación, las 76 pruebas (unitarias, de la API
y de integración), Checkstyle y el umbral de cobertura. Es exactamente lo que
ejecuta el trabajo `verificar` del pipeline: lo que pasa en su computadora
pasa en la integración continua.

### 9. Lo que detecta la regla del IVA: prueba de mutación manual

Cambien a propósito el 13 % por 12 % y ejecuten las pruebas unitarias (es lo
que contiene la rama `cambio-con-error`):

```bash
git switch cambio-con-error && ./gradlew unitTest
```

```text
  PedidoMapperTest             copiaSoloLoQueLaVistaNecesita()               FAILURE
  CalculadoraIvaTest           iva_seRedondeaADosDecimales()                 FAILURE
  CalculadoraIvaTest           totalConIva_sumaElTrecePorCientoAlSubtotal()  FAILURE
52 tests completed, 3 failed
BUILD FAILED
```

Vuelvan a `main`:

```bash
git switch main
```

**Qué observar**: si al introducir un error a propósito ninguna prueba
fallara, esa regla no estaría protegida, aunque la cobertura fuera alta.

## Parte 2 · El pipeline en GitHub Actions

### 10. El workflow

[`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) es el de la
lámina 17:

```yaml
name: CI/CD

on:
  push:
    branches: [main]
  pull_request:

jobs:
  verificar:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle
      - run: ./gradlew check

  desplegar:
    needs: verificar
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: curl -fsS -X POST "${{ secrets.DEPLOY_HOOK }}"
```

**Qué observar**:

- `verificar` se ejecuta en cada push a `main` y en cada pull request.
  `ubuntu-latest` trae Docker, por eso las pruebas con Testcontainers
  funcionan sin configuración adicional.
- `desplegar` tiene dos condiciones: `needs: verificar` (solo si el primero
  terminó en verde) e `if: github.ref == 'refs/heads/main'` (en un pull
  request, `github.ref` es `refs/pull/N/merge`, así que no se despliega).
- `curl -fsS` hace que el paso falle si la plataforma rechaza la petición, en
  lugar de terminar en verde sin haber desplegado.

### 11. Preparar la plataforma

Los pasos son los de la Sesión 12 (servicio web con lenguaje **Docker**,
plan **Free** y las mismas variables de entorno), con dos diferencias:

1. **Desactivar el despliegue automático.** Al crear el servicio, en
   **Advanced → Auto-Deploy**, elijan **Off** (en un servicio que ya existe:
   **Settings → Deploy → Auto-Deploy → Edit → Off**). Así, un push a
   `main` ya no despliega por sí solo: solo despliega el pipeline, después de
   las verificaciones.
2. **Copiar el deploy hook.** En **Settings → Deploy → Deploy Hook**,
   pulsen el botón de copiar. Es una URL de la forma
   `https://api.render.com/deploy/srv-...?key=...`: cualquiera que la conozca
   puede iniciar un despliegue, por eso es un secreto.

El servicio de esta demostración es `eif509-demo-sesion13`, en
`https://eif509-demo-sesion13.onrender.com`, y usa la misma base PostgreSQL
gestionada de la Sesión 12.

### 12. Guardar el deploy hook como secreto del repositorio

En GitHub: **Settings → Secrets and variables → Actions → New repository
secret**, nombre `DEPLOY_HOOK` y, como valor, la URL copiada. Con la GitHub
CLI, desde la carpeta del repositorio:

```bash
gh secret set DEPLOY_HOOK
```

El comando pide el valor sin mostrarlo en pantalla. Para comprobarlo:

```bash
gh secret list
```

```text
DEPLOY_HOOK	2026-10-15T19:30:18Z
```

**Qué observar**: el archivo del workflow solo nombra el secreto
(`${{ secrets.DEPLOY_HOOK }}`). Los archivos del repositorio forman parte
de su historial; los secretos de GitHub se guardan cifrados y en los
registros de ejecución aparecen como `***`. El nombre debe ser exactamente
`DEPLOY_HOOK`: si no existe, la URL llega vacía y `desplegar` falla en el
paso `curl` (por ejemplo, con `blank argument where content is expected`).

### 13. Un cambio que pasa el pipeline y se despliega solo

La rama `cambio-visible` agrega un párrafo a la descripción de la API en
[`DemoApplication.java`](src/main/java/cr/una/eif509/demo/DemoApplication.java).
Intégrenla a `main`:

```bash
git switch main && git pull && git merge --no-edit origin/cambio-visible && git push
```

En la pestaña **Actions** aparece una ejecución nueva de **CI/CD**:

1. `verificar` (unos dos minutos): compila, ejecuta las 76 pruebas, Checkstyle
   y el umbral de cobertura.
2. Cuando termina en verde, empieza `desplegar` (unos segundos): llama al
   deploy hook. En los registros, la URL aparece como `***`.
3. En Render, pestaña **Events**, aparece `Deploy started` con el commit del
   merge y la indicación **Triggered via Deploy Hook**: lo inició el
   pipeline, no el push. La construcción y el arranque tardan unos cinco minutos en el plan
   gratuito; mientras tanto, la versión anterior sigue atendiendo.

Desde la terminal, para seguir la ejecución:

```bash
gh run watch
```

Y para saber si la versión nueva ya está publicada:

```bash
curl -s https://eif509-demo-sesion13.onrender.com/v3/api-docs | grep -c "pipeline CI/CD"
```

`0` significa que sigue la versión anterior; `1`, que la nueva ya está en
línea. Recarguen Swagger UI: la descripción de la API termina con «Versión
publicada por el pipeline CI/CD de la Sesión 13: solo llega a producción lo
que pasó las pruebas.» Si no aparece, puede ser la caché del navegador:
recarguen sin caché o ábranla en una ventana privada.

**Qué observar**: después del merge no hubo ningún paso manual. Lo que llegó
a producción pasó antes por las 76 pruebas, Checkstyle y el umbral de
cobertura.

### 14. Un cambio que rompe una prueba no llega a producción

La rama `cambio-con-error` cambia el IVA de 13 % a 12 %. Abran un pull
request:

```bash
gh pr create --base main --head cambio-con-error --title "IVA al 12 %" --body "Cambio que rompe la regla del IVA (demostración de la Sesión 13)."
```

En la pestaña **Actions** (o en la pestaña **Checks** del pull request),
`verificar` termina en rojo:

```text
CalculadoraIvaTest > totalConIva_sumaElTrecePorCientoAlSubtotal() FAILED
CalculadoraIvaTest > iva_seRedondeaADosDecimales() FAILED
PedidoMapperTest > copiaSoloLoQueLaVistaNecesita() FAILED
76 tests completed, 3 failed
```

Para ver el estado desde la terminal:

```bash
gh pr checks cambio-con-error
```

**Qué observar**: la prueba del IVA falló, así que el pipeline se detuvo. El
trabajo `desplegar` aparece como omitido (*skipped*): no se ejecutó (en un
pull request tampoco se ejecutaría, por la condición de la rama) y
producción sigue igual. Al
terminar, cierren el pull request sin integrarlo:

```bash
gh pr close cambio-con-error
```

### 15. (Opcional) Proteger la rama main

Hoy, GitHub todavía permite integrar el pull request aunque el pipeline esté
en rojo. Para impedirlo: **Settings → Branches → Add branch ruleset** (o
**Add classic branch protection rule**), rama `main`, y activen **Require a
pull request before merging** y **Require status checks to pass**, con el
check `verificar`. Desde ese momento, un cambio con pruebas en rojo no se
puede integrar, y los cambios a `main` llegan por pull request revisado por
la pareja. (En este repositorio no está activada, para poder integrar
`cambio-visible` con `git push` en el paso 13.)

## La actividad: ¿qué no está probando esta prueba?

| Prueba | Qué no verifica | Cómo corregirla | En este repositorio |
|---|---|---|---|
| 1 · Sin aserción | Ejecuta el cálculo pero no revisa el resultado | Agregar una aserción sobre el total esperado | `CalculadoraIvaTest` (paso 5) |
| 2 · Prueba el doble | Verifica la respuesta del mock que la misma prueba configuró | Probar la clase real; simular solo sus colaboradores | `FacturaServiceTest` prueba el `FacturaService` real y simula solo el repositorio |
| 3 · Depende del orden | Usa datos que creó otra prueba | Cada prueba crea sus propios datos; sin `static` compartido ni `@Order` | Las pruebas `*IT` crean sus propios pedidos |
| 4 · Verifica la implementación | Revisa llamadas internas, no el resultado | Verificar el estado final y solo la interacción que es parte de la regla | `FacturaServiceTest` (paso 6) |

## Para repetir la demostración

Después de la clase, `main` ya contiene el cambio visible. Para repetir el
paso 13, creen una rama nueva desde `main` con otro cambio visible (por
ejemplo, otro texto en la descripción de la API), súbanla e intégrenla de la
misma forma. El paso 14 se repite tal cual: `cambio-con-error` sigue
rompiendo la regla del IVA.

## Para su propio proyecto (taller, opción A)

1. Copien [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml)
   y el bloque `checkstyle` de [`build.gradle`](build.gradle); ejecuten
   `./gradlew checkstyleMain checkstyleTest` y corrijan lo que reporte.
2. Verifiquen que `jacocoTestCoverageVerification` apunta al paquete de su
   capa de negocio y que `check` depende de él.
3. Reemplacen su workflow por [`ci-cd.yml`](.github/workflows/ci-cd.yml).
4. En la plataforma, desactiven el Auto-Deploy y copien el deploy hook
   (paso 11); guárdenlo como secreto `DEPLOY_HOOK` (paso 12).
5. Hagan un push a `main` y sigan la ejecución en **Actions**.

## Comandos útiles

| Acción | Comando |
|---|---|
| Todas las verificaciones (lo que ejecuta el pipeline) | `./gradlew check` |
| Solo las pruebas unitarias (sin Docker) | `./gradlew unitTest` |
| Solo el análisis estático | `./gradlew checkstyleMain checkstyleTest` |
| Reporte de cobertura | `build/reports/jacoco/test/html/index.html` |
| Reporte de Checkstyle | `build/reports/checkstyle/main.html` |
| Ver los secretos del repositorio (solo nombres) | `gh secret list` |
| Seguir la última ejecución del pipeline | `gh run watch` |
| Historial de ejecuciones | `gh run list` |
| ¿Ya está publicada la versión nueva? | `curl -s https://eif509-demo-sesion13.onrender.com/v3/api-docs \| grep -c "pipeline CI/CD"` |
| Despertar el servicio en la nube | `curl -s -o /dev/null -w "%{http_code}\n" https://eif509-demo-sesion13.onrender.com/api/v1/productos` |

## Solución de problemas

| Problema | Causa | Solución |
|---|---|---|
| `verificar` falla por Testcontainers (`Could not find a valid Docker environment`) | El runner no tiene Docker | Usar `runs-on: ubuntu-latest`; en macOS y Windows los runners de GitHub no traen Docker |
| `./gradlew check` falla en local por Testcontainers | Docker Desktop no está en ejecución | Abrir Docker Desktop y reintentar |
| `desplegar` no aparece después de un push a `main` | `verificar` terminó en rojo, o la condición de la rama no coincide | Revisar `verificar`; la condición debe ser exactamente `if: github.ref == 'refs/heads/main'` |
| `desplegar` falla en el paso `curl` (`blank argument` o `no URL specified`) | El secreto no existe o tiene otro nombre | Crear el secreto con el nombre exacto `DEPLOY_HOOK` (paso 12) |
| `desplegar` falla con `curl: (22) ... 404` | El deploy hook se regeneró en la plataforma | Copiar el hook nuevo y actualizar el secreto |
| La plataforma despliega en cada push, sin esperar al pipeline | El Auto-Deploy sigue activo | **Settings → Deploy → Auto-Deploy → Off** (paso 11) |
| El cambio visible no aparece | La plataforma todavía está construyendo, o es la caché del navegador | Revisar **Events** en Render; recargar sin caché o abrir en una ventana privada |
| La primera respuesta tarda uno o dos minutos | El plan gratuito suspendió el servicio por inactividad | Es esperable; dejar el bucle de «Antes de empezar» en ejecución |
| `jacocoTestCoverageVerification` falla | La capa de negocio bajó del 70 % | Ver el reporte de cobertura y agregar pruebas a las reglas sin cubrir |
| `checkstyleMain` falla | Una regla de Checkstyle no se cumple | El mensaje indica archivo, línea y regla; el detalle está en `build/reports/checkstyle/main.html` |
| `git merge origin/cambio-visible` responde `not something we can merge` | El clon no tiene las ramas remotas actualizadas | Ejecutar `git fetch` y reintentar |
| `gh: command not found` | Falta la GitHub CLI | Instalarla, o hacer el paso desde la página de GitHub |
| `Unable to locate a Java Runtime` | Falta el JDK o `JAVA_HOME` | Ver Requisitos previos |

Si todo falla durante la clase, la pestaña **Actions** conserva el historial:
muestren una ejecución anterior exitosa (con `desplegar` en verde) y una
fallida. El repositorio ya tiene ambas: la del commit del README en `main`
y la del pull request de ensayo «IVA al 12 % (ensayo)», cerrado sin
integrar.

## Relación con los laboratorios

**Laboratorio 6** (entrega el jueves 22 de octubre a las 6:00 p. m.): este
repositorio no agrega requisitos nuevos; si su despliegue todavía tiene
problemas, el taller de hoy es para resolverlos (opción B).

**Laboratorio 7** (se asigna el jueves 22): pedirá al menos 40 pruebas en
verde en la integración continua y un pipeline como el de este repositorio.
Este proyecto tiene 76 pruebas en tres niveles, el umbral de cobertura, el
análisis estático y el despliegue automático desde `main`.

---

> **Material de referencia del curso.** Las pruebas y el pipeline de su
> laboratorio deben corresponder a su propio proyecto; no copien este
> ejemplo.
