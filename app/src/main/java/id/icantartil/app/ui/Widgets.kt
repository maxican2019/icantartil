package id.icantartil.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import id.icantartil.app.BuildConfig
import id.icantartil.app.data.Loop
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val Shape6 = RoundedCornerShape(6.dp)
val Shape8 = RoundedCornerShape(8.dp)

fun fmtTime(ms: Long): String = DateTimeFormatter.ofPattern("HH.mm").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
fun fmtDateTime(ms: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy HH.mm", Locale("id")).format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
fun fmtMs(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)
fun loopText(l: Loop): String =
    "ulang ${l.perAyah}× · rentang ${if (l.perRange <= 0) "∞" else "${l.perRange}×"}" + if (l.pauseSec > 0) " · jeda ${l.pauseSec} dtk" else ""

@Composable
fun PlayGlyph(c: Color, sz: Dp = 26.dp) = Canvas(Modifier.size(sz)) {
    val w = size.width
    drawPath(Path().apply { moveTo(w * .28f, w * .16f); lineTo(w * .84f, w * .5f); lineTo(w * .28f, w * .84f); close() }, c)
}

@Composable
fun PauseGlyph(c: Color, sz: Dp = 26.dp) = Canvas(Modifier.size(sz)) {
    val w = size.width
    drawRect(c, Offset(w * .25f, w * .16f), Size(w * .17f, w * .68f))
    drawRect(c, Offset(w * .58f, w * .16f), Size(w * .17f, w * .68f))
}

@Composable
fun SkipGlyph(c: Color, forward: Boolean, sz: Dp = 28.dp) = Canvas(Modifier.size(sz)) {
    val w = size.width
    val p = Path().apply {
        if (forward) { moveTo(w * .22f, w * .2f); lineTo(w * .66f, w * .5f); lineTo(w * .22f, w * .8f) }
        else { moveTo(w * .78f, w * .2f); lineTo(w * .34f, w * .5f); lineTo(w * .78f, w * .8f) }
        close()
    }
    drawPath(p, c)
    drawRect(c, Offset(if (forward) w * .72f else w * .2f, w * .2f), Size(w * .08f, w * .6f))
}

@Composable
fun NumBadge(text: String, on: Boolean = false, size: Dp = 36.dp) {
    val bd = if (on) DeepGreen else Gold
    Box(
        Modifier.size(size).clip(Shape6).background(if (on) Gold else Green).border(2.dp, bd, Shape6),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (on) DeepGreen else LightGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
}

@Composable
fun ListRow(
    badge: String, title: String, sub: String, bg: Color = Gold, on: Boolean = false,
    onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Shape6).background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumBadge(badge, on)
        Column(Modifier.weight(1f)) {
            Text(title, color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, color = Ink, fontSize = 12.sp)
        }
        trailing()
    }
}

@Composable
fun PlaySquare(playing: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(Shape6).background(DeepGreen).clickable(onClick = onClick)
            .semantics { contentDescription = if (playing) "Jeda" else "Putar ulang" },
        contentAlignment = Alignment.Center,
    ) { if (playing) PauseGlyph(LightGold, 22.dp) else PlayGlyph(LightGold, 22.dp) }
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Button(onClick, modifier.heightIn(min = 52.dp), shape = Shape6,
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepGreen)) {
        Text(text, fontWeight = FontWeight.Bold)
    }

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), shape = Shape6, border = BorderStroke(2.dp, Gold),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightGold)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    OutlinedButton(onClick, modifier.heightIn(min = 44.dp), shape = Shape6, border = BorderStroke(1.5.dp, Gold),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightGold)) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }

@Composable
fun Stepper(label: String, value: String, modifier: Modifier = Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(
        modifier.clip(Shape6).background(DeepGreen).border(1.dp, Gold, Shape6).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Mist, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp).clickable(onClick = onMinus).semantics { contentDescription = "Kurangi $label" }, contentAlignment = Alignment.Center) {
                Text("−", color = LightGold, fontSize = 26.sp)
            }
            Text(value, color = LightGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.size(44.dp).clickable(onClick = onPlus).semantics { contentDescription = "Tambah $label" }, contentAlignment = Alignment.Center) {
                Text("+", color = LightGold, fontSize = 26.sp)
            }
        }
    }
}

@Composable
fun TopBar(title: String, sub: String? = null, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 14.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = LightGold) }
        Column {
            if (sub != null) Text(sub, color = Mist, fontSize = 12.sp)
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoopChips(vm: AppVm, after: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Tiap ayat ${vm.loop.perAyah}×", Modifier.weight(1f)) { vm.cyclePerAyah(); after() }
        Chip("Rentang ${if (vm.loop.perRange <= 0) "∞" else "${vm.loop.perRange}×"}", Modifier.weight(1f)) { vm.cycleRange(); after() }
        Chip("Jeda ${vm.loop.pauseSec} dtk", Modifier.weight(1f)) { vm.cyclePause(); after() }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) LightGold else Mist, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Menu bawah: tombol Play di tengah memutar ulang rentang terakhir, lalu berubah jadi Pause. */
@Composable
fun NavBar(cur: Screen, playing: Boolean, go: (Screen) -> Unit, onPlay: () -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Gold))
        Row(Modifier.fillMaxWidth().height(64.dp).background(DeepGreen).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem("Surat", cur == Screen.Home, Modifier.weight(1f)) { go(Screen.Home) }
            NavItem("Rentang", cur == Screen.Range, Modifier.weight(1f)) { go(Screen.Range) }
            Box(
                Modifier.padding(horizontal = 6.dp).offset(y = (-18).dp).size(60.dp)
                    .border(4.dp, Green, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Gold)
                    .clickable(onClick = onPlay)
                    .semantics { contentDescription = if (playing) "Jeda" else "Putar rentang terakhir" },
                contentAlignment = Alignment.Center,
            ) { if (playing) PauseGlyph(DeepGreen) else PlayGlyph(DeepGreen) }
            NavItem("Riwayat", cur == Screen.History, Modifier.weight(1f)) { go(Screen.History) }
            NavItem("Unduhan", cur == Screen.Storage, Modifier.weight(1f)) { go(Screen.Storage) }
        }
    }
}

@Composable
fun BannerAd() {
    Box(Modifier.fillMaxWidth().background(Color.White), contentAlignment = Alignment.Center) {
        AndroidView(factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        })
    }
}

@Composable
fun BusyOverlay(msg: String) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Gold)
            Spacer(Modifier.height(12.dp))
            Text(msg, color = Color.White)
        }
    }
}
