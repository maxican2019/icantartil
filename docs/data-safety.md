# Jawaban formulir Data safety (Play Console → App content)

Dasar: panduan resmi Google "Google Play's data disclosure requirements" untuk Google Mobile Ads SDK
(developers.google.com/admob/android/play-data-disclosure). **Periksa halaman itu sebelum mengirim**: halamannya mencatat versi SDK terbaru,
sedangkan aplikasi ini memakai `play-services-ads:23.3.0`. Anda bertanggung jawab atas jawaban akhir.

| Pertanyaan | Jawaban |
|---|---|
| Apakah aplikasi mengumpulkan atau membagikan tipe data pengguna yang diwajibkan? | **Ya** (lewat SDK iklan) |
| Apakah semua data dienkripsi saat transit? | **Ya** (semua koneksi HTTPS; cleartext dimatikan) |
| Apakah pengguna bisa meminta data dihapus? | Pengembang tidak menyimpan data pengguna. Untuk data iklan, pengguna dapat mereset/menghapus ID iklan di pengaturan Android |

## Tipe data yang diumumkan (semuanya: dikumpulkan, dibagikan ke Google/AdMob, **opsional**)
| Kategori Play | Tipe data | Tujuan |
|---|---|---|
| Lokasi | **Perkiraan lokasi** (dari alamat IP) | Iklan atau pemasaran; analitik; pencegahan penipuan |
| Aktivitas aplikasi | **Interaksi aplikasi** | Iklan atau pemasaran; analitik; pencegahan penipuan |
| Info dan performa aplikasi | **Diagnostik** | Analitik; pencegahan penipuan |
| ID perangkat atau lainnya | **ID perangkat atau ID lain** (ID iklan, app set ID) | Iklan atau pemasaran; analitik; pencegahan penipuan |

Tidak dikumpulkan: nama, email, nomor telepon, alamat, kontak, foto, file, pesan, lokasi tepat, data kesehatan atau keuangan. Riwayat dan pengaturan hanya disimpan di perangkat dan tidak dikirim ke mana pun.

## Deklarasi lain di App content
- **Berisi iklan:** Ya.
- **ID iklan (Advertising ID):** Ya, dipakai untuk iklan.
- **Kebijakan privasi:** URL halaman `docs/index.html` (GitHub Pages).
- **Akses aplikasi:** semua fungsi tersedia tanpa login.
- **Target audiens:** usulan: **13 tahun ke atas** (jangan pilih kelompok di bawah 13 tahun agar tidak terkena kebijakan Keluarga/Families).
- **Aplikasi berita, pemerintah, keuangan, kesehatan, pelacakan COVID:** Tidak.
- **Rating konten (IARC):** isi kuesioner dengan jujur: konten keagamaan (Al-Qur'an), tanpa kekerasan, tanpa konten buatan pengguna, ada iklan.
