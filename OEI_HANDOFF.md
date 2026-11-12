# OEI MD–WH App: Dokumen Serah Terima (Handoff)

> Untuk Claude di chat baru: baca seluruh dokumen ini sebelum membalas. Ini rangkuman lengkap dari chat sebelumnya. Semua keputusan di bagian 3–6 **sudah disetujui pengguna** kecuali yang ditandai **[BELUM DIPUTUSKAN]** atau **[BELUM DIVERIFIKASI]**. Jangan mengulang pertanyaan yang sudah terjawab di sini.

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
- Pengguna **tidak berlangganan Claude Code**. Ia mengetik kode **manual di Android Studio** berdasarkan panduan dan kode yang kamu berikan. Jadi kode harus lengkap, tepat letak filenya, dan bisa disalin.
- Kerjakan **bertahap**, jangan memberi seluruh kode sekaligus. Tahap berikutnya hanya setelah pengguna setuju.
- Pengguna **menghemat prompt**: kumpulkan pertanyaan dalam satu giliran (maksimal beberapa poin), berikan hasil yang lengkap dan sudah diuji, jangan bolak-balik.
- **Uji dan verifikasi dulu sebelum mengirim.** Pengguna menegur ketika ada yang cacat (contoh: dropdown dinamis di xlsx awalnya tidak bekerja; itu kesalahan pengujian).
- Jangan mengeksekusi pembuatan file/kode besar sebelum pengguna menyatakan setuju.
- Jangan minta pengguna menempelkan **token** atau URL rahasia ke chat.
- Gaya jawaban yang disukai: terstruktur (tabel/daftar), detail kecil yang bermanfaat disukai, jelaskan dampak keputusan, akui kesalahan dengan lugas.

---

## 3. Tech stack (final)

| Lapisan | Pilihan |
|---|---|
| Aplikasi | Kotlin + Jetpack Compose, MVVM (ViewModel + StateFlow) |
| Backend/DB | Google Sheets + Google Apps Script (Web App, **read-only**) |
| Gambar | Google Drive (publik), dirender dengan **Coil**, cache disk |
| Cache lokal | **Room** (data list + jumlah hitungan MD) |
| Jaringan | Retrofit + OkHttp (parser JSON bebas: Moshi/kotlinx.serialization) |
| Export | File **.xlsx** ditulis lokal di HP. Usulan: penulis XLSX minimal sendiri (ZipOutputStream + XML) tanpa library berat seperti Apache POI **[usulan, belum dikonfirmasi eksplisit]** |
| Offline | Offline-first: data tersinkron disimpan di HP, bisa dipakai tanpa sinyal |

Tidak ada penulisan balik ke Sheets, tidak ada WorkManager antrean upload (export murni lokal).

---

## 4. Alur dan perilaku aplikasi (disetujui)

### 4.1 Peran
- **WH**: tidak menyentuh aplikasi. Mengisi semua data di Spreadsheet.
- **MD**: satu-satunya pengguna aplikasi.
- Nama MD diambil dari **profil di aplikasi** (diisi sekali, tersimpan di HP).

### 4.2 Alur MD
`Cari toko → tombol list toko di atas (list default langsung tampil) → update jumlah → Export / Clear`

