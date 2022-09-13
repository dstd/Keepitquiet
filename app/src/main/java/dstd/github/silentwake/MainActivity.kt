package dstd.github.silentwake

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.widget.SeekBar
import dstd.github.silentwake.databinding.ActivityMainBinding

class MainActivity: Activity() {
    private val settings = App.dependencies.settings

    private lateinit var views: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        views.volumeTitle.apply {
            isChecked = settings.active
            setOnClickListener { applyActiveState(views.volumeTitle.isChecked) }
        }

        views.volumeLevel.apply {
            setOnSeekBarChangeListener(seekBarListener)
            max = 100
        }

        views.takeCurrent.setOnClickListener { takeCurrentVolume() }

        applyVolumeLevel()
    }

    private fun applyActiveState(active: Boolean) {
        settings.active = active
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
