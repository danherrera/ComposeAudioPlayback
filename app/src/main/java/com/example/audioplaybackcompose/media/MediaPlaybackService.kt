package com.example.audioplaybackcompose.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.example.audioplaybackcompose.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit

class MediaPlaybackService : MediaBrowserServiceCompat() {

  companion object {
    private const val MEDIA_ROOT_ID = "media_root_id"
    private const val NOTIFICATION_CHANNEL_ID = "notification_channel_id"
    private const val MEDIA_SERVICE_ID = 1
  }

  private class BecomingNoisyReceiver(
    private val pause: () -> Unit
  ) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
        pause()
      }
    }
  }

  private val delayedStopRunnable = Runnable {
    mediaSession.controller.transportControls.stop()
  }

  private val handler = Handler()
  private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
  private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_GAIN -> {
        mediaSession.controller.transportControls.play()
      }
      AudioManager.AUDIOFOCUS_LOSS -> {
        mediaSession.controller.transportControls.pause()
        handler.postDelayed(delayedStopRunnable, TimeUnit.SECONDS.toMillis(30))
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        mediaSession.controller.transportControls.pause()
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        mediaSession.controller.transportControls.pause()
      }
    }
  }
  private lateinit var audioFocusRequest: AudioFocusRequest
  private lateinit var mediaSession: MediaSessionCompat
  private lateinit var mediaPlayer: MediaPlayer
  private val becomingNoisyReceiver =
    BecomingNoisyReceiver { mediaSession.controller.transportControls.pause() }
  private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

    private var progressJob: Job? = null

    private fun createMediaNotification(
      context: Context,
      mediaSession: MediaSessionCompat
    ): Notification {
      val controller = mediaSession.controller
      val mediaMetaData = controller.metadata
      val description = mediaMetaData.description
      return NotificationCompat.Builder(context,
        NOTIFICATION_CHANNEL_ID
      ).apply {
        setChannelId(NOTIFICATION_CHANNEL_ID)
        setContentTitle(description.title)
        setContentText(description.subtitle)
        setSubText(description.description)
        setLargeIcon(description.iconBitmap)

        setContentIntent(controller.sessionActivity)

        setDeleteIntent(
          MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_STOP
          )
        )

        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        setSmallIcon(android.R.drawable.ic_media_play)
        color = Color.YELLOW

        addAction(
          NotificationCompat.Action(
            android.R.drawable.ic_media_pause,
            context.getString(R.string.pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
              context,
              PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
          )
        )

        setStyle(
          androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0)
            .setShowCancelButton(true)
            .setCancelButtonIntent(
              MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_STOP
              )
            )
        )
      }.build()
    }

    private fun updatePlaybackState(withNewState: Int = mediaSession.controller.playbackState.state) {
      mediaSession.setPlaybackState(
        PlaybackStateCompat.Builder()
          .setState(
            withNewState,
            mediaPlayer.currentPosition.toLong(),
            mediaPlayer.playbackParams.speed
          )
          .setExtras(Bundle().apply {
            putLong(
              MediaMetadataCompat.METADATA_KEY_DURATION,
              mediaPlayer.duration.toLong()
            )
          })
          .build()
      )
    }


    override fun onPlay() {
      val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
      audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
        setOnAudioFocusChangeListener(audioFocusChangeListener)
        setAudioAttributes(AudioAttributes.Builder().run {
          setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          build()
        })
        build()
      }
      val result = audioManager.requestAudioFocus(audioFocusRequest)
      if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        startService(Intent(this@MediaPlaybackService, MediaPlaybackService::class.java))
        mediaSession.isActive = true
        mediaPlayer.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        registerReceiver(becomingNoisyReceiver, noisyIntentFilter)
        this@MediaPlaybackService.startForeground(
          MEDIA_SERVICE_ID,
          createMediaNotification(baseContext, mediaSession)
        )

        GlobalScope.launch {
          progressJob = launch(Dispatchers.IO) {
            while (true) {
              delay(1000L)
              updatePlaybackState()
            }
          }
        }
      }
    }

    override fun onPause() {
      progressJob?.cancel()
      mediaPlayer.pause()
      updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
      unregisterReceiver(becomingNoisyReceiver)
      this@MediaPlaybackService.stopForeground(false)
    }

    override fun onStop() {
      progressJob?.cancel()
      val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
      audioManager.abandonAudioFocusRequest(audioFocusRequest)
      unregisterReceiver(becomingNoisyReceiver)
      this@MediaPlaybackService.stopSelf()
      mediaSession.isActive = false
      mediaPlayer.stop()
      updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
      this@MediaPlaybackService.stopForeground(false)
    }
  }

  private val mediaPlayerPreparedListener = MediaPlayer.OnPreparedListener {
    mediaSession.controller.transportControls.play()
  }


  override fun onCreate() {
    super.onCreate()

    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
      .createNotificationChannel(
        NotificationChannel(
          NOTIFICATION_CHANNEL_ID,
          "App Media",
          NotificationManager.IMPORTANCE_LOW
        )
      )

    mediaPlayer = MediaPlayer().apply {
      setWakeMode(this@MediaPlaybackService, PowerManager.PARTIAL_WAKE_LOCK)
      setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      )
      setOnPreparedListener(mediaPlayerPreparedListener)
    }

    mediaSession = MediaSessionCompat(
      this,
      MediaPlaybackService::class.java.simpleName
    ).apply {
      setPlaybackState(
        PlaybackStateCompat.Builder()
          .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SEEK_TO
                    or PlaybackStateCompat.ACTION_PAUSE
          ).build()
      )

      setCallback(mediaSessionCallback)

      setMetadata(
        MediaMetadataCompat.Builder()
          .build()
      )

      setSessionToken(sessionToken)
    }
  }

  override fun onLoadChildren(
    parentId: String,
    result: Result<MutableList<MediaBrowserCompat.MediaItem>>
  ) {
    result.sendResult(mutableListOf())
  }

  override fun onGetRoot(
    clientPackageName: String,
    clientUid: Int,
    rootHints: Bundle?
  ): BrowserRoot? {
    try {
      mediaPlayer.setDataSource(
        "https://play.podtrac.com/npr-500005/edge1.pod.npr.org/anon.npr-mp3/npr/newscasts/2020/07/27/newscast120740.mp3?awCollectionId=500005&awEpisodeId=895736125&orgId=1&d=300&p=500005&story=895736125&t=podcast&e=895736125&size=4500000&ft=pod&f=500005"
      )
      mediaPlayer.prepareAsync()
    } catch (ioException: IOException) {

    } catch (illegalStateException: IllegalStateException) {

    }
    return BrowserRoot(MEDIA_ROOT_ID, null)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mediaSession, intent)
    return super.onStartCommand(intent, flags, startId)
  }
}