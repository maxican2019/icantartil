package id.icantartil.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.icantartil.app.audio.Narrator
import id.icantartil.app.audio.TafsirText
import id.icantartil.app.data.Reciter
import id.icantartil.app.data.Reciters
import kotlinx.coroutines.launch

private val Warn = Color(0xFFFF8A80)

// ---------------------------------------------------------------- Roda angka

/** Roda angka: geser ke atas/bawah (menggulung seperti roda, lalu menempel ke angka terdekat) atau ketuk angka. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheel(
    value: Int, min: Int, max: Int, onChange: (Int) -> Unit, onCenterClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val rowH = 40.dp
    val rowPx = with(LocalDensity.current) { rowH.toPx() }
    val n = (max - min + 1).coerceAtLeast(1)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - min).coerceIn(0, n - 1))
    val fling = rememberSnapFlingBehavior(state)
    val scope = rememberCoroutineScope()
    val change by rememberUpdatedState(onChange)
    val center by remember(n) {
        derivedStateOf { (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0).coerceIn(0, n - 1) }
    }
    // Laporkan nilai saat roda berhenti berputar.
    LaunchedEffect(state, min) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling -> if (!scrolling) change(min + center) }
    }
    // Ikuti perubahan dari luar (tombol +/-, ketik manual): roda berputar ke angka baru.
    LaunchedEffect(value, min, n) {
        if (min + center != value && !state.isScrollInProgress) state.animateScrollToItem((value - min).coerceIn(0, n - 1))
    }
    LazyColumn(state = state, flingBehavior = fling, contentPadding = PaddingValues(vertical = rowH), modifier = modifier.height(rowH * 3)) {
        items(n) { i ->
            val d = kotlin.math.abs(i - center)
            Box(
                Modifier.fillMaxWidth().height(rowH).clickable { if (d == 0) onCenterClick() else scope.launch { state.animateScrollToItem(i) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (min + i).toString(),
                    color = when (d) { 0 -> LightGold; 1 -> Mist; else -> Mist.copy(alpha = 0.5f) },
                    fontSize = if (d == 0) 28.sp else 18.sp,
                    fontWeight = if (d == 0) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun RoundBtn(text: String, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Text(text, color = LightGold, fontSize = 26.sp)
    }
}

/** Pemilih nomor ayat: tombol −/+ (seperti sebelumnya), roda angka, dan ketik manual (ketuk angka tengah). */
@Composable
fun AyahStepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    var typing by remember { mutableStateOf(false) }
    Column(
        modifier.clip(Shape6).background(DeepGreen).border(1.dp, Gold, Shape6).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Mist, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn("−", "Kurangi $label") { onChange((value - 1).coerceIn(min, max)) }
            NumberWheel(value, min, max, onChange, { typing = true }, Modifier.weight(1f))
            RoundBtn("+", "Tambah $label") { onChange((value + 1).coerceIn(min, max)) }
        }
        Text("geser, atau ketuk angka", color = Mist, fontSize = 12.sp)
    }
    if (typing) NumberDialog(label, value, min, max, { typing = false }) { onChange(it); typing = false }
}

@Composable
fun NumberDialog(title: String, value: Int, min: Int, max: Int, onDismiss: () -> Unit, onOk: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    val n = text.toIntOrNull()
    val valid = n != null && n in min..max
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                text, { text = it.filter(Char::isDigit).take(3) }, singleLine = true, isError = !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Isi angka $min sampai $max", color = Mist) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold, unfocusedBorderColor = Mist, cursorColor = Gold,
                ),
            )
        },
        confirmButton = { TextButton({ if (n != null && n in min..max) onOk(n) }, enabled = valid) { Text("OK", color = LightGold) } },
        dismissButton = { TextButton(onDismiss) { Text("Batal", color = Mist) } },
        containerColor = DeepGreen, titleContentColor = LightGold, textContentColor = Color.White,
    )
}

// ---------------------------------------------------------------- Konfirmasi hapus riwayat

