# IcanTartil — Murojaah Bersama, Aku Bisa!

Aplikasi Android (Kotlin, Jetpack Compose, Media3, SQLite) untuk murojaah: pilih rentang ayat, putar dengan loop per ayat dan per rentang, riwayat, tafsir, dan cache audio 200 MB.
Sumber data: **EQuran.id API v2** (`https://equran.id/api/v2`), tanpa API key dan tanpa proxy.

## Alur data
| Data | Cara kerja |
|---|---|
| Teks 114 surat (Arab, latin, terjemahan Indonesia) | Disimpan di **SQLite lokal** dan dibaca dari lokal (cepat, offline). Sekali saja diisi: dari **bundel bawaan APK** jika ada, jika tidak diunduh dari server (114 permintaan, dengan progres). Tombol *Perbarui data* di layar Unduhan mengunduh ulang. |
| Audio | Streaming dari URL yang diberikan API, dengan cache LRU 200 MB. Rentang diunduh dulu sebelum diputar; tombol *Unduh seluruh surat* untuk satu surat penuh. |
| Tafsir | Diambil **saat dibutuhkan** (tombol *Tafsir ayat N* di Pemutar), lalu disimpan di database. |
| Cadangan jika API mati | **Bundel JSON bawaan APK** (`assets/quran_bundle.json`), dibuat otomatis oleh langkah *Unduh bundel* di GitHub Actions (`tools/fetch_bundle.py`). Berkas ini tidak di-commit ke repo. Tafsir tidak ikut bundel. |

## Cara build
1. Push folder ini ke GitHub. Tidak ada secret wajib.
2. Tab **Actions → Build APK**. APK ada di *Artifacts*. Langkah pertama mengunduh bundel dari equran.id; jika gagal, build tetap lanjut dan aplikasi akan mengunduh data saat pertama dibuka.
3. Opsional: secret `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_REWARDED_ID`. Jika kosong, dipakai ID uji Google.
4. Cloudflare Worker dan secret `PROXY_BASE_URL`/`APP_KEY` dari versi sebelumnya **tidak dipakai lagi** dan boleh dihapus.

## Iklan
- Banner hanya di Beranda, Riwayat, dan Unduhan. Tidak pernah di Pemutar.
- **1 iklan → bebas iklan 12 jam. 2 iklan → bebas iklan 24 jam.** Berakhir paling lambat pukul 00.00 (jam lokal), lalu iklan aktif lagi. Logika di `ads/AdFree.kt` dan dites (`AdFreeTest`).
- *Dukung developer (sukarela)* menampilkan satu iklan tanpa imbalan.

## Izin dan lisensi (baca sebelum rilis)
Syarat layanan EQuran.id (equran.id/terms) **tidak menyebut iklan atau penggunaan komersial secara eksplisit**. Yang tertulis:
- API gratis untuk penggunaan wajar dan boleh dipakai untuk pengembangan aplikasi; ada rate limiting.
- **Data yang diperoleh tidak boleh dijual atau diperjualbelikan.**
- Konten boleh dibagikan dengan mencantumkan sumber (atribusi sudah ada di layar Unduhan).
- Terjemahan, tafsir, audio, dan fitur buatan EQuran.id dilindungi hak cipta.

Karena itu, **tanyakan tertulis ke EQuran.id** sebelum rilis publik: (1) apakah aplikasi beriklan boleh memakai API-nya, (2) apakah teks boleh dibundel di dalam APK, (3) apakah mengunduh seluruh 114 surat sekali saja dapat diterima.
Jika jawabannya tidak, hapus langkah *Unduh bundel* di workflow, dan pertimbangkan sumber lain yang izinnya tegas.

## Yang belum diverifikasi
- **Kode belum dikompilasi di lingkungan pembuatnya.** Build pertama di GitHub Actions adalah ujinya.
- Bentuk respons: `surat/{id}` (`nomorAyat`, `teksArab`, `teksLatin`, `teksIndonesia`, `audio{"05":url}`) sesuai dokumentasi. Bidang `nama`, `arti`, `tempatTurun` pada daftar surat, dan `tafsir[].ayat/teks` pada endpoint tafsir, **belum terkonfirmasi** (situs memblokir akses otomatis). Jika kosong atau berbeda, sesuaikan di `data/QuranDb.kt` dan `QuranRepository.parseTafsir`.
- ID qari `05` = Misyari Rasyid Al-Afasy berdasarkan urutan di dokumentasi. Pilihan qari belum ada di antarmuka.
- URL audio berawalan `http://` diubah otomatis ke `https://`.

## Belum ada
Pemutaran di latar belakang dengan notifikasi, pilihan qari, audio terjemahan (TTS), font Arab Amiri.
