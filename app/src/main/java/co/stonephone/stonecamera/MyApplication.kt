package co.stonephone.stonecamera

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle

class MyApplication : Application() {
    companion object {
        private lateinit var instance: MyApplication
        private var currentActivity: Activity? = null

        fun getAppContext(): Context {
            return instance.applicationContext
        }

        fun getAppActivity(): Activity? {
            return currentActivity
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Registering the ActivityLifecycleCallbacks to track the current Activity
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }
        })
    }
}
