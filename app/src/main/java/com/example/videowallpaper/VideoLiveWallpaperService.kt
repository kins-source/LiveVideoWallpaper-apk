package com.example.videowallpaper

import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

/**
 * Renders a looping video as a live wallpaper.
 * The video file path is read from SharedPreferences, written there
 * by MainActivity after the user picks a video.
 */
class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startPlayback(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            mediaPlayer?.let {
                if (visible) it.start() else it.pause()
            }
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
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setSurface(holder.surface)
                isLooping = true
                setVolume(0f, 0f) // wallpapers should be silent
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
        }

        private fun releasePlayer() {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
        }
    }
}
