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

## Struktur

```
.
├── index.html      # shell UI + loader ikon & wt.obf.js
├── styles.css      # design tokens + animasi Material Expressive
├── js/
│   ├── api.js      # klien API, sesi, parser token
│   └── app.js      # state, alur login, file manager, animasi
├── serve.py        # server statis lokal
└── README.md
```
