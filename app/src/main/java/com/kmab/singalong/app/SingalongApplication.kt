package com.kmab.singalong.app

import android.app.Application

class SingalongApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashDiagnostics.install(this)
    }
    val container: AppContainer by lazy { AppContainer(this) }
}
