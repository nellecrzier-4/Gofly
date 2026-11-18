# Gofile Expressive

File manager GoFile berbasis web dengan **tema Material 3 Expressive**: warna aksen berani, bentuk membulat, ikon ekspresif, dan animasi spring. Tanpa build step — cukup HTML, CSS, dan JavaScript native.

## Alur login (magic link)

1. Masukkan email GoFile → aplikasi memanggil `POST https://api.gofile.io/accounts` dengan body `{ "email": "..." }`.
2. GoFile mengirim **login link** berbentuk `https://gofile.io/login/<token32>` ke email tersebut.
3. Tempel link itu (atau langsung token 32 karakternya) ke kolom **Link** → aplikasi memanggil `GET /accounts/website` dengan `Authorization: Bearer <token>` untuk memverifikasi dan mengambil `rootFolder`.
4. Berhasil masuk → tab utama menampilkan isi **root folder** akun GoFile kamu (live/sinkron dari API).

> Tombol *tempel dari clipboard* tersedia, dan token bisa juga diketik manual.

## Fitur utama

- **Tab utama = root folder**: breadcrumb navigasi, masuk ke folder bertingkat, buka file di tab baru.
- **Sinkron nyata**: tombol *Sinkron* memuat ulang isi folder langsung dari GoFile.
- **Cari, urutkan, dan ganti tampilan** grid/list; folder selalu tampil lebih dulu.
- **Statistik**: jumlah item, folder, file, dan total ukuran.
- **Material Expressive**:
  - token warna dinamis (ungu/violet + aksen pink), permukaan kaca, blob ambient;
  - Material Symbols Rounded dengan axis `FILL`, `wght`, `GRAD`, `opsz` — ikon bisa "berisi" saat aktif;
  - animasi emphasized/spring, stagger card, ripple, ikon bergoyang, breadcrumb halus, skeleton shimmer, snackbar meluncur, dan View Transitions saat pindah folder.
- **Responsif** dan menghormati `prefers-reduced-motion`.

## Menjalankan

Butuh Python 3. Tidak perlu Node/npm.

```bash
cd gofile-expressive
python3 serve.py --port 5173
```

Buka <http://localhost:5173>.

Atau langsung buka `index.html` di browser (beberapa browser membatasi clipboard API di `file://`, jadi server lokal lebih disarankan).

## Catatan teknis

- Endpoint mengikuti apa yang dipakai situs resmi `gofile.io`:
  - `POST /accounts` — kirim login link (membuat akun jika belum ada),
  - `GET /accounts/website` — tukar token jadi profil + `rootFolder`,
  - `GET /contents/{id}` — children folder dengan header `X-Website-Token` dan `X-BL`.
- `X-Website-Token` dihasilkan oleh skrip resmi GoFile `https://gofile.io/js/wt.obf.js` yang dimuat dari CDN di `index.html`. Skrip ini dapat dirotasi oleh GoFile; aplikasi memuatnya segar setiap halaman dibuka.
- Token disimpan di `localStorage` browser (`gofile-expressive-session`) hanya untuk sesi ini. Tombol **Keluar** menghapusnya. Jangan pakai di komputer bersama tanpa membersihkan sesi.
- Permintaan langsung dari browser ke `api.gofile.io` diizinkan (CORS). Jika GoFile mengubah kebijakan CORS atau proteksi website-token, aplikasi perlu disesuaikan.

## Aplikasi Android (native Kotlin)

Folder `android/` berisi proyek Android native (Kotlin + Jetpack Compose). **Tidak ada `WebView`** — seluruh UI dan panggilan API berjalan native:

- **Login native**: kirim magic link, tempel link/token, atau masuk sebagai tamu. Alurnya dua langkah dengan indikator progres, validasi email, aksi keyboard/IME, dan pesan kesalahan yang bisa dibaca. Token website dihitung langsung dengan SHA-256 memakai rumus resmi GoFile.
- **Tema Material 3**: dynamic color di Android 12+ dengan fallback terang/gelap lengkap, bentuk dan tipografi berbasis token Material, edge-to-edge, serta blur lembut yang hanya dipakai sebagai latar dekoratif layar masuk (permukaan form tetap solid).
- **Browser folder native**: daftar folder/file dari `GET /contents/{id}`, navigasi masuk/keluar, refresh, dan logout.
- **Kelola folder**: buat folder baru (`POST /contents/createFolder`) dan hapus file/folder (`DELETE /contents`) langsung dari daftar.
- **Unggah file** dari pemilih file Android → `POST https://upload.gofile.io/uploadfile` ke folder yang sedang dibuka (dengan token akun).
- **Unduh file** ke folder *Downloads* perangkat (MediaStore di Android 10+, folder `Gofile Expressive/Downloads` di Android 9 ke bawah). Fallback otomatis mencoba ulang tanpa header akun jika node CDN menolak header tersebut.
- **Foreground service** bertipe `dataSync` + *partial wake lock*: transfer tetap berjalan saat aplikasi ditutup, pindah ke home screen, atau layar mati.
- **Notifikasi progres** (channel `gofile_transfers`) menampilkan persentase dan hasil akhir; notifikasi bisa diketuk untuk kembali ke aplikasi.
- **Izin wajib**: `POST_NOTIFICATIONS` (Android 13+) diminta sebelum transfer, dan pengguna diarahkan untuk mengizinkan *Ignore battery optimizations* agar sistem tidak mematikan transfer di latar belakang.
- **Panel transfer Compose**: progres upload/download diamati langsung dari `TransferStore` (StateFlow), termasuk keadaan indeterminate saat ukuran belum diketahui.
- **Tanpa jembatan JavaScript**: tidak ada `AndroidBridge`, tidak ada `window.*`, dan tidak bergantung pada halaman GitHub Pages.

### Build APK

```bash
cd android
export ANDROID_HOME=/path/ke/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
./gradlew :app:assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

Min SDK 26, target/compile SDK 35, JDK 17, AGP 8.6.1, Kotlin 2.0.20. Workflow `.github/workflows/android.yml` mem-build APK otomatis di GitHub Actions dan menyimpannya sebagai artefak `gofile-expressive-apk` setiap push ke `main`.

> Aplikasi Android berdiri sendiri dan tidak memuat URL web apa pun. Versi web di GitHub Pages tetap tersedia untuk browser/desktop.

## Struktur

```
.
├── index.html      # shell UI + loader ikon & wt.obf.js
├── styles.css      # design tokens + animasi Material Expressive + UI transfer
├── js/
│   ├── api.js      # klien API, sesi, parser token
│   └── app.js      # state, alur login, file manager, jembatan Android
├── android/        # proyek native Kotlin + Compose (tanpa WebView) + foreground service
├── .github/workflows/
│   ├── ci.yml              # validasi sumber
│   ├── deploy-pages.yml    # deploy web ke GitHub Pages
│   └── android.yml         # build APK
├── serve.py        # server statis lokal
└── README.md
```
