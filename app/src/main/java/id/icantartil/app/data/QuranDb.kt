package id.icantartil.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/** Database lokal (SQLite): teks 114 surat, audio per ayat, dan cache tafsir. */
class QuranDb(ctx: Context) : SQLiteOpenHelper(ctx, "quran.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE surat(nomor INTEGER PRIMARY KEY, nama TEXT, latin TEXT, arti TEXT, jumlah INTEGER)")
        db.execSQL("CREATE TABLE ayat(surat INTEGER, nomor INTEGER, arab TEXT, latin TEXT, indo TEXT, audio TEXT, PRIMARY KEY(surat, nomor))")
        db.execSQL("CREATE TABLE tafsir(surat INTEGER PRIMARY KEY, json TEXT, at INTEGER)")
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
        db.execSQL("CREATE TABLE audio_url(reciter TEXT, surat INTEGER, nomor INTEGER, url TEXT, PRIMARY KEY(reciter, surat, nomor))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS audio_url(reciter TEXT, surat INTEGER, nomor INTEGER, url TEXT, PRIMARY KEY(reciter, surat, nomor))")
        }
    }

    private fun scalar(sql: String): Int = readableDatabase.rawQuery(sql, null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun isComplete(): Boolean = scalar("SELECT COUNT(*) FROM surat") == 114 && loadedAyahSurahs().size == 114

    fun loadedAyahSurahs(): Set<Int> {
        val out = HashSet<Int>()
        readableDatabase.rawQuery("SELECT DISTINCT surat FROM ayat", null).use { while (it.moveToNext()) out += it.getInt(0) }
        return out
    }

    fun chapters(): List<Chapter> {
        val out = ArrayList<Chapter>()
        readableDatabase.rawQuery("SELECT nomor,nama,latin,arti,jumlah FROM surat ORDER BY nomor", null).use { c ->
            while (c.moveToNext()) out += Chapter(c.getInt(0), c.getString(2) ?: "", c.getString(1) ?: "", c.getInt(4), c.getString(3) ?: "")
        }
        return out
    }

    fun ayahs(surat: Int, from: Int, to: Int, reciter: String): List<Ayah> {
        val out = ArrayList<Ayah>()
        readableDatabase.rawQuery(
            "SELECT nomor,arab,latin,indo,audio FROM ayat WHERE surat=? AND nomor BETWEEN ? AND ? ORDER BY nomor",
            arrayOf(surat.toString(), from.toString(), to.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out += Ayah(c.getInt(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", pickAudio(c.getString(4) ?: "{}", reciter))
            }
        }
        return out
    }

    /** Ambil URL audio qari terpilih; jika tidak ada, pakai qari pertama yang tersedia. http diubah ke https. */
    private fun pickAudio(json: String, reciter: String): String {
        val o = try { JSONObject(json) } catch (e: Exception) { return "" }
        var u = o.str(reciter)
        if (u.isBlank()) { val k = o.keys(); if (k.hasNext()) u = o.str(k.next()) }
        return if (u.startsWith("http://")) "https://" + u.removePrefix("http://") else u
    }

    fun putSurat(arr: JSONArray) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val v = ContentValues().apply {
                    put("nomor", o.getInt("nomor")); put("nama", o.str("nama")); put("latin", o.str("namaLatin"))
                    put("arti", o.str("arti")); put("jumlah", o.getInt("jumlahAyat"))
                }
                db.insertWithOnConflict("surat", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun putAyatJson(surat: Int, arr: JSONArray) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("ayat", "surat=?", arrayOf(surat.toString()))
            val st = db.compileStatement("INSERT INTO ayat(surat,nomor,arab,latin,indo,audio) VALUES(?,?,?,?,?,?)")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                st.clearBindings()
                st.bindLong(1, surat.toLong()); st.bindLong(2, o.getLong("nomorAyat"))
                st.bindString(3, o.str("teksArab")); st.bindString(4, o.str("teksLatin")); st.bindString(5, o.str("teksIndonesia"))
                st.bindString(6, o.optJSONObject("audio")?.toString() ?: "{}")
                st.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun tafsirJson(surat: Int): String? =
        readableDatabase.rawQuery("SELECT json FROM tafsir WHERE surat=?", arrayOf(surat.toString())).use { if (it.moveToFirst()) it.getString(0) else null }

    fun putTafsir(surat: Int, json: String) {
        val v = ContentValues().apply { put("surat", surat); put("json", json); put("at", System.currentTimeMillis()) }
        writableDatabase.insertWithOnConflict("tafsir", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Jumlah ayat dari semua surat sebelum [surat]; nomor ayat global = globalStart + nomor ayat. */
    fun globalStart(surat: Int): Int =
        readableDatabase.rawQuery("SELECT COALESCE(SUM(jumlah),0) FROM surat WHERE nomor < ?", arrayOf(surat.toString())).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun ayahCount(surat: Int): Int =
        readableDatabase.rawQuery("SELECT jumlah FROM surat WHERE nomor=?", arrayOf(surat.toString())).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun cloudUrls(reciter: String, surat: Int): Map<Int, String> {
        val out = HashMap<Int, String>()
        readableDatabase.rawQuery("SELECT nomor,url FROM audio_url WHERE reciter=? AND surat=?", arrayOf(reciter, surat.toString())).use { c ->
            while (c.moveToNext()) out[c.getInt(0)] = c.getString(1)
        }
        return out
    }

    fun putCloudUrls(reciter: String, surat: Int, urls: Map<Int, String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val st = db.compileStatement("INSERT OR REPLACE INTO audio_url(reciter,surat,nomor,url) VALUES(?,?,?,?)")
            for ((n, u) in urls) {
                st.clearBindings()
                st.bindString(1, reciter); st.bindLong(2, surat.toLong()); st.bindLong(3, n.toLong()); st.bindString(4, u)
                st.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun getMeta(k: String): String? =
        readableDatabase.rawQuery("SELECT v FROM meta WHERE k=?", arrayOf(k)).use { if (it.moveToFirst()) it.getString(0) else null }

    fun setMeta(k: String, v: String) {
        val cv = ContentValues().apply { put("k", k); put("v", v) }
        writableDatabase.insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
