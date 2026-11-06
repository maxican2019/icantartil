# Panduan publikasi IcanTartil ke Google Play

Batas yang perlu Anda tahu: AAB yang ditandatangani dibangun oleh GitHub Actions (workflow **Release AAB**), dan **Anda sendiri** yang membuat keystore, akun Play Console, serta mengunggahnya.

## 1. Akun developer
- Daftar di play.google.com/console (biaya pendaftaran sekali bayar) dan selesaikan verifikasi identitas.
- **Akun pribadi yang dibuat setelah 13 November 2023** wajib menjalankan **closed test dengan minimal 12 tester selama 14 hari berturut-turut** sebelum boleh mengajukan akses produksi. Akun organisasi dikecualikan. Rencanakan sekitar 3 minggu untuk rilis pertama.

## 2. Buat keystore upload (sekali saja) dan isi secret GitHub

**Apa ini?** Keystore adalah satu berkas (`.jks`) yang berfungsi seperti "tanda tangan" Anda. GitHub memakainya untuk menandatangani AAB sebelum Anda unggah ke Play Store. Anda membuatnya **sekali** di komputer sendiri, lalu menyimpannya sebagai secret di GitHub.

### 2A. Pasang Java (jika belum ada)
Perintah `keytool` datang bersama Java (JDK). Cek dulu: buka terminal (Windows: cari **PowerShell**; Mac: aplikasi **Terminal**), ketik `keytool` lalu Enter. Jika muncul daftar bantuan, lewati ke 2B. Jika muncul "tidak dikenali / command not found":
1. Buka **adoptium.net**, unduh **Temurin 17 (JDK)** untuk sistem Anda, lalu pasang (Next sampai selesai).
2. **Tutup terminal lalu buka lagi**, ketik `keytool` lagi untuk memastikan.

### 2B. Buat keystore
1. Tentukan **satu kata sandi** dan catat. Pakai huruf dan angka saja, minimal 12 karakter, tanpa spasi atau simbol (contoh bentuk: `Tartil7Kx92mQpL4`). Jangan pakai contoh ini.
2. Di terminal, pindah ke folder yang mudah diingat: Windows `cd $HOME\Documents`, Mac `cd ~/Documents`.
3. Jalankan **satu baris** ini. Ganti dua kali `KATASANDIKAMU` dengan kata sandi Anda (harus sama), dan `Nama Kamu` dengan nama Anda:
```
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storepass KATASANDIKAMU -keypass KATASANDIKAMU -dname "CN=Nama Kamu, O=IcanTartil, C=ID"
```
4. Berhasil jika muncul tulisan `[Storing upload-keystore.jks]`, dan berkas `upload-keystore.jks` ada di folder Documents.
5. **Cadangkan sekarang:** salin berkas `.jks` dan kata sandinya ke minimal 2 tempat (misalnya pengelola kata sandi dan flashdisk). **Jangan** unggah ke GitHub, dan jangan kirim ke siapa pun, termasuk ke saya.

### 2C. Ubah keystore menjadi teks (base64)
GitHub hanya menyimpan teks, jadi berkas diubah menjadi satu baris teks panjang (sekitar 3.500 karakter).
- **Windows (PowerShell):** hasil langsung masuk ke clipboard.
```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Clipboard
```
- **Mac:**
```
base64 -i upload-keystore.jks | tr -d '\n' | pbcopy
```
- **Linux:** `base64 -w0 upload-keystore.jks` lalu salin hasilnya.

### 2D. Isi 4 secret di GitHub
Buka repo di github.com → **Settings** → di menu kiri **Secrets and variables** → **Actions** → tab **Secrets** → tombol hijau **New repository secret**. Buat empat secret ini (nama harus persis, huruf besar):

| Name | Secret (isi) |
|---|---|
| `KEYSTORE_BASE64` | Tempel isi clipboard dari langkah 2C (satu baris panjang, tanpa spasi tambahan) |
| `KEYSTORE_PASSWORD` | Kata sandi dari langkah 2B |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | Kata sandi yang **sama persis** dengan `KEYSTORE_PASSWORD` |

Setiap kali selesai mengisi, klik **Add secret**. Setelah empat-empatnya ada, daftar secret akan menampilkan keempat namanya (nilainya tidak bisa dilihat lagi, itu normal).

### 2E. Pastikan berhasil
Jalankan workflow **Release AAB** (langkah 3). Jika muncul pesan "Secret KEYSTORE_BASE64 belum diisi", nama secret salah ketik atau belum tersimpan. Jika gagal dengan pesan password atau alias, periksa isi `KEYSTORE_PASSWORD`, `KEY_ALIAS`, dan `KEY_PASSWORD`.

