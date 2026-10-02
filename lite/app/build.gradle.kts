plugins {
    id("com.android.application")
}

android {
    namespace = "com.liebeblack.divtrack.lite"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.liebeblack.divtrack.lite"
        minSdk = 14
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}
