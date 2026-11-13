# OEI MD–WH App: Dokumen Serah Terima (Handoff 004) - Versi Alpha 0.1

> Untuk AI di chat berikutnya: baca seluruh dokumen ini sebelum membalas. Ini rangkuman lengkap dari proyek **OEI MD App** (Versi Alpha 0.1 — Aplikasi Pertama Kali Berjalan Lancar di HP). Semua keputusan di bagian **3, 4, 5, 6, dan 7 (Pedoman UI/UX Final)** **sudah dikunci dan disetujui pengguna — JANGAN diubah tanpa persetujuan eksplisit pengguna.**

---

## 1. Visi dan misi

**Perusahaan:** OEI, bergerak di bidang seni: pembuatan dan penjualan barang turunan seni (Artprints, Greeting Cards, Postcards, Coasters, dst.) yang dijual lewat toko mitra.

**Tiga divisi:** Marketing, Warehouse (WH), Merchandiser (MD).

**Masalah nyata sekarang:**
- MD mengecek stok di toko mitra dengan mengunduh gambar JPG list dari Google Drive, mencoret/mencentang manual di HP (X = habis, angka merah = kurang, √ = terpenuhi), lalu mengunggah ulang ke Drive.
- Tiap toko punya list berbeda, list sering berubah, tidak ada divisi pembuat list, sehingga WH kewalahan dan MD sering memakai list yang sudah tidak relevan.
- MD harus terus mengunduh gambar.
- WH harus membuka JPG laporan MD dan menyimpulkan sendiri kekurangannya.

**Pelajaran penting:** dulu pernah dibuat web-app tetapi **gagal karena hanya menguntungkan WH dan menyulitkan MD di lapangan.**

**Misi aplikasi (APK Android):** jalan tengah yang **intuitif untuk MD di lapangan** dan **tidak menambah beban WH**.
- MD tidak lagi mengunduh gambar, tidak mencoret manual, tidak merekap manual.
- WH tetap bekerja di Google Sheets (tempat yang sudah ia kuasai) dan perubahannya langsung sampai ke MD.
- Laporan MD berupa file terstruktur dengan "Stok Dibutuhkan" yang sudah terhitung.

**Prinsip yang harus dijaga setiap keputusan:** (1) utamakan kemudahan MD di lapangan (satu tangan, cepat, tahan sinyal buruk); (2) WH tidak boleh mendapat pekerjaan tambahan; (3) hindari salah ketik dan data usang; (4) MD tidak boleh kehilangan *muscle memory* dari format list lama.

---

## 2. Cara kerja pengguna (penting untuk gaya kerjamu)

- Bahasa: **Indonesia**.
- **Ini adalah proyek pengembangan aplikasi Android pertama pengguna.** Pengguna mengetik/menyalin kode **manual di Android Studio** berdasarkan panduan dan kode yang diberikan AI. Jadi kode harus lengkap, tepat letak filenya, sebutkan **jenis file (`Class`, `Object`, `Interface`, atau `File`)** saat membuat file baru di Android Studio, dan siap disalin tanpa potongan kode yang dihilangkan.
- **Sertakan saran `Commit Message`** setiap kali memberikan pembaruan/penambahan kode yang krusial.
- **Keahlian UI/UX: 0 (Nol).** Seluruh tampilan layar (UI) dan pengalaman pengguna (UX) mengikuti **Bagian 7 (Pedoman Lengkap UI/UX Final)** yang telah disepakati melalui prototipe interaktif dan sudah diimplementasikan ke kode Compose.
- Kerjakan **bertahap**, jangan memberi seluruh kode sekaligus. Tahap berikutnya hanya setelah pengguna setuju.
- Pengguna **menghemat prompt**: kumpulkan pertanyaan dalam satu giliran (maksimal beberapa poin), berikan hasil yang lengkap dan sudah diuji, jangan bolak-balik.
- **Uji dan verifikasi dulu sebelum mengirim.**
- Jangan mengeksekusi pembuatan file/kode besar sebelum pengguna menyatakan setuju.
- Jangan minta pengguna menempelkan **token** atau URL rahasia ke chat.
- Gaya jawaban yang disukai: terstruktur (tabel/daftar), detail kecil yang bermanfaat disukai, jelaskan dampak keputusan, akui kesalahan dengan lugas.

