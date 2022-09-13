package dstd.github.silentwake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        logd { "onReceive ${intent.action}" }
        if (intent.action == Intent.ACTION_BOOT_COMPLETED)
            StateService.applyActiveState()
    }
}