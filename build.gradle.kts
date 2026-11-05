plugins {
    // API 36 (wajib untuk Google Play sejak 31 Agustus 2026) butuh AGP 8.9.1 ke atas; AGP 8.13 butuh Gradle 8.13+.
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
}
