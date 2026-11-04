package id.icantartil.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.icantartil.app.R
import id.icantartil.app.audio.AudioEngine
import id.icantartil.app.audio.PlayState
import id.icantartil.app.data.Chapter
import id.icantartil.app.data.Reciters
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppRoot(vm: AppVm = viewModel()) {
    val st by AudioEngine.state.collectAsState()
    val ctx = LocalContext.current
    BackHandler(vm.screen != Screen.Home) { vm.screen = Screen.Home }
    LaunchedEffect(vm.toast) { vm.toast?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); vm.toast = null } }
    Surface(Modifier.fillMaxSize(), color = Green, contentColor = Color.White) {
        Box {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (vm.screen) {
                        Screen.Home -> HomeScreen(vm)
                        Screen.Range -> RangeScreen(vm)
                        Screen.Player -> PlayerScreen(vm, st)
                        Screen.History -> HistoryScreen(vm, st)
                        Screen.Storage -> StorageScreen(vm)
                    }
                }
                // Banner hanya di layar non-ibadah, dan hanya jika tidak sedang bebas iklan.
                if (!vm.isAdFree && vm.screen in setOf(Screen.Home, Screen.History, Screen.Storage)) BannerAd()
                if (vm.screen != Screen.Player) {
                    NavBar(vm.screen, st.active && st.playing, { s -> if (s == Screen.Range) { vm.openRange() } else { vm.screen = s } }, vm::center)
                }
            }
            vm.busy?.let { BusyOverlay(it) }
            vm.tafsirView?.let { (title, body) -> TafsirDialog(vm, title, body) }
            if (vm.showReciters) ReciterDialog(vm)
        }
    }
}

/** Samakan variasi ejaan (Furqon/Furqan, Baqoroh/Baqarah, Imron/Imran) agar pencarian tetap cocok. */
private fun fold(s: String): String = s.lowercase()
    .replace(Regex("[^a-z]"), "")
    .replace("kh", "h").replace("sy", "s").replace("sh", "s").replace("dh", "d").replace("th", "t").replace("gh", "g")
    .replace("oo", "u").replace("ee", "i").replace("ou", "u").replace("aa", "a").replace("ii", "i").replace("uu", "u")
    .replace("q", "k").replace("o", "a")

private fun chapterMatches(c: Chapter, q: String): Boolean {
    val t = q.trim()
    if (t.isEmpty() || t == c.id.toString() || c.arabic.contains(t)) return true
    val f = fold(t)
    return f.isNotEmpty() && (fold(c.name).contains(f) || fold(c.meaning).contains(f))
}