---

## 3. Tech stack (final)

| Lapisan | Pilihan |
|---|---|
| Aplikasi | Kotlin + Jetpack Compose, MVVM (ViewModel + Compose State) |
| Backend/DB | Google Sheets + Google Apps Script (Web App, **read-only**) |
| Gambar | Google Drive (publik), dirender dengan **Coil** (`coil-compose`), cache disk |
| Cache lokal | **Room** (data jumlah hitungan MD) + `SharedPreferences` (cache JSON `sync` terakhir untuk offline-first) |
| Jaringan | Retrofit + OkHttp + Gson (`ApiResponse.kt`, `ApiService.kt`, `RetrofitClient.kt`) |
| Preferensi | `SharedPreferences` (`SettingPreferences.kt`: Nama MD, URL Web App, Token, Counter ID Laporan, Tema, Last Store ID, Cache JSON, Waktu Sync Terakhir) |
| Export | File **.xlsx** ditulis lokal di HP menggunakan penulis XLSX murni Kotlin (`ZipOutputStream` + XML di `XlsxExporter.kt`) tanpa library berat seperti Apache POI |
| Offline | Offline-first: data tersinkron disimpan di HP, bisa dipakai tanpa sinyal |

Tidak ada penulisan balik ke Sheets, tidak ada WorkManager antrean upload (export murni lokal).

---

## 4. Alur dan perilaku aplikasi (disetujui)

### 4.1 Peran
- **WH**: tidak menyentuh aplikasi. Mengisi semua data di Spreadsheet.
- **MD**: satu-satunya pengguna aplikasi.
- Nama MD, URL Web App, dan Token diambil dari **Pengaturan di aplikasi** (tersimpan di HP dan dapat diubah kapan saja lewat antarmuka).

### 4.2 Alur MD
`Cek Konfigurasi Awal (jika kosong -> Pengaturan) → Cari toko → tombol list toko di atas (list default langsung tampil) → update jumlah → Export / Clear`

1. **Pencarian toko** (kolom search): tidak peka huruf besar-kecil, cocok ke **nama toko** dan **area_cakupan** (ketik "Ubud" → toko ber-nama Ubud + toko di area Ubud). Saat mengetik muncul *suggestion* terdekat tanpa menekan tombol Search. Sebelum mengetik: pintasan toko terakhir dibuka. Urutan abjad "natural" (`Toko 2` sebelum `Toko 10`). Toko tanpa list aktif disembunyikan.
2. **Halaman toko**: semua list toko tampil sebagai **baris tombol (pills) di atas**. List *default* langsung terbuka (list dengan `jenis_produk.urutan_tampil` terkecil, yaitu **MP**). Tombol berisi nama list + progres kecil (`terisi/total`, di mana `terisi` = jumlah item reguler dengan `count > 0` dan `total` = total item reguler pada list tersebut).
3. Pindah ke tombol list lain **tidak menghilangkan** angka yang sudah diisi.
4. **Tab Vertical / Horizontal** hanya muncul untuk list dengan `pisah_orientasi = true` (MP dan SMP). Jenis lain satu grid campur.
5. **Grid 3 kolom**, lalu scroll ke bawah. Tiap kartu: **gambar di atas**, di bawahnya label `ALIAS-KODE` (contoh `MP-012`), target (`T:X`), lencana status (`Habis` / `Kurang X` / `Penuh ✓`), lalu **`− [angka] +` selebar penuh kartu tepat di bawah**. Angka bisa **diketik manual** (papan angka numerik, tidak boleh negatif, dengan tombol pintas *"Isi Sesuai Target"*). Ketuk gambar untuk memperbesar (*Zoom Lightbox*).
6. **Warna kartu**: merah jika jumlah = 0 (`Habis`), kuning jika 0 < jumlah < target (`Kurang X`), hijau jika jumlah ≥ target (`Penuh ✓`). **Semua item mulai dari 0** (tidak ada status "belum dihitung / –"). Nilai 0 yang tidak diubah = stok 0, dijaga lewat dialog konfirmasi saat export.
7. **Urutan item** dikirim server dalam field `urut` (jangan diurutkan ulang secara berbeda):
    - List Standar: **best seller dulu → target terbesar → kode kecil ke besar** (mengikuti urutan list lama di PDF agar MD tidak kehilangan *muscle memory*).
    - List Carousel: mengikuti kolom `posisi` (diatur WH).
