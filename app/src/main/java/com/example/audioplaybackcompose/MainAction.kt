package com.example.audioplaybackcompose

sealed class MainAction {
  object NavigateToMedia : MainAction()
}