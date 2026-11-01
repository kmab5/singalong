package com.kmab.singalong.app

import android.app.Application

class SingalongApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
