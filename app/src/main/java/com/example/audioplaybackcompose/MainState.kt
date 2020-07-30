package com.example.audioplaybackcompose

data class MainState(
    val isPlaying: Boolean = false,
    val progress: Float = 0f
)