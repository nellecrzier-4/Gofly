# Changelog

Semua perubahan penting pada Gofile Expressive. Format mengikuti Keep a
Changelog, dan versi mengikuti Semantic Versioning. Berkas ini dibaca apa
adanya oleh aplikasi (Setelan > Apa yang baru), jadi tulis sebagai teks biasa:
tanpa tabel, tanpa HTML, dan baris pendek.

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
- Terima berkas dari aplikasi lain (bagikan ke Gofile Expressive).
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