8. **Kartu `alias-000`** (contoh `MP-000`, `GC-000`): satu baris tetap di tiap list untuk **jumlah produk di luar list**. Tampil di aplikasi sebagai kartu dengan **gambar kosong** (kotak abu-abu bertitik *"Di luar list"*), label, lencana *"Tanpa Target"*, tombol `−/+`, angka bisa diketik. Dibuat otomatis oleh aplikasi (tidak ada di Sheets).
    - List `pisah_orientasi` (MP/SMP): ada kartu `-000` terpisah di tab Vertical (`000-V`) dan tab Horizontal (`000-H`) (angka terpisah). Saat export dijumlahkan.
    - Jenis lain: satu kartu `-000` per list, diambil apa adanya.
9. **Clear**: dua tingkat, *"Clear list ini"* dan *"Clear semua list toko ini"*. Wajib konfirmasi + opsi **Urungkan** 5 detik.
10. **Data tersimpan di Room** per kombinasi `storeId + listId + kode`, jadi aman saat pindah list, aplikasi ditutup, HP restart. List generic yang dipakai dua toko tetap punya hitungan **terpisah per toko**.
11. **Deteksi perubahan list**: tiap list punya `hash` dari server. Jika hash berubah saat sinkron, perbarui list dan tampilkan info *"List diperbarui oleh WH"*. Hitungan yang sudah diisi **dipertahankan per kode**, target yang berubah ikut diperbarui, item baru mulai dari 0, item yang dihapus WH hilang.

### 4.3 Export (satu-satunya mode, wajib lokal)
- **Satu tombol Export per toko.** Hasil: **satu file .xlsx, satu tab (sheet)**, tersimpan di HP (tombol *Simpan Download* dan *Bagikan File*). **Tidak** ada kirim ke Sheets/Drive. MD mengunggah file ke Drive **secara manual**.
- **Dialog verifikasi** sebelum export: menampilkan nama file otomatis, daftar semua list toko dengan badge status (**Belum dibuka / Semua 0 / Terisi**), kotak centang wajib **"Saya sudah memeriksa semua list"**; tombol *Buat File XLSX* aktif hanya setelah dicentang. Peringatan jika ada list belum dibuka atau semuanya 0.
- **Nama file:** `LP0042-09112026-Budi-Balizen_Sanur.xlsx` = `id_laporan-tanggal(ddMMyyyy)-nama_md-toko`. Tanpa jam dan tanpa nama_list. `id_laporan` = `LP` + nomor urut 4 digit yang tersimpan di HP (dibuat di aplikasi). Spasi dan karakter terlarang diganti `_`.
- **Isi sheet** (urutan dari atas):
  ```
  LAPORAN PENGECEKAN STOK
  Toko         : Balizen Sanur
  Nama MD      : Budi
  Tanggal cek  : 09/11/2026        <- format dd/mm/yyyy, tanpa jam
  ID Laporan   : LP0042
  (baris kosong)
  <Nama list> [· Vertical/Horizontal]      <- judul tabel, di atas header
  Alias_Jenis_Produk-KODE | Jumlah_Target | Jumlah_Dihitung | Stok_Dibutuhkan | Best_Seller
  MP-132 | 3 | 2 | 1 | Ya
  ...
  (4 baris kosong murni tanpa teks label tambahan)
  <tabel list berikutnya, header sendiri>
  ```
    - Urutan list = urutan `list_ids` toko (MP, SMP, lalu lainnya). Urutan baris dalam tabel = urutan tampil di aplikasi.
    - **List MP/SMP dipecah menjadi dua tabel:** **Vertical dulu, lalu Horizontal**. Baris total `MP-000` (jumlah kartu −000 Vertical + Horizontal) hanya ada di **baris paling bawah tabel Vertical**. Tabel Horizontal tanpa baris `-000`. Jika list hanya punya item Horizontal, total `-000` ditaruh di tabel pertama yang ada.
    - Jenis lain: satu tabel, baris `GC-000` di paling bawah.
    - Baris `-000`: `Jumlah_Target` = `-`, `Stok_Dibutuhkan` = `-`, `Best_Seller` = `Tidak`. Dicetak tebal dengan garis atas, tanpa aksen merah.
    - `Stok_Dibutuhkan = max(Target − Dihitung, 0)`.
    - **Aksen merah:** jumlah 0 → seluruh baris latar merah muda + teks merah tebal; 0 < jumlah < target → sel `Stok_Dibutuhkan` merah; terpenuhi → tanpa warna. Format harus terbuka baik di Google Sheets.

