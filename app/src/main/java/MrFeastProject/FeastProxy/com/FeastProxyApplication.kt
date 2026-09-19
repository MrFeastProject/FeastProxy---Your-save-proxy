package MrFeastProject.FeastProxy.com

import android.app.Application
import android.util.Log

class FeastProxyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global crash guard to handle system developer options toggles (e.g. Sensors Off)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)

            val msg = throwable.message?.lowercase().orEmpty()
            val cls = throwable.javaClass.name
            val isSensorOrFrameworkNoise = msg.contains("sensor") ||
                    cls.contains("Sensor") ||
                    (throwable is SecurityException && msg.contains("permission")) ||
                    msg.contains("deadobject")

            if (isSensorOrFrameworkNoise) {
                Log.w(TAG, "Gracefully suppressed framework/sensor exception to keep FeastProxy running.")
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        private const val TAG = "FeastProxyApp"
    }
}