1. **Pencarian toko** (kolom search): tidak peka huruf besar-kecil, cocok ke **nama toko** dan **area_cakupan** (ketik "Ubud" → toko ber-nama Ubud + toko di area Ubud). Saat mengetik muncul *suggestion* terdekat tanpa menekan tombol Search. Tombol Search menampilkan semua hasil urut abjad. Sebelum mengetik: pintasan toko terakhir dibuka. Urutan abjad "natural" (Toko 2 sebelum Toko 10). Toko tanpa list aktif disembunyikan.
2. **Halaman toko**: semua list toko tampil sebagai **baris tombol di atas**. List *default* langsung terbuka (list dengan `jenis_produk.urutan_tampil` terkecil, yaitu **MP**). Tombol berisi nama list + progres kecil (contoh `Rak · 12/40`) **[definisi angka progres belum dirinci; usulan: item yang sudah disentuh / total item; konfirmasi saat implementasi]**.
3. Pindah ke tombol list lain **tidak menghilangkan** angka yang sudah diisi.
4. **Tab Vertical / Horizontal** hanya muncul untuk list dengan `pisah_orientasi = true` (MP dan SMP). Jenis lain satu grid campur.
5. **Grid 3 kolom**, lalu scroll ke bawah. Tiap kartu: **gambar di atas**, di bawahnya label `ALIAS-KODE` (contoh `MP-012`, tanda hubung, bukan titik), target, lalu **`− [angka] +` tepat di bawah gambar**. Angka bisa **diketik manual** (papan angka, tidak boleh negatif). Usulan tambahan: ketuk gambar untuk memperbesar.
6. **Warna kartu**: merah jika jumlah = 0 (habis), kuning jika 0 < jumlah < target, hijau jika jumlah ≥ target. **Semua item mulai dari 0** (tidak ada status "belum dihitung / –"). Keputusan pengguna: 0 yang tidak diubah = stok 0, dijaga lewat dialog konfirmasi saat export.
7. **Urutan item** dikirim server dalam field `urut` (jangan diurutkan ulang secara berbeda):
   - List Standar: **best seller dulu → target terbesar → kode kecil ke besar** (mengikuti urutan list lama di PDF agar MD tidak kehilangan muscle memory).
   - List Carousel: mengikuti kolom `posisi` (diatur WH).
8. **Kartu `alias-000`** (contoh `MP-000`, `GC-000`): satu baris tetap di tiap list untuk **jumlah produk di luar list**. Tampil di aplikasi sebagai kartu dengan **gambar kosong** (kotak abu-abu bertitik), label, tombol −/+, angka bisa diketik. Tidak ada target. Dibuat otomatis oleh aplikasi (tidak ada di Sheets).
   - List `pisah_orientasi` (MP/SMP): ada kartu `-000` terpisah di tab Vertical dan tab Horizontal (angka terpisah). Saat export dijumlahkan.
   - Jenis lain: satu kartu `-000` per list, diambil apa adanya.
9. **Clear**: dua tingkat, "Clear list ini" dan "Clear semua list toko ini". Wajib konfirmasi + opsi **Urungkan** beberapa detik.
10. **Data tersimpan di Room** per kombinasi toko + list + kode gambar, jadi aman saat pindah list, aplikasi ditutup, HP restart. List generic yang dipakai dua toko tetap punya hitungan **terpisah per toko**.
11. **Deteksi perubahan list**: tiap list punya `hash` dari server. Jika hash berubah saat sinkron, perbarui list dan tampilkan info "List diperbarui oleh WH". Hitungan yang sudah diisi **dipertahankan per kode**, target yang berubah ikut diperbarui, item baru mulai dari 0, item yang dihapus WH hilang.

