# Decisiones de arquitectura (ADR)

Cada entrada responde a la misma pregunta: **qué se eligió, qué se descartó y qué cuesta
esa elección**. Las decisiones discutibles se marcan como tales.

---

## 1. AGP 9.4 con Kotlin integrado (built-in Kotlin)

**Decisión.** Toolchain de 2026: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.4.20 sin el plugin
`org.jetbrains.kotlin.android`.

**Contexto.** AGP 9 aporta el compilador de Kotlin integrado y el DSL nuevo
(`android.newDsl`). El plugin `kotlin-android` ya no se aplica y `kotlinOptions {}` no
existe: se usa `kotlin { compilerOptions { jvmTarget } }`.

**Alternativa descartada.** Quedarse en AGP 8.13 + Kotlin 2.2.20 (camino conservador).
Se descartó porque el objetivo era el stack más reciente, y porque con AGP 8 el wiring de
KSP/Kotlin es el clásico que ya nadie va a mejorar.

**Coste y mitigación.** AGP 9 es joven y su combinación con KSP/Hilt/Nav3 puede tener
aristas. Por eso:
- Todas las versiones están fijadas a artefactos que existen en Google Maven / Maven Central.
- `gradle.properties` documenta la **válvula de escape**: `android.builtInKotlin=false` y
  `android.newDsl=false` desactivan el modo nuevo sin bajar de AGP 9.
- El compilador de Compose se aplica con `org.jetbrains.kotlin.plugin.compose`, en la misma
  versión que Kotlin (2.4.20), que es lo que documenta la guía oficial del compilador para
  proyectos con AGP 9.

---

## 2. Navigation 3 en lugar de Navigation Compose

**Decisión.** Nav3 1.2.0 con `NavDisplay`, `rememberNavBackStack` y claves `@Serializable`.

**Motivo.** El back stack es explícito y propiedad de la app (nada de rutas por cadena ni
`NavHost`), y el grafo se resuelve con `entryProvider` tipado.

**Coste.** Hay que añadir `androidx.lifecycle:lifecycle-viewmodel-navigation3` para que cada
entrada tenga su propio `ViewModelStoreOwner`; sin ese decorador, los ViewModel quedarían
atados a la Activity y no se limpiarían al salir de una pestaña.

**Nota de diseño.** Tres pestañas planas (Tasas, Calculadora, Ajustes): el back stack se
mantiene acotado a `[raíz, pestaña actual]`, de modo que "atrás" vuelve al panel de tasas y
desde ahí sale de la app. No se registra un `BackHandler` propio: `NavDisplay` ya gestiona el
gesto y añadir otro provocaría una doble pulsación (pila vacía y contenido en blanco, un bug
visible).

**Rendimiento.** El `entryProvider` se construye dentro de un `remember`: si se creara en
cada recomposición, `NavDisplay` recibiría un grafo nuevo cada vez y volvería a resolver la
entrada activa (scroll perdido, pestaña reconstruida). Es un detalle pequeño con un efecto
visible en gama baja.

---

## 3. Room como única fuente de verdad (Online-First)

**Decisión.** Toda lectura de la UI viene de Room. La red solo escribe.

**Alternativas descartadas.**
- *Repository con caché en memoria*: el usuario perdería los datos al cerrar la app y
  habría que inventar estados "cargando" que no aportan nada.
- *Mostrar directamente la respuesta de la red*: si la API cambia un campo, la pantalla se
  queda vacía; y sin conexión no habría nada que ver.

**Consecuencia.** Hay un pequeño coste de escritura en cada sincronización y, a cambio, la
app abre con datos, funciona sin conexión y **nunca** pierde lo que ya tenía por un fallo
de red.

---

## 4. Multi-proveedor con resolución por tasa

**Decisión.** `ProviderRegistry` resuelve **cada tasa** con el primer proveedor (por
prioridad) que la publique, y no exige que un mismo proveedor responda todo.

**Evidencia recogida antes de decidir** (probado en vivo, 2026-09-30):

