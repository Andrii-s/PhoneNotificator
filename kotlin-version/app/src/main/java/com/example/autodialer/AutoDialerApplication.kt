package com.example.autodialer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class AutoDialerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initTimber()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, plant a crash-reporting tree (e.g., Firebase Crashlytics)
            Timber.plant(ReleaseTree())
        }
        Timber.i("AutoDialerApplication initialised — version %s (%d)",
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    /** Silent production tree — only logs warnings and errors. */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < android.util.Log.WARN) return
            // TODO: forward to crash-reporting SDK here
            android.util.Log.println(priority, tag ?: "AutoDialer", message)
        }
    }
}
