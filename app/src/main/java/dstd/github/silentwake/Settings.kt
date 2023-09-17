package dstd.github.silentwake

import android.content.Context
import dstd.github.silentwake.utils.SimplePreferences

class Settings(context: Context): SimplePreferences(context) {
    var stateNotification: Boolean by BoolPreference(true)
    var combineLevels: Boolean by BoolPreference(false)
    var reduceWhenOff: Boolean by BoolPreference(true)
    var reduceWhenStopMusic: Boolean by BoolPreference(false)
    var reducedVolumeLevel: Int by IntPreference(0)

    var debug: Boolean by BoolPreference(false)
}

