package com.example.audioplaybackcompose

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
import com.example.audioplaybackcompose.media.MediaActivity
import com.example.audioplaybackcompose.theme.AudioPlaybackComposeTheme

class MainActivity : AppCompatActivity() {

  private object Main : StateAction<MainState, MainAction>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val navMiddleware = Main.PreReducerMiddleware { _, action ->
        if (action == MainAction.NavigateToMedia) {
          startActivity(Intent(this, MediaActivity::class.java))
        }
      }

      val (state, dispatch) = redux(
        initialState = MainState(),
        middlewares = listOf(navMiddleware)
      )

      AudioPlaybackComposeTheme {
        Surface(color = MaterialTheme.colors.background) {
          Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { dispatch(MainAction.NavigateToMedia) }) {
              Text(text = "Media")
            }
          }
        }
      }
    }
  }
}

