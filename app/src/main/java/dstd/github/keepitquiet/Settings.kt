package dstd.github.keepitquiet

import android.content.Context
import android.media.AudioManager
import dstd.github.keepitquiet.utils.SimplePreferences

class Settings(context: Context): SimplePreferences(context) {
    var combineLevels: Boolean by BoolPreference(false)
    var highlightMutedMusic: Boolean by BoolPreference(true)
    var reduceWhenOff: Boolean by BoolPreference(true)
    var musicStopLevel: Int by IntPreference(0)
    var reduceWhenStopMusic: Boolean by BoolPreference(false)
    var screenOffLevel: Int by IntPreference(0)

    var debug: Boolean by BoolPreference(false)
}

fun volumePercent(value: Int): Int {
    val am = App.context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return if (max > 0) 100 * value / max else 0
}