import java.util.Properties

// :core:network — Retrofit + OkHttp (multi-proveedor), interceptores y errores de red.
//
// SEGURIDAD: las URLs base NO están hardcodeadas en Kotlin. Se leen de local.properties
// (fuera del control de versiones) y se inyectan como campos de BuildConfig. Si el archivo
// no existe, se usan los endpoints públicos verificados para que CI compruebe sin secretos.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun configuredValue(key: String, fallback: String): String =
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.liebeblack.divtrack.core.network"
    compileSdk = 37

    defaultConfig {
        minSdk = 26

        buildConfigField(
            "String",
            "DOLARAPI_BASE_URL",
            "\"${configuredValue("dolarapi.baseUrl", "https://ve.dolarapi.com/")}\"",
        )
        buildConfigField(
            "String",
            "YADIO_BASE_URL",
            "\"${configuredValue("yadio.baseUrl", "https://api.yadio.io/")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // api: NetworkErrorMapper devuelve DataError de :core:common en su contrato público.
    api(project(":core:common"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