---

## 5. Struktur Google Sheets (final, sudah dibuat & terverifikasi)

Tab: `Area`, `Toko`, `Jenis_Produk`, `Gambar`, `List`, `List_Toko`, `List_Item` (+ tab `PETUNJUK` untuk WH). **Kolom bertanda ⚙ adalah rumus, diletakkan paling kanan, abu-abu.**

| Tab | Kolom (kiri → kanan) |
|---|---|
| `Area` | nama_area · aktif |
| `Toko` | id_toko · nama_toko · area_cakupan (dropdown dari Area) · aktif |
| `Jenis_Produk` | jenis_produk · **alias (wajib, unik)** · pisah_orientasi (Ya/Tidak) · urutan_tampil (opsional) · aktif |
| `Gambar` | kode (Plain text, 3 digit, unik) · orientasi (Vertical/Horizontal) · link_drive · aktif · ⚙ file_id |
| `List` | id_list · nama_list (unik) · jenis_produk (dropdown) · tipe (Standar/Carousel) · aktif |
| `List_Toko` | id_toko (dropdown) · id_list (dropdown) · ⚙ nama_toko · ⚙ nama_list |
| `List_Item` | id_list (dropdown) · kode (dropdown dari Gambar) · target · best_seller (Ya/Tidak, default Tidak) · posisi (wajib untuk Carousel) · ⚙ label · ⚙ orientasi · ⚙ cek |

**Aturan data:**
- **Satu gambar = satu kode 3 digit buatan perusahaan** (`012`, `045`, `315`), bukan hasil generate sistem. Satu gambar bisa dipakai di banyak jenis produk (`MP`, `SMP`, `GC`, dst.). Tampil sebagai `alias-kode` (`MP-012`).
- **MP dan SMP wajib list terpisah** (jenis produk berbeda). **Tidak ada list campuran antar jenis produk.** Artprint (MP/SMP) hanya untuk satu jenis display.
- Pemisahan Vertical/Horizontal hanya untuk jenis dengan `pisah_orientasi = Ya` (MP, SMP).
- **List generic:** satu list bisa dipakai banyak toko lewat `List_Toko` (satu baris per toko). Satu toko bisa punya banyak list (generic + khusus). Mengubah list generic langsung berlaku di semua toko pemakainya.
- **Kode ganda dalam satu list tidak boleh.**
- Kolom `aktif`: kosong dianggap Ya. `best_seller` kosong dianggap Tidak.
- `urutan_tampil` hanya diisi yang penting (`MP = 1`, `SMP = 2`); sisanya diurutkan abjad otomatis. List dengan urutan terkecil = list default di toko.
- Setiap kolom yang merujuk tab lain **wajib dropdown** dari tab sumbernya.
- Tidak ada kolom `versi`; perubahan dideteksi lewat `hash` otomatis di server.
- Untuk menghapus data di Sheets: hapus **isi sel**, bukan **hapus baris** (rumus ⚙ ikut hilang).

