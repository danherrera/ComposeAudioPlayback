package com.example.audioplaybackcompose

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.material.Button
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import com.example.audioplaybackcompose.ui.AudioPlaybackComposeTheme

class MainActivity : AppCompatActivity() {

  private lateinit var mediaBrowser: MediaBrowserCompat
  private lateinit var mediaController: MediaControllerCompat
  private lateinit var action: (MainAction) -> Unit
  private val controllerCallback = object : MediaControllerCompat.Callback() {

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      state?.let {
        action(MainAction.PlaybackStateUpdated(it))
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
      override fun onConnected() {
        mediaController =
          MediaControllerCompat(this@MainActivity, mediaBrowser.sessionToken)
        MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
        mediaController.registerCallback(controllerCallback)
      }
    }

    mediaBrowser = MediaBrowserCompat(
      this,
      ComponentName(this, MediaPlaybackService::class.java),
      connectionCallback,
      null
    )

    setContent {
      // 1. Exploration
      val state = basicStateExample()

      // 2. Enhancement - helper function
//            val state = basicExampleWithFunction()

      // 3. Middleware function
//            val state = middlewareExample()

      AudioPlaybackComposeTheme {
        Surface(color = MaterialTheme.colors.background) {
          Column(modifier = Modifier.padding(16.dp)) {
            Greeting("Android")

            Progress(progress = state.progress)

            TransportControls(
              transport = Transport(
                isPlaying = state.isPlaying
              ),
              onClickPlayPause = { action(MainAction.ClickPlayPause) }
            )
          }
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    mediaBrowser.connect()
  }

  override fun onResume() {
    super.onResume()
    volumeControlStream = AudioManager.STREAM_MUSIC
  }

  override fun onStop() {
    super.onStop()
    MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
    mediaBrowser.disconnect()
  }

  @Composable
  private fun basicStateExample(): MainState {
    val (state, setState) = state { MainState() }

    val reducer: (MainAction, MainState) -> MainState = { action, currentState ->
      when (action) {
        is MainAction.PlaybackStateUpdated -> {
          currentState.copy(
            isPlaying = action.playbackState.state == PlaybackStateCompat.STATE_PLAYING,
            progress = action.playbackState.extras
              ?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
              ?.let { duration ->
                action.playbackState.position.toFloat() / duration
              }
              ?: 0f
          )
        }
        else -> currentState
      }
    }

    val effect: (MainAction, MainState) -> Unit = { action, currentState ->
      when (action) {
        is MainAction.PlaybackStateUpdated -> {
        }
        MainAction.ClickPlayPause -> {
          if (currentState.isPlaying) {
            mediaController.transportControls.pause()
          } else {
            mediaController.transportControls.play()
          }
        }
      }
    }

    action = {
      effect(it, state)
      setState(reducer(it, state))
    }

    return state
  }

  @Composable
  private fun basicExampleWithFunction(): MainState {
    val (state, action) = simpleStateHelper<MainState, MainAction>(
      initialState = MainState(),
      reducer = { action, currentState ->
        when (action) {
          is MainAction.PlaybackStateUpdated -> {
            currentState.copy(
              isPlaying = action.playbackState.state == PlaybackStateCompat.STATE_PLAYING,
              progress = action.playbackState.extras
                ?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?.let { duration ->
                  action.playbackState.position.toFloat() / duration
                }
                ?: 0f
            )
          }
          else -> currentState
        }
      },
      effect = { action, currentState ->
        when (action) {
          is MainAction.PlaybackStateUpdated -> {
          }
          MainAction.ClickPlayPause -> {
            if (currentState.isPlaying) {
              mediaController.transportControls.pause()
            } else {
              mediaController.transportControls.play()
            }
          }
        }
      })
    this.action = action
    return state
  }

  @Composable
  private fun middlewareExample(): MainState {
    val viewActionsMiddleware: Middleware<MainState, MainAction> = { state, action, next ->
      if (action == MainAction.ClickPlayPause) {
        if (state.isPlaying) {
          mediaController.transportControls.pause()
        } else {
          mediaController.transportControls.play()
        }
      }
      next(action)
    }

    val (state, action) = middlewareStateHelper(
      initialState = MainState(),
      reducer = { action, currentState ->
        when (action) {
          is MainAction.PlaybackStateUpdated -> {
            currentState.copy(
              isPlaying = action.playbackState.state == PlaybackStateCompat.STATE_PLAYING,
              progress = action.playbackState.extras
                ?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?.let { duration ->
                  action.playbackState.position.toFloat() / duration
                }
                ?: 0f
            )
          }
          else -> currentState
        }
      },
      middlewares = listOf(viewActionsMiddleware)
    )
    this.action = action
    return state
  }
}

@Composable
fun <S, A> simpleStateHelper(
  initialState: S,
  reducer: (A, S) -> S,
  effect: (A, S) -> Unit
): Pair<S, (A) -> Unit> {
  val (state, setState) = state { initialState }

  return state to { action ->
    effect(action, state)
    setState(reducer(action, state))
  }
}

typealias Middleware<S, A> = (state: S, action: A, next: (A) -> S) -> S

@Composable
fun <S, A> middlewareStateHelper(
  initialState: S,
  reducer: (A, S) -> S,
  middlewares: List<Middleware<S, A>>
): Pair<S, (A) -> Unit> {
  val (state, setState) = state { initialState }

  val reducedMiddleware = middlewares.reduce { acc, middleware ->
    { state, action, next ->
      middleware(state, action) { nextAction ->
        acc(state, nextAction) { accAction ->
          next(accAction)
        }
      }
    }
  }

  return state to { action ->
    reducedMiddleware(state, action) { middlewareAction ->
      reducer(middlewareAction, state).also(setState)
    }
  }
}

@Composable
fun Greeting(name: String) {
  Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  AudioPlaybackComposeTheme {
    Greeting("Android")
  }
}

@Composable
fun Progress(progress: Float) {
  LinearProgressIndicator(progress = progress)
}

data class Transport(
  val isPlaying: Boolean = false
)

@Composable
fun TransportControls(
  transport: Transport,
  onClickPlayPause: () -> Unit
) {
  Row {
    Button(onClick = onClickPlayPause) {
      Text(text = if (transport.isPlaying) "Pause" else "Play")
    }
  }
}

@Preview
@Composable
fun TransportPlayPreview() {
  AudioPlaybackComposeTheme {
    TransportControls(transport = Transport(), onClickPlayPause = {})
  }
}

@Preview
@Composable
fun TransportPausePreview() {
  AudioPlaybackComposeTheme {
    TransportControls(transport = Transport(isPlaying = true), onClickPlayPause = {})
  }
}

