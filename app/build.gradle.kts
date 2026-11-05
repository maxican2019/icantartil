plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Nilai dibaca dari -P<prop>, lalu env var, lalu default (ID AdMob IcanTartil; ID bukan rahasia).
fun cfg(prop: String, env: String, default: String): String =
    ((findProperty(prop) as String?) ?: System.getenv(env))?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "id.icantartil.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "id.icantartil.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${cfg("admobBannerId", "ADMOB_BANNER_ID", "ca-app-pub-1810348487438906/1093948523")}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${cfg("admobRewardedId", "ADMOB_REWARDED_ID", "ca-app-pub-1810348487438906/3516931882")}\"")
        manifestPlaceholders["admobAppId"] = cfg("admobAppId", "ADMOB_APP_ID", "ca-app-pub-1810348487438906~4418737981")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1")
    implementation("androidx.media3:media3-database:1.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
