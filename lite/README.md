# DivTrack Lite

Aplicación Android independiente y mínima para mostrar la tasa USD/VES del Banco Central de
Venezuela (BCV), con fuentes de respaldo identificadas explícitamente cuando el BCV no responde.

## Alcance

- Interfaz nativa Android con XML y Java; no usa Compose ni librerías de terceros.
- Mínimo declarado: Android 4.0 (API 14).
- Consulta HTTPS al BCV; si falla, intenta DolarAPI y luego ER-API, indicando siempre la
  fuente que entregó la cifra junto a la fecha del dato.
- **No acusa a ninguna fuente**: cuando la cifra viene de un respaldo, la pantalla muestra su
  nombre ("DolarAPI (respaldo)") junto a la fecha, sin mensajes de error por la fuente que
  no respondió. El estado solo describe lo que pasó (sin conexión, tiempo de espera agotado,
  datos no válidos).
- Muestra la fecha del dato (en el BCV, su "Fecha Valor") y, en otra línea, el momento de la
  última comprobación con su edad ("Comprobado: …", "comprobada hace 20 min"). Si el dato
  lleva más de un día, lo dice en ámbar.
- Cada fuente que falla queda apartada 10 minutos (circuito en memoria): no se repite su
  tiempo de espera en cada pasada. Si todas estuvieran apartadas se reintenta el orden
  completo, así que el circuito nunca deja la aplicación sin consulta.
- La cifra nunca retrocede: si la fuente devuelve una fecha de dato anterior a la guardada,
  se conserva la más reciente y se dice que ya era la más reciente.
- No se consulta la red si la tasa se comprobó hace menos de 10 minutos, así que abrir la
  pantalla otra vez no cuesta datos. Con la pantalla visible se comprueba cada 30 minutos y
  al volver a la aplicación si el dato está viejo; en segundo plano no hay trabajo alguno.
- Las respuestas se piden con la compresión que la plataforma negocia: la página del BCV
  pasa de ~150 KB a ~30 KB.
- Guarda localmente la tasa, su proveedor, la fecha del dato y el momento de la comprobación
  con `SharedPreferences`. El dato se guarda en el hilo de red en cuanto llega, antes de
  tocar la interfaz, de modo que una rotación o una pantalla apagada no lo pierden.
- Si no hay red o falla TLS, conserva el valor guardado y explica que no pudo actualizar.
- No usa HTTP, servidor proxy, trabajo en segundo plano ni permisos distintos de Internet.
- No usa Room: una sola tasa y su metadato caben en preferencias locales, con menos tamaño,
  memoria y arranque que una base de datos.

Los equipos antiguos pueden no admitir los protocolos o certificados HTTPS que requiere el
sitio actual del BCV. En esos dispositivos la tasa en caché seguirá visible, pero la actualización
puede fallar; la aplicación no rebaja la seguridad de la conexión para ocultar esa limitación,
y si termina respondiendo un respaldo, la cifra se muestra con el nombre de ese respaldo.

## Compilación

Este subproyecto tiene su propio `settings.gradle.kts` y no forma parte del grafo principal.
Desde la raíz del repositorio se puede compilar la variante release, con R8 y reducción de
recursos activados, usando el wrapper existente:

```text
gradlew.bat -p lite :app:assembleRelease
```

Requiere JDK 17 y Android SDK 37 para compilar. No se añadió una copia del wrapper de Gradle.