### 4.3 Export (satu-satunya mode, wajib lokal)
- **Satu tombol Export per toko.** Hasil: **satu file .xlsx, satu tab (sheet)**, tersimpan di HP (tombol Simpan ke Download dan Bagikan). **Tidak** ada kirim ke Sheets/Drive. MD mengunggah file ke Drive **secara manual**.
- **Dialog verifikasi** sebelum export: daftar semua list toko dengan status (**Belum dibuka / Semua 0 / Terisi**), kotak centang **"Saya sudah memeriksa semua list"**; tombol Export aktif hanya setelah dicentang. Peringatan jika ada list belum dibuka atau semuanya 0.
- **Nama file:** `LP0042-09112026-Budi-Balizen_Sanur.xlsx` = `id_laporan-tanggal(ddMMyyyy)-nama_md-toko`. Tanpa jam dan tanpa nama_list. `id_laporan` = `LP` + nomor urut yang tersimpan di HP (dibuat di aplikasi). Spasi dan karakter terlarang diganti `_`.
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
  (4 baris kosong)
  <tabel list berikutnya, header sendiri>
  ```
  - Urutan list = urutan `list_ids` toko (MP, SMP, lalu lainnya). Urutan baris dalam tabel = urutan tampil di aplikasi.
  - **List MP/SMP dipecah menjadi dua tabel:** **Vertical dulu, lalu Horizontal**. Baris total `MP-000` (jumlah kartu −000 Vertical + Horizontal) hanya ada di **baris paling bawah tabel Vertical**. Tabel Horizontal tanpa baris `-000`. Jika list hanya punya item Horizontal, total `-000` ditaruh di tabel pertama yang ada.
  - Jenis lain: satu tabel, baris `GC-000` di paling bawah.
  - Baris `-000`: Jumlah_Target = `-`, Stok_Dibutuhkan = `-`, Best_Seller = `Tidak`. Dicetak tebal dengan garis atas, tanpa aksen merah.
  - `Stok_Dibutuhkan = max(Target − Dihitung, 0)`.
  - **Aksen merah:** jumlah 0 → seluruh baris latar merah muda + teks merah tebal; 0 < jumlah < target → sel Stok_Dibutuhkan merah; terpenuhi → tanpa warna. Format harus terbuka baik di Google Sheets.

---

## 5. Struktur Google Sheets (final, sudah dibuat)

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
- **Satu gambar = satu kode 3 digit buatan perusahaan** (012, 045, 315), bukan hasil generate sistem. Satu gambar bisa dipakai di banyak jenis produk (MP, SMP, GC, dst.). Tampil sebagai `alias-kode` (MP-012).
- **MP dan SMP wajib list terpisah** (jenis produk berbeda). **Tidak ada list campuran antar jenis produk.** Artprint (MP/SMP) hanya untuk satu jenis display.
- Pemisahan Vertical/Horizontal hanya untuk jenis dengan `pisah_orientasi = Ya` (MP, SMP).
- **List generic:** satu list bisa dipakai banyak toko lewat `List_Toko` (satu baris per toko). Satu toko bisa punya banyak list (generic + khusus). Mengubah list generic langsung berlaku di semua toko pemakainya.
- **Kode ganda dalam satu list tidak boleh.**
- Kolom `aktif`: kosong dianggap Ya. `best_seller` kosong dianggap Tidak.
- `urutan_tampil` hanya diisi yang penting (MP = 1, SMP = 2); sisanya diurutkan abjad otomatis. List dengan urutan terkecil = list default di toko.
- Setiap kolom yang merujuk tab lain **wajib dropdown** dari tab sumbernya (area_cakupan, jenis_produk di List, id_toko dan id_list di List_Toko, id_list dan kode di List_Item). Pengguna menekankan ini.
- Tidak ada kolom `versi`; perubahan dideteksi lewat `hash` otomatis di server.
- Untuk menghapus data di Sheets: hapus **isi sel**, bukan **hapus baris** (rumus ⚙ ikut hilang).

**Data contoh yang sudah ada (dari PDF/PNG pengguna):** Area Sanur; Toko `BZS` Balizen Sanur; list `BZS-MP` (28 item) dan `BZS-SMP` (32 item) dari PDF "MP/SMP Balizen Sanur"; list generic `GEN-GC` "Greeting Cards Generic" (19 item, `GC-172` target 10, sisanya 5) dihubungkan ke BZS. 39 gambar (21 Vertical, 18 Horizontal). Kolom `link_drive` kosong (belum ada link asli). Dari PDF, angka besar di kiri baris gambar = target, latar hijau = best seller, tulisan di bawah gambar = alias+kode; angka besar di pojok (70/60/100), slot kosong abu-abu, dan teks "(2 each)" diabaikan.

---

## 6. Backend: Code.gs (sudah jadi, sudah dideploy, sudah diuji pengguna menampilkan JSON)

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
- `stores[].list_ids` sudah terurut (urutan_tampil jenis → abjad). `stores` sudah urut abjad; toko tanpa list aktif tidak dikirim. `lists` hanya berisi list yang dipakai toko.
- `items` sudah terurut dan punya `urut` (lihat 4.2 poin 7). `file_id` bisa kosong string jika link belum diisi.
- **URL gambar dibuat di aplikasi** (server hanya mengirim `file_id`): utama `https://drive.google.com/thumbnail?id=<file_id>&sz=w400`, cadangan `https://drive.google.com/uc?export=view&id=<file_id>`. Folder Drive harus dibagikan "Siapa saja yang memiliki link".
- Server membaca kolom berdasarkan **nama header** (bukan urutan), baris item dengan kode tak valid/ganda dilewati dan dilaporkan di `warnings`.

