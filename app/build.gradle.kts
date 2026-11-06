plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Nilai dibaca dari -P<prop>, lalu env var, lalu default (ID AdMob IcanTartil; ID bukan rahasia).
fun cfg(prop: String, env: String, default: String): String =
    ((findProperty(prop) as String?) ?: System.getenv(env))?.takeIf { it.isNotBlank() } ?: default

fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "id.icantartil.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "id.icantartil.app" // PERMANEN setelah dirilis ke Play Store
        minSdk = 26
        targetSdk = 36
        // Play mewajibkan versionCode naik di setiap unggahan; workflow rilis mengisinya dari nomor run GitHub.
        versionCode = env("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = env("VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${cfg("admobBannerId", "ADMOB_BANNER_ID", "ca-app-pub-1810348487438906/1093948523")}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${cfg("admobRewardedId", "ADMOB_REWARDED_ID", "ca-app-pub-1810348487438906/3516931882")}\"")
        manifestPlaceholders["admobAppId"] = cfg("admobAppId", "ADMOB_APP_ID", "ca-app-pub-1810348487438906~4418737981")
    }
    signingConfigs {
        create("release") {
            // Diisi dari variabel lingkungan oleh workflow rilis (keystore tidak pernah masuk repo).
            val path = env("KEYSTORE_PATH")
            if (path != null) {
                storeFile = file(path)
                storePassword = env("KEYSTORE_PASSWORD")
                keyAlias = env("KEY_ALIAS")
                keyPassword = env("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            // Minify/R8 dimatikan secara bawaan karena belum teruji. Nyalakan dengan -PminifyRelease=true setelah diuji.
            val minify = (findProperty("minifyRelease") as String?) == "true"
            isMinifyEnabled = minify
            isShrinkResources = minify
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (env("KEYSTORE_PATH") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false; checkReleaseBuilds = false }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs { useLegacyPackaging = false } // pustaka native tidak dikompres: syarat perataan 16 KB
    }
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
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