---

## 6. Backend: Code.gs (sudah jadi, sudah dideploy, sudah diuji pengguna)

Apps Script **read-only** terikat ke spreadsheet. Web App: Execute as *Me*, access *Anyone*. Token di Script Properties `APP_TOKEN`.

**Endpoint (GET):**
- `<WEB_APP_URL>?token=<TOKEN>&action=ping` → `{"ok":true,"schema":1,"time":"..."}`
- `<WEB_APP_URL>?token=<TOKEN>&action=sync` → seluruh data (satu panggilan); tambahkan `&nocache=1` untuk data segar. Cache server 5 menit.
- Galat: `{"ok":false,"error":"unauthorized" | "..."}`.

**Bentuk respons `sync`:**
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
- **URL gambar dibuat di aplikasi** (server hanya mengirim `file_id`): utama `https://drive.google.com/thumbnail?id=<file_id>&sz=w400`, cadangan `https://drive.google.com/uc?export=view&id=<file_id>`. Folder Drive harus dibagikan *"Siapa saja yang memiliki link"*.

---

## 7. Pedoman Lengkap UI/UX Final (DIKUNCI — JANGAN DIUBAH)

Sistem desain ini merupakan hasil perpaduan final yang telah diuji pada prototipe dan disetujui pengguna:
- **Mode Terang (Light Mode) = `Gallery Crisp`**: Latar abu-abu sejuk, bingkai atas foto **murni putih (`#FFFFFF`)** agar warna asli ilustrasi *Artprint* maupun *Greeting Cards* 100% akurat tanpa bias warna, dipadu border & bagian bawah kartu berwarna tegas sesuai status stok.
- **Mode Gelap (Dark Mode) = `Field Pro Dark`**: Latar obsidian gelap dengan pengisian warna kartu penuh kontras tinggi anti-silau untuk pencahayaan toko yang redup maupun terik.
- **Ergonomi Lapangan (`Warm Craft`)**: Lencana status langsung **`Kurang X`** / **`Habis`** / **`Penuh ✓`**, bilah *stepper* **`− [angka] +`** selebar penuh di dasar kartu, serta **dok tombol mengambang berbentuk kapsul (*Floating Pill Dock*)** di bagian bawah layar.

### 7.1 Aturan Larangan Regresi (Wajib Dipatuhi)
1. **TIDAK ADA teks deskripsi penjelasan pada pengaturan Tema:** Di layar Pengaturan, kontrol tema hanya berupa **3 tombol segmented yang intuitif** (`Ikuti Sistem` | `Terang` | `Gelap`), tanpa kotak teks penjelasan di bawahnya.
2. **WAJIB menampilkan 2 kolom input untuk Sinkronisasi Google Sheets di Pengaturan:** Harus tersedia kolom input untuk **URL Web App (Link Script)** dan kolom input untuk **Token Akses** agar pengguna dapat mengganti sumber database sewaktu-waktu langsung dari aplikasi tanpa perlu mengubah kode/rebuild APK.
3. **TIDAK ADA label teks pemisah tabel pada Pratinjau Export:** Pada tampilan pratinjau hasil Export maupun saat membangun file `.xlsx`, jarak antartabel cukup berupa baris kosong bersih tanpa teks keterangan buatan seperti *"4 baris pemisah tabel"*.

### 7.2 Palet Warna Resmi (Dual Theme Tokens)

