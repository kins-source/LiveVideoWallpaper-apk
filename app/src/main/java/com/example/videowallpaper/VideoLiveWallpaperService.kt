package com.example.videowallpaper

import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
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

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startPlayback(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
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
            player = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurface(holder.surface)
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f // wallpapers should be silent
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
