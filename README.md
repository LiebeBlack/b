# DivTrack

App Android nativa para rastrear en tiempo real las dos tasas que importan en Venezuela —
**dólar oficial (BCV)** y **dólar paralelo** — y convertir montos entre bolívares y dólares
en ambos sentidos, con el IGTF (3 %) opcional.

- **Kotlin 100 %**, Jetpack Compose con Material 3, Clean Architecture modular, MVI estricto.
- **Online-First**: Room es la única fuente de verdad de la UI; la red solo escribe.
- **Multi-API real**: si un proveedor cae, el otro completa las tasas que falten.
- Pensada para gama baja: superficies planas, sin animaciones infinitas, `derivedStateOf`
  donde de verdad importa y ViewModels acotados por pantalla.

---

## Estado verificado del proyecto

Lo que sigue se comprobó **ejecutando** cosas, no asumiendo:

| Comprobación | Resultado |
|---|---|
| `GET https://ve.dolarapi.com/v1/dolares` | 200 · `oficial` **860,18** Bs · `paralelo` **955,71** Bs (reverificado 2026-10-01) |
| `GET https://api.yadio.io/exrates/USD` | 200 · `USD["VES"]` presente (fuente upstream del paralelo) |
| `GET https://open.er-api.com/v6/latest/USD` | 200 · `rates["VES"]` **860,18** (tercera fuente, independiente; reverificado 2026-10-01) |
| `pydolarve.org` | **descartada**: el dominio no resuelve (DNS) |
| `api.dolarvzla.com` | **descartada**: responde 401, requiere clave privada |
| `criptoya.com/api/USD/VES` | **descartada**: responde 422 en las variantes probadas |
| `:app:assembleDebug` | **BUILD SUCCESSFUL** · APK de depuración de 22,8 MB |
| `:app:assembleRelease` | **BUILD SUCCESSFUL** con R8 y `shrinkResources` · APK de 2,37 MB |
| Tests JVM de los 4 módulos | **49 casos, 0 fallos** |
| `:app:lintDebug` | 1 error pendiente: ruta de Windows sin escapar en `local.properties` (archivo local, fuera de git) |

> Toolchain real montado y usado para compilar: JDK 21 (Temurin), `cmdline-tools` de 2026 con
> el CLI nuevo, **plataforma `android-37.0`** (ojo: `platforms;android-37` no existe) y
> `build-tools 37.0.0`.
>
> **Alcance de esta cifra.** La compilación y los 49 tests se ejecutaron **antes** de la última
> pasada de cambios (la que elimina el Histórico y arregla conexión, permisos, rendimiento y el
> leak). Esa pasada se hizo a petición explícita **sin volver a compilar ni ejecutar tests**, así
> que hasta el próximo `./gradlew :app:assembleDebug` los números de arriba describen el estado
> previo.

---

## Stack (2026)

| Pieza | Versión | Nota |
|---|---|---|
| Android Gradle Plugin | 9.4.0 | Kotlin integrado + DSL nuevo (`android.newDsl`) |
| Gradle | 9.6.0 | wrapper incluido y verificado por checksum |
| Kotlin | 2.4.20 | sin plugin `kotlin-android` (lo aporta AGP 9) |
| KSP | 2.3.12 | KSP2, ya desacoplado de la versión de Kotlin |
| Compose BOM | 2026.09.00 | Material 3 |
| Hilt | 2.60.1 | ≥ 2.59 es requisito para AGP 9 |
| Room | 2.8.5 | esquema exportado y versionado |
| Retrofit / OkHttp | 3.0.0 / 5.5.0 | conversor oficial de kotlinx.serialization |
| Navigation 3 | 1.2.0 | + `lifecycle-viewmodel-navigation3` para ViewModel por entrada |
| WorkManager | 2.12.0 | sincronización silenciosa |
| DataStore Preferences | 1.2.0 | preferencias |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | edge-to-edge nativo |

