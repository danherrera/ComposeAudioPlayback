package com.example.audioplaybackcompose

sealed class MainAction {
  sealed class Navigate : MainAction() {
    object ToMedia : Navigate()
    object ToProfile : Navigate()
  }
}