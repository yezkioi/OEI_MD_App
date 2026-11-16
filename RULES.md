# Aturan Kerja AI Agent — Proyek OEI MD App

1. **Jabaran Pekerjaan, Solusi & Risiko Sebelum Eksekusi (_Plan Before Code_):**
   - Setiap kali pengguna menjabarkan perbaikan atau fitur yang diinginkan, **wajib tuliskan terlebih dahulu jabaran analisis pekerjaan, solusi teknis, dan analisis dampaknya** sebelum menyentuh kode.
   - **JANGAN langsung menulis atau memodifikasi kode** sebelum pengguna memberikan perintah eksplisit untuk eksekusi.

2. **Perlindungan Sistem Stabil & Analisis Potensi Bug (_Stability First & Regression Check_):**
   - **Prioritas Utama:** Keandalan alur inti dan sistem yang sudah stabil (penyimpanan database lokal Room, `SharedPreferences`/cache offline, sinkronisasi & parsing JSON, kalkulasi stok, serta ekspor `.xlsx`) **selalu lebih diprioritaskan** dibandingkan perubahan UI/UX.
   - **Wajib Analisis Potensi Bug:** Setiap kali diminta mengubah kode—terutama pada _state_, fungsi, atau file yang terhubung ke fitur lain—**wajib jelaskan secara objektif apakah perubahan tersebut berdampak pada sistem yang sudah berjalan, potensi _bug_ atau regresi apa yang bisa muncul, serta cara mencegahnya** sebelum pengguna memberi izin eksekusi.
   - **Anti-Regresi Optimasi:** Jangan menonaktifkan, menghapus, atau mengubah mekanisme/optimasi yang sudah terbukti stabil (seperti konfigurasi R8/ProGuard, penyimpanan data, atau alur navigasi) demi mencari jalan pintas menyelesaikan masalah baru.

3. **Pengerjaan Bertahap Satu per Satu (Efisien & Cepat):**
   - Kerjakan perbaikan **satu tahap per giliran** agar pengguna tidak menunggu lama karena pengerjaan sekaligus.
   - Pengecualian hanya berlaku jika beberapa file **saling terkait secara langsung** (contoh: sebuah variabel atau fungsi dipanggil di `File 1`, sementara di `File 2` belum dideklarasikan) sehingga harus diperbarui bersamaan agar tidak memicu error kompilasi.

4. **Fokus pada Kode & Pastikan Bebas Garis Merah Sebelum Delegasi IDE:**
   - Fokuskan tugas AI pada **penulisan dan modifikasi kode**.
   - Pengerjaan operasional di luar kode yang bisa dilakukan pemula—seperti **Sync Gradle**, **Rebuild Project**, atau **Run** aplikasi—serahkan kepada pengguna dengan panduan singkat yang jelas.
   - **Wajib pastikan tidak ada baris kode merah (error kompilasi)** di seluruh kode yang diberikan saat mulai meminta pengguna menjalankan langkah di Android Studio.

5. **Prioritas Versi Stabil Standar Industri:**
   - Jika terjadi masalah atau konflik versi (AGP, Kotlin, KSP, Gradle, maupun library), **selalu utamakan pemakaian versi paling stabil yang saat ini digunakan profesional dunia sebagai standar industri** (hindari versi eksperimental/alpha/canary).

6. **Profil Pemula & Standar Penulisan Kode:**
   - Ini adalah **proyek pengembangan aplikasi Android pertama** pengguna; hindari asumsi bahwa pengguna sudah terbiasa dengan navigasi IDE yang kompleks.
   - Saat membuat file baru, **wajib sebutkan jenis filenya secara eksplisit** (`Class`, `Object`, `Interface`, atau `File`) beserta lokasi folder pastinya.
   - Berikan kode lengkap tanpa potongan yang disingkat, selaras dengan stack proyek (`Kotlin`, `Jetpack Compose`, `MVVM`, `Room`, `Retrofit`, `Coil`).

7. **Saran Git Commit Message Hanya Setelah Konfirmasi Berhasil:**
   - **JANGAN melampirkan saran `Commit Message`** saat baru memberikan kode perbaikan.
   - Lampirkan saran `Commit Message` (_Conventional Commits_) **hanya setelah pengguna mengonfirmasi bahwa perbaikan atau perubahan baru tersebut sudah diuji dan berhasil**.

8. **Bahasa, Komunikasi & Kepatuhan Handoff:**
   - Gunakan **Bahasa Indonesia** yang terstruktur (poin atau tabel), jelaskan dampak keputusan teknis secara singkat, dan akui dengan lugas jika terjadi kesalahan.
   - **JANGAN pernah** meminta pengguna menempelkan `Token Akses` atau `URL Web App` rahasia ke kolom chat.
   - Jika pengguna menandai `@OEI_HANDOFF.md`, gunakan dokumen tersebut sebagai acuan utama dan **patuhi seluruh aturan bisnis, struktur data, serta palet UI/UX yang sudah dikunci**.
