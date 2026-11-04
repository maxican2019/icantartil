package id.icantartil.app.data

data class Chapter(val id: Int, val name: String, val arabic: String, val count: Int, val meaning: String)
data class Ayah(val number: Int, val arabic: String, val latin: String, val indo: String, val audioUrl: String)
