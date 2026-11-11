package com.kopyamatik.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kopyamatik.app.MainActivity
import java.io.File

class AudioPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val localBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
        fun getPlayer(): ExoPlayer? = exoPlayer
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // Pauses automatically when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)    // Holds CPU WakeLock during playback
            .build()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        this.exoPlayer = player
        this.mediaSession = session
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Support both MediaSessionService intent and local binding
        if (intent?.action == "androidx.media3.session.MediaSessionService") {
            return super.onBind(intent)
        }
        return localBinder
    }

    /**
     * Loads an audio file and starts playing.
     */
    fun playFile(file: File, title: String = "Soru Çözümleri") {
        exoPlayer?.let { player ->
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist("Kopyamatik AI")
                .setDisplayTitle(title)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(file.toURI().toString())
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
    }

    /**
     * Changes the playback speed dynamically (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
     */
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun seekForward(offsetMs: Long = 10000L) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + offsetMs).coerceAtMost(player.duration)
            player.seekTo(target)
        }
    }

    fun seekBackward(offsetMs: Long = 10000L) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - offsetMs).coerceAtLeast(0L)
            player.seekTo(target)
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
