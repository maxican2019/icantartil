package id.icantartil.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Loop(val perAyah: Int = 3, val perRange: Int = 0, val pauseSec: Int = 0)
data class HistoryItem(val chapter: Int, val from: Int, val to: Int, val loop: Loop, val at: Long)

object Prefs {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) { sp = c.getSharedPreferences("icantartil", Context.MODE_PRIVATE) }

    var syncToken: String?
        get() = sp.getString("sync_token", null)
        set(v) { sp.edit().putString("sync_token", v).apply() }
    var syncFilter: String?
        get() = sp.getString("sync_filter", null)
        set(v) { sp.edit().putString("sync_filter", v).apply() }
    /** ID qari EQuran.id (01-06); 05 = Misyari Rasyid Al-Afasy menurut urutan di dokumentasi. */
    var reciter: String
        get() = sp.getString("reciter", "05") ?: "05"
        set(v) { sp.edit().putString("reciter", v).apply() }
    var lastSyncAt: Long
        get() = sp.getLong("last_sync", 0L)
        set(v) { sp.edit().putLong("last_sync", v).apply() }
    var adFreeUntil: Long
        get() = sp.getLong("adfree_until", 0L)
        set(v) { sp.edit().putLong("adfree_until", v).apply() }

    fun history(): List<HistoryItem> {
        val a = JSONArray(sp.getString("history", "[]"))
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            HistoryItem(o.getInt("c"), o.getInt("f"), o.getInt("t"), Loop(o.getInt("pa"), o.getInt("pr"), o.getInt("ps")), o.getLong("at"))
        }
    }

    fun addHistory(h: HistoryItem) {
        val list = (listOf(h) + history().filterNot { it.chapter == h.chapter && it.from == h.from && it.to == h.to }).take(50)
        val a = JSONArray()
        list.forEach { x ->
            a.put(JSONObject().put("c", x.chapter).put("f", x.from).put("t", x.to)
                .put("pa", x.loop.perAyah).put("pr", x.loop.perRange).put("ps", x.loop.pauseSec).put("at", x.at))
        }
        sp.edit().putString("history", a.toString()).apply()
    }

    fun clearHistory() { sp.edit().remove("history").apply() }
}
