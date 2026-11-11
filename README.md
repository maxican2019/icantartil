# IcanTartil — Murojaah Bersama, Aku Bisa!

Aplikasi Android (Kotlin, Jetpack Compose, Media3, SQLite) untuk murojaah: pilih rentang ayat, putar dengan loop per ayat dan per rentang, riwayat, tafsir, dan cache audio 200 MB.
Sumber data: **EQuran.id API v2** (`https://equran.id/api/v2`), tanpa API key dan tanpa proxy.

## Alur data
| Data | Cara kerja |
|---|---|
| Teks 114 surat (Arab, latin, terjemahan Indonesia) | Disimpan di **SQLite lokal** dan dibaca dari lokal (cepat, offline). Sekali saja diisi: dari **bundel bawaan APK** jika ada, jika tidak diunduh dari server (114 permintaan, dengan progres). Tombol *Perbarui data* di layar Unduhan mengunduh ulang. |
| Audio | Qari dari **EQuran.id** (6) dan **Al Quran Cloud / Islamic Network** (19 qari per ayat). URL audio Al Quran Cloud diambil dari `api.alquran.cloud/v1/surah/{n}/{edisi}` lalu disimpan; jika API gagal dipakai pola CDN `cdn.islamic.network/quran/audio/128/{edisi}/{nomor ayat global}.mp3`. Streaming dengan cache LRU 200 MB. Rentang diunduh dulu sebelum diputar; tombol *Unduh seluruh surat* untuk satu surat penuh. |
| Tafsir | Menu **Tafsir** (tab bawah) berisi daftar surat; ketuk surat untuk memuat tafsirnya (diambil saat dibutuhkan lalu disimpan di database), atau tekan *Unduh semua tafsir* untuk 114 surat sekaligus. Pembaca menampilkan **hanya teks tafsir per ayat**. Audio memakai suara sintetis bawaan Android: putar/jeda, ayat sebelumnya/berikutnya, **otomatis lanjut ke ayat berikutnya** setelah satu ayat selesai (bisa dimatikan), kecepatan, dan pilihan suara **Wanita/Pria**. Kalimat yang sedang dibaca disorot. Dari layar Pemutar, tombol *Tafsir ayat N* membuka tafsir ayat yang sedang dilantunkan. |
| Cadangan jika API mati | **Bundel JSON bawaan APK** (`assets/quran_bundle.json`), dibuat otomatis oleh langkah *Unduh bundel* di GitHub Actions (`tools/fetch_bundle.py`). Berkas ini tidak di-commit ke repo. Tafsir tidak ikut bundel. |

## Publikasi ke Play Store
Lihat **`docs/PUBLISH.md`** (langkah lengkap), `docs/store-listing.md`, `docs/data-safety.md`, kebijakan privasi `docs/index.html`, dan aset toko di `store/`. Workflow **Release AAB** membangun AAB bertanda tangan. Target API 36, AGP 8.13, Gradle 8.14.

## Cara build
1. Push folder ini ke GitHub. Tidak ada secret wajib.
2. Tab **Actions → Build APK**. APK ada di *Artifacts*. Langkah pertama mengunduh bundel dari equran.id; jika gagal, build tetap lanjut dan aplikasi akan mengunduh data saat pertama dibuka.
3. ID AdMob IcanTartil sudah menjadi nilai bawaan di `app/build.gradle.kts` (ID iklan bukan rahasia). Secret `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_REWARDED_ID` di GitHub, jika diisi, menimpa nilai itu. **Jangan klik iklan asli sendiri**; daftarkan HP-mu sebagai perangkat uji di konsol AdMob.
4. Cloudflare Worker dan secret `PROXY_BASE_URL`/`APP_KEY` dari versi sebelumnya **tidak dipakai lagi** dan boleh dihapus.

## Pemutaran di latar belakang
Audio qari diputar lewat `PlaybackService` (MediaSession + layanan latar depan `mediaPlayback`, notifikasi media, wake lock jaringan). Pembacaan tafsir memakai `NarrationService` (notifikasi dengan tombol Berhenti). Android 13+ meminta izin notifikasi sekali saat pemutaran pertama.

## Alur data tafsir
API (`/tafsir/{n}`, 114 surat) → simpan data mentah → **normalisasi teks** (`audio/TafsirText.kt`: saw, swt, r.a., a.s., Q.S., HR, dll menjadi bentuk lengkap) → SQLite (`tafsir_ayat`) → UI membaca dari lokal. Data mentah ikut disimpan sehingga aturan normalisasi bisa diperbarui (naikkan `NORM_VERSION`) tanpa mengunduh ulang. Unduhan otomatis hanya di jaringan tidak berbayar; selain itu lewat tombol *Unduh semua tafsir*. Di pembaca, tiap kalimat dipisah dan bisa diketuk untuk memutar dari kalimat itu.