| Fuente | Resultado | Decisión |
|---|---|---|
| `ve.dolarapi.com/v1/dolares` | 200 · oficial 860,17 · paralelo 955,35 (reverificado 2026-10-01) | proveedor primario |
| `api.yadio.io/exrates/USD` | 200 · `USD.VES` presente en el mapa de divisas | respaldo del paralelo (es la fuente upstream) |
| `pydolarve.org` | no resuelve por DNS | descartada |
| `api.dolarvzla.com` | 401, requiere clave | descartada |

**Consecuencia.** Que una API caiga degrada una tasa, no la app. Los fallos se registran como
`ProviderFailure` y la sincronización se marca como parcial (aviso suave).

---

## 5. Semántica del IGTF documentada y probada

**Decisión.** En `USD → Bs` el IGTF se **suma** al neto (`totalBs = netBs × 1,03`); en
`Bs → USD` el monto tecleado **es** el total y el impuesto se **descuenta** para hallar el
neto (`netUsd = totalUsd / 1,03`).

**Por qué.** Es la confusión más habitual en apps de este tipo y el punto donde el usuario
detecta un error de inmediato. Está documentado en el KDoc de `Conversion` y cubierto por
tests con el ejemplo exacto del pliego: 10 USD a 36,5 Bs con IGTF → netBs 365,00,
IGTF 10,95, total **375,95 Bs**.

---

## 6. Parser de importes tolerante, no máscara de texto

**Decisión.** El campo acepta `1234,56`, `1.234,56`, `1,234.56` y `1234.56`; el saneado
ocurre en el ViewModel (`NumberParsing.sanitizeAmountInput`) mientras el texto del campo se
deja intacto.

**Alternativa descartada.** Reformatear el texto en cada tecla desde la UI: el cursor salta
y la selección se rompe. Se prefirió un parser listo para la realidad venezolana y un campo
que se comporta como el usuario espera.

**Consecuencia.** `looksLikeDecimal` documenta la ambigüedad real (`1.234` se lee como mil
doscientos treinta y cuatro) y hay tests para cada caso.

---

## 7. Sin color dinámico (Material You)

**Decisión.** La paleta es fija; `dynamicColor` no se usa.

**Motivo.** La identidad visual (menta/ámbar, superficies planas, moderado) es parte del
producto: una app financiera que cambia de color con el fondo de pantalla del usuario
pierde jerarquía justo donde más importa (verde/rojo de tendencia, acento por tasa).

---

## 8. Manrope con dígitos tabulares

**Decisión.** Manrope variable (OFL) con `fontFeatureSettings = "tnum"` en todo lo que son
cifras.

**Motivo.** Los dígitos tabulares impiden que el valor "baile" cuando una tasa se actualiza
en vivo: es la diferencia visual entre una app cuidada y una que no lo está.

**Licencia.** `licenses/Manrope-OFL.txt`; el archivo OFL **no** se empaqueta en `res/`
(AAPT rechaza un `.txt` en una carpeta de recursos: fue un error de empaquetado real que se
corrigió moviéndolo a `licenses/`).

---

## 9. Iconos: `material-icons-core` + `extended` solo para pestañas

**Decisión.** La UI usa los iconos del paquete *core* (Refresh, Clear, Share, Warning, Info)
y se añade *extended* únicamente para las tres pestañas (Calculate, TrendingUp, Settings).

**Motivo.** Los iconos de pestaña son la primera impresión de la app y merecen el icono
correcto; el resto del proyecto no necesita 10 000 vectores. R8 elimina en release todo lo
que no se usa, así que el coste real en el APK final son tres rutas.

---

## 10. DataStore provisto por DI, no por delegado

**Decisión.** `DataStore<Preferences>` se crea con `PreferenceDataStoreFactory` y se provee
como `@Singleton`, en lugar de usar `by preferencesDataStore` en un `Context`.

**Motivo.** Dos instancias sobre el mismo archivo hacen que DataStore lance
`IllegalStateException`. Proveerlo por DI garantiza una sola por proceso y permite
sustituirlo en tests.