@Composable
fun HomeScreen(vm: AppVm) {
    var q by remember { mutableStateOf("") }
    val h = vm.history.firstOrNull()
    val err = vm.error
    val list = vm.chapters.filter { chapterMatches(it, q) }
    // Seluruh Beranda satu daftar yang bisa digulung, supaya header tidak menghabiskan layar.
    LazyColumn(Modifier.fillMaxSize().background(Color.White)) {
        item {
            Column(Modifier.fillMaxWidth().background(Green).padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 18.dp)) {
                Image(
                    painterResource(R.drawable.logo_dark), "IcanTartil — Murojaah Bersama, Aku Bisa!",
                    Modifier.width(290.dp).aspectRatio(524f / 192f).align(Alignment.CenterHorizontally),
                )
                Text(
                    "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", color = LightGold, fontSize = 22.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                )
                TextField(
                    q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, shape = Shape6,
                    placeholder = { Text("Cari surat atau nomor") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedTextColor = DeepGreen, unfocusedTextColor = DeepGreen, cursorColor = DeepGreen,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp).clip(Shape8).background(Gold).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Lanjutkan murojaah", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (h != null) "${vm.name(h.chapter)} · ayat ${h.from}–${h.to}" else "Belum ada riwayat",
                            color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(if (h != null) loopText(h.loop) else "Pilih surat di bawah untuk mulai", color = DeepGreen, fontSize = 12.sp)
                    }
                    if (h != null) PlaySquare(false) { vm.replay(h) }
                }
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(Gold))
        }
        item {
            Text(if (vm.chapters.isEmpty()) "Daftar Surat" else "Daftar Surat · ${list.size} surat", color = Green, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 10.dp))
        }
        when {
            vm.loading -> item {
                Text(
                    vm.progress?.let { "Mengunduh data Al-Qur'an… ${it.first}/${it.second} (hanya sekali, lalu bisa dipakai offline)" } ?: "Memuat…",
                    color = Ink, modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            err != null -> item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(err, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    GoldButton("Coba lagi") { vm.retry() }
                }
            }
            else -> items(list, key = { it.id }) { c ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    ListRow(c.id.toString(), c.name, "${c.count} ayat" + if (c.meaning.isNotBlank()) " · ${c.meaning}" else "", onClick = { vm.openChapter(c) }) {
                        Text(c.arabic, color = DeepGreen, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RangeScreen(vm: AppVm) {
    val c = vm.chapter(vm.chapterId)
    val count = c?.count ?: 1
    Column(Modifier.fillMaxSize().background(Green)) {
        TopBar("Pilih Rentang Ayat") { vm.screen = Screen.Home }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item {
                Column(Modifier.padding(vertical = 8.dp)) {
                    ListRow(
                        vm.chapterId.toString(), c?.name ?: "Surat ${vm.chapterId}", "${c?.count ?: "?"} ayat · ketuk untuk ganti surat",
                        bg = Color.White, onClick = { vm.screen = Screen.Home },
                    ) { Text(c?.arabic.orEmpty(), color = DeepGreen, fontSize = 24.sp) }
                    ReciterRow(vm)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AyahStepper("Dari ayat", vm.from, 1, count, Modifier.weight(1f)) { vm.updateFrom(it) }
                        AyahStepper("Sampai ayat", vm.to, vm.from, count, Modifier.weight(1f)) { vm.updateTo(it) }
                    }
                    Spacer(Modifier.height(10.dp))
                    LoopChips(vm)
                    Spacer(Modifier.height(12.dp))
                    Text("Ayat dalam rentang", color = LightGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(vm.ayahs, key = { it.number }) { a ->
                val inRange = a.number in vm.from..vm.to
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Shape6).background(if (inRange) Tint else Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NumBadge(a.number.toString(), on = inRange, size = 32.dp)
                    Column(Modifier.weight(1f)) {
                        Text(a.arabic, color = DeepGreen, fontSize = 21.sp, lineHeight = 34.sp, modifier = Modifier.fillMaxWidth())
                        if (a.indo.isNotBlank()) Text(a.indo, color = Ink, fontSize = 12.sp)
                    }
                }
            }
        }
        Column(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp)) {
            GoldButton("Putar rentang ${vm.from}–${vm.to}", Modifier.fillMaxWidth()) { vm.playRange() }
            Spacer(Modifier.height(8.dp))
            OutlineButton("Unduh seluruh surat", Modifier.fillMaxWidth()) { vm.downloadChapter() }
        }
    }
}

@Composable
fun PlayerScreen(vm: AppVm, st: PlayState) {
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            pos = AudioEngine.player.currentPosition
            dur = AudioEngine.player.duration.coerceAtLeast(0L)
            delay(300)
        }
    }
    val cur = vm.queue.firstOrNull { it.number == st.ayah }
    Column(Modifier.fillMaxSize().background(Green)) {
        TopBar("${vm.name(st.chapter)} · ayat ${st.from}–${st.to}", "Sedang diputar") { vm.screen = Screen.Home }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(Modifier.fillMaxWidth().clip(Shape8).background(Color.White).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Ayat ${st.ayah} · ulangan ${st.rep} dari ${st.perAyah}", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShapeSmall).background(Gold).padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        Text(
                            cur?.arabic.orEmpty(), color = DeepGreen, fontSize = 34.sp, lineHeight = 56.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        )
                        if (!cur?.indo.isNullOrBlank()) {
                            Text(cur?.indo.orEmpty(), color = Ink, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                        }
                    }
                    st.error?.let { Text(it, color = LightGold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp), color = Gold, trackColor = DeepGreen,
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmtMs(pos), color = Mist, fontSize = 12.sp); Text(fmtMs(dur), color = Mist, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clickable { AudioEngine.skipAyah(-1) }.semantics { contentDescription = "Ayat sebelumnya" }, contentAlignment = Alignment.Center) { SkipGlyph(LightGold, false) }
                        Spacer(Modifier.width(20.dp))
                        Box(
                            Modifier.size(68.dp).clip(RoundedCornerShapeSmall).background(Gold).clickable { AudioEngine.toggle() }
                                .semantics { contentDescription = if (st.playing) "Jeda" else "Putar" },
                            contentAlignment = Alignment.Center,
                        ) { if (st.playing) PauseGlyph(DeepGreen, 30.dp) else PlayGlyph(DeepGreen, 30.dp) }
                        Spacer(Modifier.width(20.dp))
                        Box(Modifier.size(52.dp).clickable { AudioEngine.skipAyah(1) }.semantics { contentDescription = "Ayat berikutnya" }, contentAlignment = Alignment.Center) { SkipGlyph(LightGold, true) }
                    }
                    LoopChips(vm) { vm.restart() }
                    Spacer(Modifier.height(10.dp))
                    OutlineButton("Tafsir ayat ${st.ayah}", Modifier.fillMaxWidth()) { vm.openTafsir(st.chapter, st.ayah) }
                    Spacer(Modifier.height(12.dp))
                    Text("Antrean", color = LightGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            items(vm.queue, key = { it.number }) { a ->
                val on = a.number == st.ayah
                ListRow(a.number.toString(), "Ayat ${a.number}", if (on) "Memutar · ${st.rep}/${st.perAyah}" else "Menunggu", bg = if (on) Tint else Color.White, on = on)
            }
        }
    }
}

private val RoundedCornerShapeSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)

