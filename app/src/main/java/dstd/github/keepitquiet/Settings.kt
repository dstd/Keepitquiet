package dstd.github.keepitquiet

import android.content.Context
import dstd.github.keepitquiet.utils.SimplePreferences

class Settings(context: Context): SimplePreferences(context) {
    var combineLevels: Boolean by BoolPreference(false)
    var highlightMutedMusic: Boolean by BoolPreference(true)
    var reduceWhenOff: Boolean by BoolPreference(true)
    var reduceWhenStopMusic: Boolean by BoolPreference(false)
    var reducedVolumeLevel: Int by IntPreference(0)

    var debug: Boolean by BoolPreference(false)
}