---

## Estructura

```
:core:common      Kotlin/JVM puro. Result<T>, DataError, formateo es-VE, parser de importes.
:core:network     Retrofit triple (DolarAPI + Yadio + ER-API), 4 interceptores, RateProvider + registro con circuit breaker y frescura.
:core:database    Room: tasa vigente + cierre diario (base de la flecha de tendencia).
:core:datastore   DataStore: tema, IGTF, fuente por defecto, frecuencia de sync.
:domain           Kotlin/JVM puro. Modelos, contratos de repositorio y 7 casos de uso.
:data             Implementaciones, orquestación multi-proveedor, WorkManager.
:presentation     Compose + MVI por pantalla + tema M3 + Nav3.
:app              Application (Hilt + WorkManager), MainActivity, recursos, R8.
```

Dirección de dependencias, **impuesta por Gradle** y no por convención:

```
:app ─▶ :presentation ─▶ :domain ◀─ :data ─▶ :core:{network, database, datastore}
                                        └──▶ :core:common (base pura para todos)
```

`:domain` no ve Android ni Compose ni Retrofit ni Room. Detalle en
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Cómo compilar

### Opción A — Android Studio (recomendado)

1. Abre la carpeta del proyecto (`File ▸ Open`).
2. Deja que sincronice: el wrapper fija Gradle 9.6.0 y el catálogo fija todo lo demás.
3. Ejecuta la configuración `app`. Requiere Android Studio de la generación que soporte
   AGP 9.4 (Otter 3 o posterior) y JDK 21.

### Opción B — línea de comandos

```bash
cp local.properties.example local.properties   # opcional: personaliza las URLs base
./gradlew :app:assembleDebug                   # APK de depuración
./gradlew testDebugUnitTest :domain:test :core:common:test   # tests JVM
./gradlew :app:assembleRelease -PcomposeMetrics=true         # R8 + métricas de Compose
```

Sin `local.properties` **también compila**: las URLs base caen a los endpoints públicos
verificados. Ninguna URL está escrita en el código Kotlin: entran por `BuildConfig` desde
`local.properties` (`dolarapi.baseUrl`, `yadio.baseUrl`, `exchangerateapi.baseUrl`).

### CI

`.github/workflows/android-ci.yml` se ejecuta en cada push y en cada PR: JDK 21, plataforma
`android-37.0` + `build-tools 37.0.0`, compila debug, corre los tests, compila release con R8
y publica APK, reportes y métricas del compilador de Compose como artefactos.

El APK para instalar se descarga desde **Releases**, no desde el artefacto de CI:
`app-release-unsigned.apk` no está firmado. El flujo de publicación verifica la firma y el
`applicationId` antes de subir el archivo. Sin secretos de firma configurados publica el APK
debug (`com.liebeblack.divtrack.debug`), firmado con una clave temporal del runner; para
actualizar ese canal puede ser necesario desinstalar la versión anterior. Las versiones
release (`com.liebeblack.divtrack`) deben firmarse siempre con la misma clave para instalarse
como actualización.

Ojo con el nombre de la plataforma en CI: la de API 37 se publica como **`platforms;android-37.0`**
(o `platforms/android-37.0` con el CLI nuevo). `platforms;android-37` **no existe**, y era el
motivo de que el paso de SDK no dejara nada instalado.

---

## Qué hace la app

### Panel de tasas
Valor en Bs. de cada tasa con acento de color por fuente, flecha de tendencia (verde/rojo)
comparada con el cierre anterior y pie con la procedencia del dato. Debajo, la **brecha**
paralelo/oficial en porcentaje y valor absoluto. Pull-to-refresh y botón de refresco.

### Calculadora bidireccional
Se escribe el monto y el resultado aparece **en cada tecla**: no hay botón "Calcular".
Selector segmentado Oficial/Paralelo, interruptor de IGTF (3 %) y botón para copiar el
desglose con el formato del pliego:

