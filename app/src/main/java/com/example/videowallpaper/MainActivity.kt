package com.example.videowallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView

    /**
     * 20 supported video formats. Playback compatibility ultimately
     * depends on the device's hardware/software decoders, but ExoPlayer
     * (used in VideoLiveWallpaperService) can demux all of these
     * containers, which covers the vast majority of real-world files.
     */
    private val supportedMimeTypes = arrayOf(
        "video/mp4",          // .mp4, .m4v
        "video/3gpp",         // .3gp
        "video/3gpp2",        // .3g2
        "video/x-matroska",   // .mkv
        "video/webm",         // .webm
        "video/quicktime",    // .mov
        "video/x-msvideo",    // .avi
        "video/x-flv",        // .flv
        "video/mp2ts",        // .ts, .m2ts
        "video/mpeg",         // .mpeg, .mpg
        "video/ogg",          // .ogv
        "video/x-ms-wmv",     // .wmv
        "video/x-ms-asf",     // .asf
        "video/divx",         // .divx
        "video/x-flc",        // .flc
        "video/x-flic",       // .fli
        "video/vnd.dlna.mpeg-tts", // .ts (DLNA)
        "video/h264",         // raw .h264/.264
        "video/hevc",         // raw .hevc/.265
        "video/*"             // fallback catch-all for anything else the device recognizes
    )

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) copyVideoAndLaunchPicker(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val pickButton = findViewById<MaterialButton>(R.id.pickVideoButton)

        pickButton.setOnClickListener {
            pickVideo.launch(supportedMimeTypes)
        }
    }

    private fun copyVideoAndLaunchPicker(uri: Uri) {
        try {
            val extension = queryFileExtension(uri) ?: "mp4"
            val destFile = File(filesDir, "wallpaper_video.$extension")
            // remove any previously imported video (possibly different extension)
            filesDir.listFiles { f -> f.name.startsWith("wallpaper_video.") }
                ?.forEach { it.delete() }

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

    /**
     * Determines a file extension for the picked video so ExoPlayer's
     * extractor can correctly detect the container format. Tries the
     * document's display name first, then falls back to its MIME type.
     */
    private fun queryFileExtension(uri: Uri): String? {
        // Try the display name (e.g. "myvideo.mkv") first.
        var displayName: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) displayName = it.getString(nameIndex)
            }
        }
        displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }?.let {
            return it.lowercase()
        }

        // Fall back to resolving the extension from the MIME type.
        val mimeType = contentResolver.getType(uri) ?: return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }
}
