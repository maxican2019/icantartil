# Panduan publikasi IcanTartil ke Google Play

Batas yang perlu Anda tahu: AAB yang ditandatangani dibangun oleh GitHub Actions (workflow **Release AAB**), dan **Anda sendiri** yang membuat keystore, akun Play Console, serta mengunggahnya.

## 1. Akun developer
- Daftar di play.google.com/console (biaya pendaftaran sekali bayar) dan selesaikan verifikasi identitas.
- **Akun pribadi yang dibuat setelah 13 November 2023** wajib menjalankan **closed test dengan minimal 12 tester selama 14 hari berturut-turut** sebelum boleh mengajukan akses produksi. Akun organisasi dikecualikan. Rencanakan sekitar 3 minggu untuk rilis pertama.

## 2. Buat keystore upload (sekali saja) dan isi secret GitHub
```bash
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 upload-keystore.jks      # Mac: base64 -i upload-keystore.jks | tr -d '\n'
```
- **Simpan `upload-keystore.jks` dan kata sandinya di tempat aman (cadangan di 2 tempat). Jangan di-commit ke repo.**
- Di GitHub: *Settings → Secrets and variables → Actions → New repository secret*: `KEYSTORE_BASE64` (hasil base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`upload`), `KEY_PASSWORD`.
- Saat pertama mengunggah, pilih **Play App Signing** (disarankan). Kunci upload ini bisa direset lewat Play Console jika hilang.

## 3. Bangun AAB
Tab **Actions → Release AAB → Run workflow**, isi nama versi (mis. `1.0.0`). Hasilnya di *Artifacts*: berkas `.aab` (unggah ini ke Play) dan `.apk` rilis (untuk uji di HP). `versionCode` otomatis naik tiap run. Lihat juga hasil langkah "Periksa kesiapan 16 KB".

## 4. Buat aplikasi di Play Console
Bahasa default Indonesia, jenis Aplikasi, Gratis. Isi **Store listing** dari `docs/store-listing.md`, unggah grafis dari folder `store/` dan tangkapan layar dari HP Anda.

## 5. Kebijakan privasi (wajib)
Edit `docs/index.html`: ganti `[NAMA PENGEMBANG]`, `[EMAIL KONTAK]`, `[TANGGAL]`. Aktifkan *GitHub → Settings → Pages → Deploy from a branch → `main` / folder `/docs`*. URL-nya `https://USERNAME.github.io/NAMA-REPO/`; tempel di Play Console.

## 6. App content
Isi semua bagian di *Policy → App content* sesuai `docs/data-safety.md` (Data safety, iklan, ID iklan, target audiens, rating konten).

## 7. Closed test lalu produksi
*Testing → Closed testing → Create track*: unggah AAB, tambahkan 12+ tester (email Google), minta mereka opt-in dan benar-benar memakai aplikasi selama 14 hari berturut-turut. Setelah itu ajukan *production access*, lalu buat rilis produksi. Cek **Pre-launch report** untuk crash.

## 8. Setelah rilis
Di AdMob, hubungkan aplikasi ke listing Play Store. Jangan klik iklan sendiri.

## Sebelum produksi: hal yang masih terbuka
1. **Izin sumber data dan iklan** belum tertulis dari EQuran.id (syaratnya tidak menyebut iklan atau penggunaan komersial) dan Al Quran Cloud (rekaman bisa diminta dihapus oleh qari). Mintalah konfirmasi tertulis dan simpan.
2. **Bundel data di APK:** workflow rilis menyertakan teks dan terjemahan (`quran_bundle.json`). Hapus langkah "Unduh bundel" di `release.yml` jika EQuran.id tidak mengizinkan.
3. **Pemutaran di latar belakang belum ada.** Audio bisa berhenti saat aplikasi ditutup atau layar mati lama. Pengguna murojaah umumnya mengharapkannya.
4. **Target API 36:** tampilan sudah disesuaikan (edge-to-edge). Uji di HP Android 15/16: bilah status, menu bawah, dan keyboard.
5. **Pustaka native 16 KB:** jika langkah pemeriksa 16 KB melaporkan GAGAL, jangan unggah; kabari saya hasilnya.
6. **Suara tafsir bawaan** (`id-id-x-idc-local`, `id-id-x-ide-local`) hanya ada di HP dengan data suara Google Indonesia; di HP lain dipakai suara pengganti.
