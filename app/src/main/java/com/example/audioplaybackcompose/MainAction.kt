package com.example.audioplaybackcompose

import android.support.v4.media.session.PlaybackStateCompat

sealed class MainAction {
    data class PlaybackStateUpdated(val playbackState: PlaybackStateCompat) : MainAction()
    object ClickPlayPause : MainAction()
}