---

## 11. Fuera la sección de Histórico; el cierre diario se queda

**Decisión.** Se elimina el Histórico como funcionalidad **completa**: pantalla, gráfico
(`RateLineChart`), selector de rangos (1M/3M/YTD/1A), tarjetas de estadísticas, pestaña en la
barra inferior, casos de uso (`ObserveHistoryUseCase`, `SyncHistoryUseCase`), modelos de
dominio (`HistoryRange`, `RateHistoryPoint`), el endpoint `v1/historicos/dolares/{fuente}`,
su DTO y toda la cadena de importación con TTL.

**Lo que NO se elimina, y por qué.** La tabla `rate_history` (dos filas por día, una por
fuente) sigue existiendo y el repositorio sigue escribiendo en ella el cierre del día: es la
única fuente del "cierre anterior" que alimenta la flecha de tendencia y la variación
porcentual del panel. Borrarla dejaría la tendencia plana **para siempre**, que es peor que
no tener gráfico. Lo que se eliminó es la pantalla y su importación; el dato diario se quedó
porque sostiene otra funcionalidad que el usuario sí usa.

**Nombre conservado a propósito.** La clase sigue siendo `RateHistoryEntity` y la tabla
`rate_history` aunque el concepto ya no sea "histórico": renombrarlos obligaría a una
migración de Room y la ADR 16 ya decidió que este proyecto no cambia el esquema sin migración
escrita. Los métodos que quedan sí hablan claro: `upsertDailyCloses` / `pruneDailyCloses`, y
la retención (2 años) viaja con la sincronización en lugar de vivir en un trabajo aparte, así
que no existe ninguna ruta que escriba cierres sin aplicarles el límite.

**Efecto colateral bueno.** Desaparecen del contrato público `observeHistory` y `syncHistory`,
y con ellos un `Flow` de Room que nadie consumía, un TTL de 24 h y una poda separada.

---

## 12. Copia de seguridad: fuera la base de datos

**Decisión.** Auto-backup y transferencia entre dispositivos **excluyen** la base de Room y
la caché HTTP; las preferencias sí se respaldan.

**Motivo.** Restaurar en otro dispositivo una base de tasas vieja mostraría datos caducados
como si fueran actuales. Las tasas se vuelven a descargar en segundos; el tema, el IGTF y
la frecuencia de sincronización son del usuario y sí viajan.

---

## 13. Hora de Venezuela inyectada, no la del dispositivo

**Decisión.** Todo cálculo de "día" y "cierre anterior" usa `America/Caracas` a través de un
`TimeProvider` inyectable.

**Motivo.** Un usuario de viaje (o con la zona del teléfono mal configurada) no debe ver el
cierre de otro día ni una flecha de tendencia equivocada. Además, los tests fijan el reloj y
dejan de depender del día en que se ejecutan.

---

## 14. `minSdk 26`

**Decisión.** API 26 como mínimo.

**Motivo.** Es el primer nivel con `java.time` en la plataforma: evita el *desugaring* (menos
build, menos sorpresas), cubre prácticamente todo el parque de dispositivos y permite usar
iconos adaptativos e `ImageVector` sin ramas de compatibilidad.

---

## 15. `UiText` en lugar de `Context` en los ViewModel

**Decisión.** Los eventos one-shot llevan `UiText.Res(id)`, no texto ya resuelto.

**Motivo.** Un ViewModel que toca `Context` deja de ser testeable en JVM pura. Con `UiText`,
el snackbar de "Sin conexión. Mostrando última actualización" se prueba con un simple
`assertEquals` sobre el id de recurso.

---

## 16. Room con esquema exportado y sin migración destructiva

**Decisión.** `exportSchema = true` con el esquema versionado en `core/database/schemas`, y
**sin** `fallbackToDestructiveMigration`.

**Motivo.** En una app financiera, perder datos en silencio no es una opción. Tener el JSON
de la versión 1 commiteado permite escribir la migración real cuando haya versión 2.

