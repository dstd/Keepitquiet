package dstd.github.keepitquiet

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import dstd.github.keepitquiet.databinding.ActivityMainBinding

class MainActivity: FragmentActivity(), VolumeLevelDialog.Delegate {
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

        views.reduceWhenOffLevel.apply {
            setOnClickListener { chooseLevel(TAG_TURN_OFF) }
        }

        views.reduceWhenStopMusicLevel.apply {
            setOnClickListener { chooseLevel(TAG_MUSIC_STOP) }
        }

        views.combineVolume.apply {
            isChecked = settings.combineLevels
            setOnClickListener { applyCombineState() }
        }

        views.highlightMutedMusic.apply {
            isChecked = settings.highlightMutedMusic
            setOnClickListener { applyMutedHightlight() }
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

        updateLevelValues()
        StateService.applyActiveState()
    }

    private fun applyReduceState() {
        settings.reduceWhenOff = views.reduceWhenOff.isChecked
        settings.reduceWhenStopMusic = views.reduceWhenStopMusic.isChecked
        StateService.applyActiveState()
    }

    private fun applyCombineState(checkPermissions: Boolean = true) {
        val combine = views.combineVolume.isChecked
        if (checkPermissions && combine && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (!nm.isNotificationPolicyAccessGranted) {
                AlertDialog.Builder(this)
                    .setMessage(resources.getString(R.string.dnd_permission_hint, resources.getString(R.string.app_name)))
                    .setPositiveButton(R.string.action_settings) { d, _ ->
                        views.combineVolume.isChecked = false
                        d.dismiss()
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                    .setNegativeButton(R.string.action_ignore) { d, _ ->
                        d.dismiss()
                        applyCombineState(checkPermissions = false)
                    }
                    .show()
                return
            }
        }
        settings.combineLevels = combine
        StateService.applyActiveState()
    }

    private fun applyMutedHightlight() {
        settings.highlightMutedMusic = views.highlightMutedMusic.isChecked
    }

    private fun chooseLevel(tag: String) {
        val initialLevel = when (tag) {
            TAG_TURN_OFF -> settings.screenOffLevel
            TAG_MUSIC_STOP -> settings.musicStopLevel
            else -> return
        }
        VolumeLevelDialog().withVolumeLevel(initialLevel).show(supportFragmentManager, tag)
    }

    private fun updateLevelValues() {
        views.reduceWhenOffLevel.text = getString(R.string.volume_level_percent, volumePercent(settings.screenOffLevel))
        views.reduceWhenStopMusicLevel.text = getString(R.string.volume_level_percent, volumePercent(settings.musicStopLevel))
    }

    override fun onVolumeChanged(dialog: VolumeLevelDialog, value: Int) {
        when (dialog.tag) {
            TAG_TURN_OFF ->
                settings.screenOffLevel = value
            TAG_MUSIC_STOP ->
                settings.musicStopLevel = value
            else ->
                return
        }

        updateLevelValues()
    }

    companion object {
        const val TAG_TURN_OFF = "tag1"
        const val TAG_MUSIC_STOP = "tag2"
    }
}
