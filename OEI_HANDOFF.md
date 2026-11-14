# OEI MD–WH App: Dokumen Serah Terima (Handoff 005.0 — V0.2-Alpha Stable Edition)

> **Instruksi untuk AI Agent:** Baca dokumen ini sebelum memodifikasi kode di `D:\Project Android/OEIMDApp`. Seluruh keputusan pada **Bagian 2, 3, 4, dan 5** sudah dikunci dan disetujui pengguna — **JANGAN diubah tanpa persetujuan eksplisit.** Sembilan belas poin peningkatan Quality of Life (QoL), performa, dan penyempurnaan UI/UX telah **SELESAI dan TERUJI** pada rilis **V0.2-Alpha**.

## 1. Visi, Masalah & Prinsip Inti

* **Konteks Bisnis:** Perusahaan seni OEI mendistribusikan produk (Artprints, Greeting Cards, Postcards, Coasters, dll.) ke toko mitra melalui 3 divisi: Marketing, Warehouse (WH), dan Merchandiser (MD).
* **Solusi Aplikasi:** Menggantikan proses manual MD (unduh JPG list dari Drive, coret manual, rekap manual) menjadi aplikasi Android *offline-first* yang membaca data dari Google Sheets milik WH dan mengekspor laporan `.xlsx` lokal.
* **4 Prinsip Utama (Wajib Dijaga):**
    1. **Utamakan MD di lapangan:** Operasi satu tangan, cepat, tahan sinyal buruk.
    2. **Nol beban tambahan bagi WH:** WH tetap bekerja di Google Sheets yang sudah ada; aplikasi bersifat *read-only* terhadap Sheets.
    3. **Hindari salah ketik & data usang:** Sinkronisasi otomatis berbasis `hash`.
    4. **Jaga *muscle memory* MD:** Urutan produk mengikuti format list lama (best seller → target terbesar → kode kecil ke besar).

## 2. Tech Stack & Konfigurasi Build (Final & Stabil)

| Lapisan | Teknologi & Versi Terkunci |
|---|---|
| Build & Bahasa | AGP `8.7.3`, Kotlin `2.0.21`, KSP `2.0.21-1.0.28`, JVM Target `11` |
| UI & Arsitektur | Jetpack Compose (Material 3), MVVM (`ViewModel` + Compose State) |
| Backend | Google Sheets + Google Apps Script Web App (**GET, read-only**) |
| Gambar | Google Drive publik via **Coil** (`coil-compose` dengan `AsyncImage` tanpa subcomposition jank) |
| Database Lokal | **Room** (`item_counts`) + `SharedPreferences` (cache JSON `sync` terakhir) |
| Jaringan | Retrofit + OkHttp + Gson (`@Url` dinamis) |
| Export Laporan | Penulis `.xlsx` murni Kotlin (`ZipOutputStream` + OpenXML di `XlsxExporter.kt`) |

## 3. Aturan Bisnis & Alur Aplikasi (Dikunci)

### 3.1 Alur Pengguna & Start Destination

`Cold Start Check → (Jika URL/Token kosong: SettingsScreen) → StoreSearchScreen → StoreScreen (pills list & grid) → Export XLSX / Clear`

1. **Validasi Cold Start:** Saat aplikasi dibuka, cek `webAppUrl` (harus berawalan `http://` atau `https://`) dan `appToken` di `SettingPreferences`. Jika kosong, **langsung buka `SettingsScreen`** dan **jangan panggil endpoint internet**.
2. **Pencarian Toko (`StoreSearchScreen`):** *Real-time*, *case-insensitive*, mencocokkan `nama_toko` dan `area_cakupan`. Saat kolom kosong, tampilkan pintasan **"Toko Terakhir Dibuka"**. Urutan abjad *natural* (`Toko 2` sebelum `Toko 10`). Toko tanpa list aktif disembunyikan. Didukung gestur *Pull-to-Refresh*.
3. **Halaman Toko (`StoreScreen`):**
    * Baris *pills* list di bagian atas (urut `jenis_urutan` terkecil, default **MP** langsung terbuka), menampilkan nama list + progres `terisi/total` (item reguler dengan `count > 0`). Pindah list tidak menghapus angka yang sudah diisi, serta otomatis melakukan *side-scroll* (*auto-scroll pill aktif*).
    * **Tab Vertical / Horizontal** (Floating Orientation Bar melayang dengan bentuk `CircleShape` penuh & ikon gimmick `VerticalSplit`/`HorizontalSplit`) hanya muncul jika `pisah_orientasi == true` (MP & SMP). Jenis lain tampil dalam satu grid campur. Grid list otomatis memiliki ruang kosong (*top padding*) agar card teratas tidak tertutup.
    * **Grid 3 Kolom:** Gambar di atas (`AsyncImage` optimized anti-lag), label `ALIAS-KODE` (`MP-012`), target (`T:X`), badge status (`Habis` / `Kurang X` / `Penuh ✓`), dan tombol stepper **`− [angka] +`** selebar penuh kartu di bawah. Ketuk gambar membuka *Zoom Lightbox*; ketuk angka membuka *Modal Numeric Keypad* (maksimal 4 digit `0–9999`, *auto-replace* nilai awal, tombol pintas *"Isi Sesuai Target"*).
    * **Status Kartu:** `0` = Merah (`Habis`), `0 < x < target` = Kuning (`Kurang X`), `x >= target` = Hijau (`Penuh ✓`). Semua item mulai dari `0`.
    * **Urutan Item:** Ikuti field `urut` dari server (jangan diurutkan ulang).
