package dstd.github.keepitquiet

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class Dependencies(context: Context) {
    val settings = Settings(context)
}

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        context = this
        dependencies = Dependencies(this)
        logger.enabled = dependencies.settings.debug
        StateService.applyActiveState()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
        lateinit var dependencies: Dependencies
    }
}