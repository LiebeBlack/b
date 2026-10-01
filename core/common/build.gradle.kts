// :core:common — base PURA (Kotlin/JVM). Sin Android, sin Compose, sin Retrofit.
// Es lo único que :domain puede consumir, por eso aquí viven Result/DataError,
// constantes, formateo de moneda y utilidades de tiempo.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        // AGP 9 ya no admite kotlinOptions{}; el DSL canónico es kotlin.compilerOptions{}.
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
