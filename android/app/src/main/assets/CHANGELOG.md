# Changelog

Semua perubahan penting pada Gofly. Format mengikuti Keep a
Changelog, dan versi mengikuti Semantic Versioning. Berkas ini dibaca apa
adanya oleh aplikasi (Setelan > Apa yang baru), jadi tulis sebagai teks biasa:
tanpa tabel, tanpa HTML, dan baris pendek.

## 4.3.0 - 2026-11-23

### Antarmuka
- Tanda aplikasi di layar selamat datang kini berisi ikon awan, dan berganti
  menjadi ikon kotak masuk di langkah email dan tautan.

### Perbaikan
- Ikon ceklis di notifikasi singkat (dan ikon pengganti lain yang memakai
  morfosis ikon) tidak lagi terputar 90 derajat setelah animasi selesai.

## 4.2.0 - 2026-11-23

### Umum
- Aplikasi berganti nama menjadi Gofly.
- Pembaruan kini bisa dipasang langsung di atas versi lama tanpa uninstall:
  versionCode dihitung otomatis dari versionName, dan ID aplikasi serta
  kunci tanda tangan dipertahankan.

## 4.1.0 - 2026-11-23

### Antarmuka
- Aplikasi Android pindah penuh ke Material 3 Expressive: warna dinamis
  Material You di Android 12+, skema tonal dari aksen pilihan sebagai
  cadangan, tipografi dan bentuk bawaan M3, serta sudut bersambung untuk
  daftar dan grup kontrol.
- Komponen Expressive asli dari material3 1.5: indikator muat bermorfosis,
  progres bergelombang, navigasi singkat adaptif, dan sudut bersambung yang
  dianimasikan saat item disisipkan atau dihapus.
- Gerak memakai `MaterialTheme.motionScheme` Expressive (spatial untuk
  posisi/bentuk, effects untuk alfa/warna), dengan setelan kurangi gerak
  (SYSTEM/REDUCED/OFF) yang mematikan morph, stagger, dan goyangan.
- Efek kaca, latar titik, dan komponen visual lama dihapus; tidak ada lagi
  shader AGSL maupun blur yang hanya berjalan di Android 13+.

## 4.0.0 - 2026-11-23

Rilis besar: aplikasi Android ditulis ulang sepenuhnya secara native dengan
Kotlin dan Jetpack Compose. Tidak ada WebView, tidak ada HTML, dan semua
komunikasi GoFile lewat OkHttp. Versi web tetap tersedia sebagai PWA terpisah.

### Pratinjau berkas
- Pratinjau gambar dengan zoom dan geser, termasuk thumbnail di tampilan grid
  dan daftar.
- Pemutar video dan audio native (Media3 ExoPlayer) langsung di dalam aplikasi.
- Pembaca teks dan penampil Markdown dengan gulir yang nyaman.
- Penampil PDF yang merender halaman sesuai lebar layar.
- Berkas yang tidak bisa dipratinjau dibuka lewat aplikasi lain melalui
  FileProvider, tanpa meninggalkan aplikasi ke browser.

### Berbagi dan tautan
- Bagikan tautan berkas atau folder, salin ke papan klip, dan buat kode QR.
- Pengaturan konten: nama, deskripsi, tag, publik atau privat, kata sandi, dan
  masa berlaku tautan.
- Tautan login GoFile dan tautan folder publik (gofile.io/d/...) dibuka
  langsung di aplikasi.

### Antrean transfer
- Antrean unggah dan unduh dengan progres, kecepatan, sisa waktu, dan percobaan
  ulang otomatis.
- Jeda, lanjutkan, batalkan, dan bersihkan per item atau sekaligus.
- Riwayat transfer tersimpan supaya bisa dilihat lagi setelah aplikasi ditutup.
- Layanan latar depan dengan notifikasi progres, dan pengunduhan bisa
  dilanjutkan setelah koneksi putus.

### Integrasi Android
- Terima berkas dari aplikasi lain (bagikan ke Gofly).
- Pintasan peluncur untuk unggah dan membuka antrean transfer.
- Splash screen Android 12+ yang mengikuti tema aplikasi.
- Kunci aplikasi dengan biometrik atau PIN, diminta lagi setiap kembali dari
  latar belakang.

### Setelan
- Setelan lengkap: tema terang atau gelap, ukuran dan tampilan daftar, urutan
  folder, gerakan yang dikurangi, serta bahasa per aplikasi (Android 13+).
- Manajemen sesi, ukuran cache, ekspor log, dan halaman "Apa yang baru".

### Web (PWA)
- Klien web dengan bahasa visual glass yang bisa dipasang sebagai PWA, memakai
  API dan alur login yang sama.

### Kualitas
- Suite unit test JVM untuk token, penguraian balasan, serialisasi, dan antrean
  transfer; uji asap instrumentasi untuk layar masuk.
- CI GitHub Actions: lint, unit test, build APK debug, larangan WebView, serta
  alur rilis bertanda tangan untuk setiap tag versi.
- Lisensi Apache-2.0, panduan kontribusi, dan changelog ini.