---

## 17. Tests de flujos sin Turbine

**Decisión.** Los tests de `SharedFlow`/`StateFlow` recolectan con un
`UnconfinedTestDispatcher` y una lista propia; se eliminó la dependencia de Turbine.

**Motivo.** Un `SharedFlow` con `replay = 0` **descarta** lo emitido antes de que alguien se
suscriba: suscribirse primero y avanzar el reloj después es determinista y no depende de
tiempos. Menos dependencias y menos sorpresas intermitentes en CI.

---

## 18. Consultar la conectividad antes de sincronizar

**Decisión.** Si `ConnectivityObserver` informa que no hay red, el repositorio devuelve el
error de inmediato y no llama a ningún proveedor.

**Motivo.** Con solo el timeout de OkHttp (10 s), el usuario mira un spinner inútil y el
móvil gasta radio antes de enterarse de algo que el sistema ya sabía. Con esta comprobación
el aviso "Sin conexión. Mostrando última actualización" aparece al instante.

**Consecuencia.** El repositorio gana una dependencia más, así que los tests inyectan un
observador de conectividad simulado (y hay un test que verifica que **no** se consulta a los
proveedores cuando no hay red).

---

## 19. Sin código muerto

**Decisión.** Se eliminaron las APIs que no usaba nadie: `observeRate`, `observeLastSyncAt`,
`latestHistoryEpochDay`, `clearCurrentRates`, `enqueueImmediate`, las extensiones de flujo
`asResultFlow`/`mapCatchingAsResult` y la constante `DEFAULT_HISTORY_RANGE_MONTHS`.

**Motivo.** Una API pública no usada no es "por si acaso": es superficie que hay que
mantener, documentar y migrar. Si vuelve a hacer falta, se añade en una línea.

**Ampliación (revisión estricta posterior).** La regla se aplicó otra vez, leyendo el código sin
ejecutar nada, y cayeron: `EmptyState` y las claves `empty_rates_*` (el estado que pintaban es
inalcanzable: la app nunca queda sin datos y sin error), `ConnectivityObserver.observe()` y su
`callbackFlow` (nadie consumía el `Flow`; la app solo consulta `isOnline()`),
`UserPreferences.lastSyncAtMillis` / `setLastSyncAt` / la clave `last_sync_at` (no los escribía
ni los leía nadie), los colores `Sky`/`SkyDeep` y los campos `netUsdText`/`netBsText` del estado
de la calculadora (se calculaban y ningún composable los pintaba; el estado ahora expone
exactamente lo que la pantalla muestra) y `Spacing.xxl` (se quedó sin consumidor al eliminar
`EmptyState`: código muerto de segundo orden, que es la razón por la que cada borrado se verifica
con una búsqueda en lugar de darlo por hecho).

**Verificación.** Se hizo con búsquedas mecánicas (referencias colgantes = 0) y no a ojo.

---

## 20. El selector de la calculadora refleja lo que existe

**Decisión.** El selector de tasa se construye con `state.rateOptions` (lo que hay de verdad
en Room) y no con la lista completa de `RateSource`.

**El bug que corrige.** Antes, si un proveedor no había publicado el oficial todavía, el
usuario podía pulsar "Oficial" y la pantalla respondía "Sin tasas disponibles": la app le
offrecía algo que no tenía. Ahora esa opción simplemente no está, y la tasa por defecto cae
en una disponible.

**Por qué se detectó tarde.** El ViewModel ya calculaba `rateOptions`; era la pantalla la que
lo ignoraba y volvía a inventarse la lista. Es el motivo por el que el estado de UI expone
los datos "ya resueltos": si la pantalla puede recalcular una decisión, acaba
desincronizándose.

**Residuo de la misma clase.** Aun después de aquel arreglo, la pantalla conservaba un
`ifEmpty { RateSource.ordered() }` para el caso de Room vacío: con cero tasas volvía a ofrecer
las dos, y pulsar una dejaba el mismo "sin tasas disponibles". Se eliminó en la revisión
estricta posterior: sin tasas no hay chips que pulsar y el texto de ayuda explica qué hacer.

