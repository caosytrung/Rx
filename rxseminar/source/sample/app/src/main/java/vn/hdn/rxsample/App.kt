package vn.hdn.rxsample

import android.app.Application
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        RxJavaPlugins.setErrorHandler { Timber.e(it) }
    }
}