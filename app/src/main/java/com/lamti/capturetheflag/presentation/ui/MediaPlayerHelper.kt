package com.lamti.capturetheflag.presentation.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import timber.log.Timber
import java.io.IOException

fun Context.playSound(sound: Uri, loop: Boolean = false) {
    this@playSound.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    MediaPlayer.create(this, sound)
        ?.apply {
            setupPlayer(
                context = this@playSound,
                mediaPlayer = this,
                audioManager = this@playSound.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
                sound = sound,
                loop = loop
            )
        }
}

private fun setupPlayer(
    context: Context,
    mediaPlayer: MediaPlayer,
    audioManager: AudioManager,
    sound: Uri,
    loop: Boolean
) = with(mediaPlayer) {
    try {
        if (audioManager.isMusicActive) {
            if (isPlaying) {
                stop()
                release()
            }
        }
        try {
            Timber.e("MP: ds: $sound")
            setDataSource(context, sound)
            prepare()
        } catch (e: Exception) {
            Timber.e("MP: ${e.message}")
        }
        try {
            this.isLooping = loop
            setOnSeekCompleteListener {
                stop()
                release()
            }
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        } catch (e: Exception) {
            Timber.e("MP: looping: ${e.message}")
        }
        try {
            start()
        } catch (e: Exception) {
            Timber.e("MP: start: ${e.message}")
        }
    } catch (e: IOException) {
        release()
    }
}