---

## 21. El leak de `SystemJobService` es de Android, no nuestro

**El informe.** LeakCanary 2.14, SDK 33, proceso `com.liebeblack.divtrack.debug`:

```
GC Root: System class
├─ android.content.res.ResourcesImpl class
│    Leaking: NO (a class is never leaking)
│    Library leak match: static field android.content.res.ResourcesImpl#mAppContext
│    ↓ static ResourcesImpl.mAppContext
├─ android.app.ContextImpl instance
│    mOuterContext instance of androidx.work.impl.background.systemjob.SystemJobService
│    ↓ ContextImpl.mOuterContext
╰→ androidx.work.impl.background.systemjob.SystemJobService instance
     Leaking: YES (ObjectWatcher was watching this because ... received Service#onDestroy()
     callback and Service not held by ActivityThread)
```

**Lectura de la traza.** En toda la cadena **no aparece ni una clase de DivTrack**: la raíz es
una clase del framework, el nodo intermedio es un `ContextImpl` del framework y el objeto
vigilado es un `Service` de WorkManager que el sistema creó para ejecutar el trabajo
periódico. Además, LeakCanary **lo marca él mismo** como `Library leak match` en el borde
`ResourcesImpl.mAppContext`.

**Mecanismo real.** `ResourcesManager` (estático, en el proceso) cachea un `ResourcesImpl` por
`ResourcesKey`, y `ResourcesImpl.mAppContext` guarda una **referencia fuerte** al `ContextImpl`
que lo creó. Si el último `ResourcesImpl` de esa clave lo creó el contexto del servicio de
JobScheduler, el framework retiene ese `ContextImpl` —y con él el `Service`— hasta que se cree
otro `Resources` de la misma clave. Por eso el aviso aparece a veces y luego desaparece solo:
se autorepara. Es un comportamiento del framework (AOSP), no un patrón de la app.

**Qué se hizo y qué no.**

- Lo que **no** se hizo: silenciarlo llamando a `LeakCanary.config` desde el `Application`.
  La receta oficial (`referenceMatchers = AndroidReferenceMatchers.appDefaults + …`) exige
  una clase `Application` propia en `src/debug` registrada en el manifiesto, y esta app tiene
  su `Application` anotada con `@HiltAndroidApp`: Hilt genera el padre real de esa clase y
  meter otra en medio para *apagar un aviso* es cambiar la inicialización del grafo entero
  por ruido. No compensa el riesgo.
- Lo que **sí** se hizo: quitar de en medio el coste real del diagnóstico. LeakCanary está
  apagado por defecto y se enciende cuando se depura una fuga de verdad
  (`-Pdivtrack.leakcanary=true`); entonces analiza en un proceso aparte, de modo que el
  volcado y el análisis (medido: **73 s** en un gama baja) dejan de congelar la app. Ver
  ADR 23.

**Consecuencia.** Si al reactivar LeakCanary vuelve a aparecer **esta misma traza**, ya está
diagnosticada: es el framework reteniendo el último `Context` que creó `Resources`, y no hay
nada que arreglar en DivTrack. Cualquier traza nueva que **sí** mencione una clase propia
(ViewModel, repositorio, `Context` de la Activity) es otra historia y no se tapa con esta
conclusión.

---

## 22. Un error de red no es siempre "sin conexión"

**El síntoma.** Cualquier fallo acababa en el mismo texto: *"Sin conexión. Mostrando última
actualización"*. Un 404 del proveedor, un 503, un timeout o un cuerpo ilegible se contaban
como si el teléfono estuviera desconectado, así que el usuario revisaba su wifi mientras el
problema estaba en la API.

**Decisión.** El error viaja tipado ([DataError]) y se traduce a un texto por causa
(`DataError.toUiText()`), con el código HTTP incluido cuando lo hay. La app solo dice "sin
conexión" cuando el fallo es de conectividad de verdad.

