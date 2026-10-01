# ---------------------------------------------------------------------------
# Reglas R8 de DivTrack (solo release; el build de debug no minifica).
#
# Criterio: no se escriben reglas "por si acaso". Cada bloque responde a un mecanismo de
# reflexión que existe de verdad en el proyecto. Retrofit, OkHttp, Room y Hilt ya publican
# las suyas como consumer rules; aquí solo va lo que generamos nosotros.
# ---------------------------------------------------------------------------

# --- kotlinx.serialization ---
# El plugin genera un `Companion.serializer()` y clases `$$serializer` que se resuelven por
# reflexión al deserializar. Sin estos keeps, R8 los renombra o los elimina y la app revienta
# al recibir la primera respuesta JSON (solo en release: el error más caro de detectar).
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, InnerClasses, Signature

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.liebeblack.divtrack.**$$serializer { *; }
-keepclassmembers class com.liebeblack.divtrack.** {
    *** Companion;
}
-keepclasseswithmembers class com.liebeblack.divtrack.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer { *; }

# --- Retrofit / OkHttp ---
# Las anotaciones @GET/@POST y los genéricos de las interfaces de servicio se leen por
# reflexión al crear el proxy.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation interface com.liebeblack.divtrack.core.network.service.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Navegación (Navigation 3 + kotlinx.serialization) ---
# Las claves de navegación son `data object` serializables: se guardan en el estado de la
# Activity por nombre de clase, así que sus nombres no se pueden ofuscar.
-keepnames class com.liebeblack.divtrack.presentation.navigation.** { *; }
-keep class androidx.navigation3.** { *; }

# --- Corrutinas ---
# Los nombres de las corrutinas aparecen en los stack traces de los reportes de fallos.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# --- Vista útil en los crash reports ---
# Sin esto, los stack traces del release quedan ilegibles; con esto, los tipos del dominio
# y de la red conservan su nombre (son pocos y no aportan superficie de ataque real).
-keepnames class com.liebeblack.divtrack.domain.model.** { *; }
-keepnames class com.liebeblack.divtrack.core.common.error.DataError { *; }

# Un fallo de R8 por una classe ausente opcional no debe romper el release.
-dontwarn java.lang.instrument.**
