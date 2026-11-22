# Kontribusi

Terima kasih sudah mau membantu Gofile Expressive. Panduan ini singkat: cara
membangun, menguji, dan aturan yang tidak bisa ditawar.

## Prasyarat

- JDK 17 (Temurin atau OpenJDK).
- Android SDK dengan platform 35 dan build-tools yang cocok (Android Studio
  terbaru sudah menyiapkan ini).
- Node.js 22 hanya diperlukan untuk memeriksa JavaScript versi web.
- Python 3 untuk menjalankan server statis web.

## Membangun

```bash
cd android
./gradlew :app:assembleDebug          # APK debug
./gradlew :app:installDebug           # pasang ke perangkat/emulator
```

APK ada di `android/app/build/outputs/apk/debug/app-debug.apk`.

Versi web tidak butuh build step:

```bash
python3 serve.py --port 5173          # buka http://localhost:5173
```

## Menguji

Unit test berjalan di JVM, tanpa emulator dan tanpa jaringan:

```bash
cd android
./gradlew :app:testDebugUnitTest
```

- Laporan HTML: `android/app/build/reports/tests/testDebugUnitTest/index.html`.
- Satu kelas: `./gradlew :app:testDebugUnitTest --tests "*GofileParsingTest"`.
- Tes baru ditulis dengan JUnit4 (tanpa Robolectric) di
  `android/app/src/test/java/com/nellecrzier/gofileexpressive/`.
- `org.json` asli tersedia untuk unit test lewat dependensi `testImplementation`,
  jadi penguraian JSON diuji dengan perilaku yang sama seperti di perangkat.
- Fixture JSON bersama ada di `Fixtures.kt`.

Uji instrumentasi (butuh emulator atau perangkat):

```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```

Uji asap layar masuk tidak boleh memanggil jaringan: layar masuk hanya membaca
sesi lokal.

## Lint dan format

```bash
cd android
./gradlew :app:lintDebug      # Android Lint; laporan di app/build/reports/
```

Proyek ini belum memasang plugin ktlint di Gradle. Kalau kamu memakai ktlint
sebagai alat bantu lokal, jalankan dari akar repo:

```bash
ktlint -F "android/app/src/**/*.kt"
```

CI menjalankan `:app:lintDebug` dan `:app:testDebugUnitTest`; ktlint bersifat
opsional dan tidak memblokir PR. Ikuti gaya berkas di sekitarnya: 4 spasi,
komentar dan nama domain dalam bahasa Indonesia, tanpa baris lebih dari 100
karakter bila memungkinkan.

## Aturan keras: tanpa WebView

Aplikasi Android adalah klien native. Hal-hal berikut tidak boleh masuk ke
`android/app/src/main`:

- `WebView`, `android.webkit.*`, `addJavascriptInterface`
- `androidx.webkit` dan Custom Tabs (`androidx.browser`)
- memuat HTML, CSS, atau JavaScript dari jarak jauh untuk membangun antarmuka

Antarmuka dibangun dengan Jetpack Compose, dan jaringan hanya lewat OkHttp
(`GofileApi`). CI akan menggagalkan job bila salah satu pola di atas muncul:

```bash
grep -rniE "webview|addJavascriptInterface|androidx.webkit|customtabs" android/app/src/main
```

Perhatikan bahwa pemeriksaan ini juga menangkap komentar dan teks yang hanya
menyebut kata "WebView". Tulis "tampilan web" atau "peramban" di dokumentasi
bila perlu.

## Konvensi commit

Pakai Conventional Commits, satu perubahan logis per commit:

```
feat(android): antrean transfer bisa dijeda per item
fix(web): tautan login tidak terpotong di Safari
docs: perbarui panduan kontribusi
ci: cache Gradle untuk job Android
```

- Tipe yang dipakai: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`,
  `test`, `build`, `ci`, `chore`.
- Scope: `android`, `web`, `ci`, `docs`.
- Subjek memakai kalimat perintah, huruf kecil, tanpa titik di akhir, maksimal
  sekitar 72 karakter.
- Badan commit menjelaskan *mengapa*, bukan mengulang *apa*.
- Sertakan `Closes #123` bila commit menutup issue.

## Pull request

1. Fork, lalu buat cabang dari `main` (`feat/...`, `fix/...`).
2. Jalankan `:app:testDebugUnitTest` dan `:app:lintDebug` sebelum membuka PR.
3. Jaga PR tetap kecil dan fokus; jelaskan perilaku yang berubah dan cara
   mengujinya.
4. Jangan mengubah `android/app/build.gradle.kts` tanpa menjelaskannya di PR:
   penambahan dependensi memengaruhi ukuran APK dan waktu build.
5. CI harus hijau. PR yang menyentuh Android akan menjalankan lint, unit test,
   build APK, dan pemeriksaan larangan WebView.

## Melaporkan bug

Sertakan:

- langkah reproduksi yang singkat dan pasti,
- versi aplikasi (Setelan > Tentang) dan `versionCode`,
- merek/model perangkat, versi Android, serta apakah aplikasi dipasang dari
  rilis GitHub atau build sendiri,
- perilaku yang diharapkan dan yang terjadi,
- **log aplikasi**: buka Setelan > Ekspor log, lalu lampirkan berkas
  `gofile-expressive.log` ke issue. Log hanya ditulis lokal, tidak pernah
  dikirim ke server mana pun.

Jangan menyertakan token login, alamat email, atau tautan folder privat di
issue publik. Kalau log memuat data sensitif, ganti dengan potongan yang
relevan saja.