**Y el fallo rápido se volvió prudente.** `ConnectivityObserver` se usa para no esperar un
timeout cuando no hay red, pero **no puede** dejar la app muda: ahora solo responde `false`
cuando el sistema no reporta **ninguna** red activa, y acepta como "con red" cualquier red con
transporte wifi/celular/ethernet/VPN aunque la ROM no marque `NET_CAPABILITY_INTERNET` (pasa
con VPN y portales cautivos). Un permiso revocado o una excepción al consultar el
`ConnectivityManager` tampoco bloquean la petición: se intenta y se muestra el error real.

**Tiempos acotados.** Se añadió `callTimeout` al `OkHttpClient`: la petición completa
(conexión + reintentos + lectura) tiene un techo de 20 s. Antes, tres intentos con timeouts de
10/15 s podían tener la pantalla girando casi un minuto. Además, `RetryInterceptor` ya no
reintenta fallos deterministas: un DNS que no resuelve o un problema de TLS devuelven el mismo
error 350 ms después, y el único resultado era esperar más y gastar más radio.

**La misma regla en segundo plano.** `RateSyncWorker` devolvía `retry()` para cualquier
`Result.Error`, así que un 404 (endpoint cambiado) o una respuesta ilegible se reintentaban con
backoff sin posibilidad de mejorar. Ahora solo se reintenta lo transitorio (red, timeout, HTTP
408/429/5xx); el resto marca el intento como fallido y deja que el siguiente ciclo periódico lo
intente de nuevo. Es la frontera que ya describía `docs/ARCHITECTURE.md`.

**Por qué importa el orden.** `CacheFallbackInterceptor` sirve de la caché HTTP cuando la red
falla, pero si no hay nada cacheado ahora lanza el **error original** en lugar de un 504
sintético: el 504 haría que la app dijera "la fuente está fallando" cuando el problema era la
red del teléfono.

---

## 23. Rendimiento: fuera lo que sobra, y a propósito

**Contexto.** La app se percibía "pesada, con lag, como sin aceleración por hardware". La
aceleración estaba activa (y ahora queda escrita en el manifiesto), así que el problema no era
la GPU: era trabajo evitable en el hilo principal y en el de composición.

**Medidas tomadas.**

| Qué | Antes | Ahora |
|---|---|---|
| LeakCanary en debug | siempre activo: vigilaba cada objeto y analizaba el heap en la app (73 s de análisis medidos) | apagado por defecto; se enciende con `-Pdivtrack.leakcanary=true` y analiza en otro proceso |
| Grafo de navegación | `entryProvider` reconstruido en cada recomposición | construido dentro de un `remember` |
| Formateo de cifras | un `DecimalFormat` nuevo **por llamada** (y uno por tecla de la calculadora) | uno por hilo y por formato; los `DateTimeFormatter` (inmutables) se crean una vez |
| Pestañas | `TopLevelDestination.entries.toList()` en cada pasada de la barra | lista construida una vez |
| Histórico | gráfico `Canvas` con la serie YTD y una pantalla entera que descargaba datos | funcionalidad eliminada (ADR 11) |
| Latencia de red | sin techo global: reintentos encadenados | `callTimeout` de 20 s y sin reintentos de fallos deterministas |
| Lambdas de intención | `viewModel::onIntent` nuevo por recomposición | referencia recordada en la ruta |

**Nota honesta sobre medir.** Un build `debug` de Compose es más lento por definición (sin R8,
sin optimizaciones de release) y LeakCanary añadía su parte. Para juzgar el rendimiento de
verdad hay que mirar el APK de release; el debug ya no lleva encima la vigilancia de objetos.

---

## 24. Permisos: dos, normales, y ni uno más

**Decisión.** La app declara exactamente `INTERNET` y `ACCESS_NETWORK_STATE`. Los dos son
permisos **normales**: no hay diálogo de runtime, no hay `ActivityResultContracts` y no hay
estado de permiso que gestionar porque nunca se pide nada al usuario.

