# Arquitectura de DivTrack

## 1. Módulos y por qué están separados así

Ocho módulos Gradle. La separación no es decorativa: **Gradle impone las reglas**, así que
una violación de Clean Architecture no es un comentario en una revisión de código, es un
error de compilación.

```
:core:common      (Kotlin/JVM puro)  Result<T>, DataError, formateo, parser de importes, reloj
:core:network     (Android library)  Retrofit dual, interceptores, RateProvider + registro
:core:database    (Android library)  Room: tasa vigente + histórico diario
:core:datastore   (Android library)  DataStore: preferencias del usuario
:domain           (Kotlin/JVM puro)  modelos, contratos, 9 casos de uso
:data             (Android library)  repositorios, orquestación multi-proveedor, WorkManager
:presentation     (Android library)  Compose + MVI + tema + Navigation 3
:app              (Android app)      Application, MainActivity, recursos, R8
```

```
:app ─▶ :presentation ─▶ :domain ◀─ :data ─▶ :core:network
                                      │      ├─ :core:database
                                      │      └─ :core:datastore
                                      └──────▶ :core:common
```

Lo que esto garantiza, en la práctica:

| Regla | Cómo se cumple |
|---|---|
| El dominio no conoce Android | `:domain` usa el plugin `kotlin-jvm`: `android.*` no está en su classpath |
| El dominio no conoce red ni base de datos | `:domain` no depende de `:core:network` ni `:core:database` |
| La UI no conoce Retrofit ni Room | `:presentation` depende solo de `:domain` y `:core:common` |
| El núcleo no se contamina | `:core:common` también es `kotlin-jvm` |

`androidx.room` y `androidx.datastore` exponen contratos *puros* (`RateLocalDataSource`,
`UserPreferencesDataSource`) con la implementación al lado. Por eso `:data` sabe persistir
sin depender de las clases de la plataforma y sus tests corren en JVM sin emulador.

---

## 2. Patrón Online-First

**Room es la única fuente de verdad para la UI. La red nunca lee: solo escribe.**

```
UI ──observe──▶ Room ──emite──▶ ViewModel ──state──▶ Compose
                 ▲
        upsert   │
Proveedores ─────┘   (DolarAPI VE, Yadio)
```

Consecuencias reales:

1. **Nunca hay estado "cargando" que borre la pantalla.** La UI ya tiene datos locales.
2. **Un fallo de red no modifica nada.** El repositorio devuelve `Result.Error` y no toca
   Room; la pantalla conserva sus tarjetas y aparece el aviso de sin conexión.
3. **La sincronización de fondo actualiza las tres pantallas sin código extra:** escribe en
   Room, Room re-emite, Compose repinta.

### Resolución por tasa, no por proveedor

`ProviderRegistry` recorre los proveedores en orden de prioridad y guarda **la primera tasa
que aparece para cada fuente**. Después de cada proveedor comprueba si ya tiene todas y, si
es así, para. Esto significa que:

- DolarAPI caído → el paralelo sigue llegando por Yadio y solo se degrada el oficial.
- Los fallos no se propagan: se acumulan en `ProviderFailure` y la sincronización se marca
  como *parcial*, que es un aviso suave y no un error.

### Single-flight

`RateRepositoryImpl` serializa las sincronizaciones con un `Mutex`. El arranque, el
pull-to-refresh y WorkManager no pueden disparar tres pasadas simultáneas contra las APIs.

---

## 3. MVI por pantalla

Cada pantalla tiene el mismo contrato, sin excepciones:

```
XxxContract.kt    intent (lo que el usuario hizo) + effect (evento de un solo uso)
XxxUiState.kt     estado inmutable (@Immutable) ya formateado para pintar
XxxViewModel.kt   reduce: intent + fuentes de datos -> estado; estado -> SharedFlow de efectos
XxxScreen.kt      XxxRoute (única función que toca el ViewModel) + XxxScreen (sin estado)
```

- **`StateFlow<UiState>`** para el estado. `@Immutable` + campos primitivos/enums para que
  Compose pueda saltarse recomposiciones.
- **`SharedFlow<Effect>`** con `extraBufferCapacity = 1` y `DROP_OLDEST` para snackbars y
  portapapeles: son eventos, no estado, y no deben repetirse al rotar.
- **El ViewModel nunca toca `Context`.** Los textos viajan como `UiText.Res(id)` y se
  resuelven en Compose: así los ViewModel se testean en JVM pura.
- **Los composables de UI no reciben ViewModel.** Solo estado y lambdas; `key` estable en
  las listas.

### Estados y cálculos

