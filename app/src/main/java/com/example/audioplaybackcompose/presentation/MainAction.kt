package com.example.audioplaybackcompose.presentation

sealed class MainAction {
  sealed class Navigate : MainAction() {
    object ToMedia : Navigate()
    object ToProfile : Navigate()
  }
}