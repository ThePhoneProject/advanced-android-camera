package co.stonephone.stonecamera

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: MyApplication
    }
}
