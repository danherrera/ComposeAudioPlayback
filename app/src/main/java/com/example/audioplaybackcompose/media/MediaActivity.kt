package com.example.audioplaybackcompose.media

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
import com.example.audioplaybackcompose.Dispatch
import com.example.audioplaybackcompose.Middleware
import com.example.audioplaybackcompose.theme.AudioPlaybackComposeTheme

class MediaActivity : AppCompatActivity() {

  private lateinit var mediaBrowser: MediaBrowserCompat
  private lateinit var mediaController: MediaControllerCompat
  private lateinit var dispatch: Dispatch<MediaAction>
  private val controllerCallback = object : MediaControllerCompat.Callback() {

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      state?.let {
        dispatch(MediaAction.PlaybackStateUpdated(it))
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
      override fun onConnected() {
        mediaController =
          MediaControllerCompat(this@MediaActivity, mediaBrowser.sessionToken)
        MediaControllerCompat.setMediaController(this@MediaActivity, mediaController)
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
              onClickPlayPause = { dispatch(MediaAction.ClickPlayPause) }
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
  private fun basicStateExample(): MediaState {
    val (state, setState) = state { MediaState() }

    val reducer: (MediaAction, MediaState) -> MediaState = { action, currentState ->
      when (action) {
        is MediaAction.PlaybackStateUpdated -> {
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

    val effect: (MediaAction, MediaState) -> Unit = { action, currentState ->
      when (action) {
        is MediaAction.PlaybackStateUpdated -> {
        }
        MediaAction.ClickPlayPause -> {
          if (currentState.isPlaying) {
            mediaController.transportControls.pause()
          } else {
            mediaController.transportControls.play()
          }
        }
      }
    }

    dispatch = {
      effect(it, state)
      setState(reducer(it, state))
    }

    return state
  }

  @Composable
  private fun basicExampleWithFunction(): MediaState {
    val (state, action) = simpleStateHelper<MediaState, MediaAction>(
      initialState = MediaState(),
      reducer = { action, currentState ->
        when (action) {
          is MediaAction.PlaybackStateUpdated -> {
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
          is MediaAction.PlaybackStateUpdated -> {
          }
          MediaAction.ClickPlayPause -> {
            if (currentState.isPlaying) {
              mediaController.transportControls.pause()
            } else {
              mediaController.transportControls.play()
            }
          }
        }
      })
    this.dispatch = action
    return state
  }

  @Composable
  private fun middlewareExample(): MediaState {
    val viewActionsMiddleware: Middleware<MediaState, MediaAction> = { state, action, next ->
      if (action == MediaAction.ClickPlayPause) {
        if (state.isPlaying) {
          mediaController.transportControls.pause()
        } else {
          mediaController.transportControls.play()
        }
      }
      next(action)
    }

    val (state, action) = middlewareStateHelper(
      initialState = MediaState(),
      reducer = { action, currentState ->
        when (action) {
          is MediaAction.PlaybackStateUpdated -> {
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
    this.dispatch = action
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

