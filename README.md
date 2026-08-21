# Video Live Wallpaper (Android)

Lets a user pick any video from their device and set it as an animated
(live) wallpaper. Built with Kotlin — not Python, since Android's
`WallpaperService` API has no Python/cross-platform equivalent.

## How it works

1. `MainActivity` opens the system file picker (`ACTION_OPEN_DOCUMENT`)
   filtered to videos.
2. The chosen video is copied into the app's private storage
   (`filesDir/wallpaper_video.mp4`) and its path saved to
   `SharedPreferences`.
3. The app launches Android's built-in live wallpaper picker, pointed
   directly at `VideoLiveWallpaperService`.
4. Once the user confirms, `VideoLiveWallpaperService` uses `MediaPlayer`
   to loop the video, muted, directly onto the wallpaper surface.

## Building

1. Open this folder in **Android Studio** (Giraffe or newer).
2. Let Gradle sync — you'll need the standard `settings.gradle` and root
   `build.gradle` that Android Studio generates for a new project (only
   the `app` module's files are included here).
3. Run on a device or emulator (API 24+).
4. Tap "Choose Video" → pick a video → confirm in the wallpaper picker.

## Known limitations / things to harden before shipping

- **Battery drain**: video wallpapers are power-hungry. Consider pausing
  playback aggressively (already done on `onVisibilityChanged`) and
  warning users.
- **No trimming/compression**: large videos will use significant
  storage since they're copied in full. Consider using FFmpeg
  (e.g. via `mobile-ffmpeg`) to transcode/compress on import.
- **Android only**: iOS does not support video live wallpapers at all —
  only static Live Photos on the lock screen, set manually by the user
  in Settings. There's no App Store-legal way to automate that.
- **Permissions**: `READ_MEDIA_VIDEO` targets Android 13+; for wider
  compatibility add a fallback to `READ_EXTERNAL_STORAGE` for older
  versions.
- Not tested/compiled here — no Android SDK in this environment. Verify
  the build in Android Studio before relying on it.