## Antarmuka
- **Beranda:** ikon jam di samping kolom cari membuka Riwayat. Mengetuk kotak emas masuk ke layar *Sedang diputar* tanpa menjeda audio; tombol di kotak itu memutar/menjeda.
- **Rentang:** nomor ayat dipilih dengan tombol −/+, roda angka (geser, lalu menempel ke angka terdekat), atau ketuk angka untuk mengetik manual.
- **Riwayat:** tombol *Hapus semua* meminta konfirmasi lebih dulu.
- **Qari:** dipilih dari layar Rentang, Riwayat, Unduhan, atau **Pemutar**. Dari Pemutar, qari berganti langsung: rentang dan pengaturan loop tetap, lanjut dari ayat yang sedang diputar. Dari layar lain, pilihan berlaku untuk pemutaran berikutnya.

## Iklan
- Banner hanya di Beranda, Riwayat, dan Unduhan. Tidak pernah di Pemutar.
- **1 iklan → bebas iklan 12 jam. 2 iklan → bebas iklan 24 jam.** Berakhir paling lambat pukul 00.00 (jam lokal), lalu iklan aktif lagi. Logika di `ads/AdFree.kt` dan dites (`AdFreeTest`).
- *Tonton 1 iklan untuk dukung developer (sukarela)* menampilkan satu iklan tanpa imbalan.

## Izin dan lisensi (baca sebelum rilis)
Syarat layanan EQuran.id (equran.id/terms) **tidak menyebut iklan atau penggunaan komersial secara eksplisit**. Yang tertulis:
- API gratis untuk penggunaan wajar dan boleh dipakai untuk pengembangan aplikasi; ada rate limiting.
- **Data yang diperoleh tidak boleh dijual atau diperjualbelikan.**
- Konten boleh dibagikan dengan mencantumkan sumber (atribusi sudah ada di layar Unduhan).
- Terjemahan, tafsir, audio, dan fitur buatan EQuran.id dilindungi hak cipta.

Karena itu, **tanyakan tertulis ke EQuran.id** sebelum rilis publik: (1) apakah aplikasi beriklan boleh memakai API-nya, (2) apakah teks boleh dibundel di dalam APK, (3) apakah mengunduh seluruh 114 surat sekali saja dapat diterima.
Jika jawabannya tidak, hapus langkah *Unduh bundel* di workflow, dan pertimbangkan sumber lain yang izinnya tegas.

### Al Quran Cloud (audio qari tambahan)
Syarat mereka (alquran.cloud/terms-and-conditions, diperbarui 14 Juni 2026): rekaman dilisensikan untuk redistribusi gratis non-komersial dan boleh dialirkan serta diunduh untuk penggunaan personal dan edukasi. Rekaman **boleh dibundel ke produk komersial, tetapi hak cipta tetap pada para qari dan mereka dapat meminta konten dihapus**. Jadi izin untuk aplikasi beriklan bersyarat, bukan jaminan. Atribusi sudah ada di layar Unduhan. Hubungi mereka (islamic.network/contact.html) jika ingin kepastian tertulis.

### Pembaca tafsir
Memakai suara bawaan Android, bukan suara AI awan. **Android tidak memberi tahu jenis kelamin suara.** Pilihan Wanita/Pria memakai suara yang kamu tandai sendiri lewat *Atur suara* (dengarkan dengan *Coba*, lalu tandai). Sebelum ditandai, dipakai tebakan berdasarkan urutan suara plus penyesuaian nada. Suara dengan jenis kelamin pasti tersedia lewat API awan (mis. Google Cloud TTS), tetapi berbayar dan butuh server. Teks tafsir dibacakan langsung di HP pengguna. **Jangan** membuat dan menghosting berkas audio tafsir sendiri tanpa izin EQuran.id, karena tafsirnya dilindungi hak cipta mereka.

## Nama Arab surat
Bidang `nama` dari API menulis sebagian nama tanpa hamza (mis. الاخلاص). Aplikasi memakai tabel sendiri (`data/SuratNames.kt`) dengan ejaan baku (الإخلاص), diterapkan saat membaca sehingga data lama ikut terkoreksi.

## Yang belum diverifikasi
- **Kode belum dikompilasi di lingkungan pembuatnya.** Build pertama di GitHub Actions adalah ujinya.
- Bentuk respons: `surat/{id}` (`nomorAyat`, `teksArab`, `teksLatin`, `teksIndonesia`, `audio{"05":url}`) sesuai dokumentasi. Bidang `nama`, `arti`, `tempatTurun` pada daftar surat, dan `tafsir[].ayat/teks` pada endpoint tafsir, **belum terkonfirmasi** (situs memblokir akses otomatis). Jika kosong atau berbeda, sesuaikan di `data/QuranDb.kt` dan `QuranRepository.parseTafsir`.
- Nama 6 qari EQuran.id (01-06) mengikuti urutan di dokumentasi mereka. Tiga qari Al Quran Cloud (`ar.minshawi`, `ar.minshawimujawwad`, `ar.abdulbasitmurattal`) ditandai bertipe "translation" di API mereka; jika audionya tidak muncul, hapus dari `data/Reciters.kt`.
- URL audio berawalan `http://` diubah otomatis ke `https://`.

## Belum ada
Audio terjemahan (TTS), mode putar ayat lalu tafsir otomatis, font Arab Amiri.
