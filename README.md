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
| `GET https://ve.dolarapi.com/v1/dolares` | 200 · `oficial` **859,06** Bs · `paralelo` **954,55** Bs (2026-09-30) |
| `GET https://ve.dolarapi.com/v1/historicos/dolares/{fuente}` | 200 · serie diaria desde 2023 (base del gráfico YTD) |
| `GET https://api.yadio.io/exrates/USD` | 200 · `USD.VES` = 954,55 (fuente upstream del paralelo) |
| `pydolarve.org` | **descartada**: el dominio no resuelve (DNS) |
| `api.dolarvzla.com` | **descartada**: responde 401, requiere clave privada |
| Recursos de texto | 84 cadenas referenciadas = 84 definidas · es-VE e inglés con los mismos argumentos |
| Símbolos internos importados | 109/109 existen (los 2 restantes son `R` y `BuildConfig`, generados) |
| Estructura del código | 122/122 archivos con `package` = ruta, llaves balanceadas, 0 imports sin uso |
| Grafo de inyección | Cada dependencia de cada constructor tiene `@Provides`, `@Binds` o `@IntoSet` |
| Entorno local | **sin JDK, sin Android SDK, sin Gradle**: la compilación se valida en CI |

> Consecuencia práctica: **el proyecto no se ha compilado en esta máquina**. Todo el
> toolchain está fijado a versiones verificadas contra Google Maven / Maven Central y el
> workflow de GitHub Actions compila debug, ejecuta los tests y valida R8 en release.

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
:core:network     Retrofit dual (DolarAPI + Yadio), 4 interceptores, RateProvider + registro.
:core:database    Room: tasas vigentes e histórico diario.
:core:datastore   DataStore: tema, IGTF, fuente por defecto, frecuencia de sync.
:domain           Kotlin/JVM puro. Modelos, contratos de repositorio y 9 casos de uso.
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
`local.properties` (`dolarapi.baseUrl`, `yadio.baseUrl`).

### CI

`.github/workflows/android-ci.yml` se ejecuta en cada push y en cada PR: JDK 21, SDK 37,
compila debug, corre los tests, compila release con R8 y publica APK, reportes y métricas
del compilador de Compose como artefactos.

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

### Histórico
Rangos 1M / 3M / YTD / 1A sobre la serie diaria real del proveedor, dibujada en un `Canvas`
propio (sin librerías de gráficos). Tocar o arrastrar muestra el valor de un día concreto
(crosshair) y debajo van las estadísticas del rango: último, mínimo, máximo y variación.

### Ajustes
Tema (Sistema / Claro / Oscuro, aplicado al instante), tasa por defecto de la calculadora,
IGTF por defecto, sincronización en segundo plano con su frecuencia (15/30/60/120 min),
versión instalada y atribución de proveedores.

---

## Comportamiento sin conexión

1. Al abrir, la UI pinta al instante lo que hay en Room.
2. En paralelo se lanza la sincronización.
3. Si la red responde, Room se actualiza y **las tres pantallas se repintan solas**.
4. Si falla, los datos locales **no se tocan** y aparece el aviso
   *"Sin conexión. Mostrando última actualización"*.
5. Si un proveedor responde y otro no, la sincronización es **parcial**: se avisa y se
   conservan las tasas que sí llegaron.
6. Si el dispositivo no tiene red, la app lo sabe **antes** de intentarlo: el aviso sale al
   instante, sin esperar al timeout de 10 s.

---

## Tests

55 casos JVM (sin emulador), centrados en lo que puede romperse en silencio:

| Módulo | Qué se prueba |
|---|---|
| `:core:common` | Parser de importes es-VE (`1.234,56` / `1,234.56` / `1.234`), formateo de moneda, `Result` y cancelación de corrutinas |
| `:domain` | IGTF bidireccional (ejemplo del pliego: $10 a 36,5 → **375,95 Bs**), brecha, rangos del histórico |
| `:data` | Online-First: la red escribe y no destruye caché, resolución **por tasa** entre proveedores, fallo inmediato sin conectividad, TTL de 24 h del histórico |
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
