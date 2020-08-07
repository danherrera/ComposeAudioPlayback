package com.example.audioplaybackcompose.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import com.example.audioplaybackcompose.R
import com.example.audioplaybackcompose.StateAction
import com.example.audioplaybackcompose.presentation.media.MediaActivity
import com.example.audioplaybackcompose.presentation.profile.ProfileActivity
import com.example.audioplaybackcompose.presentation.theme.AudioPlaybackComposeTheme
import com.example.audioplaybackcompose.redux
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private object Main : StateAction<MainState, MainAction>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val navMiddleware = Main.PreReducerMiddleware { _, action ->
        if (action is MainAction.Navigate) {
          val nextActivityClass = when (action) {
            MainAction.Navigate.ToMedia -> MediaActivity::class.java
            MainAction.Navigate.ToProfile -> ProfileActivity::class.java
          }
          startActivity(Intent(this, nextActivityClass))
        }
      }

      val (state, dispatch) = redux(
        initialState = MainState(),
        middlewares = listOf(navMiddleware)
      )

      AudioPlaybackComposeTheme {
        Surface(color = MaterialTheme.colors.background) {
          Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { dispatch(MainAction.Navigate.ToMedia) }) {
              Text(text = getString(R.string.activity_main_menu_media))
            }
            Button(onClick = { dispatch(MainAction.Navigate.ToProfile) }) {
              Text(text = getString(R.string.activity_main_menu_profile))
            }
          }
        }
      }
    }
  }
}

