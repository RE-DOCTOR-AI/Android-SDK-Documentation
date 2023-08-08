plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "com.tvs.android"
        minSdk = 24
        targetSdk = 33
        versionCode = 3
        versionName = "1.2.0(15)"

        // Required when setting minSdkVersion to 20 or lower
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("org.jetbrains.kotlinx:atomicfu:0.18.3")
    implementation("org.jetbrains.kotlinx:multik-core:0.2.1")
    implementation("org.jetbrains.kotlinx:multik-kotlin:0.2.1")
    implementation("de.voize:pytorch-lite-multiplatform:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    //SDK files
    implementation(fileTree("libs"))
}