#### A. Mode Terang — `Gallery Crisp`
| Peran Warna | Kode Hex | Penggunaan |
|---|---|---|
| App Background | `#F4F6F9` | Latar belakang utama layar |
| Elevated Surface | `#FFFFFF` | Top App Bar, permukaan kartu, dialog |
| Subtle Surface | `#F8FAFC` / `#F1F5F9` | Kolom input, tombol ikon netral, kartu `-000` |
| Border / Divider | `#E2E8F0` | Garis batas kartu & pembatas antarbagian |
| Text Primary | `#0F172A` | Judul toko, kode item, angka utama |
| Text Secondary | `#475569` | Label area, target (`T:X`), subjudul |
| Primary Action / Pill | `#0F172A` (teks `#FFFFFF`) | Pill list aktif, tombol Export XLSX |
| **Kartu: Habis (`0`)** | Border `#DC2626` (2dp) | Bingkai foto `#FFFFFF`, area bawah `#FEF2F2`, badge bg `#DC2626` teks `#FFFFFF` (`"Habis"`), tombol `−/+` bg `#FEE2E2` teks `#991B1B`, kotak angka `#FFFFFF` teks `#991B1B` |
| **Kartu: Kurang (`0 < x < T`)** | Border `#D97706` (2dp) | Bingkai foto `#FFFFFF`, area bawah `#FFFBEB`, badge bg `#FEF3C7` teks `#B45309` (`"Kurang X"`), tombol `−/+` bg `#FEF3C7` teks `#92400E`, kotak angka `#FFFFFF` teks `#92400E` |
| **Kartu: Penuh (`x ≥ T`)** | Border `#059669` (2dp) | Bingkai foto `#FFFFFF`, area bawah `#ECFDF5`, badge bg `#D1FAE5` teks `#047857` (`"Penuh ✓"`), tombol `−/+` bg `#D1FAE5` teks `#065F46`, kotak angka `#FFFFFF` teks `#065F46` |

#### B. Mode Gelap — `Field Pro Dark`
| Peran Warna | Kode Hex | Penggunaan |
|---|---|---|
| App Background | `#090D16` | Latar belakang obsidian utama |
| Elevated Surface | `#111827` | Top App Bar, permukaan dialog, dok mengambang |
| Subtle Surface | `#1E293B` / `#0F172A` | Kolom input, pill list non-aktif, kartu `-000` |
| Border / Divider | `#1E293B` / `#334155` | Garis batas elemen |
| Text Primary | `#F8FAFC` | Judul toko, kode item, angka utama |
| Text Secondary | `#94A3B8` | Label area, target (`T:X`), subjudul |
| Primary Action / Pill | `#FBBF24` (teks `#090D16`) | Pill list aktif, tombol Export XLSX |
| **Kartu: Habis (`0`)** | Border `#EF4444` (2dp) | Isi kartu penuh `#271117`, badge bg `#7F1D1D` teks `#FCA5A5` (`"Habis"`), tombol `−/+` bg `#7F1D1D` teks `#FFFFFF`, kotak angka `#180A0E` teks `#FCA5A5` |
| **Kartu: Kurang (`0 < x < T`)** | Border `#F59E0B` (2dp) | Isi kartu penuh `#261A0C`, badge bg `#78350F` teks `#FDE68A` (`"Kurang X"`), tombol `−/+` bg `#78350F` teks `#FDE68A`, kotak angka `#170F07` teks `#FDE68A` |
| **Kartu: Penuh (`x ≥ T`)** | Border `#10B981` (2dp) | Isi kartu penuh `#0C261D`, badge bg `#064E3B` teks `#A7F3D0` (`"Penuh ✓"`), tombol `−/+` bg `#064E3B` teks `#A7F3D0`, kotak angka `#071711` teks `#A7F3D0` |

### 7.3 Tipografi & Proporsi Angka
- **Angka Tabular (`fontFeatureSettings = "tnum"`):** Wajib diterapkan pada kode item (`MP-015`), angka di tengah *stepper*, lencana progres list (`4/10`), serta papan ketik numerik agar angka tidak bergeser secara horizontal saat MD menekan tombol `+`/`−` dengan cepat.
- **Label Kode Item:** Font monospace tebal (`Bold`) dengan tanda hubung (`MP-015`, `GC-172`).

