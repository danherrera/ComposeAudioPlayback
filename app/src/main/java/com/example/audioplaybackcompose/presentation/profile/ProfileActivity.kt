package com.example.audioplaybackcompose.presentation.profile

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.audioplaybackcompose.R
import com.example.audioplaybackcompose.StateAction
import com.example.audioplaybackcompose.presentation.MainAction
import com.example.audioplaybackcompose.presentation.theme.AudioPlaybackComposeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

  object Profile : StateAction<ProfileState, ProfileAction>
  private val viewModel: ProfileViewModel by viewModels()

  private val reducer = Profile.Reducer { state, action ->
    when (action) {
      is ProfileAction.FirstNameTextChanged -> state.copy(firstName = action.firstName)
      is ProfileAction.LastNameTextChanged -> state.copy(lastName = action.lastName)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val (state, dispatch) = Profile.Redux(
        initialState = ProfileState(),
        reducer = reducer
      )

      AudioPlaybackComposeTheme {
        Surface(color = MaterialTheme.colors.background) {
          Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
              state.firstName,
              onValueChange = { dispatch(ProfileAction.FirstNameTextChanged(it)) },
              label = { Text("First Name") }
            )
            OutlinedTextField(
              state.lastName,
              onValueChange = { dispatch(ProfileAction.LastNameTextChanged(it)) },
              label = { Text("Last Name") }
            )
          }
        }
      }
    }
  }
}