4. **Kartu Khusus `ALIAS-000` (Produk di Luar List):**
    * Dibuat otomatis oleh aplikasi di akhir grid dengan placeholder kotak abu-abu bertitik *"Di luar list"*, badge *"Tanpa Target"*, tanpa warna merah/kuning/hijau.
    * List `pisah_orientasi` (MP/SMP) memiliki kartu terpisah di tab Vertical (`000-V`) dan Horizontal (`000-H`), lalu dijumlahkan saat export.
5. **Clear & Undo:** Dua opsi (*"Clear list ini"* dan *"Clear semua list toko ini"*) dengan modal konfirmasi ukuran dinamis (mendukung nama panjang) + *Snackbar* **Urungkan** selama 5 detik dengan garis indikator waktu mundur (*countdown progress line*).
6. **Persistensi & Deteksi Perubahan WH:** Disimpan di Room per `storeId + listId + kode`. Jika `hash` list dari server berubah saat sinkronisasi, perbarui struktur list dan tampilkan info *"List diperbarui oleh WH"* sambil mempertahankan angka hitungan MD per kode.

### 3.2 Aturan Export `.xlsx` (Lokal)

* **Dialog Verifikasi:** Menampilkan nama file otomatis, status tiap list (**Belum dibuka / Semua 0 / Terisi**), peringatan jika ada list belum dibuka/semua 0, dan kotak centang wajib **"Saya sudah memeriksa semua list"** sebelum tombol *Buat File XLSX* aktif.
* **Format Nama File:** `LP0042-09112026-Budi-Balizen_Sanur.xlsx` (`id_laporan-ddMMyyyy-nama_md-toko`, karakter terlarang diganti `_`).
* **Struktur Sheet (1 Tab):**
    * Header: `LAPORAN PENGECEKAN STOK`, `Toko`, `Nama MD`, `Tanggal cek` (`dd/MM/yyyy`), `ID Laporan`, diikuti 1 baris kosong.
    * Kolom tabel: `Alias_Jenis_Produk-KODE | Jumlah_Target | Jumlah_Dihitung | Stok_Dibutuhkan | Best_Seller`.
    * Jarak antartabel: **4 baris kosong murni** tanpa teks keterangan tambahan.
    * **List MP/SMP:** Dipecah menjadi tabel **Vertical dulu, lalu Horizontal**. Baris total `MP-000` diletakkan di baris terbawah tabel pertama yang ada (`Jumlah_Target = -`, `Stok_Dibutuhkan = -`, `Best_Seller = Tidak`, cetak tebal + garis atas).
    * **Pewarnaan:** `count == 0` → seluruh baris latar merah muda + teks merah tebal; `0 < count < target` → sel `Stok_Dibutuhkan` merah; `count >= target` → tanpa warna.

## 4. Struktur Google Sheets & API (`Code.gs`)

* **Tab Sheets:** `Area`, `Toko`, `Jenis_Produk`, `Gambar`, `List`, `List_Toko`, `List_Item`.
* **Endpoint GET:**
    * `<WEB_APP_URL>?token=<TOKEN>&action=ping`
    * `<WEB_APP_URL>?token=<TOKEN>&action=sync` (opsional `&nocache=1`)