```
DivTrack · Monto: $10 | Tasa Oficial (BCV): 36,50 Bs | IGTF: Sí | Total: 375,95 Bs
Detalle — Oficial (BCV) | Neto: $10,00 | IGTF: 10,95 Bs | Total: $10,30
Calculado con DivTrack
```

Aritmética del IGTF (la parte fácil de equivocar, por eso está documentada y probada):

- **USD → Bs**: `netBs = usd × tasa`, `totalBs = netBs + netBs × 3 %`.
- **Bs → USD**: el monto tecleado **es** el total; `netUsd = totalUsd / 1,03`.

### Ajustes
Tema (Sistema / Claro / Oscuro, aplicado al instante), tasa por defecto de la calculadora,
IGTF por defecto, sincronización en segundo plano con su frecuencia (15/30/60/120 min),
versión instalada y atribución de proveedores.

---

## Comportamiento sin conexión

1. Al abrir, la UI pinta al instante lo que hay en Room.
2. En paralelo se lanza la sincronización.
3. Si la red responde, Room se actualiza y **las tres pantallas se repintan solas**.
4. Si falla, los datos locales **no se tocan** y aparece un aviso **con la causa real**:
   *"Sin conexión a internet…"* si es conectividad, *"La fuente está fallando (HTTP 503)"* si
   el que falla es el proveedor, *"El proveedor tardó demasiado en responder"* si es un
   timeout. Antes todo eso se contaba como "sin conexión" y el usuario revisaba su wifi sin
   motivo.
5. Si un proveedor responde y otro no, la sincronización es **parcial**: se avisa y se
   conservan las tasas que sí llegaron.
6. Si el dispositivo no tiene red, la app lo sabe **antes** de intentarlo: el aviso sale al
   instante, sin esperar al timeout. Y si no sabe seguro si hay red, **lo intenta igual**: un
   falso "no hay internet" es peor que 200 ms de espera.
7. Cuando el fallo es de conectividad, el propio aviso ofrece **"Abrir ajustes de red"**: un
   error sin salida deja al usuario atascado.

---

## Tests

49 casos JVM (sin emulador), centrados en lo que puede romperse en silencio:

| Módulo | Qué se prueba |
|---|---|
| `:core:common` | Parser de importes es-VE (`1.234,56` / `1,234.56` / `1.234`), formateo de moneda, `Result` y cancelación de corrutinas |
| `:domain` | IGTF bidireccional (ejemplo del pliego: $10 a 36,5 → **375,95 Bs**) y brecha |
| `:data` | Online-First: la red escribe y no destruye caché, resolución **por tasa** entre proveedores, fallo inmediato sin conectividad, cierre diario para la tendencia |
| `:presentation` | Cálculo automático al teclear, saneado al pegar, cambio de tasa/dirección, brecha del panel, eventos one-shot, reprogramación de WorkManager |

---

## Correcciones de la revisión final

Una pasada de revisión completa (leyendo el código, no confiando en el recuerdo) encontró y
corrigió esto:

| Hallazgo | Corrección |
|---|---|
| **Bug**: la calculadora ofrecía siempre las dos tasas, aunque una no existiera todavía. Elegirla llevaba a "sin tasas disponibles". | El selector usa `state.rateOptions`: solo ofrece lo que hay en Room, y la tasa por defecto cae en una disponible. Con test propio. |
| La flecha de tendencia se dibuja en un `Canvas`: invisible para TalkBack. | `TrendArrow` publica su significado en la descripción de accesibilidad. |
| El estado `isOffline` existía pero no se veía en pantalla (solo el snackbar, que se va solo). | Aviso persistente bajo la barra mientras dure la falta de conexión. |
| El refresco automático al abrir mostraba el indicador de pull-to-refresh. | El indicador solo aparece en el refresco manual. |
| Código muerto: `findActivity`, `ThemeMode.next`, `cycleThemeMode`, `isDark`, `trendDescription`, una extensión de flujos sin usar y una constante sin usar. | Eliminado, junto con los 3 imports que quedaron huérfanos. |
| Referencias con nombre completo (`com.liebeblack...TrendDirection`) dentro de `when`. | Imports e imports de ayuda; sin rutas completas en el cuerpo. |

