package dstd.github.silentwake

import android.content.Context
import dstd.github.silentwake.utils.SimplePreferences

class Settings(context: Context): SimplePreferences(context) {
    var active: Boolean by BoolPreference(true)
    var reducedVolumeLevel: Int by IntPreference(0)
    var reduced: Int? by optionalIntPreference
}