* **Format Respons `sync`:**
  ```json
  {
    "ok": true, "schema": 1, "generated_at": "ISO8601", "data_hash": "12hex",
    "jenis": [{"nama":"Mounted Artprint","alias":"MP","pisah_orientasi":true,"urutan":1}],
    "areas": ["Sanur"],
    "stores": [{"id":"BZS","nama":"Balizen Sanur","area":"Sanur","list_ids":["BZS-MP","BZS-SMP","GEN-GC"]}],
    "lists": {
      "BZS-MP": {
        "id":"BZS-MP","nama":"Balizen Sanur - MP","tipe":"Standar|Carousel",
        "jenis_alias":"MP","jenis_nama":"Mounted Artprint","jenis_urutan":1,
        "pisah_orientasi":true,"hash":"12hex",
        "items":[{"kode":"015","label":"MP-015","orientasi":"Horizontal|Vertical",
                  "target":3,"best_seller":true,"posisi":null,"file_id":"<DriveFileId>","urut":1}]
      }
    },
    "warnings": ["..."]
  }
  ```
* **URL Gambar Drive:** Utama `https://drive.google.com/thumbnail?id=<file_id>&sz=w400`, cadangan `https://drive.google.com/uc?export=view&id=<file_id>`.

## 5. Sistem Desain & Palet Warna (Dikunci)

* **Aturan Dasar:** Bingkai foto produk di Light Mode wajib `#FFFFFF` (akurasi warna karya seni). Wajib `fontFeatureSettings = "tnum"` (angka tabular) pada kode item, stepper, progres `terisi/total`, dan numeric keypad. Badge `★ Best` universal: bg `#020617` (`0.85f`), teks `#FCD34D`. Penguncian skala font (`fontScale = 1.0f`) di tingkat aplikasi untuk memastikan ketahanan terhadap preferensi skala font perangkat.

| Token / Status | Mode Terang (`Gallery Crisp`) | Mode Gelap (`Field Pro Dark`) |
|---|---|---|
| **Permukaan & Teks** | Bg `#F4F6F9` · Card `#FFFFFF` · Subtle `#F8FAFC`/`#F1F5F9` · Border `#E2E8F0` · Teks `#0F172A`/`#475569` · Pill `#0F172A` (teks `#FFFFFF`) | Bg `#090D16` · Card `#111827` · Subtle `#1E293B`/`#0F172A` · Border `#1E293B`/`#334155` · Teks `#F8FAFC`/`#94A3B8` · Pill `#FBBF24` (teks `#090D16`) |
| **Habis (`0`)** | Border `#DC2626` · Bawah `#FEF2F2` · Badge `#DC2626` (`#FFFFFF`) · Stepper `#FEE2E2` (`#991B1B`) · Angka `#FFFFFF` (`#991B1B`) | Border `#EF4444` · Kartu `#271117` · Badge `#7F1D1D` (`#FCA5A5`) · Stepper `#7F1D1D` (`#FFFFFF`) · Angka `#180A0E` (`#FCA5A5`) |
| **Kurang (`0 < x < T`)** | Border `#D97706` · Bawah `#FFFBEB` · Badge `#FEF3C7` (`#B45309`) · Stepper `#FEF3C7` (`#92400E`) · Angka `#FFFFFF` (`#92400E`) | Border `#F59E0B` · Kartu `#261A0C` · Badge `#78350F` (`#FDE68A`) · Stepper `#78350F` (`#FDE68A`) · Angka `#170F07` (`#FDE68A`) |
| **Penuh (`x >= T`)** | Border `#059669` · Bawah `#ECFDF5` · Badge `#D1FAE5` (`#047857`) · Stepper `#D1FAE5` (`#065F46`) · Angka `#FFFFFF` (`#065F46`) | Border `#10B981` · Kartu `#0C261D` · Badge `#064E3B` (`#A7F3D0`) · Stepper `#064E3B` (`#A7F3D0`) · Angka `#071711` (`#A7F3D0`) |

## 6. Peta Kode Proyek (`app/src/main/java/com/example/oeimdapp/`)

