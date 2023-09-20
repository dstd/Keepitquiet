package dstd.github.keepitquiet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

@Suppress("ClassName") // as an alias of `android.media`
object android_media {
    const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    const val VOLUME_CHANGED_EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    const val VOLUME_CHANGED_EXTRA_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    const val VOLUME_CHANGED_EXTRA_PREV_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
    const val VOLUME_CHANGED_EXTRA_SHOW_UI = "android.media.EXTRA_VOLUME_SHOW_UI"
}
class StateService : Service() {
    private val receiver = StateReceiver()
    private val settings = App.dependencies.settings
    private val queue = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        logd { "started" }
        IntentFilter(Intent.ACTION_SCREEN_OFF).also { registerReceiver(receiver, it) }
        IntentFilter(android_media.VOLUME_CHANGED_ACTION).also { registerReceiver(receiver, it) }
        listenAudioFocus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (settings.combineLevels)
            combineVolumeLevels()

        showForegroundNotice()
        return START_STICKY
    }

    private fun showForegroundNotice() {
        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), intentFlags)
        val notif = NotificationCompat.Builder(this, foregroundChannel)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(ONGOING_NOTIF_ID, notif)
    }

    private val foregroundChannel: String get() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return ""
        val channel = NotificationChannel(ONGOING_CHANNEL, getString(R.string.service_notification_text), NotificationManager.IMPORTANCE_NONE)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return ""
        nm.createNotificationChannel(channel)
        return channel.id
    }

    private fun listenAudioFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val context = App.context ?: return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.registerAudioPlaybackCallback(audioPlaybackListener, queue)
    }

    private fun unlistenAudioFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val context = App.context ?: return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.unregisterAudioPlaybackCallback(audioPlaybackListener)
    }

    private var hasMusic = false
    private val audioPlaybackListener = @RequiresApi(Build.VERSION_CODES.O) object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            logd { "playback changed - ${configs.map { it.audioAttributes.usage }}" }
            val isPlaying = configs.any { it.audioAttributes.usage == AudioAttributes.USAGE_MEDIA }
            if (settings.reduceWhenStopMusic && hasMusic && !isPlaying) {
                queue.removeCallbacks(reduceVolumeTask)
                queue.postDelayed(reduceVolumeTask, 10000)
            }
            if (isPlaying)
                queue.removeCallbacks(reduceVolumeTask)

            if (settings.highlightMutedMusic) {
                val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (am != null && !hasMusic && isPlaying && am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                    logd { "playback started with 0 volume" }
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                }
            }

            hasMusic = isPlaying
        }
    }

    private val reduceVolumeTask = Runnable {
        StateService.reduceVolume(source = "stop-playing")
    }

    override fun onDestroy() {
        logd { "destroyed" }
        super.onDestroy()

        unlistenAudioFocus()
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent): IBinder {
        throw Exception("Binding not supported")
    }

    companion object {
        private const val ONGOING_CHANNEL = "channel1"
        private const val ONGOING_NOTIF_ID = 1

        fun applyActiveState() {
            val context = App.context ?: return
            val settings = App.dependencies.settings
            val isActive = settings.reduceWhenOff || settings.reduceWhenStopMusic || settings.combineLevels
            logd { "apply active state - $isActive" }

            val intent = Intent(context, StateService::class.java)
            if (isActive)
                context.startService(intent) //ContextCompat.startForegroundService(context, intent)
            else
                context.stopService(intent)
        }

        fun reduceVolume(source: String) {
            val stream = AudioManager.STREAM_MUSIC
            val context = App.context ?: return
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = am.getStreamMaxVolume(stream)
            val current = am.getStreamVolume(stream)
            val new = App.dependencies.settings.reducedVolumeLevel
            if (new < current) {
                logd { "reduce due to $source, $current > $new of $max" }
                am.setStreamVolume(AudioManager.STREAM_MUSIC, new, 0)
            }
        }

        fun combineVolumeLevels() =
            combineVolumeLevels(AudioManager.STREAM_NOTIFICATION)

        fun combineVolumeLevels(sourceStream: Int) {
            val context = App.context ?: return
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

            val targetStream = when (sourceStream) {
                AudioManager.STREAM_NOTIFICATION -> AudioManager.STREAM_RING
                AudioManager.STREAM_RING -> AudioManager.STREAM_NOTIFICATION
                else -> return logd { "volume ignored: $sourceStream" }
            }
            val sourceLevel = am.getStreamVolume(sourceStream)
            am.setStreamVolume(targetStream, sourceLevel, 0)

            logd { "volume changed: $targetStream=$sourceLevel, from $sourceStream" }
        }
    }
}

class StateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
            android_media.VOLUME_CHANGED_ACTION -> onVolumeChange(intent)
        }
    }

    private fun onScreenOff() {
        if (!settings.reduceWhenOff)
            return

        val context = App.context ?: return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (am.isMusicActive)
            return logd { "wont reduce, music playing" }

        StateService.reduceVolume(source = "screen-off")
    }

    private fun onVolumeChange(intent: Intent) {
        if (!settings.combineLevels)
            return

        val stream = intent.getIntExtra(android_media.VOLUME_CHANGED_EXTRA_STREAM_TYPE, -1).takeIf { it >= 0 } ?: return
        val newValue = intent.getIntExtra(android_media.VOLUME_CHANGED_EXTRA_STREAM_VALUE, -1).takeIf { it >= 0 } ?: return
        val prevValue = intent.getIntExtra(android_media.VOLUME_CHANGED_EXTRA_PREV_STREAM_VALUE, -1).takeIf { it >= 0 } ?: return
        if (newValue == prevValue)
            return

        StateService.combineVolumeLevels(stream)
    }

    private val settings = App.dependencies.settings
}