Una segunda pasada, esta vez **funcional** (siguiendo el recorrido del usuario en lugar de
leer archivo por archivo), encontró dos fallos que ningún test cubría porque solo se ven al
manipular la interfaz:

| Hallazgo | Corrección |
|---|---|
| **Bug funcional** *(Histórico, pantalla ya eliminada)*: el índice del crosshair vivía dentro de un `derivedStateOf` que capturaba `pointCount` por valor y quedaba congelado en la primera composición. | Se corrigió resolviendo el índice contra la serie real; la pantalla completa se eliminó después (ver «Esta pasada»). |
| **Bug visual** *(Histórico, pantalla ya eliminada)*: el margen interno del gráfico se restaba en píxeles crudos (`14f`), así que en densidad alta el trazado se pegaba al borde. | Se corrigió con una constante en `dp` convertida con `toPx()`; la pantalla completa se eliminó después. |

Una tercera pasada, esta vez **auditando la interfaz** (¿cada control hace algo? ¿cada estado
se pinta? ¿compila?), encontró **dos errores de compilación** que ninguna de las dos
revisiones anteriores vio porque no buscaban esto:

| Hallazgo | Corrección |
|---|---|
| **Error de compilación** *(Histórico, pantalla ya eliminada)*: `HistoryScreen` agrupaba las tarjetas de estadísticas con `key(...)` sin importar `androidx.compose.runtime.key`. El proyecto no compilaba. | Import añadido entonces; el archivo ya no existe. |
| **Error de compilación**: `SpreadChip` pasaba dos `String?` a `stringResource(id, vararg formatArgs: Any)`, que no acepta nulos, y el `if (hasData)` no hace *smart cast*. | Dos copias locales no nulas y comprobación directa, que sí lo hace. |
| *(Histórico, pantalla ya eliminada)* Las series del gráfico se recordaban solo por `state.series`: al cambiar de tema, el gráfico conservaba los colores del tema anterior. | `colors` entraba en las claves del `remember`. |
| Código muerto: `CalculatorUiState.selectedRate`, `SettingsUiState.isLoading`, `DashboardUiState.rate()`, `DivTrackColors.positive`, `ExchangeRate.changeAbsolute`, `SyncSummary.resolvedFromCacheOnly`, `CurrencyFormatters.dollars()`, `CurrencyFormatters.monthLabel()`, el endpoint `getDollar()` y un fallback redundante al resolver la tasa de la calculadora. | Eliminado. Nada de eso se leía ni se llamaba en ningún sitio, tests incluidos. |

Lo que esta pasada dejó **verificado**, no corregido:

| Comprobación | Resultado |
|---|---|
| Textos | 84 claves en es-VE y en inglés con los mismos argumentos, y las 8 llamadas con formato pasan el número exacto de argumentos |
| Interactividad | Cada botón, selector, interruptor y gesto llega a una intención; 0 `onClick` vacíos, 0 `TODO`. El único `onRetry = {}` está en un `@Preview` |
| Estados de pantalla | Panel pinta carga, vacío, error y datos; Ajustes solo datos, porque su fuente es un DataStore ya en memoria |
| Grafo de inyección | 25 constructores `@Inject` y un campo: toda dependencia tiene `@Provides`, `@Binds` o `@IntoSet` |
| Estructura | 112/112 archivos con `package` = ruta, llaves y paréntesis balanceados, 0 imports sin uso, 0 declaraciones huérfanas |

---

## Esta pasada: Histórico fuera, conexión, permisos, rendimiento y el leak

Cinco cosas, en el orden en que molestaban:

