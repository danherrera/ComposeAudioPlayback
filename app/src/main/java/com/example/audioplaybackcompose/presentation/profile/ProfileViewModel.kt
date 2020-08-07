package com.example.audioplaybackcompose.presentation.profile

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class ProfileViewModel @ViewModelInject constructor(
  @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

}