# Gofile Expressive

File manager GoFile berbasis web dengan **bahasa visual glass ala COUI** (terinspirasi CherryPick-Icons): permukaan monokrom, satu aksen biru, sudut membulat besar, baris berkelompok dengan garis rambut, dan chrome kaca. Tanpa build step: cukup HTML, CSS, dan JavaScript native.

## Alur login (magic link)

1. Masukkan email GoFile → aplikasi memanggil `POST https://api.gofile.io/accounts` dengan body `{ "email": "..." }`.
2. GoFile mengirim **login link** berbentuk `https://gofile.io/login/<token32>` ke email tersebut.
3. **Android**: ketuk link di email → link dibuka **langsung oleh aplikasi** (App Link `gofile.io/login`), token ditangkap native, sesi dibuat otomatis — **tanpa membuka browser**. Boleh juga tempel link/token manual ke kolom **Link**.
4. **Web**: tempel link itu (atau langsung token 32 karakternya) ke kolom **Link** → aplikasi memanggil `GET /accounts/website` dengan `Authorization: Bearer <token>` untuk memverifikasi dan mengambil `rootFolder`.
5. Berhasil masuk → tab utama menampilkan isi **root folder** akun GoFile kamu (live/sinkron dari API).

> Tombol *tempel dari clipboard* tersedia, dan token bisa juga diketik manual. GoFile API tidak menyediakan endpoint kata sandi (`/accounts/tokens` tidak ada), jadi magic link adalah satu-satunya cara masuk akun ber-email.

## Fitur utama

