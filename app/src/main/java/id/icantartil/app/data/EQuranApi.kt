package id.icantartil.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Server menjawab, tapi dengan status error (beda dengan IOException = tidak ada koneksi). */
class HttpException(val code: Int, msg: String = "") : Exception("HTTP $code $msg")

fun JSONObject.str(key: String): String = if (isNull(key)) "" else getString(key)

private val http = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

private suspend fun httpGetJson(url: String): JSONObject = withContext(Dispatchers.IO) {
    val req = Request.Builder().url(url).header("User-Agent", "IcanTartil/0.1 (Android)").build()
    http.newCall(req).execute().use { r ->
        val body = r.body?.string().orEmpty()
        if (!r.isSuccessful) throw HttpException(r.code, body.take(200))
        JSONObject(body)
    }
}

/** Klien untuk EQuran.id API v2 (tanpa API key). Respons berbentuk { code, message, data }. */
object EQuranApi {
    private const val BASE = "https://equran.id/api/v2"
    /** @return seluruh pembungkus respons; ambil isinya lewat getJSONObject/getJSONArray("data"). */
    suspend fun get(path: String): JSONObject = httpGetJson(BASE + path)
}

/** Klien untuk Al Quran Cloud API (tanpa API key). Respons berbentuk { code, status, data }. */
object CloudApi {
    private const val BASE = "https://api.alquran.cloud/v1"
    suspend fun get(path: String): JSONObject = httpGetJson(BASE + path)
}
