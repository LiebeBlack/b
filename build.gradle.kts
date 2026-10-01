// Build raíz de DivTrack.
//
// IMPORTANTE (AGP 9): el plugin `org.jetbrains.kotlin.android` NO se declara ni se aplica.
// AGP 9 incorpora soporte de Kotlin integrado (built-in Kotlin) y el plugin antiguo es
// incompatible con el DSL nuevo. Los plugins de compilador de Kotlin que sí se aplican
// (Compose y kotlinx.serialization) van con la versión de Kotlin del catálogo, tal como
// documenta la guía oficial del compilador de Compose para AGP 9.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.room) apply false
}
