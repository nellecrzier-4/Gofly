plugins {
    id("org.jetbrains.kotlin.android")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sumber tunggal versi aplikasi. versionCode DIHITUNG dari versionName
// (MAJOR*10000 + MINOR*100 + PATCH), jadi menaikkan versionName otomatis
// menaikkan versionCode. Android hanya mau memperbarui APK terpasang bila
// versionCode baru lebih besar, applicationId sama, dan tanda tangan sama.
// Naikkan versi di sini setiap menambah fitur (lihat CONTRIBUTING.md).
val appVersionName = "4.3.0"
val appVersionCode = appVersionName.split(".").let { (major, minor, patch) ->
    major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt()
}

android {
    namespace = "com.nellecrzier.gofly"
    compileSdk = 36

    defaultConfig {
        // JANGAN diubah: applicationId lama dipertahankan supaya Gofly bisa
        // memperbarui pemasangan yang sudah ada tanpa uninstall.
        applicationId = "com.nellecrzier.gofileexpressive"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // Kunci debug tetap (ikut repo) supaya setiap build CI memakai tanda tangan
        // yang sama. Runner GitHub membuat debug.keystore baru tiap jalan, itulah
        // yang membuat pembaruan APK gagal dan memaksa uninstall.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Rilis ditandatangani hanya bila keystore tersedia lewat variabel
        // lingkungan (CI: secrets). Tanpa itu, build rilis tetap bisa dibuat
        // untuk pemeriksaan, tanpa ditandatangani.
        create("release") {
            val storePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Tanpa keystore rilis, tandatangani dengan kunci debug supaya APK
            // release (R8 + baseline profile, jauh lebih cepat dari debug)
            // tetap bisa dipasang untuk dicoba.
            signingConfig = if (!System.getenv("RELEASE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    testOptions {
        // org.json disediakan dependensi test; stub Android lain mengembalikan
        // nilai default alih-alih melempar "not mocked".
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    // Memasang baseline profile saat pemasangan: Compose tidak lagi menjalankan
    // kodenya lewat interpreter pada pembukaan layar pertama.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Jaringan: satu klien dengan timeout + interceptor header.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Sesi terenkripsi di Keystore.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Preferensi pengguna (sort, tampilan, favorit).
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Thumbnail gambar/video di tampilan grid.
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Pratinjau media native: video/audio lewat Media3 ExoPlayer.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    // Kunci aplikasi biometrik/PIN.
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    // Kode QR untuk berbagi tautan, digambar di Compose.
    implementation("com.google.zxing:core:3.5.3")
    // Splash Screen API (Android 12+).
    implementation("androidx.core:core-splashscreen:1.0.1")
    // WindowSizeClass untuk tata letak tablet/lipat.
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.compose.material3.adaptive:adaptive:1.0.0")
    // Bahasa per-aplikasi (API 33+ LocaleManager, fallback AppCompat).
    implementation("androidx.appcompat:appcompat:1.7.0")
    // Tema XML Material 3 DayNight + overlay warna dinamis untuk splash/aktivitas.
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    // Folder upload lewat SAF.
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Komponen Expressive (LoadingIndicator, ButtonGroup, FloatingToolbar,
    // wavy progress) baru publik di 1.5.0-alpha10 dan masih memakai Compose 1.8.x.
    implementation("androidx.compose.material3:material3:1.5.0-alpha10")
    implementation("androidx.compose.material:material-icons-extended")
    // Bentuk M3 Expressive (RoundedPolygon/Morph) untuk sudut bersambung.
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("androidx.test:core:1.6.1")
    // Implementasi nyata org.json untuk unit test (android.jar hanya stub).
    testImplementation("org.json:json:20240303")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
