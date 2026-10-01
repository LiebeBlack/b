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

**Nota de diseño.** Cuatro pestañas planas: el back stack se mantiene acotado a
`[raíz, pestaña actual]`, de modo que "atrás" vuelve al panel de tasas y desde ahí sale de
la app. No se registra un `BackHandler` propio: `NavDisplay` ya gestiona el gesto y añadir
otro provocaría una doble pulsación (pila vacía y contenido en blanco, un bug visible).

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
| `ve.dolarapi.com/v1/dolares` | 200 · oficial 859,06 · paralelo 954,55 | proveedor primario |
| `ve.dolarapi.com/v1/historicos/dolares` | 200 · serie diaria | único con histórico: alimenta el gráfico |
| `api.yadio.io/exrates/USD` | 200 · `USD.VES` = 954,55 | respaldo del paralelo (es la fuente upstream) |
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
y se añade *extended* únicamente para las cuatro pestañas (Calculate, Insights, TrendingUp,
Settings).

**Motivo.** Los iconos de pestaña son la primera impresión de la app y merecen el icono
correcto; el resto del proyecto no necesita 10 000 vectores. R8 elimina en release todo lo
que no se usa, así que el coste real en el APK final son cuatro rutas.

---

## 10. DataStore provisto por DI, no por delegado

**Decisión.** `DataStore<Preferences>` se crea con `PreferenceDataStoreFactory` y se provee
como `@Singleton`, en lugar de usar `by preferencesDataStore` en un `Context`.

**Motivo.** Dos instancias sobre el mismo archivo hacen que DataStore lance
`IllegalStateException`. Proveerlo por DI garantiza una sola por proceso y permite
sustituirlo en tests.

---

## 11. Histórico con TTL de 24 h y retención de 2 años

**Decisión.** La importación del histórico se salta si la última fue hace menos de 24 h
(salvo acción manual); la tabla se poda a 2 años.

**Motivo.** El histórico de días pasados no cambia: solo el día en curso necesita refresco.
Sin TTL, abrir la pantalla de histórico castigaría la red y la batería por nada.

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
