import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 33
    namespace = "com.tvs.android"

    defaultConfig {
        applicationId = "com.tvs.android"
        minSdk = 26 // Required by kinference dependency
        targetSdk = 33
        versionCode = 21
        versionName = "1.6.0(23)"

        // Required when setting minSdkVersion to 20 or lower
//        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))

            defaultConfig {
                // Check for the property and add it if exists
                localProperties["tvs.sdk.key"]?.let {
                    buildConfigField("String", "ReRoctorLicenseKey", "\"$it\"")
                }
            }
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:atomicfu:0.18.3")
    implementation("org.jetbrains.kotlinx:multik-core:0.2.1")
    implementation("org.jetbrains.kotlinx:multik-kotlin:0.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    //ktor
    implementation("io.ktor:ktor-client-core:2.1.2")
    implementation("io.ktor:ktor-client-cio:2.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.2")

    //kinference
    implementation("io.kinference:inference-api:0.2.20-kotlin18")
    implementation("io.kinference:ndarray-api:0.2.20-kotlin18")
    implementation("io.kinference:inference-ort:0.2.20-kotlin18")
    implementation("io.kinference:inference-core:0.2.20-kotlin18")

    //SDK dependencies
    implementation("org.bitbucket.b_c:jose4j:0.7.8")
    implementation(fileTree("libs"))
}
