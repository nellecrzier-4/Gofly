# Upgrade report — 4.0

Ringkasan pekerjaan pada brief "Complete Gofile Expressive", apa yang selesai,
apa yang tidak, dan keputusan desainnya. Versi: 4.0 (versionCode 13).

## Verifikasi

| Pemeriksaan | Hasil |
|---|---|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL, APK `app/build/outputs/apk/debug/` |
| `./gradlew :app:testDebugUnitTest` | 45 test, 0 gagal |
| `./gradlew :app:lintDebug` | 0 error (71 warning) |
| `npm run check` (web) | lolos (`api.js`, `app.js`, `qr.js`, `sw.js`) |
| `python3 -m py_compile serve.py` | lolos |
| Guard WebView (CI) | lolos — hanya komentar/teks yang menyebut "WebView" |
| `grep -rn AndroidBridge js/ index.html` | kosong |

Tidak diuji di perangkat/emulator: tidak ada perangkat di lingkungan build.
Yang bisa diverifikasi secara statis (tipe, resource, lint, unit test) sudah
diperiksa; perilaku runtime di layar belum.

## Per fase

**Phase 0 — fondasi.** Sudah ada di snapshot (pemisahan `ui/`, ViewModel,
OkHttp, `GofileConfig`, hierarki `GoFileException`, cache folder, sesi
terenkripsi). Ditambahkan: penguraian balasan dan pemetaan galat dipisah ke
`GofileParsing` agar bisa diuji tanpa jaringan; `AppPrefs` diperluas menjadi
model preferensi penuh; tema dipindah ke satu objek token.

**Phase 1 — file manager.** Snapshot sudah punya paginasi, pencarian, urutan,
filter, grid/list, breadcrumb, pull-to-refresh, skeleton, cache offline,
rename, move/copy, multi-pilih, undo-hapus, detail, favorit, recent, pencarian
lintas folder, dan FAB. Ditambahkan: pratinjau saat mengetuk berkas, kerapatan
daftar (rapat/nyaman/longgar), mode konfirmasi hapus (tahan atau dialog),
jendela Undo mengikuti Setelan.

**Phase 2 — berbagi dan pengaturan konten.** Selesai: lembar bagikan dengan
pratinjau tautan, salin, `ACTION_SEND`, kode QR (ZXing digambar di Compose),
pengaturan konten (publik, kata sandi, kedaluwarsa, deskripsi, tag). Tidak
dibuat karena API tidak menyediakannya: daftar tautan berbagi, statistik, dan
cabut tautan.

**Phase 3 — transfer.** Selesai: antrean dengan batas paralel, batal/jeda/lanjut
dari baris dan notifikasi, unduh ulang `Range` + `If-Range` + cek `Content-Range`
(200 → mulai dari nol dengan pesan jelas), coba ulang 3× backoff, kecepatan dan
ETA, riwayat persisten (`filesDir/transfer/history.json`, 200 entri), hanya
Wi-Fi + NetworkCallback, folder tujuan SAF dengan akhiran unik saat bentrok,
verifikasi MD5 bila tersedia, notifikasi dengan aksi buka/bagikan, dan pesan
galat ramah untuk kuota/berkas terlalu besar/penyimpanan penuh/jaringan
putus/token kedaluwarsa.

Batas yang diakui: unggahan tidak bisa dilanjutkan (server tidak mendukung), jadi
"lanjutkan" untuk unggahan mengulang dari nol dan state-nya mengatakan itu;
folder yang diulang bisa terduplikasi karena `createFolder` tidak
mengembalikan id folder baru.

**Phase 4 — integrasi sistem.** Selesai: `ACTION_SEND`/`SEND_MULTIPLE`, pintasan
statis (Unggah, Transfer), Splash Screen API, tautan `gofile.io/d/<id>`, kunci
aplikasi biometrik/PIN yang aktif lagi setiap kembali dari latar belakang.
Tidak dibuat: widget Glance dan Quick Settings tile.

**Phase 5 — pratinjau.** Selesai seluruhnya: gambar (zoom, geser, pager galeri,
slideshow, geser-turun untuk menutup), video/audio (Media3 dengan header CDN),
teks/kode/Markdown (Compose, batas 1 MB, nomor baris), PDF (`PdfRenderer`),
chooser "Buka dengan" lewat FileProvider. Markdown tidak dapat diklik tautannya
karena itu berarti meluncurkan Intent dari dalam renderer.

