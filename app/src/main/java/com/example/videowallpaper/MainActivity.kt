package com.example.videowallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) copyVideoAndLaunchPicker(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val pickButton = findViewById<Button>(R.id.pickVideoButton)

        pickButton.setOnClickListener {
            pickVideo.launch(arrayOf("video/*"))
        }
    }

    private fun copyVideoAndLaunchPicker(uri: Uri) {
        try {
            val destFile = File(filesDir, "wallpaper_video.mp4")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                .edit()
                .putString("video_path", destFile.absolutePath)
                .apply()

            statusText.text = "Video saved. Opening wallpaper picker..."
            launchLiveWallpaperPicker()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchLiveWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, VideoLiveWallpaperService::class.java)
        )
        startActivity(intent)
    }
}
