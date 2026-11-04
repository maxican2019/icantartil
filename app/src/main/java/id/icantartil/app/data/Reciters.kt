package id.icantartil.app.data

/** source: "eq" = audio dari EQuran.id (kunci 01-06), "ac" = Al Quran Cloud / Islamic Network (identifier edisi audio). */
data class Reciter(val id: String, val name: String, val source: String, val key: String)

object Reciters {
    const val EQ = "eq"
    const val AC = "ac"

    val all: List<Reciter> = listOf(
        // EQuran.id (nama menurut urutan di dokumentasi mereka; belum terkonfirmasi satu per satu)
        Reciter("eq:01", "Abdullah Al-Juhany", EQ, "01"),
        Reciter("eq:02", "Abdul Muhsin Al-Qasim", EQ, "02"),
        Reciter("eq:03", "Abdurrahman As-Sudais", EQ, "03"),
        Reciter("eq:04", "Ibrahim Al-Dossari", EQ, "04"),
        Reciter("eq:05", "Misyari Rasyid Al-Afasy", EQ, "05"),
        Reciter("eq:06", "Yasser Al-Dosari", EQ, "06"),
        // Al Quran Cloud: edisi audio "versebyverse" (per ayat) dari api.alquran.cloud/v1/edition/format/audio
        Reciter("ac:ar.alafasy", "Mishary Alafasy", AC, "ar.alafasy"),
        Reciter("ac:ar.abdurrahmaansudais", "Abdurrahmaan As-Sudais", AC, "ar.abdurrahmaansudais"),
        Reciter("ac:ar.saoodshuraym", "Saood Ash-Shuraym", AC, "ar.saoodshuraym"),
        Reciter("ac:ar.mahermuaiqly", "Maher Al-Muaiqly", AC, "ar.mahermuaiqly"),
        Reciter("ac:ar.hudhaify", "Ali Al-Hudhaify", AC, "ar.hudhaify"),
        Reciter("ac:ar.husary", "Mahmoud Khalil Al-Husary", AC, "ar.husary"),
        Reciter("ac:ar.husarymujawwad", "Al-Husary (Mujawwad)", AC, "ar.husarymujawwad"),
        Reciter("ac:ar.minshawi", "Muhammad Siddiq Al-Minshawi", AC, "ar.minshawi"),
        Reciter("ac:ar.minshawimujawwad", "Al-Minshawi (Mujawwad)", AC, "ar.minshawimujawwad"),
        Reciter("ac:ar.abdulbasitmurattal", "Abdul Basit (Murattal)", AC, "ar.abdulbasitmurattal"),
        Reciter("ac:ar.abdulsamad", "Abdul Basit Abdus-Samad", AC, "ar.abdulsamad"),
        Reciter("ac:ar.ahmedajamy", "Ahmed ibn Ali Al-Ajamy", AC, "ar.ahmedajamy"),
        Reciter("ac:ar.muhammadayyoub", "Muhammad Ayyoub", AC, "ar.muhammadayyoub"),
        Reciter("ac:ar.muhammadjibreel", "Muhammad Jibreel", AC, "ar.muhammadjibreel"),
        Reciter("ac:ar.hanirifai", "Hani Ar-Rifai", AC, "ar.hanirifai"),
        Reciter("ac:ar.shaatree", "Abu Bakr Ash-Shaatree", AC, "ar.shaatree"),
        Reciter("ac:ar.abdullahbasfar", "Abdullah Basfar", AC, "ar.abdullahbasfar"),
        Reciter("ac:ar.ibrahimakhbar", "Ibrahim Al-Akhdar", AC, "ar.ibrahimakhbar"),
        Reciter("ac:ar.aymanswoaid", "Ayman Sowaid", AC, "ar.aymanswoaid"),
    )

    private val default = all.first { it.id == "eq:05" }
    fun byId(id: String): Reciter = all.firstOrNull { it.id == id } ?: default
}