### 7.4 Spesifikasi Komponen & Detail Mikro
1. **Layar Pengaturan (`SettingsScreen`):** Segmented button 3 pilihan (`Ikuti Sistem` | `Terang` | `Gelap`), input `Nama Merchandiser (MD)`, input `URL Web App (Link Script)`, input `Token Akses`, tombol `"Sinkronkan Data Sekarang"` & `"Simpan & Kembali"`.
2. **Layar Pencarian Toko (`StoreSearchScreen`):** Pencarian real-time (`nama_toko` & `area_cakupan`) dengan tombol hapus `×`, kartu pintasan `"Toko Terakhir Dibuka"`, urutan abjad *natural*, *Empty State* ber-border putus-putus.
3. **Layar Utama Toko (`StoreScreen` & `StoreDialogs`):**
    - Rasio bingkai gambar dinamis: `Vertical` (`4:4.6`), `Horizontal` (`4:3`), non-orientasi (`1:1`), kartu `ALIAS-000` (`1:1`).
    - Lencana `★ Best` universal (`#020617` alpha 85% dengan teks emas `#FCD34D`).
    - Full-width tactile stepper `36.dp` (`[ − ] | [ angka ] | [ + ]`), Modal Numeric Keypad dengan perilaku *auto-replace* nilai awal + tombol pintas *"Isi Sesuai Target"*, Zoom Lightbox dengan stepper langsung, Dialog Clear 2 tingkat + Snackbar **Urungkan** 5 detik, dan Floating Pill Dock.
4. **Aturan Layar Awal (*Start Destination*):**
    - Saat aplikasi dibuka (*cold start*): wajib mengecek apakah `webAppUrl` (diawali `http://` atau `https://`) dan `appToken` sudah terisi di `SettingPreferences`. Jika belum terisi, **langsung buka `SettingsScreen`** dan **jangan memanggil endpoint `sync`**.

---

## 8. Status saat ini (Peta Kode Lengkap)

