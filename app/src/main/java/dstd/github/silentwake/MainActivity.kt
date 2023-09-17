package dstd.github.silentwake

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import dstd.github.silentwake.databinding.ActivityMainBinding

class MainActivity: Activity() {
    private val settings = App.dependencies.settings

    private lateinit var views: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        views.reduceWhenOff.apply {
            isChecked = settings.reduceWhenOff
            setOnClickListener { applyReduceState() }
        }
        views.reduceWhenStopMusic.apply {
            isChecked = settings.reduceWhenStopMusic
            setOnClickListener { applyReduceState() }
        }

        views.volumeLevel.apply {
            setOnSeekBarChangeListener(seekBarListener)
            max = 100
        }

        views.takeCurrent.setOnClickListener { takeCurrentVolume() }

        views.combineVolume.apply {
            isChecked = settings.combineLevels
            setOnClickListener { applyCombineState() }
        }

        views.debug.apply {
            fun updateState() = if (isChecked)
                setText(R.string.enable_debug)
            else
                text = null

            isChecked = settings.debug
            setOnClickListener {
                isChecked.also { settings.debug = it }.also { logger.enabled = it }
                updateState()
            }
            updateState()
        }

        applyVolumeLevel()
        StateService.applyActiveState()
    }

    private fun applyReduceState() {
        settings.reduceWhenOff = views.reduceWhenOff.isChecked
        settings.reduceWhenStopMusic = views.reduceWhenStopMusic.isChecked
        StateService.applyActiveState()
    }

    private fun applyCombineState() {
        val combine = views.combineVolume.isChecked
        if (combine && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (!nm.isNotificationPolicyAccessGranted) {
                views.combineVolume.isChecked = false
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                return
            }
        }
        settings.combineLevels = combine
        StateService.applyActiveState()
    }

    private fun takeCurrentVolume() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).takeIf { it != 0 } ?: return
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumePercents = current * 100 / max

        settings.reducedVolumeLevel = volumePercents
        applyVolumeLevel()
    }

    private fun applyVolumeLevel() {
        views.volumeLevel.apply {
            progress = settings.reducedVolumeLevel
            seekBarListener.onProgressChanged(this, progress, false)
        }
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            views.volumePercent.text = getString(R.string.volume_level_percent, progress)
            if (fromUser)
                settings.reducedVolumeLevel = seekBar.progress
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
    }
}