**Phase 6 — akun dan setelan.** Selesai: tier/kuota/ringkasan akun, sesi
kedaluwarsa ditangani sebagai layar, semua baris Setelan berfungsi (tema,
aksen, warna dinamis, bahasa per-aplikasi, ukuran teks, kerapatan, tampilan dan
urutan bawaan, hapus, undo, hanya Wi-Fi, transfer paralel, folder unduhan,
layar tetap menyala, suara notifikasi, kunci aplikasi, cache, pustaka, ekspor
log, tentang, changelog). Tidak dibuat: multi-akun. Rincian penyimpanan per tipe
hanya muncul bila balasan API memuatnya; GoFile belum mengonfirmasi kuncinya,
jadi UI menyatakan "tidak disediakan API" alih-alih menggambar data palsu.

**Phase 7 — tata letak dan aksesibilitas.** Sebagian: token terkonsentrasi,
target sentuh 48dp pada kontrol baru, `contentDescription` pada kontrol ikon,
kontras AA pada palet terang dan gelap, teks mengikuti skala font sistem,
tema terang lengkap. Tidak dikerjakan: dua panel tablet/lipat
(`WindowSizeClass` sudah menjadi dependensi tetapi belum dipakai), transisi
shared element, dan tinjauan TalkBack menyeluruh.

**Phase 8 — klien web.** Selesai: seluruh jalur `AndroidBridge` dihapus; unggah
(pemilih + seret-lepas, 3 paralel, progres per berkas), buat folder, hapus,
unduh, rename, pindah/salin, bagikan, menu klik kanan, pintasan papan tombol,
multi-pilih, detail, pratinjau, cache folder offline, PWA (`manifest.webmanifest`,
`sw.js`, `offline.html`, ikon), token website dihitung lokal dengan Web Crypto
dan fallback ke skrip resmi, keluar membersihkan sesi + cache + peringatan
komputer bersama.

**Phase 9 — test, CI, rilis.** Selesai: 45 unit test (token, website token,
parsing folder/pencarian, pemetaan galat, JSON item, transfer store), satu
instrumented smoke test layar login, `android.yml` (lint + test + build + guard
WebView + artefak), `release.yml` (tanda tangan dari secret, GitHub Release pada
`v*`), `LICENSE`, `CONTRIBUTING.md`, `CHANGELOG.md`, README yang diperbarui,
log crash lokal + ekspor.

**Phase 10 — polish.** Sebagian: elipsis tengah pada nama panjang, pratinjau
dan detail yang konsisten, snackbar/pil umpan balik, validasi nama langsung,
animasi daftar bawaan Compose, token di satu tempat, edge-to-edge, "apa yang
baru" dari `CHANGELOG.md`, aksi buka pada berkas terunduh. Tidak dikerjakan:
bilah transfer mini di atas navigasi, lencana jumlah transfer, coachmark gestur,
header lengket, fast scroller, dan audit performa (baseline profile,
penghitungan recomposition).

## Keputusan desain

- **Satu objek token `Hyper`.** Getter membaca set token aktif; `GofileTheme`
  menukar set lalu memaksa subtree digambar ulang dengan `key(...)`. Ini
  menghindari menyentuh ratusan pemanggil saat menambah tema terang dan warna
  dinamis. Konsekuensinya: pembacaan token bukan `State`, jadi kunci itu wajib.
- **Tema terang adalah palet turunan, bukan mode Material terpisah.** Latar
  kertas hangat, kartu putih, garis rambut lebih tegas, aksen digelapkan agar
  kontras AA tetap lolos.
- **Riwayat transfer memakai JSON, bukan Room.** Brief mengizinkan keduanya;
  JSON menghindari KSP dan menjaga waktu build. Cache folder tetap memakai file
  JSON seperti sebelumnya.
- **Bahasa per-aplikasi** memakai `LocaleManager` (33+) dan
  `AppCompatDelegate` + pembungkusan konteks (26–32). Perubahan bahasa membuat
  aktivitas dibuat ulang sekali.
- **Kunci aplikasi** memakai `BiometricPrompt` dengan kredensial perangkat
  sebagai fallback, karena itu `MainActivity` turun dari `FragmentActivity`.

## Yang masih terbuka

1. Widget Glance, Quick Settings tile, multi-akun.
2. Dua panel tablet, transisi shared element.
3. Bilah transfer mini, lencana, coachmark, header lengket, fast scroller.
4. ktlint/detekt belum dikonfigurasi; CI baru menjalankan lint Android.
5. Belum ada verifikasi runtime di perangkat, termasuk TalkBack dan skala font
   200% secara visual.