- Panel: `spread` derivado una sola vez por emisión de Room.
- Calculadora: `combine` de 5 flujos + `stateIn(WhileSubscribed(5 s))`. Nadie observa =
  nadie calcula; por eso el `derivedStateOf` de la pantalla es la pieza crítica que pide el
  pliego (y está implementado con `rememberUpdatedState`, porque sin él capturaría el
  estado de la primera composición).
- Histórico: se re-observa la serie con `collectLatest` al cambiar de rango (nunca dos
  consultas vivas) y el detalle del día seleccionado se deriva con `derivedStateOf`.

---

## 4. Capa de red

Un solo `OkHttpClient` compartido (pool de conexiones y caché de disco de 10 MB) y dos
instancias de `Retrofit` que solo cambian de `baseUrl`.

Orden de interceptores, de fuera hacia dentro:

1. **HeadersInterceptor** — `Accept`, `Accept-Language: es-VE`, `User-Agent`.
2. **CacheFallbackInterceptor** — si la red falla, reintenta contra la caché en disco;
   si tampoco hay nada, propaga el error original.
3. **RetryInterceptor** — backoff exponencial con jitter ante `IOException`, 408, 429 y 5xx,
   respetando `Retry-After` (acotado a 3 s para no castigar al usuario).
4. **HttpLoggingInterceptor** — solo en debug, y solo cabeceras.

`NetworkErrorMapper` es el **único** sitio del proyecto que conoce `HttpException`,
`SerializationException`, etc. De ahí hacia arriba todo habla `Result<T>` + `DataError`.

Además, `RateRepositoryImpl` consulta `ConnectivityObserver` **antes** de lanzar la petición:
si no hay red, devuelve el error de inmediato en lugar de esperar al timeout de 10 s. El
usuario ve el aviso instantáneo y el dispositivo no gasta radio ni batería intentándolo.

---

## 5. Rendimiento (gama baja, 60 fps)

| Decisión | Motivo |
|---|---|
| Sin sombras: superficies planas con borde de 1 dp | La elevación se dibuja en cada fotograma |
| Esqueleto de carga estático, sin shimmer | Una animación infinita consume presupuesto de fotogramas para nada |
| Gráfico en un único `Canvas` | Sin librería de charts y sin asignaciones por fotograma (`remember(series)`) |
| Flechas de tendencia dibujadas con `Path` | Más barato que medir y componer un `ImageVector` |
| `@Immutable` en los modelos de estado | Saltos de recomposición reales |
| `key` por fuente en las listas | Actualizar una tasa no recompone la otra tarjeta |
| `keyboardType = Decimal` y saneado en el ViewModel | El campo no pelea con el cursor y el cálculo no bloquea el hilo de UI |
| Caché HTTP + WorkManager con restricción de red | Cero despertares inútiles del proceso |
| ViewModels acotados por `NavEntry` | Al salir de una pestaña, su ViewModel se limpia |

Las métricas del compilador de Compose se generan en CI (`-PcomposeMetrics=true`) y se
publican como artefacto: son la prueba objetiva de estabilidad (clases inestables, etc.).

---

## 6. Navegación (Navigation 3)

Cuatro pestañas planas sin jerarquía. `NavDisplay` con un back stack persistente
(`rememberNavBackStack`), decoradores de estado guardado y de `ViewModelStoreOwner` por
entrada, y `entryProvider` tipado con claves `@Serializable` (`data object : NavKey`).

Cambiar de pestaña mantiene la pila acotada a `[raíz, pestaña]`: "atrás" vuelve al panel de
tasas y, desde ahí, sale de la app — el comportamiento que Android espera de una barra de
navegación, sin pilas que crezcan sin límite.

El tema se observa **por encima** de la navegación (`AppViewModel` con `SharingStarted.Eagerly`),
así que cambiarlo en Ajustes repinta toda la app al instante.

---

## 7. Inyección de dependencias

Hilt en todo el proyecto. Módulos relevantes:

| Módulo Hilt | Provee |
|---|---|
| `NetworkModule` / `NetworkBindingsModule` | `Json`, `Cache`, `OkHttpClient`, 2 `Retrofit`, servicios, `RateProvider` en `Set` (`@IntoSet`), `ConnectivityObserver`, `Logger` |
| `DatabaseModule` + bindings | `DivTrackDatabase`, `RateLocalDataSource` |
| `DataStoreModule` + bindings | `DataStore<Preferences>` (una sola instancia por proceso), `UserPreferencesDataSource` |
| `DispatchersModule` | `@IoDispatcher`, `@DefaultDispatcher` (sustituibles en tests) |
| `DataModule` | `TimeProvider` (reloj en hora de Venezuela) |
| `RepositoryModule` | contratos de `:domain` → implementaciones de `:data` |

Añadir un proveedor de tasas nuevo = implementar `RateProvider` y registrarlo en
`NetworkModule`. Ni el dominio ni la UI cambian.