@Composable
fun ConfirmClearDialog(count: Int, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Hapus semua riwayat?") },
        text = { Text("$count rentang yang pernah diputar akan dihapus. Tindakan ini tidak bisa dibatalkan.") },
        confirmButton = { TextButton(onConfirm) { Text("Hapus semua", color = Warn, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onCancel) { Text("Batal", color = LightGold) } },
        containerColor = DeepGreen, titleContentColor = LightGold, textContentColor = Color.White,
    )
}

// ---------------------------------------------------------------- Qari

@Composable
fun ReciterRow(vm: AppVm) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Qari", color = Mist, fontSize = 12.sp)
            Text(Reciters.byId(vm.reciter).name, color = LightGold, fontWeight = FontWeight.Bold)
        }
        OutlineButton("Ganti qari") { vm.showReciters = true }
    }
}

@Composable
private fun ReciterItem(r: Reciter, vm: AppVm) {
    val sel = r.id == vm.reciter
    Row(Modifier.fillMaxWidth().clickable { vm.setReciter(r.id) }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(sel, { vm.setReciter(r.id) }, colors = RadioButtonDefaults.colors(selectedColor = Gold, unselectedColor = Mist))
        Text(r.name, color = if (sel) LightGold else Color.White, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ReciterDialog(vm: AppVm) {
    AlertDialog(
        onDismissRequest = { vm.showReciters = false },
        confirmButton = { TextButton({ vm.showReciters = false }) { Text("Tutup", color = LightGold) } },
        title = { Text("Pilih qari", fontSize = 17.sp) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                item { Text("EQuran.id", color = Mist, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                items(Reciters.all.filter { it.source == Reciters.EQ }, key = { it.id }) { ReciterItem(it, vm) }
                item { Text("Al Quran Cloud (Islamic Network)", color = Mist, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                items(Reciters.all.filter { it.source == Reciters.AC }, key = { it.id }) { ReciterItem(it, vm) }
            }
        },
        containerColor = DeepGreen, titleContentColor = LightGold, textContentColor = Color.White,
    )
}

// ---------------------------------------------------------------- Tafsir + pembaca

@Composable
fun TafsirDialog(vm: AppVm, title: String, body: String) {
    val ns by Narrator.state.collectAsState()
    val sentences = remember(body) { TafsirText.split(body) }
    val starts = remember(sentences) { var p = 0; sentences.map { s -> val st = p; p += s.length + 1; st } }
    val scroll = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotated = remember(sentences, ns.idx) {
        buildAnnotatedString {
            sentences.forEachIndexed { i, s ->
                if (i == ns.idx) withStyle(SpanStyle(background = Gold, color = DeepGreen)) { append(s) } else append(s)
                append(" ")
            }
        }
    }
    // Sorot kalimat yang sedang dibaca dan gulung teks mengikutinya.
    LaunchedEffect(ns.idx) {
        val l = layout
        if (l != null && ns.idx in starts.indices) scroll.animateScrollTo(l.getLineTop(l.getLineForOffset(starts[ns.idx])).toInt())
    }
    DisposableEffect(Unit) { onDispose { Narrator.stop() } } // menutup dialog menghentikan bacaan
    AlertDialog(
        onDismissRequest = { vm.closeTafsir() },
        confirmButton = { TextButton({ vm.closeTafsir() }) { Text("Tutup", color = LightGold) } },
        title = { Text(title, fontSize = 17.sp) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(if (ns.speaking) "Jeda" else if (ns.idx >= 0) "Lanjut" else "Putar", Modifier.weight(1f)) {
                        if (ns.speaking) Narrator.pause() else if (ns.idx >= 0) Narrator.resume() else Narrator.play(sentences)
                    }
                    Chip("Berhenti", Modifier.weight(1f)) { Narrator.stop() }
                    Chip(Narrator.rateLabel(ns.rate), Modifier.weight(1f)) { Narrator.cycleRate() }
                }
                ns.error?.let { Text(it, color = Warn, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(8.dp))
                Text(
                    annotated, fontSize = 14.sp, lineHeight = 22.sp, onTextLayout = { layout = it },
                    modifier = Modifier.heightIn(max = 300.dp).verticalScroll(scroll),
                )
            }
        },
        containerColor = DeepGreen, titleContentColor = LightGold, textContentColor = Color.White,
    )
}
