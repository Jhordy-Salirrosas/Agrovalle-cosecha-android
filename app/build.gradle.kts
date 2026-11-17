plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.agrovalle.cosecha"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.agrovalle.cosecha"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        // SOLO viewBinding. Sin dataBinding (evita conflictos con la compilacion
        // de la variante UnitTest en Gradle).
        viewBinding = true
    }

    testOptions {
        // Fuerza JUnit 4 en todas las tareas de test unitario (app/src/test).
        unitTests.all {
            it.useJUnit()
        }
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // ------------------------------------------------------------------
    // App
    // ------------------------------------------------------------------
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // ViewModel + SavedStateHandle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Corrutinas (incluidas por lifecycle-ktx; se declara la version explicita)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager (Sprint 2 - alertas periódicas de tiempo de espera)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room (Sprint 2 - base de persistencia local)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // ZXing: lectura real de QR / Code 128 (sin CameraX ni ML Kit)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ------------------------------------------------------------------
    // Tests unitarios (app/src/test) -> se ejecutan en la JVM local
    // ------------------------------------------------------------------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")

    // ------------------------------------------------------------------
    // Tests instrumentados (app/src/androidTest) -> requieren dispositivo/emulador
    // ------------------------------------------------------------------
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