---

## 7. Status saat ini

**Selesai dan diuji:**
- Analisis, arsitektur, struktur Sheets, aturan bisnis (bagian 4–5).
- `database_OEI.xlsx` (sudah diunggah dan dijadikan Google Sheets oleh pengguna) dan `Code.gs` (sudah dideploy). Pengguna **sudah mencoba dan JSON data Sheets berhasil tampil**.
- Pengujian yang dilakukan sebelumnya: rumus xlsx lewat LibreOffice, logika Code.gs lewat simulasi layanan Apps Script (urutan, hash, cache, list generic, Carousel, token).

**Belum dikerjakan: seluruh aplikasi Android.**

**Catatan dan risiko terbuka:**
- **[BELUM DIVERIFIKASI]** Dropdown dinamis lintas tab di Google Sheets (bug awal: rumus validasi ditulis dengan `=` di depan, sudah diperbaiki; di LibreOffice terbaca benar, di Google Sheets belum dikonfirmasi pengguna).
- **[BELUM DIVERIFIKASI]** Proteksi kolom ⚙ belum aktif; WH perlu mengaturnya manual di Google Sheets (langkah ada di tab PETUNJUK).
- **[BELUM DIVERIFIKASI]** Web App Apps Script melakukan redirect ke domain googleusercontent; pastikan Retrofit/OkHttp mengikuti redirect GET (biasanya default) dan uji nyata di aplikasi.
- Thumbnail Drive tidak resmi dan kadang dibatasi saat banyak gambar dimuat sekaligus: perlu fallback dan cache Coil.
- Token tertanam di APK bisa diekstrak; aman karena data read-only dan tidak rahasia.
- Perubahan WH sampai ke aplikasi dalam ≤ 5 menit (cache server).
- Format export (XLSX buatan sendiri) harus **diuji terbuka di Google Sheets** (warna merah, banyak tabel, lebar kolom, gabungan sel judul).
- Definisi persis "progres" pada tombol list masih perlu dirinci.

---

## 8. Rencana langkah berikut (menunggu persetujuan tiap tahap)

1. **Setup proyek Android Studio:** versi Kotlin/Compose/AGP, minSdk, dependensi (Compose, Retrofit/OkHttp + JSON, Room, Coil, navigasi), struktur paket, izin (internet). Tanyakan versi Android Studio dan perangkat uji MD hanya jika diperlukan.
2. **Lapisan data:** model sesuai JSON di bagian 6, klien API (URL + token dari pengaturan, jangan ditanam di chat), Room (entitas hitungan, cache list/toko, profil MD, counter LP), repositori sinkron + deteksi perubahan hash.
3. **Layar profil (nama MD) + pengaturan** (URL Web App dan token).
4. **Layar cari toko** (suggestion, search, toko terakhir).
5. **Layar kerja toko:** tombol list + progres, tab Vertical/Horizontal, grid 3 kolom, kartu hitung (−/+/ketik), warna, kartu `-000`, Clear + Urungkan, persistensi.
6. **Export:** dialog verifikasi, penulis XLSX, simpan ke Download, bagikan; uji terbuka di Google Sheets.
7. **Poles dan rilis:** offline, penanganan galat sinyal, ikon, build APK (debug, lalu signed release) dan uji di perangkat MD.

**Permintaan pertama untuk chat baru:** lanjutkan dari Langkah 1. Tanyakan hanya hal yang benar-benar perlu (mis. versi Android Studio, minSdk), lalu pandu pengguna membuat proyek dan menyalin kode secara bertahap.

---

## 9. Berkas yang dimiliki pengguna

- `database_OEI.xlsx` (sudah jadi Google Sheets di Drive pengguna) dan `Code.gs` (sudah dideploy). Jika kamu perlu memeriksa isinya, minta pengguna melampirkan keduanya atau hasil JSON `sync`-nya (tanpa token).
- Sumber data contoh: PDF "MP_SMP_Balizen_sanur" dan gambar list "Greeting Cards - Small Display".