| Qué | Antes | Ahora |
|---|---|---|
| **Sección de Histórico** | pestaña propia con gráfico `Canvas`, rangos 1M/3M/YTD/1A, estadísticas y descarga de la serie diaria | **eliminada por completo**: pantalla, gráfico, rangos, casos de uso, modelos, DTO y endpoint. La tabla de cierre diario se queda porque alimenta la flecha de tendencia del panel (ADR 11) |
| **"Las API no funcionan / error de conexión"** | cualquier fallo se mostraba como "sin conexión"; el `ConnectivityManager` podía decir "no hay red" con wifi perfecta y entonces **ni se intentaba**; sin techo de tiempo global; se reintentaba hasta un DNS roto | conectividad que solo bloquea cuando de verdad no hay red, errores por causa real (con código HTTP), `callTimeout` de 20 s, sin reintentos de fallos deterministas (ADR 22). Las dos APIs responden 200 hoy, comprobado en vivo |
| **Lag** | LeakCanary vigilando todos los objetos en el APK de uso diario (73 s de análisis medidos), `entryProvider` reconstruido en cada recomposición, un `DecimalFormat` nuevo por cada cifra formateada | LeakCanary apagado por defecto (`-Pdivtrack.leakcanary=true` para encenderlo) y análisis en otro proceso, grafo recordado, formateadores reutilizados, lista de pestañas construida una vez (ADR 23) |
| **Permisos** | el APK de debug declaraba `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` y `POST_NOTIFICATIONS` (venían de LeakCanary, no de la app) | exactamente `INTERNET` y `ACCESS_NETWORK_STATE`, documentados; tráfico en claro cerrado; y el aviso de conectividad ofrece "Abrir ajustes de red" (ADR 24) |
| **Leak de `SystemJobService`** | se reportaba como fuga propia | diagnosticado como retención del framework (`ResourcesImpl.mAppContext`) y documentado en la ADR 21, con la traza y el porqué |

Nada de esto se volvió a compilar ni se pasó por tests en esta pasada (fue a petición explícita).
El siguiente `./gradlew :app:assembleDebug` es lo que convierte esta tabla en verificada.

---

## Web del proyecto

`web/index.html` es una página **autocontenida** sobre el software: sin JavaScript, sin CDN,
una sola petición de red (ninguna). Explica el producto con maquetas CSS de las tres
pantallas, el flujo Online-First, el diagrama de módulos, el stack con versiones, los comandos
de compilación y una tabla honesta de **qué está comprobado y qué no**. Usa la misma paleta y
los mismos radios que `presentation/theme`, y respeta `prefers-color-scheme`.

Se abre con doble clic (no necesita servidor) o se publica tal cual en GitHub Pages:

```bash
# Settings → Pages → Deploy from a branch → /web
```

---

## Publicar en GitHub

```bash
git init
git add -A
git commit -m "DivTrack: app de tasas oficial/paralelo con Clean Architecture y MVI"
git branch -M main
git remote add origin git@github.com:<tu-usuario>/divtrack.git
git push -u origin main
```

El primer push dispara el workflow: si algo del toolchain nuevo (AGP 9 + Kotlin 2.4 +
Nav3 1.2) cambia de forma, el error aparecerá ahí, con el log completo como artefacto.

---

## Licencia y aviso

El proyecto **no declara licencia todavía**: elegir una (MIT o Apache-2.0 son las habituales
para código abierto) es una decisión tuya, y por eso no hay un `LICENSE` inventado en el
repositorio. Añádelo antes de publicar el repo.

La tipografía **Manrope** sí se distribuye bajo SIL Open Font License 1.1
([licenses/Manrope-OFL.txt](licenses/Manrope-OFL.txt)) y se puede usar y redistribuir
libremente, incluso en apps comerciales.

DivTrack publica tasas de referencia de fuentes públicas. No es una casa de cambio ni un
ente oficial y no se hace responsable de decisiones tomadas con esos valores.
