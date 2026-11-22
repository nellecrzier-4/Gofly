# Gofile Expressive

Klien GoFile dua sisi: **aplikasi Android native 100% Compose** (tanpa WebView) dan **klien web statis** (HTML/CSS/JS, tanpa build step) yang bisa dipasang sebagai PWA.

Bahasa visualnya HyperSpring: latar hitam, kartu `#0E0E10` yang dibedakan garis rambut 0,8dp, satu aksen koral `#FF5F45`, sudut 18dp, dan gerak spring (`hyperSpring(0.4f, 0.85f)`). Sejak 4.0 ada juga palet terang dan warna dinamis Material You.

## Login

1. Masukkan email GoFile → `POST /accounts` mengirim magic link.
2. **Android**: ketuk link `gofile.io/login/<token>` di email → App Link membuka aplikasi dan membuat sesi (tanpa browser). Bisa juga menempel link/token manual.
3. **Web**: tempel link/token → `GET /accounts/website` menukar token menjadi profil + `rootFolder`.
4. Tamu tetap bisa masuk lewat `POST /accounts` tanpa email.

GoFile tidak menyediakan login kata sandi; magic link adalah satu-satunya jalan untuk akun ber-email.

## Aplikasi Android

Seluruh UI Jetpack Compose, seluruh jaringan lewat `GofileApi` (OkHttp). Tidak ada `WebView`, `androidx.webkit`, Custom Tabs, atau jembatan JavaScript — CI menggagalkan PR bila muncul pemakaian nyata.

**Penjelajah berkas**: paginasi otomatis, pencarian dalam folder dengan sorotan, urut nama/ukuran/tanggal/tipe, filter tipe, tampilan daftar dan grid dengan thumbnail (Coil), breadcrumb, pull-to-refresh, skeleton, cache folder untuk mode offline, favorit, terbaru, pencarian lintas folder yang pernah dibuka, multi-pilih dengan bilah aksi kaca, rename/move/copy, detail item, dan hapus dengan jendela **Undo** (API GoFile tidak punya recycle bin — lihat "Batas API").

**Pratinjau native**: gambar (zoom, geser, galeri, slideshow), video/audio (Media3 ExoPlayer dengan header CDN), teks/kode/Markdown (Compose `AnnotatedString`, tanpa HTML), PDF (`PdfRenderer`), dan "Buka dengan" lewat chooser sistem + FileProvider.

**Berbagi**: lembar bagikan dengan pratinjau tautan, salin, `ACTION_SEND`, kode QR (ZXing digambar di Compose), dan pengaturan konten (publik, kata sandi, kedaluwarsa, deskripsi, tag) lewat `PUT /contents/{id}/update`.

**Transfer**: antrean di foreground service dengan batas paralel, jeda/lanjutkan, unduh ulang dengan `Range` + validasi `ETag`/`Last-Modified`, coba ulang dengan backoff, kecepatan + ETA, riwayat persisten, hanya-Wi-Fi, folder unduhan SAF, verifikasi MD5 bila tersedia, serta notifikasi dengan aksi buka/bagikan.

**Integrasi sistem**: terima `ACTION_SEND`/`SEND_MULTIPLE`, pintasan aplikasi, Splash Screen API, kunci aplikasi biometrik/PIN, tautan `gofile.io/d/<id>` dibuka native.

**Setelan**: tema Sistem/Gelap/Terang, lima aksen, warna dinamis, bahasa per-aplikasi (Indonesia/Inggris), ukuran teks, kerapatan daftar, tampilan dan urutan bawaan, mode konfirmasi hapus, jendela Undo, jaringan, lokasi unduhan, kunci aplikasi, bersihkan cache/pustaka, ekspor log, dan halaman Akun (tier, kuota, rincian penyimpanan bila dikirim API).

## Klien web

`index.html` + `js/` bekerja penuh di browser: unggah (pilih berkas dan seret-lepas), buat folder, hapus, unduh, rename, pindah/salin, bagikan, pencarian, urutan, filter, multi-pilih, menu klik kanan, pintasan papan tombol, pratinjau, dan detail. `manifest.webmanifest` + `sw.js` membuatnya bisa dipasang dan tetap terbuka offline untuk folder terakhir. Semua referensi `window.AndroidBridge` sudah dihapus; web dan Android tidak saling bergantung.

`X-Website-Token` dihitung lokal dengan Web Crypto (`crypto.subtle.digest`), dengan fallback ke skrip resmi `wt.obf.js`.

```bash
python3 serve.py --port 5173   # buka http://localhost:5173
npm run check                   # validasi sintaks js
```

## Build Android

```bash
cd android
export ANDROID_HOME=/path/ke/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
./gradlew :app:assembleDebug      # APK: android/app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest  # 45 unit test
./gradlew :app:lintDebug
```

Min SDK 26, compile/target SDK 35, JDK 17, AGP 8.6.1, Kotlin 2.0.20.

Rilis: `./gradlew :app:assembleRelease` memakai `signingConfig` dari variabel lingkungan `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. CI memetakannya dari secret `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, lalu membuat GitHub Release pada tag `v*`.

## Batas API GoFile

Fitur berikut **tidak** disediakan API, jadi tidak dibuat palsu; UI menyatakannya:

- **Tidak ada recycle bin / tempat sampah.** Hapus ditunda beberapa detik di balik snackbar Undo, lalu `DELETE` dikirim.
- **Tidak ada pencarian global.** Pencarian lintas folder hanya mencakup folder yang pernah dibuka dan di-cache.
- **Tidak ada daftar tautan berbagi, statistik, atau cabut tautan.**
- **Tidak ada endpoint ZIP.** Folder tidak bisa diunduh sebagai satu arsip.
- **Tidak ada nilai "publik" saat dibaca.** Toggle publik dimulai sebagai "tidak diubah" dan hanya dikirim bila disentuh.
- Endpoint pindah/salin membutuhkan akun Premium (`error-notPremium` dipetakan ke pesan yang jelas).
- Nilai untuk menghapus tanggal kedaluwarsa belum dipastikan dokumentasi; dikirim sebagai string kosong.

## Struktur

```
.
├── index.html, styles.css, js/       # klien web + PWA (sw.js, manifest.webmanifest)
├── android/                          # aplikasi native Kotlin + Compose
│   └── app/src/main/java/com/nellecrzier/gofileexpressive/
│       ├── GofileApi.kt, GofileParsing.kt, GofileRepository.kt, GofileConfig.kt
│       ├── TransferService.kt, TransferStore.kt, transfer/
│       ├── ui/ (folder, home, library, login, settings, transfer, preview, share, security, account)
│       └── vm/ (Folder, Session, Settings, Transfers)
├── .github/workflows/                # ci.yml, android.yml, release.yml, deploy-pages.yml
├── serve.py
├── CHANGELOG.md, CONTRIBUTING.md, LICENSE
└── README.md
```

Lisensi Apache-2.0. Lihat `CHANGELOG.md` untuk riwayat versi dan `CONTRIBUTING.md` sebelum mengirim perubahan.
