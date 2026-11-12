# OEI MD–WH App: Dokumen Serah Terima (Handoff 003)

> Untuk AI di chat berikutnya: baca seluruh dokumen ini sebelum membalas. Ini rangkuman lengkap dari proyek **OEI MD App**. Semua keputusan di bagian **3, 4, 5, 6, dan 7 (Pedoman UI/UX Final)** **sudah dikunci dan disetujui pengguna — JANGAN diubah tanpa persetujuan eksplisit pengguna.**

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
- **Ini adalah proyek pengembangan aplikasi Android pertama pengguna.** Pengguna mengetik kode **manual di Android Studio** berdasarkan panduan dan kode yang diberikan AI. Jadi kode harus lengkap, tepat letak filenya, sebutkan **jenis file (`Class`, `Object`, `Interface`, atau `File`)** saat membuat file baru di Android Studio, dan siap disalin tanpa potongan kode yang dihilangkan.
- **Keahlian UI/UX: 0 (Nol).** Seluruh tampilan layar (UI) dan pengalaman pengguna (UX) **wajib dibuatkan secara lengkap oleh AI** mengikuti **Bagian 7 (Pedoman Lengkap UI/UX Final)** yang telah disepakati melalui prototipe interaktif.
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
| Aplikasi | Kotlin + Jetpack Compose, MVVM (ViewModel + StateFlow / Compose State) |
| Backend/DB | Google Sheets + Google Apps Script (Web App, **read-only**) |
| Gambar | Google Drive (publik), dirender dengan **Coil**, cache disk |
| Cache lokal | **Room** (data list + jumlah hitungan MD) |
| Jaringan | Retrofit + OkHttp + Gson (`ApiResponse.kt`, `ApiService.kt`, `RetrofitClient.kt`) |
| Preferensi | `SharedPreferences` (`SettingPreferences.kt`: Nama MD, URL Web App, Token, Counter ID Laporan, Tema) |
| Export | File **.xlsx** ditulis lokal di HP. Usulan: penulis XLSX minimal sendiri (`ZipOutputStream` + XML) tanpa library berat seperti Apache POI |
| Offline | Offline-first: data tersinkron disimpan di HP, bisa dipakai tanpa sinyal |

Tidak ada penulisan balik ke Sheets, tidak ada WorkManager antrean upload (export murni lokal).

---

## 4. Alur dan perilaku aplikasi (disetujui)

### 4.1 Peran
- **WH**: tidak menyentuh aplikasi. Mengisi semua data di Spreadsheet.
- **MD**: satu-satunya pengguna aplikasi.
- Nama MD, URL Web App, dan Token diambil dari **Pengaturan di aplikasi** (tersimpan di HP dan dapat diubah kapan saja lewat antarmuka).

### 4.2 Alur MD
`Cari toko → tombol list toko di atas (list default langsung tampil) → update jumlah → Export / Clear`

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
   - List `pisah_orientasi` (MP/SMP): ada kartu `-000` terpisah di tab Vertical dan tab Horizontal (angka terpisah). Saat export dijumlahkan.
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

### 7.1 Aturan Larangan Regresi (Wajib Dipatuhi Saat Implementasi)
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

### 7.4 Spesifikasi Komponen Per Layar

1. **Layar Pengaturan & Profil MD (`SettingsScreen`):**
   - **Tema Tampilan:** Segmented button 3 pilihan sejajar: **`Ikuti Sistem`** (default) | **`Terang`** | **`Gelap`**. Tanpa teks deskripsi tambahan. Nilai disimpan di `SettingPreferences`.
   - **Profil Merchandiser (MD):** Input teks `Nama Merchandiser (MD)`.
   - **Sinkronisasi Google Sheets:**
     - Input teks **URL Web App (Link Script)** (tersimpan di `SettingPreferences.webAppUrl`).
     - Input teks **Token Akses** (tersimpan di `SettingPreferences.appToken`).
     - Tombol aksi penuh **"Sinkronkan Data Sekarang"** (memanggil `syncData` dengan URL & Token yang tersimpan) + tombol **"Simpan & Kembali"**.

2. **Layar Pencarian Toko (`StoreSearchScreen`):**
   - Header dengan judul *"Pilih Toko Mitra"*, nama MD aktif, status sinkronisasi, dan tombol ikon Pengaturan (`⚙️`).
   - Kolom pencarian real-time (mencocokkan `nama_toko` dan `area_cakupan`).
   - Bagian pintasan **"Toko Terakhir Dibuka"** saat kolom pencarian kosong.
   - Daftar seluruh toko aktif urut abjad *natural* dengan badge lokasi area (`📍 Sanur`) dan daftar list aktif.

