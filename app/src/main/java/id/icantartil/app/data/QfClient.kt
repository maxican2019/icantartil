package id.icantartil.app.data

import id.icantartil.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Server menjawab, tapi dengan status error (beda dengan IOException = tidak ada koneksi). */
class HttpException(val code: Int, msg: String = "") : Exception("HTTP $code $msg")

fun JSONObject.str(key: String): String = if (isNull(key)) "" else getString(key)

/** Semua panggilan ke konten Quran Foundation lewat proxy (client_secret tidak ada di aplikasi). */
object QfClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Toleran: tambahkan https:// jika terlewat, dan buang garis miring di akhir. */
    private fun baseUrl(): String {
        val u = BuildConfig.PROXY_BASE_URL.trim().trimEnd('/')
        return if (u.startsWith("http://") || u.startsWith("https://")) u else "https://$u"
    }

    suspend fun getJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(baseUrl() + path)
            .header("x-app-key", BuildConfig.APP_KEY)
            .build()
        http.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw HttpException(r.code, body.take(200))
            JSONObject(body)
        }
    }
}