**Por qué no hace falta pedir nada.** Consultar dos APIs HTTPS solo necesita `INTERNET`;
saber si hay red para no esperar un timeout necesita `ACCESS_NETWORK_STATE`. Ni ubicación, ni
contactos, ni almacenamiento, ni cámara, ni notificaciones (el worker de WorkManager no
publica ninguna notificación, así que `POST_NOTIFICATIONS` no aplica).

**El problema real que había.** El APK de debug declaraba `READ_EXTERNAL_STORAGE`,
`WRITE_EXTERNAL_STORAGE` y `POST_NOTIFICATIONS`: los tres venían del manifiesto fusionado de
LeakCanary, no de DivTrack. Un usuario que mira "Permisos" en los ajustes veía una app de
tasas pidiendo almacenamiento y notificaciones. Al apagar LeakCanary por defecto (ADR 23)
desaparecen solos, y lo que queda está documentado en el manifiesto: lo nuestro y lo que
aporta WorkManager (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`), que se dejan
porque son de la librería y el trabajo periódico los necesita.

**Se cierra el tráfico en claro.** `usesCleartextTraffic="false"` + `network_security_config`
con `cleartextTrafficPermitted="false"`: si algún día un `baseUrl` pasa a `http://`, el fallo
aparece en el primer intento en lugar de mandar tasas sin cifrar.

**Y se le da salida al usuario.** Cuando el fallo es de conectividad, el estado de error ofrece
"Abrir ajustes de red" (una `Intent` del sistema, sin permisos). Un aviso de "sin conexión" sin
nada que pulsar deja al usuario atascado.

---

## 25. Tres fuentes con dialectos distintos y preferencia de orden

**Contexto.** DolarAPI y Yadio comparten upstream del mercado paralelo y los cortes de ruta
hacia Venezuela los han tumbado a ambos a la vez. Además, el sistema no distinguía el
"estilo de lectura" de cada fuente: cada proveedor habla su propio dialecto JSON y ese
conocimiento tiene que vivir encapsulado en su provider.

**Decisión.**

1. **Tercera fuente independiente.** ExchangeRate-API (`open.er-api.com`) entra como
   fallback del oficial: no comparte ruta ni upstream con las dos primeras. Los candidatos
   muertos en vivo se descartaron con evidencia: pyDolarVE (DNS sin resolver) y CriptoYa
   (HTTP 422). El endpoint quedó verificado devolviendo `rates["VES"]` y
   `time_last_update_unix` en **segundos** (la app trabaja en ms).

2. **Tres estilos de lectura, un contrato.** DolarAPI publica una *lista* por fuente
   (`promedio → venta → compra`); Yadio un *mapa* dentro de `USD`; ExchangeRate-API un
   *envoltorio con estado* (`result`) y mapa plano. Cada DTO + provider encapsula el suyo;
   `RateProvider` sigue siendo una interfaz de un método.

3. **El orden lo manda el usuario.** Sin preferencia: DolarAPI → Yadio → ExchangeRateAPI.
   Con preferencia: el elegido primero y el resto por prioridad; la pasada se corta cuando
   las dos tasas están resueltas. La preferencia vive en DataStore (`preferred_provider_id`,
   cadena vacía = automático) y el dominio solo ve `null` o un id de `ProviderIds`.

4. **Diagnóstico real en Ajustes.** `testProviders()` consulta las tres APIs de verdad, sin
   escribir nada: una respuesta HTTP "sana" sin pares utilizables cuenta como caída. La UI
   muestra qué tasa trajo cada fuente y "Fuentes OK: X de Y".

5. **Solo-wifi.** El trabajo periódico acepta `wifiOnly` y usa `NetworkType.UNMETERED`.

**Por qué no reordenar el mapeo en el registro.** Cada provider ya devuelve `RemoteRate`
con la clave canónica; el registro solo resuelve *quién primero*. Meter dialectos en el
registro acoplaría todas las fuentes entre sí: añadir la cuarta fuente tocaría un archivo
del registro en lugar de uno nuevo + una línea de DI.

---

## 26. El banco que dejó de publicar: frescura, breaker y diagnóstico

**Contexto.** DolarAPI puede responder 200 con JSON perfecto sirviendo el cierre de ayer,
porque el BCV dejó de publicar. Ningún código HTTP lo delata: la única señal es la marca
de tiempo del propio dato. A eso se suma el problema opuesto: un proveedor caído retrasa
cada pasada esperando su timeout una y otra vez.

**Decisión.**

1. **Frescura por fuente.** El oficial (publicación diaria) se considera rancio pasadas
   24 h; el paralelo (por horas), pasadas 12 h. Sin marca de tiempo no se supone rancio:
   el aviso falso destruye la confianza en el aviso verdadero.

2. **Gana el dato más fresco.** En una pasada, una tasa resuelta puede ser reemplazada si
   un proveedor posterior trae marca de tiempo posterior. Y la pasada no se corta cuando
   las dos tasas "están" sino cuando están **frescas**: con el oficial rancio se sigue
   preguntando a las fuentes de cola, que es exactamente cómo el dato del día llega aunque
   el primero sirva el cierre repetido de ayer.

3. **Circuit breaker con auto-recuperación.** Un proveedor que falla queda abierto 10 min:
   se salta en las pasadas siguientes (nadie espera su timeout) y se reintenta solo al
   cumplirse el periodo. Si todos estuvieran abiertos, se reintenta el orden completo:
   el breaker nunca puede dejar la app sin pasada. El estado vive en memoria a propósito:
   un reinicio de proceso reabre los circuitos, y el fallo más común es de red, no del
   proveedor.

4. **UI honesta.** El dashboard muestra un banner distinto del de conexión cuando alguna
   tasa es rancia, y la tarjeta señala su edad ("el banco lleva más de un día sin
   publicar"). El diagnóstico de Ajustes muestra el último dato de cada fuente: "OK con un
   dato de ayer" deja de parecer contradictorio y se lee como lo que es.

**Por qué no “usar siempre el proveedor preferido” o “el primero que responde”.** Con el
banco caído, el primero que responde es precisamente el que repite el cierre de ayer. La
frescura comparada entre fuentes es la única regla que resuelve el caso sin que nadie
tenga que mirar la hora en la pantalla.

---

## 27. DivTrack Lite: un subproyecto aparte, no un sabor del principal

**Decisión.** `lite/` es un proyecto Gradle **independiente** (su propio
`settings.gradle.kts`), con Java + XML, `minSdk 14` y una sola pantalla que publica la tasa
USD/VES del BCV con respaldos identificados (DolarAPI y ER-API). No comparte código con el
grafo principal y se compila con `./gradlew -p lite :app:assembleRelease` desde la raíz.

**Motivo.** El objetivo de Lite es otro: equipos antiguos donde Compose, Room y las librerías
de AndroidX modernas no son una opción. Meterlo como *flavor* del módulo `:app` habría obligado
a que todo el proyecto principal adoptara su `minSdk` y su estilo de UI; separarlo permite que
desaparezca sin tocar una línea del principal (y viceversa).

**Consecuencia honesta.** Los dos APK se publican juntos desde el mismo CI, con nombres de
paquete distintos (`com.liebeblack.divtrack` y `com.liebeblack.divtrack.lite`), y el flujo de
release **verifica la firma y el `applicationId` de cada uno** antes de subirlos. La
contrapartida es que `lite/` no participa del wrapper ni del catálogo de versiones del
principal: sus versiones de AGP y Kotlin viven en su propio `build.gradle.kts` y hay que
subirlas a mano si se quiere ir al día.

**Por qué no se documentó antes.** El subproyecto existía y CI lo compilaba, pero ni el README
ni `docs/index.html` lo mencionaban; quien llegara al repositorio solo veía la app principal.
Queda documentado aquí, en el README y en la web del proyecto.
