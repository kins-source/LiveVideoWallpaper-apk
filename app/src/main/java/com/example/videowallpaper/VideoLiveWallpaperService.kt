package com.example.videowallpaper

import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Renders a looping video as a live wallpaper using ExoPlayer, which
 * supports a much wider range of containers/codecs than the plain
 * MediaPlayer API (mp4, mkv, webm, ts, 3gp, and more, depending on the
 * device's decoders).
 *
 * The video file path is read from SharedPreferences, written there
 * by MainActivity after the user picks a video.
 */
class VideoLiveWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "VideoLiveWallpaper"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private var wantsToPlay = false

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startPlayback(holder)
        }

        // Called once the surface actually has real dimensions. Binding
        // the video surface again here fixes the "frozen first frame"
        // issue: if the player only ever attaches to the surface handed
        // to onSurfaceCreated (which can have stale/zero dimensions),
        // the compositor can stop receiving new frames after that.
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            player?.setVideoSurface(holder.surface)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            wantsToPlay = visible
            player?.playWhenReady = visible
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            releasePlayer()
        }

        private fun startPlayback(holder: SurfaceHolder) {
            val prefs = applicationContext.getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val path = prefs.getString("video_path", null) ?: return
            val file = File(path)
            if (!file.exists()) return

            releasePlayer()
            wantsToPlay = true
            player = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurface(holder.surface)
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f // wallpapers should be silent

                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error, retrying once", error)
                        // One automatic retry: some devices deliver the
                        // surface slightly before the decoder is ready.
                        seekTo(0)
                        prepare()
                        playWhenReady = wantsToPlay
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            playWhenReady = wantsToPlay
                        }
                    }
                })

                prepare()
                playWhenReady = true
            }
        }

        private fun releasePlayer() {
            player?.release()
            player = null
        }
    }
}
