plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "cu.christianrvdv.sumador"
    compileSdk = 36

    defaultConfig {
        applicationId = "cu.christianrvdv.sumador"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.8.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    signingConfigs {
        create("release") {
            // Lee las credenciales desde variables de entorno (para GitHub Actions).
            // En local, si no hay keystore, el release se genera sin firmar en vez
            // de romper `build`; si KEYSTORE_PATH está definido pero no existe, falla.
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystoreFile = file(keystorePath ?: "keystore.jks")
            if (keystorePath != null && !keystoreFile.exists()) {
                throw GradleException("KEYSTORE_PATH está definido pero no existe: ${keystoreFile.absolutePath}")
            }
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Usa la configuración de firma release solo si el keystore existe.
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel + LiveData
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.compose.runtime.livedata)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Iconos extendidos
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt (core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ========== Hilt Work (para Workers) ==========
    implementation(libs.hilt.work)

    // Security (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)

    // Logging
    implementation(libs.timber)

    // ========== UI/UX (Accompanist, Lottie, etc.) ==========
    implementation("com.google.accompanist:accompanist-navigation-animation:0.35.0-alpha")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.35.0-alpha")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.35.0-alpha")
    implementation("com.google.accompanist:accompanist-flowlayout:0.35.0-alpha")
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // ========== Paging 3 ==========
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)

    // ========== Accompanist Pager (para el calendario) ==========
    implementation(libs.accompanist.pager)

    // ========== Firebase (BOM + Messaging) ==========
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // ========== WorkManager ==========
    implementation(libs.work.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.datastore.preferences)
}

hilt {
    enableAggregatingTask = false
}