3. **Layar Utama Toko & Grid Item (`StoreScreen`):**
   - **Top App Bar:** Tombol kembali ke Pencarian Toko, Nama Toko + badge area + indikator status sinkron & nama MD, serta tombol ikon Pengaturan (`⚙️`).
   - **Baris Tombol List (`LazyRow`):** Urut `jenis_urutan`, berisi `nama_list` + lencana progres `terisi/total`.
   - **Tab Orientasi (`Vertical` | `Horizontal`):** Segmented control 2 kolom, hanya tampil jika `pisah_orientasi == true`.
   - **Grid 3 Kolom (`LazyVerticalGrid(GridCells.Fixed(3))`):**
     - Padding bawah grid cukup lega (`~88.dp`) agar baris kartu paling bawah tidak tertutup dok mengambang.
     - **Anatomi Kartu Produk (dari atas ke bawah):**
       1. Bingkai gambar (`#FFFFFF` di Light Mode) + lencana pojok kiri atas `★ Best` jika `best_seller == true`. Ketuk gambar membuka **Dialog Zoom Lightbox** beserta kontrol `−/+`.
       2. Strip metadata: Kode `ALIAS-KODE` di kiri, target `T:X` di kanan.
       3. Lencana status selebar kartu: **`Habis`** / **`Kurang X`** / **`Penuh ✓`**.
       4. **Full-Width Tactile Stepper Bar (`height = 36.dp`)** menempel rata di dasar kartu, dibagi 3 bagian sama lebar: `[ − ]` | `[ angka ]` | `[ + ]`. Ketuk angka membuka **Modal Numeric Keypad** (angka 0–9, `C`, `⌫`, tombol cepat *"Isi Sesuai Target (X)"*, dan `OK`).
     - **Kartu `ALIAS-000`:** Ditempatkan di urutan terakhir grid dengan border putus-putus (`dashed`), kotak abu-abu bertitik *"Di luar list"*, badge *"Tanpa Target"*, dan *stepper* netral.
   - **Floating Pill Dock (Dok Kapsul Mengambang di Bawah Layar):**
     - Melayang dengan margin bawah `12.dp` dan samping `12.dp`, sudut membulat penuh (`CircleShape`), bayangan lembut (*elevation*).
     - Kiri: Tombol kapsul **Clear** → membuka dialog 2 pilihan (*"Clear list ini"* dan *"Clear semua list toko ini"*). Setelah dieksekusi, muncul *Snackbar* mengambang di atas dok dengan tombol **Urungkan** selama 5 detik.
     - Kanan: Tombol kapsul utama **Export XLSX** dilengkapi lencana jumlah list terisi (`X/Y`).

4. **Dialog Verifikasi & Pratinjau Export XLSX:**
   - **Dialog Verifikasi:** Menampilkan nama file otomatis (`LPxxxx-ddMMyyyy-NamaMD-NamaToko.xlsx`), daftar seluruh list toko beserta status (**Belum dibuka / Semua 0 / Terisi**), banner peringatan jika ada list belum dibuka atau semuanya 0, serta kotak centang wajib **"Saya sudah memeriksa semua list"**.
   - **Pratinjau & Output Sheet:** Header laporan (`LAPORAN PENGECEKAN STOK`, Toko, Nama MD, Tanggal cek, ID Laporan), diikuti tabel per list dengan jarak 4 baris kosong bersih antartabel tanpa teks label pemisah tambahan, lalu tombol **Simpan Download** dan **Bagikan File**.

### 7.5 Detail Mikro Interaksi, Proporsi & Edge Cases (Wajib Diterapkan)
1. **Rasio Aspek Bingkai Gambar Dinamis:**
   - Item `Vertical` pada list `pisah_orientasi == true` (MP/SMP): rasio aspek `4 : 4.6` (`aspectRatio(4f / 4.6f)`).
   - Item `Horizontal` pada list `pisah_orientasi == true` (MP/SMP): rasio aspek `4 : 3` (`aspectRatio(4f / 3f)`).
   - Item pada list non-orientasi (`pisah_orientasi == false`, seperti `GC`): rasio kotak `1 : 1` (`aspectRatio(1f)`).
   - Kartu khusus `ALIAS-000`: rasio kotak `1 : 1` (`aspectRatio(1f)`).
2. **Perilaku *Auto-Replace* pada Modal Numeric Keypad:**
   - Saat modal keypad terbuka, nilai lama berada dalam status *pristine* (terpilih penuh). Ketukan digit pertama (`0–9`) langsung **mengganti** nilai lama (bukan menempel di belakangnya), kecuali pengguna menekan `⌫` terlebih dahulu.
   - Batas maksimal input adalah **4 digit** (`0` hingga `9999`), tidak menerima nilai negatif maupun tanda desimal.
3. **State Loading & Error Gambar (Coil):**
   - Saat gambar Google Drive sedang diunduh: tampilkan indikator *loading* halus (*shimmer* atau `CircularProgressIndicator` kecil) di tengah bingkai gambar.
   - Jika gambar gagal dimuat (misalnya saat offline dan belum tersimpan di *disk cache* Coil): tampilkan ikon *broken image* netral + teks kode `ALIAS-KODE` di tengah bingkai agar kartu tetap jelas terbaca.
