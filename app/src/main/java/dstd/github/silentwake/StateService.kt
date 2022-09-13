package dstd.github.silentwake

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class StateService : Service() {
    private val receiver = StateReceiver()

    override fun onCreate() {
        super.onCreate()

        logd { "#started" }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        configureForeground()
        return START_STICKY
    }

    private fun configureForeground() {
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
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return ""
        service.createNotificationChannel(channel)
        return channel.id
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent): IBinder {
        throw Exception("Binding not supported")
    }

    companion object {
        const val ONGOING_CHANNEL = "channel1"
        const val ONGOING_NOTIF_ID = 1

        fun applyActiveState() {
            val context = App.context ?: return
            val isActive = App.dependencies.settings.active
            logd { "#state start/stop - ${isActive}" }

            val intent = Intent(context, StateService::class.java)
            if (isActive)
                ContextCompat.startForegroundService(context, intent)
            else
                context.stopService(intent)
        }
    }
}

class StateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> adjustOnSleep()
        }
    }

    private fun adjustOnSleep() {
        logd { "#screen off" }
        val context = App.context ?: return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumePercents = App.dependencies.settings.reducedVolumeLevel
        am.setStreamVolume(AudioManager.STREAM_MUSIC, max * volumePercents / 100, 0)
    }
}