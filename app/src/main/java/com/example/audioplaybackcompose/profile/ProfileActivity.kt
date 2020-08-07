package com.example.audioplaybackcompose.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.ui.core.setContent
import com.example.audioplaybackcompose.StateAction

class ProfileActivity : AppCompatActivity() {

  object Profile : StateAction<ProfileState, ProfileAction>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val (state, dispatch) = Profile.Redux(initialState = ProfileState())
    }
  }
}