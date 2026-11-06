package id.icantartil.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.icantartil.app.audio.Narrator
import id.icantartil.app.audio.TafsirText

@Composable
fun TafsirScreen(vm: AppVm) {
    if (vm.tafsirChapter == 0) TafsirMenu(vm) else TafsirReader(vm)
}

/** Daftar surat: tafsir yang sudah tersimpan ditandai; ketuk untuk membaca dan mendengarkan. */
@Composable
private fun TafsirMenu(vm: AppVm) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Green).padding(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 16.dp)) {
            Text("Tafsir", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Tafsir per surat dan per ayat, disimpan di perangkat. Singkatan seperti saw dan swt ditulis lengkap.", color = Mist, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            val prog = vm.tafsirProgress
            if (prog != null) {
                Text("Mengunduh tafsir ${prog.first}/${prog.second}… (boleh ditinggal)", color = LightGold, fontWeight = FontWeight.Bold)
            } else {
                GoldButton("Unduh semua tafsir · ${vm.tafsirDone.size}/114 tersimpan", Modifier.fillMaxWidth()) { vm.downloadAllTafsir() }
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Gold))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().background(Color.White).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(vm.chapters, key = { it.id }) { c ->
                val done = c.id in vm.tafsirDone
                ListRow(
                    c.id.toString(), c.name, if (done) "Tafsir tersimpan · ${c.count} ayat" else "Belum diunduh · ketuk untuk memuat",
                    onClick = { vm.openTafsirChapter(c) },
                ) { Text(c.arabic, color = DeepGreen, fontSize = 24.sp) }
            }
        }
    }
}

/** Pembaca: hanya teks tafsir per ayat. Audio otomatis lanjut ke ayat berikutnya setelah satu ayat selesai. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TafsirReader(vm: AppVm) {
    val ns by Narrator.state.collectAsState()
    val listState = rememberLazyListState()
    val items = vm.tafsirItems
    LaunchedEffect(ns.unit) { if (ns.unit in items.indices) listState.animateScrollToItem(ns.unit) }
    LaunchedEffect(vm.tafsirFocus, items) {
        val i = items.indexOfFirst { it.first == vm.tafsirFocus }
        if (i > 0) listState.scrollToItem(i)
    }
    Column(Modifier.fillMaxSize().background(Green)) {
        TopBar("Tafsir ${vm.name(vm.tafsirChapter)}", "Per ayat") { vm.leaveTafsirReader() }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clickable { Narrator.prev() }.semantics { contentDescription = "Ayat sebelumnya" }, contentAlignment = Alignment.Center) { SkipGlyph(LightGold, false) }
                Spacer(Modifier.width(20.dp))
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Gold)
                        .clickable {
                            if (ns.speaking) Narrator.pause()
                            else if (ns.unit >= 0 && ns.idx >= 0) Narrator.resume()
                            else vm.playTafsirFrom(vm.tafsirFocus)
                        }
                        .semantics { contentDescription = if (ns.speaking) "Jeda tafsir" else "Putar tafsir" },
                    contentAlignment = Alignment.Center,
                ) { if (ns.speaking) PauseGlyph(DeepGreen, 28.dp) else PlayGlyph(DeepGreen, 28.dp) }
                Spacer(Modifier.width(20.dp))
                Box(Modifier.size(52.dp).clickable { Narrator.next() }.semantics { contentDescription = "Ayat berikutnya" }, contentAlignment = Alignment.Center) { SkipGlyph(LightGold, true) }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(if (ns.female) "Suara: Wanita" else "Suara: Pria", Modifier.weight(1f)) { Narrator.useFemale(!ns.female) }
                Chip(Narrator.rateLabel(ns.rate), Modifier.weight(1f)) { Narrator.cycleRate() }
                Chip(if (ns.auto) "Lanjut otomatis" else "Satu ayat", Modifier.weight(1f)) { Narrator.toggleAuto() }
            }
            Text("Ketuk kalimat untuk memutar dari kalimat itu", color = Mist, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            ns.error?.let { Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp) }
        }
        if (items.isEmpty()) {
            Text("Tafsir untuk surat ini tidak tersedia.", color = Mist, modifier = Modifier.padding(20.dp))
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            itemsIndexed(items, key = { _, it -> it.first }) { i, (n, text) ->
                val sents = remember(text) { TafsirText.split(text) }
                val current = i == ns.unit
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Shape6).background(if (current) Tint else Color.White).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NumBadge(n.toString(), on = current, size = 32.dp)
                    // Tiap kalimat adalah blok sendiri dengan pemisah; mengetuknya memulai audio dari kalimat itu.
                    Column(Modifier.weight(1f)) {
                        if (sents.isEmpty()) Text("Tidak ada tafsir untuk ayat ini.", color = Ink, fontSize = 15.sp)
                        sents.forEachIndexed { k, s ->
                            val active = current && k == ns.idx
                            val bring = remember { BringIntoViewRequester() }
                            LaunchedEffect(active) { if (active) bring.bringIntoView() }
                            Text(
                                s, color = DeepGreen, fontSize = 15.sp, lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bring).clip(Shape6)
                                    .background(if (active) Gold else Color.Transparent)
                                    .clickable { vm.playTafsirSentence(n, k) }
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                            )
                            if (k < sents.lastIndex) HorizontalDivider(color = Color(0xFFD5DDD8))
                        }
                    }
                }
            }
        }
    }
}
