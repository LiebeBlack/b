// DivTrack — configuración de repositorios y grafo de módulos.
//
// Regla de oro de la arquitectura (la impone Gradle, no la convención):
//   :app -> :presentation -> :domain <- :data -> :core:{network, database, datastore}
//   :core:common es la base pura (JVM) que todos pueden consumir y NADIE la contamina con Android.
//   :domain es un módulo Kotlin/JVM puro: no puede ver Android, Compose, Retrofit ni Room.

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DivTrack"

// --- Aplicación y capas ---
include(":app")
include(":domain")
include(":data")
include(":presentation")

// --- Núcleo compartido ---
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
