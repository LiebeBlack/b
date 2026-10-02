# DivTrack Lite

Aplicación Android independiente y mínima para mostrar la tasa USD/VES del Banco Central de
Venezuela (BCV), con fuentes de respaldo identificadas explícitamente cuando el BCV no responde.

## Alcance

- Interfaz nativa Android con XML y Java; no usa Compose ni librerías de terceros.
- Mínimo declarado: Android 4.0 (API 14).
- Consulta HTTPS al BCV; si falla, intenta DolarAPI y luego ER-API, indicando siempre la
  fuente que entregó la cifra.
- Los valores alternativos se presentan como respaldo y no se etiquetan como una publicación
  del BCV.
- Guarda localmente la tasa, su proveedor y la fecha del dato con `SharedPreferences`.
- Si no hay red o falla TLS, conserva el valor guardado y explica que no pudo actualizar.
- No usa HTTP, servidor proxy, sincronización periódica ni permisos distintos de Internet.
- No usa Room: una sola tasa y su metadato caben en preferencias locales, con menos tamaño,
  memoria y arranque que una base de datos.

Los equipos antiguos pueden no admitir los protocolos o certificados HTTPS que requiere el
sitio actual del BCV. En esos dispositivos la tasa en caché seguirá visible, pero la actualización
puede fallar; la aplicación no rebaja la seguridad de la conexión para ocultar esa limitación.

## Compilación

Este subproyecto tiene su propio `settings.gradle.kts` y no forma parte del grafo principal.
Desde la raíz del repositorio se puede compilar la variante release, con R8 y reducción de
recursos activados, usando el wrapper existente:

```text
gradlew.bat -p lite :app:assembleRelease
```

Requiere JDK 17 y Android SDK 37 para compilar. No se añadió una copia del wrapper de Gradle.
