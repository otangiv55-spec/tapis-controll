package com.example.tapiscontrol

import android.app.Application
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Musica di sottofondo da file audio del telefono. Ha un suo lettore, separato da quello del video, e non
 * chiede il "focus audio": si mescola al video e agli avvisi invece di metterli in pausa.
 */
class MusicController(app: Application, private val store: WorkoutStore) {
    private val exo: ExoPlayer = ExoPlayer.Builder(app).build()

    val playlist = MutableStateFlow(store.loadMusic())     // indirizzi (uri) dei brani scelti
    val playing = MutableStateFlow(false)
    val volume = MutableStateFlow(store.loadMusicVolume())
    val current = MutableStateFlow("")                      // "2/5 · titolo"

    init {
        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            false,
        )
        exo.repeatMode = Player.REPEAT_MODE_ALL
        exo.volume = volume.value
        exo.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playing.value = playWhenReady
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateCurrent()

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = updateCurrent()
        })
        load(playlist.value)
    }

    private fun load(list: List<String>) {
        if (list.isEmpty()) {
            exo.clearMediaItems()
        } else {
            exo.setMediaItems(list.map { MediaItem.fromUri(Uri.parse(it)) })
            exo.prepare()
        }
        updateCurrent()
    }

    private fun updateCurrent() {
        val n = exo.mediaItemCount
        if (n == 0) {
            current.value = ""
            return
        }
        val idx = exo.currentMediaItemIndex + 1
        val title = exo.mediaMetadata.title?.toString()
        current.value = "$idx/$n · " + (title ?: "brano $idx")
    }

    /** Sostituisce l'elenco dei brani e parte subito. */
    fun setPlaylist(uris: List<String>) {
        playlist.value = uris
        store.saveMusic(uris)
        load(uris)
        if (uris.isNotEmpty()) exo.play()
    }

    fun clear() {
        exo.pause()
        setPlaylist(emptyList())
    }

    fun toggle() {
        if (exo.mediaItemCount == 0) return
        if (exo.playWhenReady) exo.pause() else exo.play()
    }

    fun next() {
        if (exo.mediaItemCount > 0) exo.seekToNextMediaItem()
    }

    fun previous() {
        if (exo.mediaItemCount > 0) exo.seekToPreviousMediaItem()
    }

    fun setVolume(v: Float) {
        val c = v.coerceIn(0f, 1f)
        volume.value = c
        exo.volume = c
        store.saveMusicVolume(c)
    }

    fun release() {
        exo.release()
    }
}
