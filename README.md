# IcanTartil — Murojaah Bersama, Aku Bisa!

Aplikasi Android (Kotlin, Jetpack Compose, Media3) untuk murojaah: pilih rentang ayat, putar dengan loop per ayat dan per rentang, riwayat, serta cache audio 200 MB.
Sumber konten **hanya Quran Foundation (Quran.com)**, karena syarat developernya mengizinkan iklan.

## Isi repo
| Folder | Fungsi |
|---|---|
| `app/` | Aplikasi Android |
| `proxy/` | Proxy Cloudflare Worker. Menyimpan `client_secret` Quran Foundation di server, bukan di APK |
| `.github/workflows/android.yml` | Build APK otomatis di GitHub Actions |

## Langkah cepat
1. **Akses API.** Minta akses Content API di api-docs.quran.foundation untuk mendapatkan `client_id` dan `client_secret` (mulai dari lingkungan `prelive`).
2. **Deploy proxy.**
   ```bash
   cd proxy
   npm i -g wrangler && wrangler login
   wrangler secret put QF_CLIENT_ID
   wrangler secret put QF_CLIENT_SECRET
   wrangler secret put APP_KEY        # kata sandi bebas, dipakai aplikasi sebagai pagar sederhana
   wrangler deploy                    # catat URL-nya
   ```
   Ubah `QF_ENV` di `wrangler.toml` ke `prelive` jika memakai kredensial prelive.
3. **Push ke GitHub**, lalu isi *Settings → Secrets and variables → Actions*:
   | Secret | Isi |
   |---|---|
   | `PROXY_BASE_URL` | URL Worker, tanpa garis miring di akhir |
   | `APP_KEY` | Sama dengan `APP_KEY` di proxy |
   | `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_REWARDED_ID` | Opsional. Jika kosong, dipakai **ID uji Google** |
4. Buka tab **Actions → Build APK**. APK ada di *Artifacts* (`IcanTartil-debug-apk`).
5. Pasang di HP (izinkan sumber tidak dikenal).

Build lokal: buka folder ini di Android Studio, atau `gradle wrapper --gradle-version 8.9` lalu `./gradlew assembleDebug -PproxyBaseUrl=https://...`.

## Iklan
- Banner tampil hanya di Beranda, Riwayat, dan Unduhan. Tidak pernah di Pemutar atau saat ayat dibaca.
- **Tonton 1 iklan → bebas iklan 12 jam. Tonton 2 iklan → bebas iklan 24 jam.**
- Masa bebas iklan **berakhir paling lambat pukul 00.00** (jam lokal perangkat), lalu iklan aktif lagi. Aturan ini berlaku untuk 12 jam maupun 24 jam. Logikanya ada di `ads/AdFree.kt` dan sudah dites (`AdFreeTest`).
- Tombol **Dukung developer (sukarela)** menampilkan satu iklan tanpa imbalan.
- Jika pengguna berhenti setelah iklan pertama pada paket 2 iklan, ia mendapat 12 jam. Masa yang lebih panjang tidak pernah dipersingkat.
- Sebelum rilis: buat akun AdMob, ganti ID uji, dan blokir kategori sensitif di konsol AdMob. Consent UMP (untuk wilayah EEA) belum dipasang.

## Kepatuhan Quran Foundation (alat sinkronisasi)
- **Data surat dan ayat** memakai API biasa dengan cache maksimal 6 hari. File yang lebih tua dari 7 hari dihapus (`data/QfRepository.kt`, objek `Policy`).
- **Audio** dijaga lewat Content Sync (`resources/sync`, grup `recitations:7`) sebagai change-feed (`sync/ContentSync.kt`):
  - berjalan harian lewat WorkManager, dan saat aplikasi dibuka jika sinkron terakhir lebih dari 3 hari;
  - jika ada perubahan atau token tidak valid, cache audio dibersihkan agar diunduh ulang;
  - jika lewat 7 hari dan server menolak (bukan sekadar offline), semua cache dibersihkan;
  - jika offline, salinan lama tetap boleh dipakai.
- **Perlu dikonfirmasi ke Quran Foundation** (tanyakan tertulis): (a) apakah mp3 yang disimpan pemutar termasuk "QF Content" yang dibatasi 1 minggu; (b) apakah boleh tidak mengunduh snapshot bootstrap karena aplikasi tidak menyimpan salinan baris data. Fitur "simpan permanen" sengaja ditunda sampai (a) jelas.
- Baca ulang Developer Terms untuk kewajiban atribusi dan larangan lain sebelum rilis.

## Yang belum diverifikasi
- **Kode belum pernah dikompilasi di lingkungan pembuatnya** (tidak ada akses Android SDK/Google Maven). Yang sudah dites hanya logika `AdFree` (7 pengujian lulus). Build pertama di GitHub Actions adalah uji pertamanya; siapkan kemungkinan perbaikan kecil.
- Bentuk respons API ditulis dari dokumentasi dan belum diuji langsung: `verses/by_chapter` (`audio.url`, `text_uthmani`), `resources/sync` (`sync.mutations`, `has_more`, `next_page_url`, `next_sync_token`).
- Asumsi: ID qari `7` = Alafasy (cek `/resources/recitations`); URL audio relatif dilengkapi dengan `https://verses.quran.foundation/`.
- Masa bebas iklan memakai jam perangkat, sehingga bisa dimanipulasi dengan mengubah jam.

## Belum ada (rencana berikutnya)
Layanan pemutaran di latar belakang dengan notifikasi, pencarian isi ayat, terjemahan dan audio terjemahan (TTS), font Arab Amiri, simpan permanen (menunggu konfirmasi di atas).
