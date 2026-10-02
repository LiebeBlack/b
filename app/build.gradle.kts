// :app — Punto de entrada, grafo Hilt raíz, WorkManager y recursos finales.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

/**
 * LeakCanary está **apagado por defecto** y se enciende a petición:
 *
 * ```
 * ./gradlew :app:assembleDebug -Pdivtrack.leakcanary=true
 * ```
 *
 * Por qué no se deja encendido siempre en debug:
 *
 * 1. **Coste real en el APK de uso diario.** LeakCanary vigila todos los objetos con
 *    `ObjectWatcher`, y cuando algo se retiene hace un volcado de heap y lo analiza. En un
 *    gama baja ese análisis tarda más de un minuto (medido: 73 s) y congela la app. Es una
 *    herramienta de diagnóstico, no algo que deba estar vigilando mientras se usa la app.
 * 2. **Permisos que no son nuestros.** Su manifiesto aporta READ_EXTERNAL_STORAGE,
 *    WRITE_EXTERNAL_STORAGE y POST_NOTIFICATIONS, así que el APK acababa mostrando permisos
 *    que DivTrack no usa. Apagándolo, el build declara exactamente lo que necesita.
 * 3. **Falsos positivos del framework.** Ver la ADR 21: el caso clásico aquí es
 *    `SystemJobService` retenido por `ResourcesImpl.mAppContext`, que es de Android, no
 *    nuestro.
 */
val leakCanaryEnabled: Boolean = (findProperty("divtrack.leakcanary") as String?) == "true"

android {
    namespace = "com.liebeblack.divtrack"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.liebeblack.divtrack"
        minSdk = 26
        targetSdk = 37
        versionCode = 470
        versionName = "4.7"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        // DivTrackApplication usa BuildConfig.DEBUG para el nivel de log de WorkManager.
        // AGP 8+ ya no genera BuildConfig por defecto: hay que pedirlo explícitamente.
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            // R8 + shrink de recursos: CI valida que las reglas de ProGuard son correctas
            // compilando el release en cada push.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":core:common"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Solo en debug, y solo cuando se pide explícitamente (ver `leakCanaryEnabled`).
    if (leakCanaryEnabled) {
        debugImplementation(libs.leakcanary)
        // El análisis del heap en un proceso aparte: cuando se depura una fuga de verdad, la
        // app se queda con el volcado y el análisis deja de robarle fotogramas al hilo
        // principal. Es la forma documentada de que LeakCanary no afecte al rendimiento.
        debugImplementation(libs.leakcanary.process)
    }
}