### Penting
- Saat pertama kali mengunggah ke Play Console, pilih **Play App Signing**. Dengan itu Google memegang kunci penandatangan asli, dan jika keystore ini hilang Anda masih bisa meminta reset kunci upload. Tanpa Play App Signing, keystore hilang berarti aplikasi tidak bisa diperbarui lagi.
- Jika ada yang mengetahui kata sandi atau berkas keystore, ganti dengan membuat keystore baru dan minta reset kunci upload di Play Console.

## 3. Bangun AAB
Tab **Actions → Release AAB → Run workflow**, isi nama versi (mis. `1.0.0`). Hasilnya di *Artifacts*: berkas `.aab` (unggah ini ke Play) dan `.apk` rilis (untuk uji di HP). `versionCode` otomatis naik tiap run. Lihat juga hasil langkah "Periksa kesiapan 16 KB".

## 4. Buat aplikasi di Play Console
Bahasa default Indonesia, jenis Aplikasi, Gratis. Isi **Store listing** dari `docs/store-listing.md`, unggah grafis dari folder `store/` dan tangkapan layar dari HP Anda.

## 5. Kebijakan privasi (wajib)
Edit `docs/index.html`: ganti `[NAMA PENGEMBANG]`, `[EMAIL KONTAK]`, `[TANGGAL]`. Aktifkan *GitHub → Settings → Pages → Deploy from a branch → `main` / folder `/docs`*. URL-nya `https://USERNAME.github.io/NAMA-REPO/`; tempel di Play Console.

## 6. App content
Isi semua bagian di *Policy → App content* sesuai `docs/data-safety.md` (Data safety, iklan, ID iklan, target audiens, rating konten).

## 6b. Deklarasi layanan latar depan (wajib karena ada pemutaran di latar belakang)
Di Play Console: *Policy → App content → Foreground service permissions*. Pilih tipe **Media playback** dan jelaskan: "Memutar audio murottal Al-Qur'an dan membacakan tafsir (suara sintetis) saat layar mati atau aplikasi di latar belakang, dengan notifikasi media dan kontrol putar/jeda." Google dapat meminta **video singkat** yang menunjukkan fitur itu; rekam layar HP: putar audio, tekan Home, tunjukkan notifikasi dan kontrolnya.

## 7. Closed test lalu produksi
*Testing → Closed testing → Create track*: unggah AAB, tambahkan 12+ tester (email Google), minta mereka opt-in dan benar-benar memakai aplikasi selama 14 hari berturut-turut. Setelah itu ajukan *production access*, lalu buat rilis produksi. Cek **Pre-launch report** untuk crash.

## 8. Setelah rilis
Di AdMob, hubungkan aplikasi ke listing Play Store. Jangan klik iklan sendiri.

## Sebelum produksi: hal yang masih terbuka
1. **Izin sumber data dan iklan** belum tertulis dari EQuran.id (syaratnya tidak menyebut iklan atau penggunaan komersial) dan Al Quran Cloud (rekaman bisa diminta dihapus oleh qari). Mintalah konfirmasi tertulis dan simpan.
2. **Bundel data di APK:** workflow rilis menyertakan teks dan terjemahan (`quran_bundle.json`). Hapus langkah "Unduh bundel" di `release.yml` jika EQuran.id tidak mengizinkan.
3. **Pemutaran di latar belakang sudah ada** (layanan latar depan jenis `mediaPlayback`), tetapi belum teruji di perangkat nyata. Uji: putar rentang, matikan layar 10 menit, geser aplikasi dari daftar terbaru, dan pastikan audio terus berjalan. Sebagian HP (Xiaomi, Oppo, Vivo, Samsung) membunuh aplikasi di latar belakang; pengguna mungkin perlu mematikan "optimasi baterai" untuk IcanTartil.
4. **Target API 36:** tampilan sudah disesuaikan (edge-to-edge). Uji di HP Android 15/16: bilah status, menu bawah, dan keyboard.
5. **Pustaka native 16 KB:** jika langkah pemeriksa 16 KB melaporkan GAGAL, jangan unggah; kabari saya hasilnya.
6. **Suara tafsir bawaan** (`id-id-x-idc-local`, `id-id-x-ide-local`) hanya ada di HP dengan data suara Google Indonesia; di HP lain dipakai suara pengganti.
