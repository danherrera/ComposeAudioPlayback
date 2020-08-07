package com.example.audioplaybackcompose.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.unit.dp
import com.example.audioplaybackcompose.R
import com.example.audioplaybackcompose.StateAction
import com.example.audioplaybackcompose.presentation.media.MediaActivity
import com.example.audioplaybackcompose.presentation.profile.ProfileActivity
import com.example.audioplaybackcompose.presentation.theme.AudioPlaybackComposeTheme
import com.example.audioplaybackcompose.redux

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

