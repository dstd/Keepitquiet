package dstd.github.keepitquiet

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import dstd.github.keepitquiet.databinding.FragmentVolumeBinding

class VolumeLevelDialog: DialogFragment() {
    interface Delegate {
        fun onVolumeChanged(dialog: VolumeLevelDialog, value: Int)
    }

    private lateinit var views: FragmentVolumeBinding
    private var initialLevel = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
        .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
        .setPositiveButton(R.string.action_ok) { dialog, _ ->
            dialog.dismiss()
            (activity as? Delegate)?.onVolumeChanged(this, views.volumeLevel.progress)
        }
        .setOnDismissListener { activity?.finish() }
        .also {
            views = FragmentVolumeBinding.inflate(activity!!.layoutInflater)
            it.setView(views.root)
            configureView()
        }
        .create()

    private fun configureView() {
        val am = App.context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        views.volumeLevel.apply {
            setOnSeekBarChangeListener(seekBarListener)
            progress = initialLevel
            max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
            onVolumeChanged(fromUser = false)
        }

        views.takeCurrent.setOnClickListener { takeCurrentVolume() }
    }

    fun withVolumeLevel(value: Int): VolumeLevelDialog {
        initialLevel = value
        return this
    }

    private fun takeCurrentVolume() {
        val am = App.context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val value = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        views.volumeLevel.progress = value

        onVolumeChanged(fromUser = true)
    }

    private fun onVolumeChanged(fromUser: Boolean) {
        val seekBar = views.volumeLevel
        val value = seekBar.progress
        val percent = if (seekBar.max > 0) value * 100 / seekBar.max else 0
        views.volumePercent.text = getString(R.string.volume_level_percent, percent)
//        if (fromUser)
//            (activity as? Delegate)?.onVolumeChanged(this, value)
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) =
            onVolumeChanged(fromUser)
        override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
    }
}