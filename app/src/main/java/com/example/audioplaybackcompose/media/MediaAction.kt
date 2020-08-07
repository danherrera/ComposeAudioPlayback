package com.example.audioplaybackcompose.media

import android.support.v4.media.session.PlaybackStateCompat

sealed class MediaAction {
  data class PlaybackStateUpdated(val playbackState: PlaybackStateCompat) : MediaAction()
  object ClickPlayPause : MediaAction()
}