4. **Detail Isi Dialog Zoom Lightbox:**
   - Header dialog: label kode `ALIAS-KODE` tebal, badge `★ Best Seller` (jika `best_seller == true`), subjudul orientasi (`Vertical` / `Horizontal`), dan tombol tutup `×`.
   - Area gambar utama: bingkai kotak berlatar `#FFFFFF` (Light Mode) atau `#090D16` (Dark Mode) dengan sudut membulat (`16.dp`) yang memuat gambar penuh.
   - Footer dialog: keterangan `Target: X pcs`, teks status berwarna (`Status: Habis` merah / `Kurang X pcs` kuning-oranye / `Penuh ✓` hijau), serta kontrol stepper `− [angka] +` yang langsung memperbarui jumlah item.
5. **Token Warna Spesifik Lencana `★ Best` & Kartu `ALIAS-000`:**
   - Lencana `★ Best` (pojok kiri atas gambar): latar gelap semi-transparan `#020617` (`alpha = 0.85f`) dengan teks emas `#FCD34D` pada **kedua tema** (Light & Dark) agar kontras di atas ilustrasi terang maupun gelap.
   - Kartu `ALIAS-000`: garis tepi putus-putus (*dashed* `2.dp`) berwarna `#CBD5E1` (Light Mode) dan `#334155` (Dark Mode), dengan kotak placeholder *"Di luar list"* berlatar `#F1F5F9` (Light) atau `#1E293B` (Dark).
6. **UX Pencarian Toko (`StoreSearchScreen`):**
   - Kolom pencarian wajib dilengkapi tombol hapus cepat **`×`** di ujung kanan yang muncul saat kolom berisi teks.
   - Jika hasil filter kosong (tidak cocok dengan `nama_toko` maupun `area_cakupan`), tampilkan *Empty State* berupa kotak ber-border putus-putus dengan pesan *"Toko tidak ditemukan"* beserta kata kunci yang diketik.
7. **Aturan Layar Awal (*Start Destination*):**
   - Saat aplikasi dibuka (*cold start*): jika `webAppUrl` atau `appToken` di `SettingPreferences` masih kosong, arahkan langsung ke `SettingsScreen` untuk konfigurasi awal.
   - Jika konfigurasi sudah terisi, buka `StoreSearchScreen` sebagai layar utama (menampilkan kartu pintasan **"Toko Terakhir Dibuka"** di bagian paling atas jika riwayat toko terakhir tersimpan di `SettingPreferences`).

---

## 8. Status saat ini

**Selesai dan diuji:**
- Analisis, arsitektur, struktur Sheets, aturan bisnis (bagian 4–5).
- Google Sheets & Apps Script Web App sudah diverifikasi berjalan lancar dan berhasil menampilkan JSON `sync` tanpa error.
- **Lapisan data aplikasi Android** (`ApiResponse.kt`, `ApiService.kt`, `RetrofitClient.kt`, `ItemCountEntity.kt`, `ItemCountDao.kt`, `AppDatabase.kt`, `OeiRepository.kt`, `SettingPreferences.kt`, dan struktur awal `MainViewModel.kt`) sudah dibuat oleh pengguna di Android Studio.
- **Perancangan & Prototipe UI/UX Hybrid** telah selesai diuji dan dikunci lengkap beserta detail mikronya dalam **Bagian 7 (Pedoman Lengkap UI/UX Final)**.

**Belum dikerjakan:**
- Implementasi kode Jetpack Compose untuk tema (`Theme.kt`, `Color.kt`, `Type.kt`), komponen UI (`StoreSearchScreen.kt`, `StoreScreen.kt`, `SettingsScreen.kt`, dialog/modal), pembaruan `RetrofitClient` agar dinamis membaca URL dari `SettingPreferences`, serta generator file `.xlsx` lokal.

---

## 9. Rencana langkah berikut (menunggu persetujuan tiap tahap)

1. **Setup Proyek Android Studio & Lapisan Data:** (SELESAI)
2. **Perancangan & Persetujuan Prototipe UI/UX Hybrid:** (SELESAI & DIKUNCI di Bagian 7)
3. **Implementasi Kode UI Jetpack Compose & State di Android Studio (Tahap Berikutnya):**
   - Menambahkan pengaturan tema (`themeMode`) di `SettingPreferences.kt`, membuat paket `ui/theme` (`Color.kt`, `Type.kt`, `Theme.kt`) sesuai palet *Gallery Crisp* & *Field Pro Dark*, lalu menyusun layar demi layar (`SettingsScreen`, `StoreSearchScreen`, `StoreScreen`, serta dialog *Keypad*, *Zoom*, *Clear*, dan *Export XLSX*) dengan panduan langkah demi langkah yang ramah pemula.
4. **Generator XLSX Lokal & Rilis:**
   - Penulis file `.xlsx` murni Kotlin (`ZipOutputStream` + XML) dengan pewarnaan merah otomatis dan verifikasi buka di Google Sheets, uji offline di HP MD, serta build APK.
