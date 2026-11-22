# HyperSpring / Gofile Expressive — R8 rules.
#
# Aplikasi ini tidak memakai WebView, JavascriptInterface, atau refleksi JSON
# berbasis model, jadi aturan di bawah hanya menjaga hal-hal yang memang
# dibutuhkan library dan entry point Android.

# Entry point Android.
-keep class com.nellecrzier.gofileexpressive.MainActivity { *; }
-keep class com.nellecrzier.gofileexpressive.TransferService { *; }

# OkHttp / Okio: peringatan opsional untuk platform yang tidak dipakai.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Kotlin metadata dipakai Compose dan coroutine debugging.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Compose menyimpan lambda di kelas sintetis; jangan buang composable.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Coil memakai ServiceLoader untuk decoder opsional.
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 (ExoPlayer) memuat renderer lewat nama kelas.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Security-crypto / Tink memakai Provider dan refleksi terbatas.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Pesan error GoFile dipertahankan apa adanya untuk laporan bug.
-keepnames class com.nellecrzier.gofileexpressive.GoFileException { *; }

# Media3 ExoPlayer: pemutar media memakai refleksi untuk memilih decoder.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ZXing core: kelas matriks dipakai langsung, tanpa refleksi.
-keep class com.google.zxing.** { *; }

# Biometrik memakai FragmentActivity dan callback anonim.
-keep class androidx.biometric.** { *; }

# Model JSON aplikasi (dipakai lewat org.json, bukan refleksi) tetap aman,
# tetapi nama kelas model dipertahankan supaya log tidak sulit dibaca.
-keep class com.nellecrzier.gofileexpressive.GItem { *; }
-keep class com.nellecrzier.gofileexpressive.GSession { *; }
-keep class com.nellecrzier.gofileexpressive.TransferInfo { *; }

# Jangan buang metadata anotasi Compose.
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}
