# Dipakai pada build rilis (R8). Aturan pustaka (Compose, Media3, AdMob, OkHttp, coroutines) sudah ikut lewat
# consumer rules masing-masing. Sengaja TIDAK ada "-keep" luas untuk kode sendiri: tidak ada refleksi,
# dan keep luas menurunkan persentase pengaburan yang dinilai Google Play.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Nama berkas/baris tetap ada agar jejak crash di Play Console bisa dibaca setelah dipetakan (mapping.txt ikut di AAB).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