- **Tab utama = root folder**: breadcrumb navigasi, masuk ke folder bertingkat. Di Android, ketuk file = **unduh native** lewat foreground service — tanpa browser.
- **Sinkron nyata**: tombol *Sinkron* memuat ulang isi folder langsung dari GoFile.
- **Cari, urutkan, dan ganti tampilan** grid/list; folder selalu tampil lebih dulu.
- **Statistik**: jumlah item, folder, file, dan total ukuran.
- **Desain COUI** (mirrors [CherryPick-Icons](https://github.com/immorgue/CherryPick-Icons), versi web):
  - palet COUI ulang: dark `#000000`/`#1A1A1A`/`#333333` dengan teks `#E6E6E6`/`#A3A3A3`, mode terang `#F1F1F1`/`#FFFFFF`/`#E6E6E6`; satu aksen `#007AFF`;
  - vignet monokrom subtle di atas; kartu status hubung dengan garis rambut 1px; kartu file datar rambut 1px tanpa shadow lift, untuk tampilan grid dan daftar (daftar = baris terasih di sebidang seperti pil);
  - topbar menyusut saat discroll;
  - Material Symbols Rounded dengan axis `FILL`, `wght`, `GRAD`, `opsz`: ikon bisa "berisi" saat aktif;
  - segmen tampilan membentuk pil frosted yang menerjap scale 1.07 saat aktif; tombol yang tidak aktif hanya ikon.
  - animasi berdurasi pendek dengan easing redam, stagger card, ripple, skeleton shimmer, snackbar meluncur, dan View Transitions saat pindah folder.
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
  - `POST /accounts`: kirim login link (membuat akun jika belum ada),
  - `GET /accounts/website`: tukar token jadi profil + `rootFolder`,
  - `GET /contents/{id}`: children folder dengan header `X-Website-Token` dan `X-BL`.
- `X-Website-Token` dihasilkan oleh skrip resmi GoFile `https://gofile.io/js/wt.obf.js` yang dimuat dari CDN di `index.html`. Skrip ini dapat dirotasi oleh GoFile; aplikasi memuatnya segar setiap halaman dibuka.
- Token disimpan di `localStorage` browser (`gofile-expressive-session`) hanya untuk sesi ini. Tombol **Keluar** menghapusnya. Jangan pakai di komputer bersama tanpa membersihkan sesi.
- Permintaan langsung dari browser ke `api.gofile.io` diizinkan (CORS). Jika GoFile mengubah kebijakan CORS atau proteksi website-token, aplikasi perlu disesuaikan.

## Aplikasi Android (native Kotlin)

Folder `android/` berisi proyek Android native (Kotlin + Jetpack Compose). **Tidak ada `WebView`**: seluruh UI dan panggilan API berjalan native:

- **Login native**: setup tiga halaman (selamat datang → email → token) di atas latar titik yang bergerak pelan dengan dua sumber cahaya koral, judul besar yang muncul huruf per huruf, pil *Lewati*, indikator halaman berbentuk tetesan, dan tombol 56dp. Validasi email, aksi keyboard/IME, dan pesan kesalahan tetap terbaca. Bisa kirim magic link, tempel link/token, atau masuk sebagai tamu. **Link login dari email dibuka langsung oleh aplikasi (App Link), bukan browser.** Token website dihitung langsung dengan SHA-256 memakai rumus resmi GoFile.
- **Bar navigasi kaca**: satu kapsul melayang 10dp dari tepi dengan garis tepi putih 12%, kilau atas, dan pil aktif putih 16%. Tab terpilih membuka labelnya dengan spring `response 0,4 / damping 0,85`; ikon dan label aktif berwarna koral. Konten yang lewat di belakangnya **diburamkan langsung** (`RenderEffect` pada `GraphicsLayer`, Android 12+) lalu ditimpa tint tipis, jadi kapsul benar-benar tembus pandang; di Android 11 ke bawah dipakai tint 42% sebagai gantinya.
- **Gerak ala HyperSpring**: spring dinyatakan sebagai *response + damping* (`hyperSpring(0.4f, 0.85f)`), kurva `expo-out`/`power3-out` untuk reveal, judul layar 40sp yang miring ke belakang (`rotationX`) dan memudar saat digulir, baris daftar masuk berurutan, dan tombol dengan tekan karet 1,05×0,88 lalu memantul kosinus teredam. Semua animasi berhenti saat skala animator sistem 0.
- **Checklist izin**: notifikasi dan izin latar belakang ditampilkan sebagai satu kartu status dengan aksi langsung.
- **Bahasa visual HyperSpring** (diport dari [olilio3/HyperSpring](https://github.com/olilio3/HyperSpring)): latar `#000000`, kartu `#0E0E10` yang dibedakan oleh garis rambut 0,8dp `#17FFFFFF` alih-alih bayangan, satu aksen koral `#FF5F45`, teks `#F5F5F7`/`#8E8E96`/`#5C5C66`, judul 40sp dengan jarak huruf negatif, label bagian 11,5sp kapital berjarak lebar, sudut 18dp, dan tanpa mode terang maupun warna dinamis.
- **Browser folder native**: daftar folder/file dari `GET /contents/{id}`, navigasi masuk/keluar, refresh, dan logout.
- **Back prediktif**: gestur back menggeser halaman mengikuti jari (geser 36dp + skala 5%) sebelum menutup tab atau folder; `enableOnBackInvokedCallback` aktif sehingga animasi sistem ikut berjalan di Android 14+.
- **Kelola folder**: buat folder baru (`POST /contents/createFolder`) dan hapus file/folder (`DELETE /contents`) langsung dari daftar.
- **Unggah file** dari pemilih file Android → `POST https://upload.gofile.io/uploadfile` ke folder yang sedang dibuka (dengan token akun).
- **Unduh file** ke folder *Downloads* perangkat (MediaStore di Android 10+, folder `Gofile Expressive/Downloads` di Android 9 ke bawah). Fallback otomatis mencoba ulang tanpa header akun jika node CDN menolak header tersebut.
- **Foreground service** bertipe `dataSync` + *partial wake lock*: transfer tetap berjalan saat aplikasi ditutup, pindah ke home screen, atau layar mati.
- **Notifikasi progres** (channel `gofile_transfers`) menampilkan persentase dan hasil akhir; notifikasi bisa diketuk untuk kembali ke aplikasi.
- **Izin wajib**: `POST_NOTIFICATIONS` (Android 13+) diminta sebelum transfer, dan pengguna diarahkan untuk mengizinkan *Ignore battery optimizations* agar sistem tidak mematikan transfer di latar belakang.
- **Panel transfer Compose**: progres upload/download diamati langsung dari `TransferStore` (StateFlow) di dalam panelnya sendiri, termasuk keadaan indeterminate saat ukuran belum diketahui. Setiap baris bisa digeser ke kiri atau kanan untuk dihapus, dan tombol tutup membersihkan seluruh daftar.
- **Indikator loading ekspresif**: `ExpressiveLoader` menggambar satu blob berdenyut yang berputar, dipakai di tombol, sinkronisasi, dan keadaan kosong, menggantikan spinner cincin biasa.
- **Aksi merusak tanpa merah**: hapus file memakai baris *tahan untuk konfirmasi* 900ms dengan isi koral yang mengalir; ketukan pendek ditolak dengan getar dan goyangan kecil.
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