* `MainActivity.kt` — Navigasi layar, cold start check `isConfigured`, `BackHandler`, penguncian font scale `1.0f`.
* `viewmodel/MainViewModel.kt` — State aplikasi, tema, offline JSON cache, deteksi perubahan `hash`, natural sort, Clear + Undo 5 detik.
* `data/model/ApiResponse.kt` — DTO respons JSON `sync`.
* `data/remote/ApiService.kt` & `RetrofitClient.kt` — Endpoint dinamis `@Url`, OkHttp timeout, `cleanWebAppUrl()`.
* `data/local/ItemCountEntity.kt`, `ItemCountDao.kt`, `AppDatabase.kt` — Database Room `oei_md_database`.
* `data/repository/OeiRepository.kt` — Repository jaringan & Room.
* `data/prefs/SettingPreferences.kt` — `SharedPreferences` pengaturan, cache JSON, dan counter laporan.
* `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — Sistem tema *Gallery Crisp* & *Field Pro Dark* (dengan `findActivity()` dan `LocalDensity` fontScale lock).
* `ui/screens/SettingsScreen.kt` — Layar Pengaturan dengan tema berikon gimmick dan auto-scroll presisi (*BringIntoViewRequester*).
* `ui/screens/StoreSearchScreen.kt` — Layar Pencarian Toko dengan *Pull-to-Refresh*.
* `ui/screens/StoreScreen.kt` — Layar Utama Toko, grid 3 kolom anti-lag (`AsyncImage`), kartu `-000`, floating orientation bar melayang berikon gimmick, auto-scroll pill list, & countdown undo bar.
* `ui/screens/StoreDialogs.kt` — Modal Numeric Keypad, Zoom Lightbox, Clear Dialog (ukuran dinamis untuk teks panjang).
* `ui/screens/ExportDialog.kt` & `export/XlsxExporter.kt` — Dialog verifikasi export & generator `.xlsx`.

## 7. Status 10+ Poin Peningkatan QoL & Performa (STATUS: SELESAI & TERUJI — V0.2-ALPHA)

> **Catatan:** Seluruh 10 poin perbaikan QoL awal serta penyempurnaan tambahan berikut telah **SELESAI diimplementasikan, diuji, dan digabungkan ke branch `main` pada rilis V0.2-Alpha**:

1. **Rasio Kartu Produk Seragam `1:1` (`StoreScreen.kt`):** Selesai (`aspectRatio(1f)`).
2. **Soft Keyboard & Auto-Scroll Pengaturan (`SettingsScreen.kt`):** Selesai dengan `BringIntoViewRequester` presisi dan penanganan IME.
3. **Kontras Teks Banner Error di Mode Terang:** Selesai.
4. **Gestur Pull-to-Refresh (`StoreSearchScreen.kt`):** Selesai.
5. **Kontras Latar Placeholder Kartu `-000` di Mode Terang:** Selesai.
6. **Dashed Border Penuh di Keempat Sisi Kartu `-000` (`StoreScreen.kt`):** Selesai.
7. **Optimasi Performa `LazyVerticalGrid` Anti-Lag (`StoreScreen.kt`):** Selesai dengan migrasi ke `AsyncImage` Coil (bebas *subcomposition jank*).
8. **Transisi Halus Antar-Tab, Antar-List & Antar-Layar (`StoreScreen.kt`):** Selesai dengan *fluid directional slide* dan *AnimatedContent*.
9. **Floating Orientation Bar Melayang Berikon Gimmick (`StoreScreen.kt`):** Selesai dengan bentuk `CircleShape` penuh, animasi muncul/hilang (*Fade + Expand/Shrink*), dan ikon `VerticalSplit`/`HorizontalSplit`.
10. **Animasi Popup Meluas dari Titik Klik (`StoreDialogs.kt`):** Selesai.
11. **Penyempurnaan Tambahan V0.2-Alpha:**
    * *Auto-scroll* pill list aktif secara otomatis (`pillListState`).
    * Modal Clear berukuran dinamis yang mendukung nama toko & list panjang tanpa terpotong.
    * Garis hitung mundur (*countdown progress line*) pada Snackbar Urungkan (5 detik).
    * Pilihan tema pengaturan dengan ikon gimmick (`BrightnessAuto`, `WbSunny`, `DarkMode`).
    * Penguncian skala font aplikasi di `1.0f` untuk mengabaikan pengaturan skala teks sistem perangkat.

## 8. Langkah Berikutnya (Progres Selanjutnya)

1. Lakukan pengujian lapangan (*field testing*) aplikasi V0.2-Alpha oleh tim MD di lapangan.
2. Evaluasi laporan ekspor `.xlsx` dan sinkronisasi offline-first.
3. Menuju ke fase pengembangan fitur lanjutan berikutnya sesuai kebutuhan operasional OEI MD–WH.