**Seluruh kode aplikasi Android telah selesai ditulis** di dalam proyek `D:\Project Android\OEIMDApp\` (package `com.example.oeimdapp`):

1. **Lapisan Data & Jaringan:**
    - `data/model/ApiResponse.kt` — DTO lengkap respons JSON `sync`.
    - `data/remote/ApiService.kt` — Endpoint dinamis `@Url` untuk `ping` dan `sync`.
    - `data/remote/RetrofitClient.kt` — OkHttp timeout 20–30 detik + `cleanWebAppUrl()`.
    - `data/local/ItemCountEntity.kt` — Tabel Room `item_counts` (`primaryKeys = ["storeId", "listId", "kode"]`).
    - `data/local/ItemCountDao.kt` — Query & Upsert per list / per toko + restore backup Undo.
    - `data/local/AppDatabase.kt` — Singleton Room database `oei_md_database`.
    - `data/repository/OeiRepository.kt` — Jembatan Retrofit & Room.
    - `data/prefs/SettingPreferences.kt` — Penyimpanan `mdName`, `webAppUrl`, `appToken`, `reportCounter`, `themeMode`, `lastStoreId`, `cachedSyncJson`, `lastSyncTime`.
2. **Logika & State:**
    - `viewmodel/MainViewModel.kt` — State layar, theme switcher, offline cache JSON, deteksi perubahan hash list WH, natural sort, Clear + Undo 5 detik, counter laporan.
3. **Antarmuka Pengguna (Jetpack Compose):**
    - `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt` — Dual theme *Gallery Crisp* & *Field Pro Dark*.
    - `ui/screens/SettingsScreen.kt` — Layar Pengaturan.
    - `ui/screens/StoreSearchScreen.kt` — Layar Pencarian Toko.
    - `ui/screens/StoreDialogs.kt` — Modal Numeric Keypad, Zoom Lightbox, Clear Confirm Dialog.
    - `ui/screens/StoreScreen.kt` — Layar Utama Toko, grid 3 kolom, stepper, kartu `-000`, Floating Pill Dock.
4. **Export XLSX & Navigasi Utama:**
    - `export/XlsxExporter.kt` — Generator `.xlsx` murni Kotlin (`ZipOutputStream` + OpenXML) dengan pewarnaan merah otomatis, simpan ke `MediaStore.Downloads`, dan share via `FileProvider`.
    - `ui/screens/ExportDialog.kt` — Dialog verifikasi list, kotak centang wajib, & pratinjau tabel.
    - `MainActivity.kt` — Navigasi antar layar, pengecekan `isConfigured` saat cold start, & `BackHandler`.
    - `res/xml/file_paths.xml` & `AndroidManifest.xml` — Izin `INTERNET` dan konfigurasi `FileProvider`.

---

## 9. Riwayat Penanganan Masalah Teknis (Selesai pada Versi Alpha 0.1 / Handoff 004)

Seluruh kendala kompilasi, crash runtime, dan kegagalan sync Gradle telah **berhasil diselesaikan 100%** hingga aplikasi pertama kali dapat di-run dan berjalan lancar di HP. Ringkasan penanganan:

### 9.1 Migrasi Stack Gradle ke Versi Stabil Industri
- **Masalah:** Versi eksperimental AGP 9.5.1 menyebabkan *Built-in Kotlin* tidak mengkompilasi `MainActivity.kt` ke file `.dex` APK, memicu `ClassNotFoundException: Didn't find class "com.example.oeimdapp.MainActivity"`.
- **Penanganan:** Mengubah stack Gradle di `gradle/libs.versions.toml`, `build.gradle.kts`, dan `app/build.gradle.kts` ke versi stabil standar industri yang teruji:
  - **Android Gradle Plugin (AGP):** `8.7.3`
  - **Kotlin:** `2.0.21`
  - **KSP (Room Generator):** `2.0.21-1.0.28`
  - **Plugin:** `com.android.application`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`, `com.google.devtools.ksp`.

### 9.2 Penyelarasan Target JVM (Java 11)
- **Masalah:** Terjadi bentrok target JVM (`compileDebugJavaWithJavac` 11 vs `kspDebugKotlin` 22).
- **Penanganan:** Menambahkan `kotlinOptions { jvmTarget = "11" }` pada `app/build.gradle.kts` agar kompilasi Java dan Kotlin/KSP berjalan selaras pada JVM 11.

### 9.3 Perbaikan Safe Context Casting pada Theme
- **Masalah:** `(view.context as Activity)` pada `Theme.kt` memicu `ClassCastException` saat `view.context` berupa `ContextThemeWrapper`.
- **Penanganan:** Menambahkan helper `findActivity()` pada `Theme.kt` untuk melakukan *unwrapping* `ContextWrapper` secara aman tanpa crash.

---

## 10. Rencana Langkah Pengujian Selanjutnya (Pengujian Versi Alpha 0.1)

1. **Verifikasi Cold Start & Konfigurasi Awal:** Pastikan aplikasi terbuka mulus di HP langsung ke `SettingsScreen` saat URL & Token masih kosong.
2. **Uji Integrasi End-to-End di HP:**
    - Input Nama MD, URL Web App, dan Token ➔ uji tombol *Sinkronkan Data Sekarang*.
    - Uji pergantian tema (`Ikuti Sistem` / `Terang` / `Gelap`), pencarian toko & area, pengisian stok per orientasi (`Vertical`/`Horizontal`), kartu `-000`, Zoom Lightbox, Numeric Keypad, dan Clear + Urungkan 5 detik.
    - Uji **Export XLSX** (*Simpan Download* dan *Bagikan File*), lalu buka file `.xlsx` hasil generate di Google Sheets untuk memverifikasi tata letak tabel, 4 baris pemisah kosong, dan aksen warna merah.
3. **Persiapan Rilis Beta:** Menyempurnakan hasil pengujian Alpha untuk siap didistribusikan ke tim Merchandiser (MD) di lapangan.