@Composable
fun HistoryScreen(vm: AppVm, st: PlayState) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) ConfirmClearDialog(vm.history.size, onConfirm = { vm.clearHistory(); confirm = false }, onCancel = { confirm = false })
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val groups = vm.history.groupBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
    Column(Modifier.fillMaxSize().background(Green)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 22.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Riwayat", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Rentang yang pernah diputar", color = Mist, fontSize = 13.sp)
            }
            OutlineButton("Hapus semua") { if (vm.history.isNotEmpty()) confirm = true }
        }
        if (vm.history.isEmpty()) Text("Belum ada rentang yang diputar.", color = Mist, modifier = Modifier.padding(20.dp))
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 16.dp)) {
            groups.forEach { (day, list) ->
                item(key = "h$day") {
                    val label = when (day) {
                        today -> "Hari ini"
                        today.minusDays(1) -> "Kemarin"
                        else -> DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id")).format(day)
                    }
                    Text(label, color = LightGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(list, key = { "${it.chapter}:${it.from}:${it.to}" }) { h ->
                    val playing = st.active && st.playing && st.chapter == h.chapter && st.from == h.from && st.to == h.to
                    ListRow(
                        h.chapter.toString(), "${vm.name(h.chapter)} · ayat ${h.from}–${h.to}",
                        (if (playing) "Sedang diputar · " else "${fmtTime(h.at)} · ") + loopText(h.loop),
                        bg = if (playing) Tint else Color.White, on = playing, onClick = { vm.replay(h) },
                    ) { PlaySquare(playing) { if (playing) AudioEngine.toggle() else vm.replay(h) } }
                }
            }
        }
    }
}

@Composable
fun StorageScreen(vm: AppVm) {
    val act = LocalContext.current as Activity
    val used = remember(vm.cacheTick, vm.busy) { AudioEngine.cacheBytes() }
    Column(Modifier.fillMaxSize().background(Green).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Penyimpanan & Iklan", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().clip(Shape8).background(DeepGreen).border(1.dp, Gold, Shape8).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cache audio", color = Mist, fontSize = 13.sp)
                Text("${used / 1_048_576} MB / ${AudioEngine.LIMIT / 1_048_576} MB", color = LightGold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (used.toFloat() / AudioEngine.LIMIT).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp), color = Gold, trackColor = Green,
            )
            Spacer(Modifier.height(8.dp))
            Text("Audio lama yang jarang dipakai dihapus otomatis saat melewati batas.", color = Mist, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text("Data Al-Qur'an", color = Mist, fontSize = 13.sp)
            Text("${vm.chapters.size} surat tersimpan di perangkat, bisa dibaca tanpa internet.", color = LightGold, fontSize = 14.sp)
            Text(
                if (vm.syncedAt == 0L) "Sumber: bundel bawaan aplikasi" else "Diperbarui dari server: ${fmtDateTime(vm.syncedAt)}",
                color = Mist, fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("Perbarui data", Modifier.weight(1f)) { vm.refreshData() }
            OutlineButton("Bersihkan cache", Modifier.weight(1f)) { vm.clearAudio() }
        }
        Spacer(Modifier.height(12.dp))
        ReciterRow(vm)
        Spacer(Modifier.height(12.dp))
        Text("Bebas iklan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            if (vm.isAdFree) "Bebas iklan sampai pukul ${fmtTime(vm.adFreeUntil)}" else "Iklan banner sedang aktif",
            color = LightGold, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        GoldButton("Tonton 1 iklan · bebas iklan 12 jam", Modifier.fillMaxWidth()) { vm.watch(act, 1) }
        Spacer(Modifier.height(8.dp))
        GoldButton("Tonton 2 iklan · bebas iklan 24 jam", Modifier.fillMaxWidth()) { vm.watch(act, 2) }
        Spacer(Modifier.height(8.dp))
        OutlineButton("Dukung developer (sukarela)", Modifier.fillMaxWidth()) { vm.support(act) }
        Spacer(Modifier.height(10.dp))
        Text(
            "Masa bebas iklan berakhir paling lambat pukul 00.00, lalu iklan aktif kembali. " +
                "Menonton iklan bersifat sukarela dan membantu pengembangan aplikasi ini.",
            color = Mist, fontSize = 12.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Sumber data: Kementerian Agama RI, melalui EQuran.id (equran.id). " +
                "Audio murottal: Verse By Verse Quran (everyayah.com), melalui EQuran.id. " +
                "Audio qari tambahan: Al Quran Cloud / Islamic Network (cdn.islamic.network); hak cipta rekaman milik para qari.",
            color = Mist, fontSize = 12.sp,
        )
    }
}
