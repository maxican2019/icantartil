package id.icantartil.app.audio

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import id.icantartil.app.data.Ayah
import id.icantartil.app.data.Loop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class PlayState(
    val active: Boolean = false, val playing: Boolean = false,
    val chapter: Int = 0, val ayah: Int = 0, val rep: Int = 1, val perAyah: Int = 1,
    val from: Int = 0, val to: Int = 0, val error: String? = null,
)

/** Pemutar per-ayat dengan cache LRU 200 MB, loop per ayat dan per rentang, serta jeda antar ayat. */
@OptIn(UnstableApi::class)
object AudioEngine {
    const val LIMIT = 200L * 1024 * 1024
    private class Meta(val ayah: Int, val rep: Int)

    private lateinit var cache: SimpleCache
    private lateinit var cacheFactory: CacheDataSource.Factory
    lateinit var player: ExoPlayer
        private set

    private var meta: List<Meta> = emptyList()
    private var chapter = 0
    private var from = 0
    private var to = 0
    private var perAyah = 1
    private var pauseMs = 0L
    private var waiting = false
    private val main = Handler(Looper.getMainLooper())
    private val resume = Runnable {
        waiting = false
        if (player.hasNextMediaItem() || player.repeatMode == Player.REPEAT_MODE_ALL) player.play()
        publish()
    }
    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state.asStateFlow()

    fun init(app: Application) {
        cache = SimpleCache(File(app.cacheDir, "audio"), LeastRecentlyUsedCacheEvictor(LIMIT), StandaloneDatabaseProvider(app))
        cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        player = ExoPlayer.Builder(app)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true
        )
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                main.removeCallbacks(resume)
                waiting = false
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && pauseMs > 0) {
                    waiting = true
                    main.postDelayed(resume, pauseMs)
                }
                publish()
            }
            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(playing = false, error = "Audio gagal diputar (${error.errorCodeName})")
            }
        })
    }

    private fun publish() {
        val m = meta.getOrNull(player.currentMediaItemIndex)
        _state.value = PlayState(
            active = meta.isNotEmpty(), playing = player.playWhenReady || waiting,
            chapter = chapter, ayah = m?.ayah ?: 0, rep = m?.rep ?: 1, perAyah = perAyah, from = from, to = to,
        )
    }

    fun play(chapterId: Int, list: List<Ayah>, loop: Loop) {
        if (list.isEmpty()) return
        chapter = chapterId; from = list.first().number; to = list.last().number
        perAyah = loop.perAyah.coerceAtLeast(1); pauseMs = loop.pauseSec * 1000L
        val pass = ArrayList<Pair<Ayah, Int>>()
        for (a in list) for (r in 1..perAyah) pass += a to r
        val rounds = if (loop.perRange <= 0) 1 else minOf(loop.perRange, 600 / pass.size.coerceAtLeast(1)).coerceAtLeast(1)
        val items = ArrayList<MediaItem>(); val ms = ArrayList<Meta>()
        repeat(rounds) { for ((a, r) in pass) { items += MediaItem.fromUri(a.audioUrl); ms += Meta(a.number, r) } }
        meta = ms
        main.removeCallbacks(resume); waiting = false
        player.stop(); player.clearMediaItems()
        player.repeatMode = if (loop.perRange <= 0) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        player.pauseAtEndOfMediaItems = pauseMs > 0
        player.setMediaItems(items); player.prepare(); player.playWhenReady = true
        publish()
    }

    fun toggle() {
        if (player.playWhenReady) { player.pause(); return }
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0L)
        player.play()
    }

    /** Loncat ke ayat berikutnya (delta = 1) atau sebelumnya (delta = -1), melewati pengulangan. */
    fun skipAyah(delta: Int) {
        if (meta.isEmpty()) return
        val i = player.currentMediaItemIndex
        val starts = meta.indices.filter { it == 0 || meta[it].rep == 1 }
        val pos = starts.indexOfLast { it <= i }.coerceAtLeast(0)
        val target = starts.getOrNull(pos + delta) ?: if (delta > 0) starts.first() else starts.last()
        player.seekTo(target, 0L)
    }

    /** Unduh ayat ke cache sebelum diputar agar loop mulus dan bisa offline. */
    suspend fun prefetch(urls: List<String>, onProgress: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        urls.forEachIndexed { i, u ->
            CacheWriter(cacheFactory.createDataSource(), DataSpec(Uri.parse(u)), null, null).cache()
            onProgress(i + 1)
        }
    }

    fun cacheBytes(): Long = cache.cacheSpace

    fun clearCache() {
        for (k in HashSet(cache.keys)) cache.removeResource(k)
    }
}
